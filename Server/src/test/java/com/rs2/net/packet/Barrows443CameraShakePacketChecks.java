package com.rs2.net.packet;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Revision 443 Barrows collapse camera shake must use the NATIVE packet,
 * not the legacy camera-positioning packet. Cross-check the actual outbound
 * bytes with the committed 443 client's opcode size table and decoder.
 */
public final class Barrows443CameraShakePacketChecks {
    public static void main(String[] args) throws Exception {
        checkActualOutgoingBytes();
        checkNative443DecoderAndPacketLengths();
        checkBarrowsChestTriggersAndExitResets();
        System.out.println("Barrows 443 camera shake: native opcode 156, four-byte payload, "
                + "client decoder, chest trigger and exit reset passed.");
    }

    private static void checkActualOutgoingBytes() {
        assertShakeBytes(2, 3, 2, 3);
        assertShakeBytes(0, 15, 7, 8);
        assertShakeBytes(4, 1, 9, 11);

        try {
            PacketSender.buildRevision443CameraShakePacket(null, 5, 3, 2, 3);
            throw new AssertionError("A camera shake axis outside the five native indices was accepted");
        } catch (IllegalArgumentException expected) {
            // Native client allocates exactly five camera shake indices (0..4).
        }
    }

    private static void assertShakeBytes(int axis, int random, int sine, int frequency) {
        PacketWriter packet = PacketSender.buildRevision443CameraShakePacket(
                null, axis, random, sine, frequency);
        ByteBuffer bytes = packet.getBuffer();
        try {
            require(bytes.position() == 5,
                    "Native 443 shake must be five bytes total, not the old seven-byte packet");
            int[] expected = {156, axis, random, sine, frequency};
            for (int i = 0; i < expected.length; i++) {
                require((bytes.get(i) & 0xff) == expected[i],
                        "Camera shake byte " + i + " is incorrect; expected " + expected[i]
                                + " got " + (bytes.get(i) & 0xff));
            }
        } finally {
            PacketWriter.discardOutboundOpcodes(bytes);
        }
    }

    private static void checkNative443DecoderAndPacketLengths() throws Exception {
        String client = read("../Client/src/unpackaged/Client.java");
        Matcher sizesMatch = Pattern.compile(
                "incomingSizes\\s*=\\s*\\(new int\\[\\]\\{([^}]+)\\}").matcher(client);
        require(sizesMatch.find(), "Revision 443 packet length table was not found");
        String[] sizes = sizesMatch.group(1).split(",");
        require(sizes.length == 256, "Revision 443 must have 256 opcode size definitions");
        require(Integer.parseInt(sizes[156].trim()) == 4,
                "Native camera shake opcode 156 must carry four payload bytes");
        require(Integer.parseInt(sizes[96].trim()) == 6,
                "Opcode 96 must remain the six-byte camera-positioning packet");
        require(Integer.parseInt(sizes[242].trim()) == 0,
                "Camera shake reset opcode 242 must be zero-length");

        String parser = read("../Client/src/unpackaged/Bzip2Block.java");
        int start = parser.indexOf("if (Class4.frameId == 156) {");
        int end = parser.indexOf("if (Class4.frameId == 148)", start);
        require(start >= 0 && end > start, "Native shake packet handler (156) not found");
        String handler = parser.substring(start, end);
        require(count(handler, "getUbyte()") == 4,
                "Native shake handler must read four unsigned-byte parameters");
        require(handler.contains("Class44.aBooleanArray837[i_27_] = true"),
                "Native shake packet does not enable the camera shake axis");
        require(handler.contains("Class13.anIntArray197[i_27_] = i_28_")
                        && handler.contains("Class2.anIntArray49[i_27_] = i_29_")
                        && handler.contains("Class45.anIntArray857[i_27_] = i_30_"),
                "Native shake packet does not apply the three shake parameters");
        require(parser.contains("if (Class4.frameId == 242)")
                        && parser.contains("Class44.aBooleanArray837[i_82_] = false"),
                "Native camera reset must disable the five shake axes");
    }

    private static void checkBarrowsChestTriggersAndExitResets() throws Exception {
        String player = read("src/main/java/com/rs2/model/player/Player.java");
        int start = player.indexOf("public final void startBarrowsChestDamage()");
        int end = player.indexOf("public final void moveToInstancedPosition", start);
        require(start >= 0 && end > start, "Barrows chest-collapse start method missing");
        require(player.substring(start, end).contains("sendCameraShake(2, 3, 2, 3)"),
                "Opening the Barrows chest no longer triggers the original camera shake");
        require(player.contains("this.packetSender.resetCamera()")
                        || player.contains("player.packetSender.resetCamera()"),
                "Escape from Barrows does not reset the camera");

        String sender = read("src/main/java/com/rs2/net/packet/PacketSender.java");
        int resetStart = sender.indexOf("public final void resetCamera()");
        int resetEnd = sender.indexOf("public final void openDoubleDoorPair", resetStart);
        require(resetStart >= 0 && resetEnd > resetStart
                        && sender.substring(resetStart, resetEnd)
                        .contains("ServerSettings.clientBuild == 443 ? 242 : 107"),
                "Revision 443 camera-reset opcode is no longer 242");
    }

    private static int count(String text, String fragment) {
        int result = 0, pos = 0;
        while ((pos = text.indexOf(fragment, pos)) >= 0) {
            result++;
            pos += fragment.length();
        }
        return result;
    }

    private static String read(String path) throws Exception {
        return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
