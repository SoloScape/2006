import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.combat.AttackBonusType;
import com.rs2.model.combat.AttackStyleDefinition;
import com.rs2.model.combat.AttackXpMode;
import com.rs2.model.combat.CombatAction;
import com.rs2.model.combat.CombatType;
import com.rs2.model.combat.hit.HitDefinition;
import com.rs2.model.combat.hit.HitType;
import com.rs2.model.combat.special.SpecialAttackDefinition;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;

/**
 * Exercises NPC Barrows special attacks through the live CombatAction.applyHit()
 * path, not only through a source text or dispatcher test.
 */
public final class BarrowsBrotherSpecialCombatChecks {
    private static final AttackStyleDefinition MELEE =
            new AttackStyleDefinition(CombatType.MELEE, AttackXpMode.AGGRESSIVE,
                    AttackBonusType.CRUSH);

    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();

        testGuthan();
        testTorag();
        testAhrim();
        testKaril();
        testDharokRemainsSeparate();
        testVeracRemainsSeparate();
        testHitRequirementsAndIsolation();
        testPlayerSetEffectsStillWork();

        System.out.println("Barrows brothers: all six combat hit paths and NPC proc protections passed.");
    }

    private static void testGuthan() {
        Player victim = victim();
        Npc guthan = brother(2027);
        require(guthan.getMaxHitpoints() >= 30, "Guthan health definition missing");
        guthan.setCurrentHitpoints(guthan.getMaxHitpoints() - 20);
        int before = guthan.getCurrentHitpoints();
        hit(guthan, victim, 7, 10, true);
        require(guthan.getCurrentHitpoints() == before + 10,
                "Guthan did not heal for a damaging melee hit");
        require(victim.getCurrentHitpoints() == 80,
                "Guthan's proc must not replace his normal hit");

        // A killing blow may only heal the health actually removed.
        Player lowHealth = victim();
        lowHealth.setCurrentHitpoints(3);
        int beforeOverkill = guthan.getCurrentHitpoints();
        hit(guthan, lowHealth, 7, 10, true);
        require(guthan.getCurrentHitpoints() == beforeOverkill + 3,
                "Guthan over-healed from damage greater than target's remaining HP");
    }

    private static void testTorag() {
        Player victim = victim();
        Npc torag = brother(2029);
        hit(torag, victim, 8, 10, true);
        require(victim.getRunEnergyPercent() == 76,
                "Torag's damaging hit did not drain run energy; actual="
                        + victim.getRunEnergyPercent() + " raw=" + victim.getRunEnergyRaw()
                        + " hp=" + victim.getCurrentHitpoints());
        require(victim.getCurrentHitpoints() == 80, "Torag did not deal normal hit damage");
    }

    private static void testAhrim() {
        Player victim = victim();
        Npc ahrim = brother(2025);
        hit(ahrim, victim, 9, 10, true);
        require(victim.getSkillManager().getCurrentLevels()[2] == 65,
                "Ahrim's damaging hit did not lower Strength by five");
        require(victim.getSkillManager().getCurrentLevels()[16] == 70,
                "Ahrim incorrectly lowered Agility");
    }

    private static void testKaril() {
        Player victim = victim();
        Npc karil = brother(2028);
        hit(karil, victim, 10, 10, true);
        require(victim.getSkillManager().getCurrentLevels()[16] == 56,
                "Karil's damaging hit did not lower Agility by 20 percent");
        require(victim.getSkillManager().getCurrentLevels()[2] == 70,
                "Karil incorrectly lowered Strength");
    }

    private static void testDharokRemainsSeparate() {
        Player victim = victim();
        Npc dharok = brother(2026);
        int originalHp = dharok.getCurrentHitpoints();
        // Even a corrupt special-effect ID must not enable a different
        // brother's healing/draining effects on Dharok.
        hit(dharok, victim, 7, 10, true);
        require(dharok.getCurrentHitpoints() == originalHp,
                "Dharok incorrectly gained Guthan's healing special");
        require(victim.getSkillManager().getCurrentLevels()[2] == 70,
                "Dharok incorrectly caused stat drainage");
    }

    private static void testVeracRemainsSeparate() {
        Player victim = victim();
        Npc verac = brother(2030);
        hit(verac, victim, 11, 10, true);
        require(victim.getSkillManager().getCurrentLevels()[2] == 70,
                "Verac incorrectly drained Strength via NPC special dispatch");
        require(victim.getSkillManager().getCurrentLevels()[16] == 70,
                "Verac incorrectly drained Agility via NPC special dispatch");
        require(victim.getCurrentHitpoints() == 80,
                "Verac's normal attack was blocked by special-effect routing");
        // Defence/protection bypass remains in CombatManager/CombatAction:
        // a separate historical-accuracy finding tracks its exact mechanics.
    }

    private static void testHitRequirementsAndIsolation() {
        int[][] attacks = {{2027, 7}, {2029, 8}, {2025, 9}, {2028, 10}};
        for (int[] attack : attacks) {
            Npc brother = brother(attack[0]);
            Player blocked = victim();
            int beforeHp = brother.getCurrentHitpoints();
            hit(brother, blocked, attack[1], 0, true);
            require(brother.getCurrentHitpoints() == beforeHp
                    && blocked.getRunEnergyPercent() == 80
                    && blocked.getSkillManager().getCurrentLevels()[2] == 70
                    && blocked.getSkillManager().getCurrentLevels()[16] == 70,
                    "Zero-damage attack activated special for NPC " + attack[0]);

            Player missed = victim();
            hit(brother, missed, attack[1], 10, false);
            require(brother.getCurrentHitpoints() == beforeHp
                    && missed.getRunEnergyPercent() == 80
                    && missed.getSkillManager().getCurrentLevels()[2] == 70
                    && missed.getSkillManager().getCurrentLevels()[16] == 70,
                    "Missed attack activated special for NPC " + attack[0]);
        }

        Player wrong = victim();
        Npc skeleton = brother(2036);
        hit(skeleton, wrong, 8, 10, true);
        require(wrong.getRunEnergyPercent() == 80,
                "Non-Barrows NPC used Torag's special attack");

        Player wrongPairing = victim();
        hit(brother(2025), wrongPairing, 10, 10, true);
        require(wrongPairing.getSkillManager().getCurrentLevels()[16] == 70,
                "Ahrim used Karil's proc when given a mismatched effect ID");
    }

    private static void testPlayerSetEffectsStillWork() {
        Player attacker = victim();
        attacker.getSkillManager().setCurrentLevel(3, 5);
        Npc target = brother(2036);
        target.setCurrentHitpoints(20);
        HitDefinition effect = new HitDefinition(MELEE, HitType.NORMAL, 3)
                .setSpecialEffectId(7);
        SpecialAttackDefinition.applyHitSpecialEffect(attacker, target, effect, 3);
        require(attacker.getCurrentHitpoints() == 8,
                "Player equipment effects were broken by the NPC dispatch fix");
    }

    private static void hit(Npc attacker, Player target, int effectId,
                            int damage, boolean landed) {
        HitDefinition definition = new HitDefinition(MELEE, HitType.NORMAL, damage)
                .setSpecialEffectId(effectId)
                .setAlwaysHits(landed);
        new CombatAction(attacker, target, definition).applyHit();
    }

    private static Player victim() {
        Player player = new Player(null);
        player.isBot = true;
        // Entity.isPlayer() checks encodedIndex, not the Java object type.
        // Detached players need the same high-bit tag as registered players.
        player.setEncodedIndex(32768);
        player.setPosition(new Position(3551, 9700, 0));
        player.getSkillManager().setCurrentLevel(3, 90);
        player.getSkillManager().setCurrentLevel(2, 70);
        player.getSkillManager().setCurrentLevel(16, 70);
        player.setRunEnergyPercent(80);
        return player;
    }

    private static Npc brother(int id) {
        Npc npc = new Npc(id);
        npc.setPosition(new Position(3550, 9700, 0));
        return npc;
    }

    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
