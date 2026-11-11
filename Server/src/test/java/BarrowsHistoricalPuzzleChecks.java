import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.gameplay.barrows.BarrowsManager;
import com.rs2.model.player.Player;
import com.rs2.util.GameUtil;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Original 2005-2006 Barrows puzzle model sequences, answer positions,
 * route rerolls, and single-use button processing.
 */
public final class BarrowsHistoricalPuzzleChecks {
    // Models documented in the historical Apollo Barrows puzzle reference.
    private static final int[][] PROMPTS = {
        {6716, 6717, 6718}, // rotating arrows
        {6722, 6723, 6724}, // clockwise grey square
        {6728, 6729, 6730}, // rising grey area
        {6734, 6735, 6736}  // decreasing polygon sides
    };
    private static final int[][] OPTIONS = {
        {6713, 6714, 6715},
        {6719, 6720, 6721},
        {6725, 6726, 6727},
        {6731, 6732, 6733}
    };

    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        checkPuzzleModelsAndAnswerSlots();
        checkRerouteAfterWrongAnswer();
        System.out.println("Barrows 2006 puzzle model, all answers, stale click and route checks passed.");
    }

    private static void checkPuzzleModelsAndAnswerSlots() throws Exception {
        Field field = BarrowsManager.class.getDeclaredField("doorPuzzles");
        field.setAccessible(true);
        Object[] puzzles = (Object[]) field.get(null);
        require(puzzles.length == 4, "Expected all four historic Barrows puzzles");

        Set<String> seenPositions = new HashSet<String>();
        for (int puzzleId = 0; puzzleId < puzzles.length; puzzleId++) {
            Object puzzle = puzzles[puzzleId];
            Field displays = puzzle.getClass().getDeclaredField("displayOptionObjectIds");
            Field answers = puzzle.getClass().getDeclaredField("answerObjectIds");
            displays.setAccessible(true);
            answers.setAccessible(true);
            int[] sequence = (int[]) displays.get(puzzle);
            int[] choices = (int[]) answers.get(puzzle);
            require(Arrays.equals(sequence, PROMPTS[puzzleId]),
                    "Wrong historical puzzle sequence at type " + puzzleId);
            require(Arrays.equals(choices, OPTIONS[puzzleId]),
                    "Wrong historical answer models at type " + puzzleId);

            // Each of the three shuffled answer positions can be correct.
            for (int answerSlot = 0; answerSlot < 3; answerSlot++) {
                int[] shuffled = new int[3];
                shuffled[answerSlot] = choices[0];
                shuffled[(answerSlot + 1) % 3] = choices[1];
                shuffled[(answerSlot + 2) % 3] = choices[2];
                Player player = makePlayer();
                player.activeBarrowsDoorPuzzleIndex = puzzleId;
                player.activeBarrowsDoorPuzzleAnswerObjectIds = shuffled.clone();
                player.setOpenInterfaceId(4543);

                int button = 4550 + answerSlot;
                require(BarrowsManager.handlePuzzleButtonClick(player, button),
                        "Puzzle type " + puzzleId + " ignored correct slot " + answerSlot);
                require(player.barrowsDoorPuzzleSolved,
                        "Puzzle type " + puzzleId + " rejected correct slot " + answerSlot);
                require(player.activeBarrowsDoorPuzzleIndex == -1,
                        "Solved puzzle retained stale answer data");
                require(player.getOpenInterfaceId() != 4543,
                        "Solved puzzle left the interface open");

                // The packet handler must never be able to replay a solved puzzle.
                require(!BarrowsManager.handlePuzzleButtonClick(player, button),
                        "A duplicate click was accepted after solving the puzzle");
                player.setOpenInterfaceId(4543);
                require(!BarrowsManager.handlePuzzleButtonClick(player, button),
                        "A stale answer was accepted on a reopened interface");

                // Random shuffle must preserve exactly one correct answer.
                for (int trial = 0; trial < 16; trial++) {
                    int[] sample = GameUtil.shuffleIntArray(choices.clone());
                    int seen = 0;
                    for (int model : sample) if (model == choices[0]) seen++;
                    require(seen == 1, "Shuffle duplicated or deleted correct answer");
                    require(containsSame(choices, sample), "Shuffle changed one of the answer models");
                    seenPositions.add(puzzleId + ":" + Arrays.toString(sample));
                }
            }
        }
        require(seenPositions.size() >= 12, "Answer choices were not shuffled");
    }

    private static void checkRerouteAfterWrongAnswer() {
        // All 4 starting puzzle doors should remain possible in a new run.
        Set<Integer> initialDoors = new HashSet<Integer>();
        Player player = makePlayer();
        for (int iteration = 0; iteration < 200; iteration++) {
            player.configStates[452] = 0;
            BarrowsManager.generateTunnelRoute(player, false);
            int first = BarrowsManager.findOpenPuzzleDoorBit(player.configStates[452]);
            require(isCentralDoor(first), "Initial route must have exactly one centre puzzle door");
            initialDoors.add(first);

            // The 2006 game requires a different puzzle door after failure.
            int before = first;
            for (int reroll = 0; reroll < 12; reroll++) {
                BarrowsManager.generateTunnelRoute(player, false);
                int after = BarrowsManager.findOpenPuzzleDoorBit(player.configStates[452]);
                require(isCentralDoor(after), "Reroute lost or duplicated the puzzle door");
                require(after != before, "Wrong answer can leave puzzle door unchanged"
                        + " iteration=" + iteration + " reroll=" + reroll
                        + " previous=" + before + " selected=" + after
                        + " routeConfig=" + player.configStates[452]);
                require(!player.barrowsDoorPuzzleSolved, "Reroute retained solved state");
                before = after;
            }
        }
        require(initialDoors.size() == 4, "Some of the four central doors were never selected");

        Player wrong = makePlayer();
        BarrowsManager.generateTunnelRoute(wrong, false);
        int oldDoor = BarrowsManager.findOpenPuzzleDoorBit(wrong.configStates[452]);
        wrong.activeBarrowsDoorPuzzleIndex = 0;
        wrong.activeBarrowsDoorPuzzleAnswerObjectIds = new int[]{6714, 6713, 6715};
        wrong.setOpenInterfaceId(4543);
        require(BarrowsManager.handlePuzzleButtonClick(wrong, 4550), "Wrong answer ignored");
        require(!wrong.barrowsDoorPuzzleSolved, "Wrong answer unlocked centre door");
        require(oldDoor != BarrowsManager.findOpenPuzzleDoorBit(wrong.configStates[452]),
                "Wrong answer did not relocate the puzzle doorway");
        require(wrong.activeBarrowsDoorPuzzleIndex == -1, "Wrong answer left puzzle active");
        int routeAfter = wrong.configStates[452];
        require(!BarrowsManager.handlePuzzleButtonClick(wrong, 4550),
                "Late wrong answer generated a second route");
        require(routeAfter == wrong.configStates[452], "Late wrong answer changed the route");
    }

    private static Player makePlayer() {
        Player player = new Player(null);
        player.isBot = true;
        // The top-west Barrows tunnel room is a valid route starting location.
        player.setPosition(new Position(3535, 9710, 0));
        return player;
    }

    private static boolean isCentralDoor(int bit) {
        return bit == 14 || bit == 18 || bit == 19 || bit == 21;
    }

    private static boolean containsSame(int[] expected, int[] actual) {
        int[] first = expected.clone(), second = actual.clone();
        Arrays.sort(first);
        Arrays.sort(second);
        return Arrays.equals(first, second);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
