import com.rs2.ServerSettings;
import com.rs2.model.World;
import com.rs2.model.gameplay.barrows.BarrowsManager;
import com.rs2.model.gameplay.barrows.BarrowsTunnelEncounter;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Exhaustive 128-roll check of the historical Barrows door monster table,
 * undefeated-brother selection, and chest/door duplication prevention.
 */
public final class BarrowsTunnelEncounterChecks {
    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        int[] allBrothers = BarrowsTunnelEncounter.eligibleBrothers(
                new boolean[6], new boolean[6]);
        require(allBrothers.length == 6, "All six undefeated brothers should be eligible");
        require(Arrays.equals(allBrothers, new int[] {2030, 2029, 2028, 2027, 2026, 2025}),
                "Brother order no longer agrees with BarrowsManager");

        int brothers = 0, skeletons = 0, bloodworms = 0, rats = 0;
        for (int roll = 0; roll < BarrowsTunnelEncounter.ROLL_COUNT; roll++) {
            int id = BarrowsTunnelEncounter.npcForRoll(roll, roll % 2, 0, allBrothers);
            if (BarrowsTunnelEncounter.brotherIndexForNpcId(id) >= 0) {
                brothers++;
            } else if (id == 2036 || id == 2037) {
                skeletons++;
            } else if (id == 2031) {
                bloodworms++;
            } else if (id == 2032) {
                rats++;
            } else {
                throw new AssertionError("Non-historical random door NPC selected: " + id);
            }
        }
        require(brothers == 12 && skeletons == 52 && bloodworms == 32 && rats == 32,
                "Historical weighted door monster table changed: "
                        + brothers + ", " + skeletons + ", " + bloodworms + ", " + rats);
        require(BarrowsTunnelEncounter.npcForRoll(12, 0, 0, allBrothers) == 2036,
                "Skeleton A selection incorrect");
        require(BarrowsTunnelEncounter.npcForRoll(12, 1, 0, allBrothers) == 2037,
                "Skeleton B selection incorrect");
        for (int i = 0; i < allBrothers.length; i++) {
            require(BarrowsTunnelEncounter.npcForRoll(0, 0, i, allBrothers)
                    == 2030 - i, "Brother choice " + i + " incorrectly mapped");
        }

        boolean[] killed = {true, false, true, true, false, true};
        boolean[] active = {false, false, false, false, true, false};
        int[] eligible = BarrowsTunnelEncounter.eligibleBrothers(killed, active);
        require(Arrays.equals(eligible, new int[] {2029}),
                "Only the unbeaten, unsummoned brother should remain eligible");
        require(BarrowsTunnelEncounter.npcForRoll(0, 0, 0, eligible) == 2029,
                "Ambush roll chose an ineligible brother");
        require(BarrowsTunnelEncounter.eligibleBrothers(
                new boolean[6], new boolean[] {true, true, true, true, true, true}).length == 0,
                "Already-active brothers must not be duplicated");

        int[] none = BarrowsTunnelEncounter.eligibleBrothers(
                new boolean[] {true, true, true, true, true, true}, new boolean[6]);
        for (int roll = 0; roll < 12; roll++) {
            int id = BarrowsTunnelEncounter.npcForRoll(roll, roll % 2, 0, none);
            require(id == 2036 || id == 2037, "No-brother fallback must summon a skeleton");
        }
        verifyActiveBrotherDetection();

        System.out.println("Barrows weighted door rolls, undefeated brothers, and duplicate-prevention checks passed.");
    }

    private static void verifyActiveBrotherDetection() throws Exception {
        Player owner = new Player(null);
        owner.isBot = true;
        owner.setIndex(19);
        World.getPlayers()[19] = owner;
        Npc brother = new Npc(2030);
        brother.setOwnerPlayerIndex(19);
        World.getNpcs()[42] = brother;
        try {
            Method method = BarrowsManager.class.getDeclaredMethod("activeTunnelBrothers", Player.class);
            method.setAccessible(true);
            boolean[] mask = (boolean[]) method.invoke(null, owner);
            require(mask[0] && !mask[1] && !mask[2] && !mask[3] && !mask[4] && !mask[5],
                    "Live owned brother missing from duplicate-prevention scan");

            boolean[] others = (boolean[]) method.invoke(null, new Player(null));
            for (boolean value : others) require(!value, "Other player's brother leaked into pool");
            int[] remaining = BarrowsTunnelEncounter.eligibleBrothers(new boolean[6], mask);
            require(remaining.length == 5 && remaining[0] == 2029,
                    "Already active brother offered for another ambush");

            Method spawnBrother = BarrowsManager.class.getDeclaredMethod(
                    "spawnBarrowsBrother", Player.class, int.class);
            spawnBrother.setAccessible(true);
            require((Boolean) spawnBrother.invoke(null, owner, 2030),
                    "Existing brother should count as already spawned at chest");
            require(World.getNpcs()[42] == brother,
                    "Chest lookup unexpectedly replaced a live tunnel brother");
            brother.setActive(false);
            mask = (boolean[]) method.invoke(null, owner);
            require(!mask[0], "Inactive/dead brothers should not block future summons");
        } finally {
            World.getNpcs()[42] = null;
            World.getPlayers()[19] = null;
        }
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
