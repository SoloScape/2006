package com.rs2.model.gameplay.barrows;

import com.rs2.model.Position;
import com.rs2.model.npc.Npc;
import com.rs2.util.RectangularArea;

/**
 * Limits 2006-style temporary door encounters without affecting the
 * permanent monsters loaded from the world spawn table.
 *
 * A contemporary player report (12 July 2006) confirms that temporary
 * monsters disappeared after a while and that rooms had an enemy limit:
 * https://forum.tip.it/topic/52943-monsters-leaving-in-barrows/
 *
 * Neither the precise lifetime nor numerical room limit is independently
 * documented for 2006. These named, adjustable values use later preserved
 * Barrows behaviour rather than pretending the numbers were verified.
 */
public final class BarrowsTunnelPopulation {
    /** Later preserved room limit; provisional for an exact 2006 recreation. */
    public static final int MAX_LIVE_ENEMIES_PER_ROOM = 11;
    /** 600ms ticks; provisional 60-second maximum life of a door summon. */
    public static final int DOOR_SUMMON_LIFETIME_TICKS = 100;
    /** Door tiles can sit just outside the nine actual room rectangles. */
    private static final int MAX_CORRIDOR_GAP = 6;

    private BarrowsTunnelPopulation() { }

    public static boolean atCapacity(Position destination,
                                     BarrowsTunnelRoom[] rooms, Npc[] npcs) {
        return countLivingEnemies(destination, rooms, npcs) >= MAX_LIVE_ENEMIES_PER_ROOM;
    }

    public static int countLivingEnemies(Position destination,
                                         BarrowsTunnelRoom[] rooms, Npc[] npcs) {
        int roomId = nearestRoomId(destination, rooms);
        if (roomId < 0 || npcs == null) return 0;
        int count = 0;
        for (Npc npc : npcs) {
            if (npc == null || !npc.isActive() || npc.isDead()) continue;
            int id = npc.getNpcId();
            // Six brothers plus the seven original tunnel monster types.
            if (id < 2025 || id > 2037) continue;
            if (nearestRoomId(npc.getPosition(), rooms) == roomId) count++;
        }
        return count;
    }

    /**
     * Every position within a room belongs to that room. For door approaches
     * in narrow corridors, choose the nearest room by distance to its edges.
     * This keeps counts local rather than counting the entire nine-room maze.
     */
    public static int nearestRoomId(Position position, BarrowsTunnelRoom[] rooms) {
        if (position == null || position.getPlane() != 0 || rooms == null) return -1;
        int chosenId = -1;
        int bestDistance = Integer.MAX_VALUE;
        for (BarrowsTunnelRoom room : rooms) {
            if (room == null || room.roomBounds == null) continue;
            RectangularArea bounds = room.roomBounds;
            int dx = Math.max(0, Math.max(bounds.getMinX() - position.getX(),
                                         position.getX() - bounds.getMaxX()));
            int dy = Math.max(0, Math.max(bounds.getMinY() - position.getY(),
                                         position.getY() - bounds.getMaxY()));
            int distance = dx + dy;
            if (distance < bestDistance) {
                bestDistance = distance;
                chosenId = room.roomId;
            }
        }
        return bestDistance <= MAX_CORRIDOR_GAP ? chosenId : -1;
    }
}
