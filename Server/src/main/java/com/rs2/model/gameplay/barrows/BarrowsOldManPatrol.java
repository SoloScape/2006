package com.rs2.model.gameplay.barrows;

import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.npc.Npc;
import com.rs2.util.GameUtil;
import com.rs2.util.path.PathFinder;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * A single Strange Old Man visits each of the six surface mounds rather than
 * wandering within the default five-tile NPC spawn radius.
 *
 * The mound centres come from BarrowsManager's 443 surface digging areas.
 * The ordering, pauses and animation timing are reconstructed behaviour, not
 * claimed to be an exact 2006 Jagex patrol recording.
 */
public final class BarrowsOldManPatrol {
    public static final int NPC_ID = 2024;
    private static final int DIG_ANIMATION = 831;
    private static final int[][] MOUND_CENTRES = {
        {3557, 3298}, // northwest
        {3553, 3283}, // southwest
        {3565, 3276}, // south
        {3578, 3284}, // southeast
        {3574, 3298}, // northeast
        {3565, 3290}  // centre
    };

    // Weak keys ensure a despawned/replaced NPC does not retain patrol state.
    private static final Map<Npc, PatrolState> STATES =
            Collections.synchronizedMap(new WeakHashMap<Npc, PatrolState>());

    private static final class PatrolState {
        int nextMound;
        boolean travelling;
        int pauseTicks;
    }

    private BarrowsOldManPatrol() { }

    public static int moundCount() {
        return MOUND_CENTRES.length;
    }

    public static Position moundTarget(int index) {
        return new Position(MOUND_CENTRES[index][0], MOUND_CENTRES[index][1], 0);
    }

    public static boolean isBarrowsSurface(Position position) {
        return position != null && position.getPlane() == 0
                && position.getX() >= 3547 && position.getX() <= 3585
                && position.getY() >= 3270 && position.getY() <= 3306;
    }

    /**
     * @return true when this NPC is the surface Strange Old Man and its
     *         movement should not fall back to the generic roaming handler.
     */
    public static boolean process(Npc npc) {
        if (npc == null || npc.getNpcId() != NPC_ID
                || !isBarrowsSurface(npc.getSpawnPosition())) return false;

        // Do not move remote NPCs when the area has no real players.
        if (!World.hasNearbyNonBotPlayer(npc)
                || npc.isMovementLocked() || npc.isStunned()) return true;

        // A collision-checked route is already being followed.
        if (!npc.getMovementQueue().getSteps().isEmpty()) return true;

        PatrolState state = STATES.get(npc);
        if (state == null) {
            state = new PatrolState();
            STATES.put(npc, state);
        }

        if (state.travelling) {
            state.travelling = false;
            Position destination = moundTarget(state.nextMound);
            int distance = Math.max(
                    Math.abs(npc.getPosition().getX() - destination.getX()),
                    Math.abs(npc.getPosition().getY() - destination.getY()));
            if (distance <= 10) {
                // Even when the mound's centre is obstructed, the pathfinder
                // can choose the nearest walkable tile around it.
                npc.getUpdateState().setAnimation(DIG_ANIMATION);
                state.nextMound = (state.nextMound + 1) % moundCount();
                state.pauseTicks = 8 + GameUtil.randomInt(12);
            } else {
                // Interaction or a dynamic obstacle interrupted this route.
                // Try the same mound again, rather than falsely marking a visit.
                state.pauseTicks = 4;
            }
            return true;
        }

        if (state.pauseTicks > 0) {
            state.pauseTicks--;
            return true;
        }

        Position destination = moundTarget(state.nextMound);
        if (PathFinder.findPath(npc, destination.getX(), destination.getY(),
                true, 1, 1)
                && !npc.getMovementQueue().getSteps().isEmpty()) {
            state.travelling = true;
        } else {
            // A blocked mound must not trap the NPC in an endless retry loop.
            state.nextMound = (state.nextMound + 1) % moundCount();
            state.pauseTicks = 8;
        }
        return true;
    }
}
