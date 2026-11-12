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
 * One Strange Old Man walks to the six surface mounds, with short digging
 * pauses. The route and timing are reconstructed, not verified 2006 footage.
 */
public final class BarrowsOldManPatrol {
    public static final int NPC_ID = 2024;
    private static final int DIG_ANIMATION = 831;
    // The native client can loop 831 until a new animation update is sent.
    // Bound the pose independently of route progress or nearby players.
    private static final int DIG_ANIMATION_TICKS = 4;
    private static final int MAX_MOUND_DISTANCE = 3;
    private static final int BLOCKED_TILE = 0x1280100;
    private static final int[][] MOUND_CENTRES = {
        {3557, 3298}, // northwest
        {3553, 3283}, // southwest
        {3565, 3276}, // south
        {3578, 3284}, // southeast
        {3574, 3298}, // northeast
        {3565, 3290}  // centre
    };

    private static final Map<Npc, PatrolState> STATES =
            Collections.synchronizedMap(new WeakHashMap<Npc, PatrolState>());

    private static final class PatrolState {
        int nextMound;
        int destinationX;
        int destinationY;
        boolean travelling;
        boolean digging;
        int digAnimationTicks;
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

    private static int distance(Position a, int x, int y) {
        return Math.max(Math.abs(a.getX() - x), Math.abs(a.getY() - y));
    }

    private static void beginDigging(Npc npc, PatrolState state) {
        npc.getUpdateState().setAnimation(DIG_ANIMATION);
        state.digging = true;
        state.digAnimationTicks = DIG_ANIMATION_TICKS;
        state.travelling = false;
        state.nextMound = (state.nextMound + 1) % moundCount();
        state.pauseTicks = 8 + GameUtil.randomInt(12);
    }

    /**
     * Runs on every NPC logic tick, even when a conversation, movement lock,
     * loss of nearby players or an interrupted route prevents patrol progress.
     * The temporary digging animation must never be held indefinitely.
     */
    public static void tickAnimation(Npc npc) {
        if (npc == null || npc.getNpcId() != NPC_ID) return;
        PatrolState state = STATES.get(npc);
        if (state == null || !state.digging) return;
        if (--state.digAnimationTicks <= 0) {
            npc.getUpdateState().setAnimation(-1);
            state.digging = false;
            state.digAnimationTicks = 0;
        }
    }

    /**
     * @return true when this is our surface Old Man and generic roaming
     * should not be used.
     */
    public static boolean process(Npc npc) {
        if (npc == null || npc.getNpcId() != NPC_ID
                || !isBarrowsSurface(npc.getSpawnPosition())) return false;

        if (!World.hasNearbyNonBotPlayer(npc)
                || npc.isMovementLocked() || npc.isStunned()) return true;

        // PathFinder creates actual walking steps. Do not replace them,
        // replay an animation, or advance the patrol while a route is active.
        if (!npc.getMovementQueue().getSteps().isEmpty()) return true;

        PatrolState state = STATES.get(npc);
        if (state == null) {
            state = new PatrolState();
            STATES.put(npc, state);
        }

        if (state.travelling) {
            state.travelling = false;
            Position mound = moundTarget(state.nextMound);
            // A collision or an interrupted route must never count as a
            // visit merely because it stopped up to ten tiles from a mound.
            if (distance(npc.getPosition(), state.destinationX, state.destinationY) <= 1
                    && distance(npc.getPosition(), mound.getX(), mound.getY())
                            <= MAX_MOUND_DISTANCE) {
                beginDigging(npc, state);
            } else {
                state.pauseTicks = 4; // retry the same mound on foot
            }
            return true;
        }

        if (state.pauseTicks > 0) {
            state.pauseTicks--;
            return true;
        }

        // tickAnimation(), called before NPC interaction/route gates, has
        // already released the digging pose while the NPC paused here.

        Position mound = moundTarget(state.nextMound);
        // These mound centres can be blocked by scenery. Route to an exact
        // reachable tile at most three tiles from the centre, rather than
        // PathFinder's broad ten-tile approximate destination.
        for (int radius = 0; radius <= MAX_MOUND_DISTANCE; radius++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dx = -radius; dx <= radius; dx++) {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != radius) continue;
                    int x = mound.getX() + dx;
                    int y = mound.getY() + dy;
                    if ((com.rs2.util.path.WalkingCollisionMap.getTileFlags(x, y, 0)
                            & BLOCKED_TILE) != 0) continue;
                    if (!PathFinder.findPath(npc, x, y, false, 1, 1)) continue;
                    if (npc.getMovementQueue().getSteps().isEmpty()) {
                        // Already standing at an exact reachable destination.
                        beginDigging(npc, state);
                    } else {
                        state.destinationX = x;
                        state.destinationY = y;
                        state.travelling = true;
                    }
                    return true;
                }
            }
        }

        // No safe path currently exists. Remain where we are; never use
        // moveTo/teleport as a fallback for a failed walk.
        state.pauseTicks = 8;
        return true;
    }
}
