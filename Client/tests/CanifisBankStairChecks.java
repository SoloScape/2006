package unpackaged;

import com.rs2.cache.js5.Js5CacheStore;
import com.rs2.cache.js5.MapGroups;
import jagex.world.map.TraversalMap;
import java.io.File;

/** Run from Server with the client and server classes on the classpath. */
public final class CanifisBankStairChecks {
    public static void main(String[] args) throws Exception {
        Class65.anInt1145 = 54 * 64;
        JKeyListener.anInt618 = 54 * 64;
        ObjectDefinition.method732(-100);
        TraversalMap[] maps = new TraversalMap[4];
        for (int plane = 0; plane < maps.length; plane++) {
            maps[plane] = new TraversalMap(104, 104);
        }
        try (Js5CacheStore store = new Js5CacheStore(new File("cache"))) {
            byte[] terrain = MapGroups.readTerrain(store, store.readReferenceTable(5), 54, 54);
            Class46.method933(maps, 0, terrain, (byte) 117, 0, 0, 0);
        }

        int x = (3507 - Class65.anInt1145) * 128;
        int y = (3480 - JKeyListener.anInt618) * 128 + 64;
        int street = height(x - 1, y, 0);
        int bank = height(x + 128, y, 0);
        require(bank == street - 128, "Expected a full flight from street to bank floor");
        require(height(x, y, 0) == street, "Stairs jump at the street edge");
        require(height(x + 96, y, 0) == bank, "Top tread does not meet the bank floor");
        for (int offset = 1; offset <= 128; offset++) {
            int rise = height(x + offset - 1, y, 0) - height(x + offset, y, 0);
            require(rise >= 0 && rise <= 2, "Abrupt height change at stair offset " + offset);
        }
        require(height(x + 128, y + 128, 0) == bank, "Nearby bridge floor changed");
        require(height(x + 64, y, 1) == Class67.heightMap[2][51][24],
                "Upper-plane height sampling changed");
        Class63.aBoolean1120 = true;
        require(height(x + 64, y, 0) == bank, "Canifis stair correction applied in an instance");
        System.out.println("Canifis bank stair checks passed");
    }

    private static int height(int x, int y, int plane) {
        return Class14.method212(y, 9990, plane, x);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
