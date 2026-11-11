import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.gameplay.barrows.BarrowsManager;
import com.rs2.model.player.Player;
import java.lang.reflect.Field;
import java.util.ArrayDeque;

/**
 * Post-loot Barrows escape and cleanup checks against historical 2006 accounts.
 *
 * The August 2006 Barrows guide explicitly advises returning to the entry
 * ladder after looting, while falling rocks injure the player:
 * https://forum.tip.it/topic/28758-barrows-wancient-magicks-ed-22-aug-2006-aow-discussion/
 *
 * Later OSRS reference pages discuss shifting the ladder and rearranging
 * the dungeon after loot. Those later claims do not establish an identical
 * mechanic in 2006, so this reconstruction preserves the existing escape
 * route. Wrong-answer puzzles still legitimately reroute the tunnels.
 */
public final class BarrowsPostLootEscapeChecks {
    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        testTunnelVsCryptCoordinates();
        testPostLootCollapsePreservesEscapePath();
        testExitViaLadderEndsCollapseAndResetsRun();
        testTeleportOutEndsCollapseAndResetsRun();
        testPreLootExitPreservesProgress();
        testAllRoutesRemainReachable();
        System.out.println("Barrows post-loot: 2006 ladder escape, camera/damage phase, "
                + "crypt/teleport cleanup, pre-loot persistence and route reachability passed.");
    }

    private static void testTunnelVsCryptCoordinates() {
        Player player = player();
        require(BarrowsManager.isInBarrowsTunnels(player),
                "2006 chest room must be recognised as underground Barrows");
        player.setPosition(new Position(3552, 9694, 3));
        require(player.isInBarrows(), "Crypt and tunnels share Barrows region X/Y");
        require(!BarrowsManager.isInBarrowsTunnels(player),
                "The crypt on plane 3 must not be treated as collapsing tunnels");
        player.setPosition(new Position(3552, 3304, 0));
        require(!BarrowsManager.isInBarrowsTunnels(player),
                "Outside ground level is not in the Barrows tunnels");
    }

    private static void testPostLootCollapsePreservesEscapePath() throws Exception {
        Player player = player();
        BarrowsManager.generateTunnelRoute(player, false);
        player.barrowsDoorPuzzleSolved = true;
        player.setBarrowsKillCount(6);
        int originalRoute = player.configStates[452];
        int originalDoor = BarrowsManager.findOpenPuzzleDoorBit(originalRoute);
        int originalLadder = ladderIndex(originalRoute);
        require(originalDoor != -1, "Chest run has no central puzzle door");
        require(originalLadder != -1, "Chest run has no exit ladder");
        assertRouteFromCenterToLadder(originalRoute);

        // This is the phase invoked after the chest's rewards are handed out.
        player.startBarrowsChestDamage();
        require(player.barrowsChestOpened, "Looting did not begin the collapse phase");
        require(player.configStates[452] == originalRoute,
                "Post-loot collapse silently randomised the 2006 maze without period evidence");
        require(ladderIndex(player.configStates[452]) == originalLadder,
                "The original 2006 escape ladder was moved on chest loot");
        require(player.barrowsDoorPuzzleSolved,
                "The already solved central doorway was re-locked after chest loot");
        assertRouteFromCenterToLadder(player.configStates[452]);
        // Walk inside the tunnel without ending the collapse prematurely.
        player.moveTo(new Position(3553, 9694, 0));
        require(player.barrowsChestOpened, "Moving within tunnels cleared the collapse");
        require(player.configStates[452] == originalRoute,
                "Moving in tunnels unexpectedly changed the exit maze");
        require(player.getBarrowsKillCount() == 6,
                "Walking inside tunnels reset Barrows before reaching the exit");
    }

    private static void testExitViaLadderEndsCollapseAndResetsRun() throws Exception {
        Player player = lootedPlayer();
        player.barrowsRunsCompleted = 7;
        // This is the destination reached by the existing tunnel ladder handler:
        // a brother's crypt on plane 3 in the same Barrows X/Y area.
        player.moveTo(new Position(3546, 9684, 3));
        assertReset(player, "Climbing out through a crypt ladder");
        require(player.isInBarrows(),
                "Fixture must still be in the Barrows X/Y region after ladder");
        require(player.barrowsRunsCompleted == 7,
                "Exiting reset the player's lifetime number of Barrows runs");

        // The next run gets an independent layout, with no leftover puzzle state.
        player.setPosition(new Position(3552, 9694, 0));
        BarrowsManager.generateTunnelRoute(player, false);
        require(BarrowsManager.findOpenPuzzleDoorBit(player.configStates[452]) != -1,
                "Fresh Barrows run cannot generate its own maze");
        assertRouteFromCenterToLadder(player.configStates[452]);
        require(!player.barrowsChestOpened,
                "A new run incorrectly inherited the previous chest collapse state");
    }

    private static void testTeleportOutEndsCollapseAndResetsRun() {
        Player player = lootedPlayer();
        player.moveTo(new Position(3492, 3210, 0));
        assertReset(player, "Teleporting from the chest room");
        require(!player.isInBarrows(), "Teleport did not leave the Barrows region");
    }

    private static void testPreLootExitPreservesProgress() {
        Player player = player();
        BarrowsManager.generateTunnelRoute(player, false);
        player.setBarrowsBrotherKilled(0, true);
        player.setBarrowsKillCount(3);
        int originalRoute = player.configStates[452];
        player.moveTo(new Position(3546, 9684, 3));
        require(player.getBarrowsKilledBrothers()[0],
                "Leaving the tunnels before looting incorrectly deleted a brother kill");
        require(player.getBarrowsKillCount() == 3,
                "Leaving the tunnels before looting lost the ongoing kill count");
        require(player.configStates[452] == originalRoute,
                "Leaving before looting unexpectedly reset the pending maze");
    }

    private static void testAllRoutesRemainReachable() throws Exception {
        Player player = player();
        for (int roll = 0; roll < 250; roll++) {
            BarrowsManager.generateTunnelRoute(player, false);
            assertRouteFromCenterToLadder(player.configStates[452]);
            require(ladderIndex(player.configStates[452]) >= 0,
                    "No valid 2006 tunnel exit on generated layout " + roll);
        }
    }

    private static Player lootedPlayer() {
        Player player = player();
        BarrowsManager.generateTunnelRoute(player, false);
        player.barrowsDoorPuzzleSolved = true;
        player.activeBarrowsDoorPuzzleIndex = 2;
        player.activeBarrowsDoorPuzzleAnswerObjectIds = new int[] {6713, 6714, 6715};
        player.setBarrowsBrotherKilled(0, true);
        player.setBarrowsKillCount(6);
        player.startBarrowsChestDamage();
        return player;
    }

    private static void assertReset(Player player, String context) {
        require(!player.barrowsChestOpened, context + " left the collapse active");
        require(player.getBarrowsKillCount() == 0, context + " kept kill count");
        for (boolean killed : player.getBarrowsKilledBrothers()) {
            require(!killed, context + " kept a defeated brother marker");
        }
        require(!player.barrowsDoorPuzzleSolved, context + " retained solved puzzle");
        require(player.activeBarrowsDoorPuzzleIndex == -1,
                context + " retained a stale active puzzle");
        require(player.configStates[452] == 0, context + " retained stale maze doors");
        require(player.configStates[453] == 0, context + " retained opened chest config");
    }

    private static int ladderIndex(int config) {
        int found = -1;
        for (int n = 0; n < 4; n++) {
            if ((config & (1 << (6 + n))) == 0) continue;
            if (found != -1) return -1;
            found = n;
        }
        return found;
    }

    private static void assertRouteFromCenterToLadder(int config) throws Exception {
        int ladder = ladderIndex(config);
        require(ladder != -1, "There must be exactly one ladder");
        int[] corners = {1, 3, 7, 9};

        Field roomsField = BarrowsManager.class.getDeclaredField("tunnelRooms");
        roomsField.setAccessible(true);
        Object[] rooms = (Object[]) roomsField.get(null);
        Class<?> type = rooms[0].getClass();
        Field connectedField = type.getDeclaredField("connectedRoomIds");
        Field bitsField = type.getDeclaredField("doorBitIndexes");
        connectedField.setAccessible(true);
        bitsField.setAccessible(true);

        boolean[] reached = new boolean[10];
        ArrayDeque<Integer> queue = new ArrayDeque<Integer>();
        reached[5] = true;
        queue.add(5);
        while (!queue.isEmpty()) {
            int roomId = queue.removeFirst();
            Object room = rooms[roomId - 1];
            int[] neighbors = (int[]) connectedField.get(room);
            int[] bits = (int[]) bitsField.get(room);
            for (int i = 0; i < neighbors.length; i++) {
                if ((config & (1 << bits[i])) != 0 || reached[neighbors[i]]) continue;
                reached[neighbors[i]] = true;
                queue.addLast(neighbors[i]);
            }
        }
        require(reached[corners[ladder]],
                "No connected escape from the chest room to the ladder, route=" + config);
        require(BarrowsManager.findOpenPuzzleDoorBit(config) != -1,
                "There must be exactly one unlocked central door");
    }

    private static Player player() {
        Player player = new Player(null);
        player.isBot = true;
        player.setEncodedIndex(32768);
        player.setPosition(new Position(3552, 9694, 0));
        player.refreshLocalViewArea();
        return player;
    }

    private static void require(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
