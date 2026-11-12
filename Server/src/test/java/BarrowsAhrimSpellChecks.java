import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.c.AhrimCombatDefinition;
import com.rs2.model.combat.attack.CombatAttack;
import com.rs2.model.combat.attack.MagicCombatAttack;
import com.rs2.model.combat.effect.StatDrainEffect;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;
import com.rs2.model.skill.magic.SpellDefinition;
import java.lang.reflect.Field;
import java.util.EnumSet;

/** The original Ahrim encounter uses Fire Wave and all three classic
 * non-damaging stat-reduction spells, including Weaken.
 */
public final class BarrowsAhrimSpellChecks {
    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        Npc ahrim = new Npc(2025);
        ahrim.setPosition(new Position(3552, 9694, 0));
        Player opponent = new Player(null);
        opponent.isBot = true;
        opponent.setPosition(new Position(3553, 9694, 0));
        CombatAttack[] attacks = new AhrimCombatDefinition().createAttacks(ahrim, opponent);
        require(attacks.length == 4,
                "2006 Ahrim must have Fire Wave, Confuse, Weaken and Curse");
        Field spellField = MagicCombatAttack.class.getDeclaredField("spell");
        spellField.setAccessible(true);
        EnumSet<SpellDefinition> all = EnumSet.noneOf(SpellDefinition.class);
        for (CombatAttack attack : attacks) {
            require(attack instanceof MagicCombatAttack, "Ahrim lost a magic attack option");
            all.add((SpellDefinition) spellField.get(attack));
        }
        require(all.equals(EnumSet.of(SpellDefinition.FIRE_WAVE,
                SpellDefinition.CONFUSE, SpellDefinition.WEAKEN,
                SpellDefinition.CURSE)), "Ahrim's native four-spell table is incomplete");
        checkDrain(SpellDefinition.CONFUSE, 0);
        checkDrain(SpellDefinition.WEAKEN, 2);
        checkDrain(SpellDefinition.CURSE, 1);
        System.out.println("Ahrim 2006: all four original magic spells, including "
                + "Weaken, and distinct Attack/Strength/Defence effects passed.");
    }

    private static void checkDrain(SpellDefinition spell, int skill) {
        require(spell.getPrimaryEffect() instanceof StatDrainEffect
                        && ((StatDrainEffect) spell.getPrimaryEffect()).getSkillId() == skill,
                spell + " targets the wrong skill");
    }

    private static void require(boolean success, String message) {
        if (!success) throw new AssertionError(message);
    }
}
