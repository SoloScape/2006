package com.rs2.model.gameplay.barrows;

import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.npc.Npc;
import com.rs2.model.player.Player;
import com.rs2.util.GameUtil;
import com.rs2.util.path.WalkingCollisionMap;
import java.util.ArrayDeque;

/**
 * Chooses collision-safe, reachable spawns for Barrows tunnel encounters and
 * sarcophagus brothers. Never registers an NPC on a blocked, unreachable,
 * occupied or player-landing tile.
 */
public final class BarrowsNpcSpawnPlacement {
    private static final int MAX_STEPS = 6;
    // Same occupancy/floor mask used by the walking collision flags. Wall-edge
    // restrictions are checked separately with canTravelBetween().
    private static final int BLOCKED_TILE = 0x1280100;
    private static final int[][] DIRECTIONS = {{1,0},{0,1},{-1,0},{0,-1}};

    private BarrowsNpcSpawnPlacement() { }

    /**
     * arrival is the tile the player will occupy after walking through a door
     * (or their current tile for a sarcophagus). before is the player's current
     * tile, which must also remain unoccupied.
     */
    public static Position findSafeSpawn(Position arrival, Position before, int npcSize) {
        if (arrival == null || before == null || arrival.getPlane() != before.getPlane()) {
            return null;
        }
        Position candidate = search(arrival, arrival, before, npcSize);
        if (candidate == null && !arrival.equals(before)) {
            // Closed door collisions can still be present when the server
            // queues the step. Spawn on the player's *reachable* side instead.
            candidate = search(before, arrival, before, npcSize);
        }
        return candidate;
    }

    private static Position search(Position origin, Position arrival, Position before, int size) {
        if (!isOpen(origin.getX(), origin.getY(), origin.getPlane())) return null;
        int plane = origin.getPlane();
        int width = 2 * MAX_STEPS + 1;
        boolean[][] seen = new boolean[width][width];
        ArrayDeque<Position> queue = new ArrayDeque<Position>();
        seen[MAX_STEPS][MAX_STEPS] = true;
        queue.add(origin);
        int bestScore = Integer.MAX_VALUE;
        int ties = 0;
        Position selected = null;
        while (!queue.isEmpty()) {
            Position current = queue.removeFirst();
            int steps = Math.abs(current.getX() - origin.getX())
                    + Math.abs(current.getY() - origin.getY());
            if (steps > 0 && fits(current, size, arrival, before)) {
                // Prefer a little breathing room rather than stacking the NPC
                // directly against the door, player or sarcophagus.
                int score = Math.abs(steps - 2) * 10 + steps;
                if (score < bestScore) {
                    bestScore = score;
                    selected = current;
                    ties = 1;
                } else if (score == bestScore && GameUtil.randomInt(++ties) == 0) {
                    selected = current;
                }
            }
            if (steps >= MAX_STEPS) continue;
            for (int[] direction : DIRECTIONS) {
                int x = current.getX() + direction[0];
                int y = current.getY() + direction[1];
                int ix = x - origin.getX() + MAX_STEPS;
                int iy = y - origin.getY() + MAX_STEPS;
                if (ix < 0 || iy < 0 || ix >= width || iy >= width || seen[ix][iy]) continue;
                seen[ix][iy] = true;
                if (!isOpen(x, y, plane)) continue;
                if (!WalkingCollisionMap.canTravelBetween(
                        current.getX(), current.getY(), x, y, plane, 1, 1)) continue;
                queue.addLast(new Position(x, y, plane));
            }
        }
        return selected;
    }

    public static boolean isOpen(int x, int y, int plane) {
        return isBarrowsTile(x, y, plane)
                && (WalkingCollisionMap.getTileFlags(x, y, plane) & BLOCKED_TILE) == 0;
    }

    private static boolean isBarrowsTile(int x, int y, int plane) {
        if (plane == 0) return x >= 3527 && x <= 3576 && y >= 9670 && y <= 9719;
        if (plane == 3) return x >= 3526 && x <= 3583 && y >= 9680 && y <= 9723;
        return false;
    }

    private static boolean fits(Position candidate, int size, Position arrival, Position before) {
        size = Math.max(1, size);
        int x = candidate.getX(), y = candidate.getY(), plane = candidate.getPlane();
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                if (!isOpen(x + dx, y + dy, plane)) return false;
            }
        }
        // A large NPC cannot straddle a wall which divides otherwise clear
        // tiles; the entire footprint must be a connected walkable surface.
        for (int dx = 0; dx < size; dx++) {
            for (int dy = 0; dy < size; dy++) {
                if (dx > 0 && !WalkingCollisionMap.canTravelBetween(
                        x + dx - 1, y + dy, x + dx, y + dy, plane, 1, 1)) return false;
                if (dy > 0 && !WalkingCollisionMap.canTravelBetween(
                        x + dx, y + dy - 1, x + dx, y + dy, plane, 1, 1)) return false;
            }
        }
        if (overlaps(x, y, size, arrival.getX(), arrival.getY(), 1)
                || overlaps(x, y, size, before.getX(), before.getY(), 1)) return false;

        for (Npc npc : World.getNpcs()) {
            if (npc == null || !npc.isActive() || npc.isDead()
                    || npc.getPosition().getPlane() != plane) continue;
            if (overlaps(x, y, size, npc.getPosition().getX(),
                    npc.getPosition().getY(), Math.max(1, npc.getSize()))) return false;
        }
        for (Player player : World.getPlayers()) {
            if (player == null || player.getPosition().getPlane() != plane) continue;
            if (overlaps(x, y, size, player.getPosition().getX(),
                    player.getPosition().getY(), Math.max(1, player.getSize()))) return false;
        }
        return true;
    }

    private static boolean overlaps(int ax, int ay, int as, int bx, int by, int bs) {
        return ax < bx + bs && bx < ax + as && ay < by + bs && by < ay + as;
    }
}
