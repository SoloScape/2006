package com.rs2.model.gameplay.barrows;

/**
 * Fallback combat sound effects for the native Barrows tunnel monsters.
 *
 * The legacy NPC metadata has no attack, hit or death sound IDs for 2031-2037.
 * Reuse sound sets from other NPCs with matching or similar creature animations
 * that are known to map to actual revision-443 JS5 effects:
 *
 * - Rat (NPC 86):     attack 17,  hit 16,  death 15.
 * - Spider (NPC 58):  attack 132, hit 135, death 137.
 * - Skeleton (NPC 90): attack 108, hit 110, death 109.
 *
 * The bloodworm and larger crypt-creature choices are approximations, NOT
 * authenticated 2006 sound IDs. Existing defined NPC sounds always take
 * priority, and Ahrim's attacks use their SpellDefinition cast sound IDs.
 */
public final class BarrowsCryptCombatSounds {
    private static final int RAT_ATTACK = 17, RAT_HIT = 16, RAT_DEATH = 15;
    private static final int SPIDER_ATTACK = 132, SPIDER_HIT = 135, SPIDER_DEATH = 137;
    private static final int SKELETON_ATTACK = 108, SKELETON_HIT = 110, SKELETON_DEATH = 109;

    private BarrowsCryptCombatSounds() { }

    public static int attack(int npcId, int configuredSoundId) {
        return configuredSoundId >= 0 ? configuredSoundId : fallback(npcId, 0);
    }

    public static int hit(int npcId, int configuredSoundId) {
        return configuredSoundId >= 0 ? configuredSoundId : fallback(npcId, 1);
    }

    public static int death(int npcId, int configuredSoundId) {
        return configuredSoundId >= 0 ? configuredSoundId : fallback(npcId, 2);
    }

    private static int fallback(int npcId, int type) {
        if (npcId < 2031 || npcId > 2037) return -1;
        if (npcId == 2032 || npcId == 2033) {
            return type == 0 ? RAT_ATTACK : type == 1 ? RAT_HIT : RAT_DEATH;
        }
        if (npcId == 2036 || npcId == 2037) {
            return type == 0 ? SKELETON_ATTACK : type == 1 ? SKELETON_HIT : SKELETON_DEATH;
        }
        // Bloodworms/crypt spiders have no verified 2006 creature sound data
        // in the old definition table. An existing mapped spider sound set
        // is preferable to silently discarding every attack and hit packet.
        return type == 0 ? SPIDER_ATTACK : type == 1 ? SPIDER_HIT : SPIDER_DEATH;
    }
}
