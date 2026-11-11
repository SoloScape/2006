package com.rs2.model.gameplay.barrows;

import java.util.ArrayList;
import java.util.List;

/**
 * Barrows tunnel door encounter table.
 *
 * Unlike the fixed room spawns, opening a tunnel door can summon a bloodworm,
 * a normal crypt rat, a skeleton, or an undefeated Barrows brother.
 *
 * 12/128 brothers, 52/128 skeletons, 32/128 bloodworms, 32/128 crypt rats
 * match the documented legacy encounter table. A brother roll with no
 * eligible brother falls back to a skeleton, so valid door openings never
 * attempt to spawn a defeated or already-present brother.
 */
public final class BarrowsTunnelEncounter {
    public static final int ROLL_COUNT = 128;
    public static final int BROTHER_ROLLS = 12;
    public static final int SKELETON_ROLLS = 52;
    public static final int BLOODWORM_ROLLS = 32;
    public static final int RAT_ROLLS = 32;
    public static final int BLOODWORM = 2031;
    public static final int CRYPT_RAT = 2032;
    public static final int[] BROTHER_NPC_IDS = {2030, 2029, 2028, 2027, 2026, 2025};
    private static final int SKELETON_A = 2036;
    private static final int SKELETON_B = 2037;

    private BarrowsTunnelEncounter() { }

    /** The two masks use the same brother ordering as BarrowsManager. */
    public static int[] eligibleBrothers(boolean[] killed, boolean[] active) {
        if (killed == null || active == null
                || killed.length < BROTHER_NPC_IDS.length
                || active.length < BROTHER_NPC_IDS.length) {
            throw new IllegalArgumentException("Barrows brother state must contain six entries");
        }
        List<Integer> candidates = new ArrayList<Integer>(BROTHER_NPC_IDS.length);
        for (int i = 0; i < BROTHER_NPC_IDS.length; i++) {
            if (!killed[i] && !active[i]) candidates.add(BROTHER_NPC_IDS[i]);
        }
        int[] ids = new int[candidates.size()];
        for (int i = 0; i < ids.length; i++) ids[i] = candidates.get(i);
        return ids;
    }

    /**
     * Pure encounter roll, intended to be tested exhaustively for every roll.
     * brotherIndex is chosen uniformly from [0, eligible.length) by the caller.
     */
    public static int npcForRoll(int roll, int skeletonVariant,
                                 int brotherIndex, int[] eligible) {
        if (roll < 0 || roll >= ROLL_COUNT) {
            throw new IllegalArgumentException("Door encounter roll must be between 0 and 127");
        }
        if (roll < BROTHER_ROLLS && eligible != null && eligible.length != 0) {
            if (brotherIndex < 0 || brotherIndex >= eligible.length) {
                throw new IllegalArgumentException("Invalid eligible brother index");
            }
            return eligible[brotherIndex];
        }
        if (roll < BROTHER_ROLLS + SKELETON_ROLLS) {
            return (skeletonVariant & 1) == 0 ? SKELETON_A : SKELETON_B;
        }
        if (roll < BROTHER_ROLLS) {
            // No eligible brother remained; retain a monster encounter.
            return (skeletonVariant & 1) == 0 ? SKELETON_A : SKELETON_B;
        }
        if (roll < BROTHER_ROLLS + SKELETON_ROLLS + BLOODWORM_ROLLS) {
            return BLOODWORM;
        }
        return CRYPT_RAT;
    }

    public static int brotherIndexForNpcId(int npcId) {
        for (int i = 0; i < BROTHER_NPC_IDS.length; i++) {
            if (BROTHER_NPC_IDS[i] == npcId) return i;
        }
        return -1;
    }
}
