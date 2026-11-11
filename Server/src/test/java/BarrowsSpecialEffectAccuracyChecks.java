import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.combat.AttackBonusType;
import com.rs2.model.combat.AttackStyleDefinition;
import com.rs2.model.combat.AttackXpMode;
import com.rs2.model.combat.BarrowsSpecialEffect;
import com.rs2.model.combat.CombatType;
import com.rs2.model.combat.hit.HitDefinition;
import com.rs2.model.combat.hit.HitType;
import com.rs2.model.combat.special.SpecialAttackDefinition;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;

/**
 * Effect-by-effect Barrows audit: independently checks original brothers,
 * attack types, four equipotential proc rolls, and amount/zero-hit semantics.
 *
 * 2006 effect descriptions:
 * https://forum.tip.it/topic/24034-leesters-barrows-guide-edit-12-jul-2006-aow-discussion/
 * https://forum.tip.it/topic/52625-a-new-special-effect-for-torag-the-corrupteds-barrows-set/
 * Jagex's 2014 retrospective explicitly documents 1/4 set proc rates:
 * https://forums.rs/en/380,381,122,65512837.html
 *
 * Exact original private 2006 probability implementation is not available;
 * 25% is the documented one-in-four approximation for this reconstruction.
 */
public final class BarrowsSpecialEffectAccuracyChecks {
    private static final AttackStyleDefinition MELEE =
            new AttackStyleDefinition(CombatType.MELEE, AttackXpMode.AGGRESSIVE,
                    AttackBonusType.CRUSH);

    public static void main(String[] args) {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();

        testOneInFourPerBrotherAndCorrectStyle();
        testOneInFourPerPlayerEquipmentSet();
        testToragNpcAndPlayerRunDrain();
        testOtherOriginalEffectMagnitudes();
        testZeroDamageMustNotTrigger();
        System.out.println("Barrows proc-rate and effect-value checks passed: all brothers, "
                + "player sets, hit styles, 25% selectors, run-energy magnitudes and zero hits.");
    }

    private static void testOneInFourPerBrotherAndCorrectStyle() {
        requireRate(2027, CombatType.MELEE, 7, "Guthan");
        requireRate(2029, CombatType.MELEE, 8, "Torag");
        requireRate(2025, CombatType.MAGIC, 9, "Ahrim");
        requireRate(2028, CombatType.RANGED, 10, "Karil");
        requireRate(2030, CombatType.MELEE, 11, "Verac");
        for (int roll = 0; roll < 4; roll++) {
            require(BarrowsSpecialEffect.selectEffectId(npc(2026), CombatType.MELEE, roll) == -1,
                    "Dharok must retain the continuous missing-HP bonus, not random procs");
            require(BarrowsSpecialEffect.selectEffectId(npc(2036), CombatType.MELEE, roll) == -1,
                    "Unrelated crypt NPC must never roll a Barrows effect");
            require(BarrowsSpecialEffect.selectEffectId(null, CombatType.MELEE, roll) == -1,
                    "Null attacker activated a Barrows effect");
        }
        require(BarrowsSpecialEffect.PROC_ROLL_MAX_INCLUSIVE == 3,
                "The Barrows roll must sample exactly four equipotential values");
    }

    private static void requireRate(int npcId, CombatType correctStyle, int expectedId, String brother) {
        int successfulRolls = 0;
        Npc npc = npc(npcId);
        for (int roll = 0; roll <= BarrowsSpecialEffect.PROC_ROLL_MAX_INCLUSIVE; roll++) {
            int effect = BarrowsSpecialEffect.selectEffectId(npc, correctStyle, roll);
            if (effect != -1) {
                successfulRolls++;
                require(effect == expectedId, brother + " incorrectly selected effect " + effect);
            }
        }
        require(successfulRolls == 1, brother + " proc must occur exactly once in four rolls");
        for (CombatType wrong : new CombatType[]{CombatType.MELEE, CombatType.MAGIC, CombatType.RANGED}) {
            if (wrong == correctStyle) continue;
            require(BarrowsSpecialEffect.selectEffectId(npc, wrong, 0) == -1,
                    brother + " proc activated on wrong attack style " + wrong);
        }
    }

    private static void testOneInFourPerPlayerEquipmentSet() {
        for (int set = 0; set < 5; set++) {
            Player wearer = player();
            CombatType style;
            int effect;
            switch (set) {
                case 0: wearer.setGuthanSetEffectActive(true);
                    style = CombatType.MELEE; effect = 7; break;
                case 1: wearer.setToragSetEffectActive(true);
                    style = CombatType.MELEE; effect = 8; break;
                case 2: wearer.setAhrimSetEffectActive(true);
                    style = CombatType.MAGIC; effect = 9; break;
                case 3: wearer.setKarilSetEffectActive(true);
                    style = CombatType.RANGED; effect = 10; break;
                default: wearer.setVeracSetEffectActive(true);
                    style = CombatType.MELEE; effect = 11; break;
            }
            for (int roll = 0; roll < 4; roll++) {
                require(BarrowsSpecialEffect.selectEffectId(wearer, style, roll)
                                == (roll == 0 ? effect : -1),
                        "Full equipment set proc has wrong chance/effect: " + set + " roll " + roll);
            }
            for (CombatType wrong : new CombatType[]{CombatType.MELEE, CombatType.MAGIC, CombatType.RANGED}) {
                if (wrong == style) continue;
                require(BarrowsSpecialEffect.selectEffectId(wearer, wrong, 0) == -1,
                        "Full-set effect fired on wrong combat style for set " + set);
            }
        }
        require(BarrowsSpecialEffect.selectEffectId(player(), CombatType.MELEE, 0) == -1,
                "Player without a complete Barrows set activated a proc");
    }

    private static void testToragNpcAndPlayerRunDrain() {
        Npc torag = npc(2029);
        Player wearer = player();
        wearer.setToragSetEffectActive(true);
        for (int raw : new int[]{0, 50, 100, 500, 1000, 4050, 5000, 8000, 10000}) {
            int expected = raw - raw / 5;
            Player npcVictim = player();
            npcVictim.setRunEnergyRaw(raw);
            apply(torag, npcVictim, 8, 10);
            require(npcVictim.getRunEnergyRaw() == expected,
                    "NPC Torag energy should reduce by 20% of remaining amount at raw=" + raw
                            + "; got " + npcVictim.getRunEnergyRaw() + ", expected " + expected);

            Player setVictim = player();
            setVictim.setRunEnergyRaw(raw);
            apply(wearer, setVictim, 8, 10);
            require(setVictim.getRunEnergyRaw() == expected,
                    "Full Torag player set must use the same proportional energy drain at " + raw);
        }
        Player repeated = player();
        repeated.setRunEnergyPercent(80);
        apply(torag, repeated, 8, 10);
        require(repeated.getRunEnergyPercent() == 64, "80% run should become 64%");
        apply(torag, repeated, 8, 10);
        require(repeated.getRunEnergyPercent() == 51, "64% run should become 51% after second proc");
    }

    private static void testOtherOriginalEffectMagnitudes() {
        Player guthanTarget = player();
        Npc guthan = npc(2027);
        guthan.setCurrentHitpoints(guthan.getMaxHitpoints() - 15);
        int before = guthan.getCurrentHitpoints();
        apply(guthan, guthanTarget, 7, 8);
        require(guthan.getCurrentHitpoints() == before + 8,
                "Guthan must heal by the damage dealt");

        Player ahrimTarget = player();
        apply(npc(2025), ahrimTarget, 9, 10);
        require(ahrimTarget.getSkillManager().getCurrentLevels()[2] == 65,
                "Ahrim's Strength drain changed from five levels");
        require(ahrimTarget.getSkillManager().getCurrentLevels()[16] == 70,
                "Ahrim unexpectedly drained Agility");

        Player karilTarget = player();
        apply(npc(2028), karilTarget, 10, 10);
        require(karilTarget.getSkillManager().getCurrentLevels()[16] == 56,
                "Karil must drain 20% of current Agility (70 -> 56)");
        require(karilTarget.getSkillManager().getCurrentLevels()[2] == 70,
                "Karil unexpectedly drained Strength");

        Player other = player();
        apply(npc(2030), other, 11, 10);
        require(other.getSkillManager().getCurrentLevels()[2] == 70
                        && other.getSkillManager().getCurrentLevels()[16] == 70,
                "Verac's bypass effect must not drain skills");
        // Detailed armour/prayer bypass in both combat modes is separately
        // exercised by VeracsDefilerCombatChecks.java.
    }

    private static void testZeroDamageMustNotTrigger() {
        Player target = player();
        target.setRunEnergyRaw(5100);
        apply(npc(2029), target, 8, 0);
        require(target.getRunEnergyRaw() == 5100, "Zero-damage Torag NPC proc drained run");

        Player wearer = player();
        wearer.setToragSetEffectActive(true);
        apply(wearer, target, 8, 0);
        require(target.getRunEnergyRaw() == 5100, "Zero-damage player Torag proc drained run");

        apply(npc(2025), target, 9, 0);
        apply(npc(2028), target, 10, 0);
        apply(wearer, target, 9, 0);
        apply(wearer, target, 10, 0);
        require(target.getSkillManager().getCurrentLevels()[2] == 70,
                "Zero-damage Ahrim proc drained Strength");
        require(target.getSkillManager().getCurrentLevels()[16] == 70,
                "Zero-damage Karil proc drained Agility");

        Npc guthan = npc(2027);
        guthan.setCurrentHitpoints(guthan.getMaxHitpoints() - 15);
        int before = guthan.getCurrentHitpoints();
        apply(guthan, target, 7, 0);
        require(guthan.getCurrentHitpoints() == before,
                "Zero-damage Guthan NPC proc healed");

        Player guthanWearer = player();
        guthanWearer.setGuthanSetEffectActive(true);
        guthanWearer.setCurrentHitpoints(50);
        apply(guthanWearer, target, 7, 0);
        require(guthanWearer.getCurrentHitpoints() == 50,
                "Zero-damage Guthan player proc healed");
    }

    private static void apply(com.rs2.model.Entity attacker, Player target, int effect, int damage) {
        SpecialAttackDefinition.applyHitSpecialEffect(
                attacker, target, new HitDefinition(MELEE, HitType.NORMAL, damage)
                        .setSpecialEffectId(effect), damage);
    }

    private static Player player() {
        Player player = new Player(null);
        player.isBot = true;
        player.setEncodedIndex(32768);
        player.setPosition(new Position(3551, 9700, 0));
        player.getSkillManager().setCurrentLevel(3, 90);
        player.getSkillManager().setCurrentLevel(2, 70);
        player.getSkillManager().setCurrentLevel(16, 70);
        return player;
    }

    private static Npc npc(int id) {
        Npc npc = new Npc(id);
        npc.setPosition(new Position(3550, 9700, 0));
        return npc;
    }

    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
}
