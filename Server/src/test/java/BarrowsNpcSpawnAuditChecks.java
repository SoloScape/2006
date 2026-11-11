import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.objects.ObjectDefinition;
import com.rs2.util.path.WalkingCollisionMap;
import java.nio.file.Files;
import java.nio.file.Paths;

/** Prints a full audit of static Barrows monsters against the revision 443 map. */
public final class BarrowsNpcSpawnAuditChecks {
    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        ObjectDefinition.loadRevision443();
        NpcDefinition.loadDefinitions();
        WalkingCollisionMap.loadCollisionMaps();

        byte[] data = Files.readAllBytes(Paths.get("data/npcs/Npc spawn.dat"));
        int count = u16(data, 1), inBarrows = 0, blocked = 0, zeroHp = 0;
        int[] counts = new int[7];
        for (int i = 0, off = 3; i < count; i++, off += 9) {
            int id = u16(data, off) - count - i;
            int x = u16(data, off + 4), y = u16(data, off + 6), plane = data[off + 8] & 255;
            if (x < 3529 || x > 3574 || y < 9672 || y > 9717 || plane != 0) continue;
            inBarrows++;
            int f = WalkingCollisionMap.getTileFlags(x, y, plane);
            Npc npc = new Npc(id);
            int size = Math.max(1, npc.getDefinition().getSize());
            boolean impassable = false;
            for (int dx = 0; dx < size; dx++) {
                for (int dy = 0; dy < size; dy++) {
                    if ((WalkingCollisionMap.getTileFlags(x+dx, y+dy, plane) & 0x1280100) != 0) {
                        impassable = true;
                    }
                }
            }
            if (id >= 2031 && id <= 2037) counts[id - 2031]++;
            if (impassable) blocked++;
            if (npc.getMaxHitpoints() <= 0) zeroHp++;
            System.out.println("BARROWS STATIC id=" + id + " name=" + npc.getDefinition().getName()
                    + " pos=" + x + "," + y + "," + plane
                    + " size=" + size + " flags=0x" + Integer.toHexString(f)
                    + " blocked=" + impassable + " hp=" + npc.getMaxHitpoints());
        }
        int[][] entries = {
            {3578,9706,3},{3568,9683,3},{3546,9684,3},
            {3534,9704,3},{3556,9718,3},{3557,9703,3}
        };
        for (int[] entry : entries) {
            int f = WalkingCollisionMap.getTileFlags(entry[0],entry[1],entry[2]);
            System.out.println("BARROWS CRYPT ENTRY " + entry[0] + "," + entry[1] + "," + entry[2]
                + " flags=0x" + Integer.toHexString(f)
                + " blocked=" + ((f & 0x1280100) != 0));
        }
        int nonZero = 0, blockedTiles = 0;
        for (int x = 3526; x <= 3578; x++) {
            for (int y = 9669; y <= 9720; y++) {
                int flags = WalkingCollisionMap.getTileFlags(x,y,0);
                if (flags != 0) nonZero++;
                if ((flags & 0x1280100) != 0) blockedTiles++;
            }
        }
        System.out.println("BARROWS 443 MAP nonzeroFlags=" + nonZero
                + " blockedFootprintTiles=" + blockedTiles);
        System.out.println("BARROWS AUDIT total=" + inBarrows
                + " blocked=" + blocked + " zeroHitpoints=" + zeroHp
                + " npc2031-2037=" + java.util.Arrays.toString(counts));
        if (inBarrows != 32) throw new AssertionError("Expected 32 fixed Barrows tunnel NPCs");
    }
    private static int u16(byte[] d,int offset) {
        return (d[offset] & 255) << 8 | d[offset+1] & 255;
    }
}
