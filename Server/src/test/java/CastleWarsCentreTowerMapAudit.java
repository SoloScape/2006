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
        ServerSettings.cacheVersion = 443;
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
                    if (plane != 0 || name.toLowerCase().contains("stair")
                            || name.toLowerCase().contains("ladder")
                            || name.toLowerCase().contains("tower")) {
                        System.out.println("CENTRE-OBJ "+x+" "+y+" "+plane
                                +" id="+objectId+" type="+(info >> 2)+" orient="+(info & 3)+" name="+name);
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
            }
        }
        System.out.println("CENTRE-RAW-REGION-AUDIT-PASSED");
    }
}
