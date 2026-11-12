import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.combat.AttackValidationResult;
import com.rs2.model.combat.CombatCycleEvent;
import com.rs2.model.dialogue.BarrowsNpcDialogues;
import com.rs2.model.dialogue.DialogueManager;
import com.rs2.model.gameplay.barrows.BarrowsManager;
import com.rs2.model.item.DegradableEquipmentHandler;
import com.rs2.model.item.ItemDefinition;
import com.rs2.model.npc.ExtraNpcSpawns;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.objects.ObjectDefinition;
import com.rs2.model.objects.ObjectManager;
import com.rs2.model.player.Player;
import com.rs2.model.quest.QuestDefinition;
import com.rs2.util.path.WalkingCollisionMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/** True two-player Barrows regression using separately registered player
 * indexes and the native 443 NPC and item-spawn tables.
 */
public final class Barrows2006RestorationChecks {
    private static final int OPEN_BIT = 1 << 16;

    public static void main(String[] args) throws Exception {
        boolean previousF2P = ServerSettings.freeToPlayWorld;
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        ItemDefinition.loadDefinitions();
        QuestDefinition.loadDefinitions();
        ObjectDefinition.loadRevision443();
        WalkingCollisionMap.loadCollisionMaps();
        try {
            checkFirstEverRun();
            checkTwoPlayerOwnershipAndKillCredit();
            checkTwoPlayerChestState();
            checkReconnectRoundTrip();
            checkMembershipGate();
            checkCombatWearStopsAfterLeaving();
            checkNative2006NpcAndSpawnData();
            System.out.println("Barrows restoration: fresh-run randomisation, independent "
                    + "two-player combat/credit/chest, members entry, post-combat wear, "
                    + "original NPCs and native spade removal passed.");
        } finally {
            ServerSettings.freeToPlayWorld = previousF2P;
        }
    }

    private static Player player(int index, int x, int y, int plane) {
        Player p = new Player(null);
        p.isBot = true; // Fixtures bypass the unrelated account membership database.
        p.setIndex(index);
        p.setEncodedIndex(32768 + index);
        p.setPosition(new Position(x, y, plane));
        p.refreshLocalViewArea();
        return p;
    }

    private static void checkFirstEverRun() {
        boolean[] seen = new boolean[6];
        for (int i = 0; i < 180; i++) {
            Player p = new Player(null);
            int index = p.getBarrowsTargetBrotherIndex();
            require(index >= 0 && index < 6, "First-ever hidden brother out of range");
            seen[index] = true;
        }
        for (int i = 0; i < seen.length; i++) {
            require(seen[i], "Fresh characters never receive hidden brother " + i);
        }
        Player p = player(5, 3552, 9694, 0);
        p.setBarrowsTargetBrotherIndex(4);
        require(p.getBarrowsTargetBrotherIndex() == 4,
                "A loaded unfinished run must retain its saved hidden brother");
    }

    private static void checkTwoPlayerOwnershipAndKillCredit() {
        ServerSettings.freeToPlayWorld = false;
        Player alice = player(1, 3552, 9694, 0);
        Player bob = player(2, 3553, 9694, 0);
        Player oldAlice = World.getPlayers()[1], oldBob = World.getPlayers()[2];
        World.getPlayers()[1] = alice;
        World.getPlayers()[2] = bob;
        try {
            Npc brother = new Npc(2030);
            brother.setPosition(new Position(3554, 9694, 0));
            brother.setActive(true);
            brother.setOwnerPlayerIndex(1);
            require(BarrowsManager.canFightBarrowsNpc(alice, brother),
                    "Owner cannot attack their summoned brother");
            require(!BarrowsManager.canFightBarrowsNpc(bob, brother),
                    "Another player can attack an owned brother");
            require(!BarrowsManager.canFightBarrowsNpc(brother, bob),
                    "A summoned brother may target a non-owner");
            require(BarrowsManager.canFightBarrowsNpc(brother, alice),
                    "Summoned brother may not attack its owner");
            require(CombatCycleEvent.validateAttack(bob, brother)
                            == AttackValidationResult.INVALID_TARGET,
                    "Combat validation ignored Barrows encounter ownership");

            BarrowsManager.recordNpcKill(bob, brother);
            require(bob.getBarrowsKillCount() == 0 && bob.barrowsRewardPotential == 0,
                    "Other player received a stolen Barrows kill");
            BarrowsManager.recordNpcKill(alice, brother);
            require(alice.isBarrowsBrotherKilled(0)
                            && alice.getBarrowsKillCount() == 1,
                    "Owner did not receive their Barrows brother credit");
            require(bob.getBarrowsKillCount() == 0,
                    "Other player's Barrows kill count was modified");

            Npc orphan = new Npc(2030);
            orphan.setPosition(new Position(3554, 9694, 0));
            require(!BarrowsManager.canFightBarrowsNpc(bob, orphan)
                            || !orphan.hasAssignedOwnerPlayer(),
                    "Static or unowned Barrows monster classification changed");
            BarrowsManager.recordNpcKill(bob, orphan);
            require(bob.getBarrowsKillCount() == 0,
                    "Unowned/unspawned brother granted boss kill credit");

            // The thirty-two fixed room monsters remain public.
            Npc skeleton = new Npc(2036);
            skeleton.setActive(true);
            skeleton.setPosition(new Position(3554, 9694, 0));
            require(BarrowsManager.canFightBarrowsNpc(bob, skeleton),
                    "Original static crypt monsters became owner-only");

            Npc doorSkeleton = new Npc(2036);
            doorSkeleton.setOwnerPlayerIndex(1);
            doorSkeleton.setActive(true);
            doorSkeleton.setPosition(new Position(3554, 9694, 0));
            require(!BarrowsManager.canFightBarrowsNpc(bob, doorSkeleton),
                    "Private door summons remain stealable");
        } finally {
            World.getPlayers()[1] = oldAlice;
            World.getPlayers()[2] = oldBob;
        }
    }

    private static void checkTwoPlayerChestState() {
        ServerSettings.freeToPlayWorld = false;
        Player alice = player(1, 3552, 9694, 0);
        Player bob = player(2, 3552, 9695, 0);
        alice.setBarrowsTargetBrotherIndex(0);
        bob.setBarrowsTargetBrotherIndex(1);
        alice.setBarrowsBrotherKilled(0, true);
        bob.setBarrowsBrotherKilled(1, true);
        int globalObjectsBefore = countDynamicBarrowsChests();
        require(BarrowsManager.handleFirstObjectAction(alice, 6774),
                "Player A cannot interact with the original Barrows chest");
        require((alice.configStates[453] & OPEN_BIT) != 0,
                "Chest did not open for the interacting player");
        require(bob.configStates[453] == 0,
                "Opening another player's chest changed this player's state");
        require(countDynamicBarrowsChests() == globalObjectsBefore,
                "Opening a private chest changed global world objects");
        require(BarrowsManager.handleFirstObjectAction(bob, 6774),
                "Player B could not independently open their own chest");
        require((bob.configStates[453] & OPEN_BIT) != 0,
                "Player B's chest was not independently accessible");
        require(countDynamicBarrowsChests() == globalObjectsBefore,
                "Two private chest opens created a shared dynamic chest");
    }

    /** Actual Player -> JSON character save -> new Player reload, not a
     * fabricated copy of the fields. It verifies pre-loot and post-loot states.
     */
    private static void checkReconnectRoundTrip() throws Exception {
        boolean sqlite = ServerSettings.sqlitePlayerSaveEnabled;
        ServerSettings.sqlitePlayerSaveEnabled = false;
        String username = "BarrQa" + Long.toHexString(System.nanoTime() & 0xffff);
        java.io.File dir = new java.io.File("data/characters");
        java.io.File save = new java.io.File(dir, username + ".json");
        java.io.File backup = new java.io.File(dir, username + ".json.bak");
        java.io.File dat = new java.io.File(dir, username + ".dat");
        java.io.File staging = new java.io.File(dir,
                ".json-staging/" + username + ".dat");
        require(!save.exists() && !dat.exists(), "Refusing to replace an existing test character");
        try {
            Player original = new Player(null);
            original.setUsername(username);
            original.setPassword("barrows-test");
            original.setPosition(new Position(3552, 9694, 0));
            original.setBarrowsTargetBrotherIndex(4);
            original.setBarrowsBrotherKilled(0, true);
            original.setBarrowsBrotherKilled(2, true);
            original.setBarrowsKillCount(4);
            original.barrowsRewardPotential = 483;
            original.barrowsDoorPuzzleSolved = true;
            original.configStates[452] = 1 << 17 | 1 << 8 | 1 << 6;
            original.barrowsChestOpened = false;

            require(com.rs2.util.CharacterFileManager.savePlayer(original),
                    "Failed to serialize a pre-loot Barrows character");
            Player resumed = new Player(null);
            resumed.setUsername(username);
            com.rs2.util.CharacterFileManager.loadPlayerFromFile(
                    "./data/characters/", resumed);
            require(resumed.getBarrowsTargetBrotherIndex() == 4,
                    "Login randomised an unfinished run's hidden brother");
            require(resumed.isBarrowsBrotherKilled(0)
                    && resumed.isBarrowsBrotherKilled(2)
                    && resumed.getBarrowsKillCount() == 4
                    && resumed.barrowsRewardPotential == 483,
                    "Pre-loot kills/reward potential lost in real character reload");
            require(resumed.barrowsDoorPuzzleSolved
                    && resumed.configStates[452] == original.configStates[452],
                    "Tunnel puzzle configuration lost on reconnect");
            require(!resumed.barrowsChestOpened,
                    "Pre-loot reconnect started chest collapse prematurely");

            original.barrowsChestOpened = true;
            original.configStates[453] = 1 << 16;
            require(com.rs2.util.CharacterFileManager.savePlayer(original),
                    "Failed to save the looted collapsing Barrows state");
            Player looted = new Player(null);
            looted.setUsername(username);
            com.rs2.util.CharacterFileManager.loadPlayerFromFile(
                    "./data/characters/", looted);
            require(looted.barrowsChestOpened,
                    "Looted chest state was lost across a real character save");
            BarrowsManager.restoreChestConfigOnLogin(looted);
            require((looted.configStates[453] & OPEN_BIT) != 0,
                    "443 Barrows chest did not restore private open morph before InitialVarps");
            require(looted.getBarrowsKillCount() == 4
                    && looted.getBarrowsTargetBrotherIndex() == 4,
                    "Looted reconnect unexpectedly rerolled the ongoing run");
        } finally {
            Files.deleteIfExists(save.toPath());
            Files.deleteIfExists(backup.toPath());
            Files.deleteIfExists(dat.toPath());
            Files.deleteIfExists(staging.toPath());
            ServerSettings.sqlitePlayerSaveEnabled = sqlite;
        }
    }

    private static int countDynamicBarrowsChests() {
        int count = 0;
        for (int x = 3546; x <= 3557; x++)
            for (int y = 9689; y <= 9700; y++) {
                com.rs2.model.objects.DynamicObject obj =
                        ObjectManager.findDynamicObjectAt(x, y, 0);
                if (obj != null && (obj.getWorldObject().getObjectId() == 6774
                        || obj.getWorldObject().getObjectId() == 6775)) count++;
            }
        return count;
    }

    private static void checkMembershipGate() {
        Player p = player(3, 3557, 3297, 0);
        ServerSettings.freeToPlayWorld = true;
        require(BarrowsManager.digIntoCrypt(p), "F2P Barrows Dig was not intercepted");
        require(p.getPosition().getY() < 9000, "F2P player entered a members crypt");
        require(BarrowsManager.handleFirstObjectAction(p, 6772),
                "A forbidden crypt interaction was not consumed");
        require(p.getBarrowsKillCount() == 0, "F2P player gained Barrows progress");
        ServerSettings.freeToPlayWorld = false;
        require(BarrowsManager.digIntoCrypt(p), "Members-world Dig did not work");
        require(p.getPosition().getPlane() == 3,
                "Eligible member was not admitted to their Barrows crypt");
    }

    private static void checkCombatWearStopsAfterLeaving() {
        Player p = player(4, 3552, 9694, 0);
        Npc attacker = new Npc(2030);
        attacker.setActive(true);
        attacker.setPosition(new Position(3553, 9694, 0));
        p.setCombatTarget(null);
        p.getSingleCombatTimer().setTargetDelay(attacker, 8);
        require(DegradableEquipmentHandler.isEngagedInCombat(p),
                "Recent incoming attack failed to count as combat");
        p.setPosition(new Position(3552, 9694, 3));
        require(!DegradableEquipmentHandler.isEngagedInCombat(p),
                "Incoming combat timer wore Barrows gear after a ladder/plane change");
        p.setPosition(new Position(3552, 3294, 0));
        require(!DegradableEquipmentHandler.isEngagedInCombat(p),
                "Distant attacker wore gear after leaving underground");
        p.setCombatTarget(attacker);
        require(!DegradableEquipmentHandler.isEngagedInCombat(p),
                "Outgoing cross-region target kept wearing Barrows equipment");
    }

    private static void checkNative2006NpcAndSpawnData() throws Exception {
        require("Strange Old Man".equals(NpcDefinition.forId(2024).getName()),
                "Revision-443 Stranger Old Man is not NPC 2024");
        require("NPC #1671".equals(NpcDefinition.forId(1671).getName()),
                "Accidentally used an obsolete pre-443 NPC ID");
        require(BarrowsNpcDialogues.supports(2024)
                        && BarrowsNpcDialogues.supports(3797),
                "Original Barrows and repair Squire dialogue not registered");
        require(!BarrowsNpcDialogues.supports(1695),
                "Old crone cannot masquerade as Strange Old Man");
        String spawns = new String(Files.readAllBytes(Paths.get(
                "data/npcs/extra_spawns.csv")), "UTF-8");
        require(spawns.contains("2024,Strange Old Man,3566,3290,0,1"),
                "Strange Old Man 443 world spawn missing");
        byte[] data = Files.readAllBytes(Paths.get("data/world/Item spawn.dat"));
        int count = u16(data, 1);
        require(data.length == 3 + count * 10,
                "Binary item spawn table is damaged");
        int tome = 0, spades = 0;
        for (int i = 0; i < count; i++) {
            int off = 3 + i * 10;
            int id = u16(data, off) - count - i;
            int x = u16(data, off + 5), y = u16(data, off + 7);
            int plane = data[off + 9] & 255;
            if (id == 4707 && x == 3571 && y == 3312 && plane == 0) tome++;
            if (id == 952 && plane == 0
                    && ((x == 3571 && y == 3310)
                    || (x == 3572 && y == 3312))) spades++;
        }
        require(tome == 1, "Original 2005 crumbling tome table spawn was lost");
        require(spades == 0, "Post-2006 two-spade building spawn was not removed");
        ExtraNpcSpawns.load();
        boolean foundMan = false, foundSquire = false;
        for (Npc npc : World.getNpcs()) {
            if (npc == null) continue;
            if (npc.getNpcId() == 2024 && npc.getPosition().getX() == 3566
                    && npc.getPosition().getY() == 3290) foundMan = true;
            if (npc.getNpcId() == 3797 && npc.getPosition().getX() == 2652
                    && npc.getPosition().getY() == 2660) foundSquire = true;
        }
        require(foundMan && foundSquire, "2006 NPCs not actually registered by spawn loader");
        require(DialogueManager.startDialogue(player(6, 2652, 2660, 0), 3797),
                "Void Knight smith Squire's Talk-to action has no dialogue");
    }

    private static int u16(byte[] data, int off) {
        return (data[off] & 255) << 8 | (data[off + 1] & 255);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
