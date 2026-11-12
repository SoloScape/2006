import com.rs2.model.EntityTargetMovement;
import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.combat.CombatType;
import com.rs2.model.gameplay.castlewars.CastleWarsManager;
import com.rs2.model.player.Player;
import com.rs2.model.player.PlayerUpdateTask;
import com.rs2.model.player.ModernPlayerUpdateTask;
import com.rs2.model.skill.magic.SpellDefinition;
import com.rs2.util.path.ProjectileCollisionMap;
import com.rs2.util.path.WalkingCollisionMap;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * The actual 443 centre-island tower tops and ground are both on plane 0.
 * Plane-1 bridge flags move the raised walkway collision down to plane 0.
 */
public final class CastleWarsMiddleIslandTowerCombatChecks {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Map<Player, CastleWarsManager.Team> teams =
                (Map<Player, CastleWarsManager.Team>)field(
                        CastleWarsManager.class, "gamePlayers").get(null);
        Field projectileRegions = field(ProjectileCollisionMap.class, "regions");
        Object oldProjectileRegions = projectileRegions.get(null);
        Field walkingRegions = field(WalkingCollisionMap.class, "regionLookup");
        Object oldWalkingRegions = walkingRegions.get(null);
        Constructor<ProjectileCollisionMap> projectileCtor =
                ProjectileCollisionMap.class.getDeclaredConstructor(int.class);
        projectileCtor.setAccessible(true);
        Constructor<WalkingCollisionMap> walkingCtor =
                WalkingCollisionMap.class.getDeclaredConstructor(int.class);
        walkingCtor.setAccessible(true);
        Method blockProjectile = ProjectileCollisionMap.class.getDeclaredMethod(
                "setTileFlag", int.class, int.class, int.class, int.class);
        blockProjectile.setAccessible(true);
        Method blockWalk = WalkingCollisionMap.class.getDeclaredMethod(
                "setTileFlag", int.class, int.class, int.class, int.class);
        blockWalk.setAccessible(true);
        int regionId = (37 << 8) | 48;
        Player upper = new Player(null);
        Player ground = new Player(null);
        upper.isBot = true;
        ground.isBot = true;
        upper.setSize(1); ground.setSize(1);
        upper.setIndex(75); ground.setIndex(76);
        upper.setEncodedIndex(32768 + 75); ground.setEncodedIndex(32768 + 76);
        Player beforeUpper = World.players[75];
        Player beforeGround = World.players[76];
        World.players[75] = upper;
        World.players[76] = ground;
        try {
            projectileRegions.set(null, new ProjectileCollisionMap[]{
                    projectileCtor.newInstance(regionId)});
            WalkingCollisionMap[] lookups = new WalkingCollisionMap[65536];
            lookups[regionId] = walkingCtor.newInstance(regionId);
            walkingRegions.set(null, lookups);
            teams.put(upper, CastleWarsManager.Team.SARADOMIN);
            teams.put(ground, CastleWarsManager.Team.ZAMORAK);

            upper.setPosition(new Position(2397, 3107, 0));
            ground.setPosition(new Position(2397, 3099, 0));
            require(!CastleWarsManager.isMiddleIslandTowerTop(new Position(2400, 3103, 0)),
                    "The crossing between towers is not a firing platform");
            require(!CastleWarsManager.isMiddleIslandTowerTop(new Position(2397, 3107, 1)),
                    "Plane 1 is decorative bridge geometry, not the actual tower collision");
            upper.setQueuedCombatSpell(SpellDefinition.WIND_STRIKE);
            ground.setQueuedCombatSpell(SpellDefinition.WIND_STRIKE);
            require(CastleWarsManager.isMiddleIslandTowerTop(upper.getPosition()),
                    "Central island top not recognized");
            require(CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Castle Wars tower and ground opponents not recognized");
            for (CombatType style : new CombatType[]{CombatType.RANGED, CombatType.MAGIC}) {
                require(EntityTargetMovement.canReachTarget(upper, ground, 10, style),
                        "Tower-to-ground " + style + " blocked");
                require(EntityTargetMovement.canReachTarget(ground, upper, 10, style),
                        "Ground-to-tower " + style + " blocked");
            }
            require(EntityTargetMovement.canReachTarget(upper, ground, 10),
                    "Tower mage movement keeps trying to walk downstairs");
            require(EntityTargetMovement.canReachTarget(ground, upper, 10),
                    "Ground mage movement keeps trying to climb the tower");
            require(CastleWarsManager.canSeeMiddleIslandTowerCombatant(upper, ground)
                            && CastleWarsManager.canSeeMiddleIslandTowerCombatant(ground, upper),
                    "Combatants must be visible across tower levels");
            for (Class<?> encoder : new Class<?>[]{PlayerUpdateTask.class,
                                                    ModernPlayerUpdateTask.class}) {
                String name = encoder == PlayerUpdateTask.class
                        ? "isWithinLocalPlayerViewport" : "isWithinViewport";
                Method viewport = encoder.getDeclaredMethod(name, Player.class, Player.class);
                viewport.setAccessible(true);
                require((Boolean)viewport.invoke(null, upper, ground)
                                && (Boolean)viewport.invoke(null, ground, upper),
                        encoder.getName() + " hides tower opponents on another plane");
            }
            require(!EntityTargetMovement.canReachTarget(upper, ground, 1, CombatType.MELEE)
                            && !EntityTargetMovement.canReachTarget(
                                    ground, upper, 1, CombatType.MELEE),
                    "Melee must never reach across middle island levels");

            // The nearest clipped parapet should not stop ranged/magic.
            blockProjectile.invoke(null, 2397, 3106, 0, 0x100);
            blockWalk.invoke(null, 2397, 3106, 0, 0x100);
            require(CastleWarsManager.hasCastleWarsProjectileLineOfSight(upper, ground),
                    "Tower lip blocks every shot");
            // An actual second wall farther along must block both ways.
            blockProjectile.invoke(null, 2397, 3102, 0, 0x100);
            blockWalk.invoke(null, 2397, 3102, 0, 0x100);
            require(!CastleWarsManager.hasCastleWarsProjectileLineOfSight(upper, ground)
                            && !CastleWarsManager.hasCastleWarsProjectileLineOfSight(ground, upper),
                    "Middle island tower can shoot through a courtyard building");
            require(!EntityTargetMovement.canReachTarget(upper, ground, 10, CombatType.RANGED),
                    "Projectile collision not enforced on tower shooting lane");

            ground.setPosition(new Position(2420, 3078, 0));
            require(!CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Tower exception reaches a different castle");
            ground.setPosition(new Position(2397, 3099, 2));
            require(!CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Tower exception reaches an arbitrary different floor");
            ground.setPosition(new Position(2397, 3099, 1));
            require(!CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Tower exception leaks to a different map plane");
            ground.setPosition(new Position(2397, 3099, 0));
            require(CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Real same-plane tower exception not restored");
            teams.put(ground, CastleWarsManager.Team.SARADOMIN);
            require(!CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Tower exception permits friendly fire");
            teams.put(ground, CastleWarsManager.Team.ZAMORAK);
            // The second real tower must allow attacks in both directions.
            upper.setPosition(new Position(2402, 3100, 0));
            ground.setPosition(new Position(2402, 3108, 0));
            require(CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Second central tower not identified");
            require(EntityTargetMovement.canReachTarget(upper, ground, 10, CombatType.RANGED)
                            && EntityTargetMovement.canReachTarget(ground, upper, 10, CombatType.MAGIC),
                    "Second central tower cannot exchange ranged and magic attacks");
            upper.setPosition(new Position(2500, 3103, 0));
            require(!CastleWarsManager.isMiddleIslandTowerCrossLevelPair(upper, ground),
                    "Tower exception leaks outside Castle Wars");
        } finally {
            teams.remove(upper); teams.remove(ground);
            World.players[75] = beforeUpper;
            World.players[76] = beforeGround;
            projectileRegions.set(null, oldProjectileRegions);
            walkingRegions.set(null, oldWalkingRegions);
        }
        System.out.println("Castle Wars middle island tower ranged, magic, floor and LoS checks passed");
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field f = owner.getDeclaredField(name);
        f.setAccessible(true);
        return f;
    }
    private static void require(boolean condition, String why) {
        if (!condition) throw new AssertionError(why);
    }
}
