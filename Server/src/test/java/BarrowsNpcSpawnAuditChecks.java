import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.gameplay.barrows.BarrowsNpcSpawnPlacement;
import com.rs2.model.gameplay.barrows.BarrowsCryptCombatSounds;
import com.rs2.net.packet.AudioIds443;
import com.rs2.model.skill.magic.SpellDefinition;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.objects.ObjectDefinition;
import com.rs2.util.path.WalkingCollisionMap;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            if (!npc.getDefinition().isAttackable()) {
                throw new AssertionError("Barrows monster lacks Attack action: " + id);
            }
            if (!impassable) {
                verifyPlacement(new Position(x, y, plane), size);
            }
            System.out.println("BARROWS STATIC id=" + id + " name=" + npc.getDefinition().getName()
                    + " pos=" + x + "," + y + "," + plane
                    + " size=" + size + " flags=0x" + Integer.toHexString(f)
                    + " blocked=" + impassable + " hp=" + npc.getMaxHitpoints());
        }
        int[][] entries = {
            {3578,9706,3},{3568,9683,3},{3546,9684,3},
            {3534,9704,3},{3556,9718,3},{3557,9703,3}
        };
        for (int brother = 0; brother < entries.length; brother++) {
            int[] entry = entries[brother];
            Npc npc = new Npc(2030 - brother);
            int f = WalkingCollisionMap.getTileFlags(entry[0],entry[1],entry[2]);
            System.out.println("BARROWS CRYPT BROTHER id=" + npc.getNpcId()
                + " size=" + npc.getDefinition().getSize()
                + " pos=" + entry[0] + "," + entry[1] + "," + entry[2]
                + " flags=0x" + Integer.toHexString(f)
                + " hp=" + npc.getMaxHitpoints());
            if (npc.getMaxHitpoints() <= 0) throw new AssertionError("Inactive Barrows brother definition");
            if (!npc.getDefinition().isAttackable())
                throw new AssertionError("Barrows brother lacks Attack action: " + npc.getNpcId());
            verifyPlacement(new Position(entry[0], entry[1], entry[2]),
                    Math.max(1,npc.getDefinition().getSize()));
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
        verifyDoorApproaches();
        verifySyntheticBlockedAndWallCases();
        verifyCryptCombatSounds();
        System.out.println("BARROWS AUDIT total=" + inBarrows
                + " blocked=" + blocked + " zeroHitpoints=" + zeroHp
                + " npc2031-2037=" + java.util.Arrays.toString(counts));
        if (inBarrows != 32) throw new AssertionError("Expected 32 fixed Barrows tunnel NPCs");
    }
    /**
     * Attack and victim-hit packets use Npc.getAttackSoundId() and
     * Npc.getHitSoundId(). Confirm none are silently dropped by the
     * revision-443 legacy-to-JS5 audio translation.
     */
    private static void verifyCryptCombatSounds() {
        final int[][] expected = {
            {132, 135, 137}, // 2031: bloodworm (provisional spider-like audio)
            {17, 16, 15},   // 2032: crypt rat
            {17, 16, 15},   // 2033: larger crypt rat
            {132, 135, 137},// 2034: crypt spider
            {132, 135, 137},// 2035: crypt spider
            {108, 110, 109},// 2036: skeleton
            {108, 110, 109} // 2037: skeleton
        };
        for (int id = 2031; id <= 2037; id++) {
            Npc npc = new Npc(id);
            int[] wanted = expected[id - 2031];
            int[] actual = {
                npc.getAttackSoundId(), npc.getHitSoundId(), npc.getDeathSoundId()
            };
            for (int phase = 0; phase < actual.length; phase++) {
                if (actual[phase] != wanted[phase]) {
                    throw new AssertionError("Crypt NPC " + id
                            + " missing attack/hit/death audio for phase " + phase
                            + ": " + actual[phase]);
                }
                if (AudioIds443.sound(actual[phase]) < 0) {
                    throw new AssertionError("Crypt NPC " + id
                            + " uses an unmapped revision-443 sound " + actual[phase]);
                }
            }
        }
        // Ahrim's MagicCombatAttack uses the spell's cast audio instead of
        // an NPC melee sound. Preserve the four real spell cast effects.
        SpellDefinition[] ahrimSpells = {
            SpellDefinition.FIRE_WAVE, SpellDefinition.CONFUSE,
            SpellDefinition.WEAKEN, SpellDefinition.CURSE
        };
        for (SpellDefinition spell : ahrimSpells) {
            if (AudioIds443.sound(spell.getCastSoundId()) < 0)
                throw new AssertionError("Unmapped Ahrim spell audio: " + spell);
        }
        for (int id = 2025; id <= 2030; id++) {
            Npc brother = new Npc(id);
            if (AudioIds443.sound(brother.getHitSoundId()) < 0
                    || AudioIds443.sound(brother.getDeathSoundId()) < 0)
                throw new AssertionError("Missing brother hit/death sound: " + id);
            if (id != 2025 && AudioIds443.sound(brother.getAttackSoundId()) < 0)
                throw new AssertionError("Missing brother attack sound: " + id);
        }
        // Existing cache sounds must be preserved and unrelated NPCs must not
        // gain synthesized combat audio.
        Npc normalRat = new Npc(86);
        if (normalRat.getAttackSoundId() != 17 || normalRat.getHitSoundId() != 16)
            throw new AssertionError("Crypt fallback changed non-Barrows NPC sounds");
        if (BarrowsCryptCombatSounds.attack(9999, -1) != -1)
            throw new AssertionError("Crypt fallback leaked to unrelated NPCs");
        System.out.println("BARROWS AUDIO all six brothers and seven crypt NPCs mapped to 443 assets");
    }

    private static void verifyPlacement(Position origin, int npcSize) {
        Position chosen = BarrowsNpcSpawnPlacement.findSafeSpawn(origin, origin, npcSize);
        if (chosen == null) throw new AssertionError("No reachable spawn near " + origin
                + " for " + npcSize + "x" + npcSize + " NPC");
        if (origin.equals(chosen)) throw new AssertionError("Spawn overlaps player at " + origin);
        if (chosen.getPlane() != origin.getPlane()) throw new AssertionError("Wrong spawn plane " + chosen);
        for (int dx = 0; dx < npcSize; dx++) {
            for (int dy = 0; dy < npcSize; dy++) {
                if ((WalkingCollisionMap.getTileFlags(chosen.getX()+dx, chosen.getY()+dy,
                        chosen.getPlane()) & 0x1280100) != 0)
                    throw new AssertionError("NPC footprint enters wall " + chosen);
            }
        }
    }

    private static void verifyDoorApproaches() throws Exception {
        String src = new String(Files.readAllBytes(Paths.get(
                "src/main/java/com/rs2/model/gameplay/barrows/BarrowsManager.java")),
                StandardCharsets.UTF_8);
        Pattern door = Pattern.compile("openDoubleDoorPair\\(([^)]*)\\)");
        Matcher match = door.matcher(src);
        int doorPairs = 0, openApproaches = 0;
        while (match.find()) {
            doorPairs++;
            String[] values = match.group(1).split(",");
            if (values.length != 7) throw new AssertionError("Unexpected door data " + match.group(1));
            int x1=Integer.parseInt(values[2].trim()), y1=Integer.parseInt(values[3].trim());
            int x2=Integer.parseInt(values[4].trim()), y2=Integer.parseInt(values[5].trim());
            for (int sign : new int[]{-1, 1}) {
                int x = x1 == x2 ? x1 + sign : x1;
                int y = y1 == y2 ? y1 + sign : y1;
                if (!BarrowsNpcSpawnPlacement.isOpen(x, y, 0)) continue;
                openApproaches++;
                // Every open door approach needs a reachable spawn; giant
                // crypt rats require a 2x2 footprint and are checked too.
                verifyPlacement(new Position(x, y, 0), 1);
                Position large = BarrowsNpcSpawnPlacement.findSafeSpawn(
                        new Position(x,y,0), new Position(x,y,0), 2);
                if (large != null) {
                    for (int dx=0;dx<2;dx++) for (int dy=0;dy<2;dy++) {
                        if (!BarrowsNpcSpawnPlacement.isOpen(large.getX()+dx,large.getY()+dy,0))
                            throw new AssertionError("Giant crypt rat inside a wall: "+large);
                    }
                }
            }
        }
        System.out.println("BARROWS DOORS pairs=" + doorPairs + " openSides=" + openApproaches);
        if (doorPairs != 32 || openApproaches < 20) {
            throw new AssertionError("Barrows tunnel door enumeration failed");
        }
    }

    private static void verifySyntheticBlockedAndWallCases() throws Exception {
        // Inject a miniature 443 collision region to test a narrow passage and
        // a completely surrounded doorway, then restore the real cache map.
        int x=3533, y=9711, regionId=(x>>6<<8)|(y>>6);
        Field lookupField=WalkingCollisionMap.class.getDeclaredField("regionLookup");
        lookupField.setAccessible(true);
        WalkingCollisionMap[] lookup=(WalkingCollisionMap[])lookupField.get(null);
        WalkingCollisionMap original=lookup[regionId];
        Constructor<WalkingCollisionMap> ctor=WalkingCollisionMap.class
                .getDeclaredConstructor(int.class);
        ctor.setAccessible(true);
        WalkingCollisionMap synthetic=ctor.newInstance(regionId);
        Field flagsField=WalkingCollisionMap.class.getDeclaredField("tileFlags");
        flagsField.setAccessible(true);
        int[][][] masks=(int[][][])flagsField.get(synthetic);
        int[][] floor=new int[64][64];
        masks[0]=floor;
        lookup[regionId]=synthetic;
        try {
            for(int[] row:floor)java.util.Arrays.fill(row,0x200000);
            floor[x&63][y&63]=0;
            // A two-tile northern passage is the only viable exit.
            floor[x&63][(y+1)&63]=0;
            floor[x&63][(y+2)&63]=0;
            Position center=new Position(x,y,0);
            Position safe=BarrowsNpcSpawnPlacement.findSafeSpawn(center,center,1);
            if(safe==null||safe.getX()!=x||safe.getY()!=y+2)
                throw new AssertionError("Failed to choose reachable open passage " + safe);

            for(int[] row:floor)java.util.Arrays.fill(row,0x200000);
            floor[x&63][y&63]=0;
            if(BarrowsNpcSpawnPlacement.findSafeSpawn(center,center,1)!=null)
                throw new AssertionError("Spawned into a sealed-off wall");

            for(int[] row:floor)java.util.Arrays.fill(row,0);
            // Tile is clear, but a blocking wall edge must prevent travel.
            floor[(x+1)&63][y&63]=128;
            Position result=BarrowsNpcSpawnPlacement.findSafeSpawn(center,center,1);
            if(result==null||result.equals(center))
                throw new AssertionError("Did not reject an occupied player tile");
            for(int dx=0;dx<2;dx++)for(int dy=0;dy<2;dy++){
                if(dx==0 && dy==0)continue;
                floor[(x+dx)&63][(y+dy)&63]=0x200000;
            }
            Position large=BarrowsNpcSpawnPlacement.findSafeSpawn(center,center,2);
            if (large != null) {
                if (large.getX() <= x && x < large.getX()+2
                        && large.getY() <= y && y < large.getY()+2)
                    throw new AssertionError("Large monster overlaps player");
                for (int dx=0; dx<2; dx++) for (int dy=0; dy<2; dy++) {
                    if (!BarrowsNpcSpawnPlacement.isOpen(large.getX()+dx, large.getY()+dy, 0))
                        throw new AssertionError("Large monster straddles blocked footprint");
                }
            }
        } finally {
            lookup[regionId]=original;
        }
        System.out.println("BARROWS SYNTHETIC blocked/wall/size cases passed");
    }

    private static int u16(byte[] d,int offset) {
        return (d[offset] & 255) << 8 | d[offset+1] & 255;
    }
}
