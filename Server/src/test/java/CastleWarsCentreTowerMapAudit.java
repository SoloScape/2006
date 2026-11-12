import com.rs2.ServerSettings;
import com.rs2.cache.js5.WorldMaps;
import com.rs2.model.objects.ObjectDefinition;
import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.EntityTargetMovement;
import com.rs2.model.player.Player;
import com.rs2.model.combat.CombatType;
import com.rs2.model.skill.magic.SpellDefinition;
import java.lang.reflect.Field;
import java.util.Map;
import com.rs2.model.gameplay.castlewars.CastleWarsManager;
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
        // The real 443 plane-1 bridge flags, after clipping translation,
        // define exactly which plane-0 tiles are the two tower roofs.
        int towerTiles = 0;
        for (int y = 3098; y <= 3109; ++y) {
            for (int x = 2394; x <= 2405; ++x) {
                boolean bridge = (tileSettings[1][x - 2368][y - 3072] & 2) != 0;
                boolean actualTop = CastleWarsManager.isMiddleIslandTowerTop(
                        new Position(x, y, 0));
                if (bridge != actualTop) {
                    throw new AssertionError("443 tower flag differs from combat footprint at "
                            + x + "," + y + ": map=" + bridge + ", combat=" + actualTop);
                }
                if (bridge) ++towerTiles;
            }
        }
        if (towerTiles != 40) throw new AssertionError("Expected 40 actual 443 raised tower tiles, got " + towerTiles);
        verifyCombatOnReal443Map();
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

    /**
     * Unlike the isolated synthetic reach test, this loads the real 443
     * collision maps and searches for actual exposed firing lanes from
     * BOTH towers. A false-positive square between the towers cannot pass.
     */
    @SuppressWarnings("unchecked")
    private static void verifyCombatOnReal443Map() throws Exception {
        Field field = CastleWarsManager.class.getDeclaredField("gamePlayers");
        field.setAccessible(true);
        Map<Player, CastleWarsManager.Team> teams =
                (Map<Player, CastleWarsManager.Team>)field.get(null);
        Player upper = new Player(null);
        Player ground = new Player(null);
        upper.isBot = true; ground.isBot = true;
        upper.botEnabled = true; ground.botEnabled = true;
        upper.botPrimaryCombatStyle = 2; ground.botPrimaryCombatStyle = 2;
        upper.botActiveCombatStyle = 2; ground.botActiveCombatStyle = 2;
        upper.setQueuedCombatSpell(SpellDefinition.WIND_STRIKE);
        ground.setQueuedCombatSpell(SpellDefinition.WIND_STRIKE);
        upper.setSize(1); ground.setSize(1);
        upper.setIndex(75); ground.setIndex(76);
        upper.setEncodedIndex(32768+75); ground.setEncodedIndex(32768+76);
        Player oldUpper = World.players[75], oldGround = World.players[76];
        World.players[75] = upper; World.players[76] = ground;
        teams.put(upper, CastleWarsManager.Team.SARADOMIN);
        teams.put(ground, CastleWarsManager.Team.ZAMORAK);
        int[] valid = new int[2];
        String[] examples = new String[2];
        try {
            for (int y = 3098; y <= 3109; ++y) {
                for (int x = 2394; x <= 2405; ++x) {
                    Position tower = new Position(x, y, 0);
                    if (!CastleWarsManager.isMiddleIslandTowerTop(tower)) continue;
                    if ((WalkingCollisionMap.getTileFlags(x,y,0) & 0x1280100) != 0) continue;
                    int whichTower = y <= 3102 ? 0 : 1;
                    upper.setPosition(tower);
                    for (int by = 3093; by <= 3115; ++by) {
                        for (int bx = 2390; bx <= 2409; ++bx) {
                            Position groundTile = new Position(bx,by,0);
                            if (CastleWarsManager.isMiddleIslandTowerTop(groundTile)
                                    || (WalkingCollisionMap.getTileFlags(bx,by,0)
                                            & 0x1280100) != 0) continue;
                            // Evaluate shots within the normal bow range,
                            // not arbitrary 15-tile sightlines.
                            if (Math.max(Math.abs(x - bx), Math.abs(y - by)) > 8)
                                continue;
                            ground.setPosition(groundTile);
                            if (!CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground)
                                    || !CastleWarsManager.hasCastleWarsProjectileLineOfSight(upper, ground)
                                    || !CastleWarsManager.hasCastleWarsProjectileLineOfSight(ground, upper))
                                continue;
                            for (CombatType type : new CombatType[]{CombatType.MAGIC, CombatType.RANGED}) {
                                if (!EntityTargetMovement.canReachTarget(upper,ground,10,type)
                                        || !EntityTargetMovement.canReachTarget(ground,upper,10,type)) {
                                    throw new AssertionError("Real 443 tower lane fails typed attack "
                                            + tower+" -> "+groundTile+" style="+type);
                                }
                            }
                            if (!EntityTargetMovement.canReachTarget(upper,ground,10)
                                    || !EntityTargetMovement.canReachTarget(ground,upper,10)) {
                                throw new AssertionError("Real 443 mage chases off tower "
                                        + tower+" -> "+groundTile);
                            }
                            ++valid[whichTower];
                            if (examples[whichTower] == null) examples[whichTower] =
                                    tower+" -> "+groundTile;
                        }
                    }
                }
            }
            System.out.println("REAL-443-TOWER-SHOTS south="+valid[0]+" north="+valid[1]
                    + " firstSouth="+examples[0]+" firstNorth="+examples[1]);
            if (valid[0] < 10 || valid[1] < 10) {
                throw new AssertionError("443 map has no practical projectile lanes for one "
                        + "or both middle-island towers");
            }
        } finally {
            teams.remove(upper);teams.remove(ground);
            World.players[75]=oldUpper;World.players[76]=oldGround;
            upper.setIndex(-1);ground.setIndex(-1);
        }
    }
}
