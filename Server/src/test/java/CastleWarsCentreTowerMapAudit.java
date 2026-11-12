import com.rs2.ServerSettings;
import com.rs2.cache.js5.WorldMaps;
import com.rs2.model.objects.ObjectDefinition;
import com.rs2.util.path.MapDataReader;
import com.rs2.util.path.WalkingCollisionMap;
import com.rs2.util.path.ProjectileCollisionMap;

/** Read the actual 443 map square behind the centre-island tower chase bug. */
public final class CastleWarsCentreTowerMapAudit {
    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        ObjectDefinition.loadRevision443();
        WorldMaps.Square region = null;
        for (WorldMaps.Square s : WorldMaps.load()) {
            if (s.regionId == ((37 << 8) | 48)) { region = s; break; }
        }
        if (region == null) throw new AssertionError("Missing central Castle Wars map square");
        WalkingCollisionMap.loadCollisionMaps();
        ProjectileCollisionMap.loadCollisionMaps();
        MapDataReader locations = new MapDataReader(region.locations);
        int objectId = -1;
        while (true) {
            int deltaId = locations.readUnsignedSmart();
            if (deltaId == 0) break;
            objectId += deltaId;
            int packed = 0;
            while (true) {
                int delta = locations.readUnsignedSmart();
                if (delta == 0) break;
                packed += delta - 1;
                int x = 2368 + (packed >> 6 & 63);
                int y = 3072 + (packed & 63);
                int plane = packed >> 12;
                int info = locations.readUnsignedByte();
                if (x >= 2386 && x <= 2414 && y >= 3088 && y <= 3120) {
                    ObjectDefinition def = ObjectDefinition.forId(objectId);
                    String name = def == null ? "?" : def.getName();
                    if (plane != 0 || x >= 2393 && x <= 2406
                            && y >= 3097 && y <= 3110) {
                        System.out.println("CENTRE-OBJ "+x+" "+y+" "+plane
                                +" id="+objectId+" type="+(info >> 2)+" orient="+(info & 3)+" name="+name);
                    }
                }
            }
        }
        MapDataReader terrain = new MapDataReader(region.terrain);
        int[][][] tileSettings = new int[4][64][64];
        for (int plane = 0; plane < 4; ++plane) {
            for (int lx = 0; lx < 64; ++lx) {
                for (int ly = 0; ly < 64; ++ly) {
                    while (true) {
                        int opcode = terrain.readUnsignedByte();
                        if (opcode == 0) break;
                        if (opcode == 1) {terrain.skipByte(1); break;}
                        if (opcode <= 49) {terrain.skipByte(1); continue;}
                        if (opcode <= 81) tileSettings[plane][lx][ly] = opcode - 49;
                    }
                }
            }
        }
        for (int plane = 0; plane <= 2; ++plane) {
            System.out.println("CENTRE-PLANE "+plane);
            for (int y = 3117; y >= 3090; --y) {
                StringBuilder row = new StringBuilder();
                for (int x = 2387; x <= 2413; ++x) {
                    int w=WalkingCollisionMap.getTileFlags(x,y,plane);
                    int p=ProjectileCollisionMap.getTileFlags(x,y,plane);
                    row.append((w & 0x200000)!=0 ? 'X'
                            : (w & 0x100)!=0 ? 'W'
                            : (p & 0x200000)!=0 ? 'x'
                            : (p & 0x100)!=0 ? 'P'
                            : w!=0 ? 'c' : p!=0 ? 'p' : '.');
                }
                System.out.println("CENTRE-MAP "+plane+" y="+y+" "+row.toString());
                if (plane == 1) {
                    StringBuilder settings = new StringBuilder();
                    for (int x = 2387; x <= 2413; ++x) {
                        int f = tileSettings[plane][x - 2368][y - 3072];
                        settings.append((f & 2) != 0 ? 'B' : (f & 1) != 0 ? 'X'
                                : f == 0 ? '.' : 'f');
                    }
                    System.out.println("CENTRE-FLAGS "+plane+" y="+y+" "+settings);
                }
            }
        }
        System.out.println("CENTRE-RAW-REGION-AUDIT-PASSED");
    }
}
