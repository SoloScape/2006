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
 * are the documented OSRS-era odds used as a reference; the precise
 * probabilities in 2006 have not been independently verified.
 * A brother roll with no undefeated brother falls back to a skeleton.
 * Historical bug preserved deliberately: 2006 door rolls did not exclude an
 * already-live copy of a brother, so duplicates could appear in the tunnels.
 * See https://forum.tip.it/topic/35596-strange-barrows-or-hidden-update/
 * and Jagex's 22 November 2011 patch (which later prevented duplicate spawns).
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

    /**
     * Eligible for a tunnel door ambush if not yet defeated, even when another
     * living copy of the same brother is already nearby. The latter is the
     * documented 2006 duplicate-brother bug (fixed by Jagex in November 2011).
     * The chest/sarcophagus logic retains its separate active-brother checks.
     */
    public static int[] eligibleBrothers(boolean[] killed) {
        if (killed == null || killed.length < BROTHER_NPC_IDS.length) {
            throw new IllegalArgumentException("Barrows kill state must contain six entries");
        }
        List<Integer> candidates = new ArrayList<Integer>(BROTHER_NPC_IDS.length);
        for (int i = 0; i < BROTHER_NPC_IDS.length; i++) {
            if (!killed[i]) candidates.add(BROTHER_NPC_IDS[i]);
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
        if (roll < BROTHER_ROLLS) {
            if (eligible != null && eligible.length != 0) {
                if (brotherIndex < 0 || brotherIndex >= eligible.length) {
                    throw new IllegalArgumentException("Invalid eligible brother index");
                }
                return eligible[brotherIndex];
            }
            // No eligible brother remained; retain a monster encounter.
            return (skeletonVariant & 1) == 0 ? SKELETON_A : SKELETON_B;
        }
        if (roll < BROTHER_ROLLS + SKELETON_ROLLS) {
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
