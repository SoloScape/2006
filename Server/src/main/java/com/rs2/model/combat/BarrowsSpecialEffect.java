package com.rs2.model.combat;

import com.rs2.model.Entity;
import com.rs2.model.npc.Npc;
import com.rs2.model.player.Player;

/**
 * Original Barrows set-effect eligibility and proc selection.
 *
 * Period sources (2006) describe the six effects and their attack styles:
 * https://forum.tip.it/topic/24034-leesters-barrows-guide-edit-12-jul-2006-aow-discussion/
 * https://forum.tip.it/topic/52625-a-new-special-effect-for-torag-the-corrupteds-barrows-set/
 *
 * A 1-in-4 trigger is also documented by pre-2015 Jagex-era retrospectives:
 * https://forums.rs/en/380,381,122,65512837.html
 * Exact private 2006 server-side proc odds cannot be independently recovered.
 */
public final class BarrowsSpecialEffect {
    /** One of the four equally likely roll values activates the set effect. */
    public static final int PROC_ROLL_MAX_INCLUSIVE = 3;

    private BarrowsSpecialEffect() {
    }

    /**
     * Choose a special-effect ID after a single 0..3 roll.
     * -1 means no eligible effect. Dharok uses a continuous HP-based bonus
     * rather than a random proc.
     */
    public static int selectEffectId(Entity attacker, CombatType attackType, int roll) {
        if (attacker == null || attackType == null || roll != 0) return -1;

        if (attacker.isNpc()) {
            switch (((Npc)attacker).getNpcId()) {
                case 2027: return attackType == CombatType.MELEE ? 7 : -1; // Guthan
                case 2029: return attackType == CombatType.MELEE ? 8 : -1; // Torag
                case 2025: return attackType == CombatType.MAGIC ? 9 : -1; // Ahrim
                case 2028: return attackType == CombatType.RANGED ? 10 : -1; // Karil
                case 2030: return attackType == CombatType.MELEE ? 11 : -1; // Verac
                default: return -1;
            }
        }

        if (!attacker.isPlayer()) return -1;
        Player player = (Player)attacker;
        if (attackType == CombatType.MELEE) {
            if (player.isGuthanSetEffectActive()) return 7;
            if (player.isToragSetEffectActive()) return 8;
            if (player.isVeracSetEffectActive()) return 11;
        } else if (attackType == CombatType.MAGIC && player.isAhrimSetEffectActive()) {
            return 9;
        } else if (attackType == CombatType.RANGED && player.isKarilSetEffectActive()) {
            return 10;
        }
        return -1;
    }
}
