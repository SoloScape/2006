import com.rs2.ServerSettings;
import com.rs2.model.player.Player;
import com.rs2.net.packet.ClientPackets;
import com.rs2.net.packet.IncomingPacket;
import com.rs2.net.packet.InterfaceBridge;
import com.rs2.net.packet.PacketBuffer;
import com.rs2.net.packet.handler.InterfaceActionPacketHandler;
import java.nio.ByteBuffer;

/**
 * Verifies the native revision-443 Barrows puzzle's model mappings and answer
 * clicks. Both native click packet families must reach the legacy solver.
 */
public final class Barrows443PuzzleInterfaceChecks {
    private static final int GROUP = 25;
    private static final int LEGACY_ROOT = 4543;
    private static final int[] PROMPT_CHILDREN = {6, 7, 8};
    private static final int[] ANSWER_CHILDREN = {2, 3, 5};

    public static void main(String[] args) {
        ServerSettings.clientBuild = 443;

        require(InterfaceBridge.translateGroup(LEGACY_ROOT) == GROUP,
                "Barrows puzzle must open the native 443 group 25");

        for (int i = 0; i < 3; i++) {
            int prompt = 4545 + i;
            int answer = 4550 + i;
            require(InterfaceBridge.translate(prompt) == (GROUP << 16 | PROMPT_CHILDREN[i]),
                    "Wrong native model widget for puzzle sequence " + i);
            require(InterfaceBridge.translate(answer) == (GROUP << 16 | ANSWER_CHILDREN[i]),
                    "Wrong native model widget for puzzle answer " + i);
            require(InterfaceBridge.toLegacyComponent(GROUP << 16 | ANSWER_CHILDREN[i]) == answer,
                    "Native puzzle answer " + i + " does not map to the legacy button");

            // The correct answer can appear in any of the three shuffled slots.
            // Confirm either packet family reaches BarrowsManager's solver.
            for (boolean widgetSelect : new boolean[]{true, false}) {
                Player player = new Player(null);
                player.isBot = true; // Detached test player: no network writes.
                player.setOpenInterfaceId(LEGACY_ROOT);
                player.activeBarrowsDoorPuzzleIndex = 0; // Correct model is 6713.
                player.activeBarrowsDoorPuzzleAnswerObjectIds = new int[]{6714, 6715, 6714};
                player.activeBarrowsDoorPuzzleAnswerObjectIds[i] = 6713;

                click(player, ANSWER_CHILDREN[i], widgetSelect);
                require(player.barrowsDoorPuzzleSolved,
                        "Barrows answer " + i + " failed via opcode "
                                + (widgetSelect ? "153" : "54"));
            }
        }

        // Ignore late widget clicks after the puzzle panel has been closed.
        for (boolean widgetSelect : new boolean[]{true, false}) {
            Player player = new Player(null);
            player.isBot = true;
            player.setOpenInterfaceId(6179);
            player.activeBarrowsDoorPuzzleIndex = 0;
            player.activeBarrowsDoorPuzzleAnswerObjectIds = new int[]{6713, 6714, 6715};
            click(player, ANSWER_CHILDREN[0], widgetSelect);
            require(!player.barrowsDoorPuzzleSolved,
                    "Closed Barrows puzzle accepted a stale answer click");
        }

        System.out.println("Barrows native 443 puzzle interface checks passed.");
    }

    private static void click(Player player, int answerChild, boolean widgetSelect) {
        int packedId = GROUP << 16 | answerChild;
        ByteBuffer payload;
        int opcode;
        if (widgetSelect) {
            payload = ByteBuffer.allocate(6);
            payload.order(java.nio.ByteOrder.LITTLE_ENDIAN).putShort((short) 0);
            payload.order(java.nio.ByteOrder.BIG_ENDIAN).putInt(packedId);
            opcode = ClientPackets.WIDGET_SELECT;
        } else {
            payload = ByteBuffer.allocate(4);
            payload.putInt(packedId);
            opcode = ClientPackets.INTERFACE_BUTTON;
        }
        payload.flip();
        new InterfaceActionPacketHandler().handle(player,
                new IncomingPacket(opcode, payload.remaining(),
                        PacketBuffer.wrapReader(payload)));
    }

    private static void require(boolean valid, String failureMessage) {
        if (!valid) throw new AssertionError(failureMessage);
    }
}
