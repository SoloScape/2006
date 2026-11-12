import com.rs2.ServerSettings;
import com.rs2.model.World;
import com.rs2.model.Position;
import com.rs2.model.gameplay.barrows.BarrowsTunnelRoom;
import com.rs2.model.gameplay.barrows.BarrowsTunnelPopulation;
import com.rs2.model.gameplay.barrows.BarrowsManager;
import com.rs2.model.gameplay.barrows.BarrowsTunnelEncounter;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;
import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Exhaustive 128-roll check of the historical Barrows door monster table,
 * undefeated-brother selection, authentic 2006 tunnel-door duplicates,
 * and independent chest/sarcophagus duplicate guards.
 */
public final class BarrowsTunnelEncounterChecks {
    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        int[] allBrothers = BarrowsTunnelEncounter.eligibleBrothers(new boolean[6]);
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
        int[] eligible = BarrowsTunnelEncounter.eligibleBrothers(killed);
        require(Arrays.equals(eligible, new int[] {2029, 2026}),
                "All unbeaten brothers should be eligible, regardless of living copies");
        require(BarrowsTunnelEncounter.npcForRoll(0, 0, 0, eligible) == 2029
                        && BarrowsTunnelEncounter.npcForRoll(0, 0, 1, eligible) == 2026,
                "Ambush roll selected a defeated brother or excluded another unbeaten brother");

        // Deliberately reproduce the historical bug: even if all six brothers
        // already have living copies nearby, no *undefeated* one is removed.
        int[] withLivingCopies = BarrowsTunnelEncounter.eligibleBrothers(new boolean[6]);
        require(Arrays.equals(withLivingCopies, allBrothers),
                "Existing live brothers must not suppress another 2006 tunnel ambush");

        int[] none = BarrowsTunnelEncounter.eligibleBrothers(
                new boolean[] {true, true, true, true, true, true});
        for (int roll = 0; roll < 12; roll++) {
            int id = BarrowsTunnelEncounter.npcForRoll(roll, roll % 2, 0, none);
            require(id == 2036 || id == 2037, "No-brother fallback must summon a skeleton");
        }
        verifyActiveBrotherDetectionAndHistoricalDuplicateEligibility();
        verify2006RoomPopulationAndExpiry();

        System.out.println("Barrows weighted door rolls, live 2006 tunnel brother duplicates, "
                + "defeated-brother exclusion and independent chest guards passed.");
    }

    private static void verifyActiveBrotherDetectionAndHistoricalDuplicateEligibility() throws Exception {
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
            // Even with a live copy of Verac in the World NPC registry, a
            // door roll must still be allowed to summon another Verac.
            Method doorPool = BarrowsManager.class.getDeclaredMethod(
                    "eligibleTunnelBrothers", Player.class);
            doorPool.setAccessible(true);
            int[] remaining = (int[]) doorPool.invoke(null, owner);
            require(remaining.length == 6 && remaining[0] == 2030,
                    "Live Verac incorrectly excluded from 2006 door ambush pool");
            require(BarrowsTunnelEncounter.npcForRoll(0, 0, 0, remaining) == 2030,
                    "2006 door encounter could not choose the already-live Verac");

            owner.setBarrowsBrotherKilled(0, true);
            int[] afterKill = (int[]) doorPool.invoke(null, owner);
            require(afterKill.length == 5 && afterKill[0] == 2029,
                    "Killed Verac should be excluded even if a live duplicate remains");
            owner.setBarrowsBrotherKilled(0, false);

            Method spawnBrother = BarrowsManager.class.getDeclaredMethod(
                    "spawnBarrowsBrother", Player.class, int.class);
            spawnBrother.setAccessible(true);
            require((Boolean) spawnBrother.invoke(null, owner, 2030),
                    "Chest/sarcophagus must still recognise an already-spawned brother");
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

    private static void verify2006RoomPopulationAndExpiry() throws Exception {
        java.lang.reflect.Field roomsField = BarrowsManager.class.getDeclaredField("tunnelRooms");
        roomsField.setAccessible(true);
        BarrowsTunnelRoom[] rooms = (BarrowsTunnelRoom[]) roomsField.get(null);
        Position northWest = new Position(3534, 9710, 0);
        require(BarrowsTunnelPopulation.nearestRoomId(northWest, rooms) == 1,
                "North-west tunnel must be room 1");
        require(BarrowsTunnelPopulation.nearestRoomId(new Position(3534, 9710, 3), rooms) == -1,
                "Crypt monsters must not count toward tunnel-room limit");
        require(BarrowsTunnelPopulation.DOOR_SUMMON_LIFETIME_TICKS > 0,
                "Temporary door NPCs must expire even while fighting");
        Npc[] npcs = new Npc[13];
        for (int i = 0; i < 11; i++) {
            Npc npc = new Npc(2031);
            npc.setActive(true);
            npc.setPosition(new Position(3530 + (i % 5), 9707 + (i / 5), 0));
            npcs[i] = npc;
        }
        require(BarrowsTunnelPopulation.countLivingEnemies(northWest, rooms, npcs) == 11,
                "Must count eleven permanent or temporary living NPCs");
        require(BarrowsTunnelPopulation.atCapacity(northWest, rooms, npcs),
                "Door ambush spawned in a room already containing eleven enemies");
        npcs[0].setActive(false);
        require(!BarrowsTunnelPopulation.atCapacity(northWest, rooms, npcs),
                "Inactive room NPCs still prevented a new encounter");
        Npc otherRoomNpc = new Npc(2031);
        otherRoomNpc.setActive(true);
        otherRoomNpc.setPosition(new Position(3568, 9711, 0));
        npcs[12] = otherRoomNpc;
        require(BarrowsTunnelPopulation.countLivingEnemies(northWest, rooms, npcs) == 10,
                "Monsters in another room must not count toward this room's limit");
        Npc unrelated = new Npc(1);
        unrelated.setActive(true);
        unrelated.setPosition(new Position(3532, 9710, 0));
        npcs[11] = unrelated;
        require(BarrowsTunnelPopulation.countLivingEnemies(northWest, rooms, npcs) == 10,
                "Non-Barrows NPCs must not count toward the room's enemy limit");
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
