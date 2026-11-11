import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.combat.AttackBonusType;
import com.rs2.model.combat.AttackStyleDefinition;
import com.rs2.model.combat.AttackXpMode;
import com.rs2.model.combat.CombatAction;
import com.rs2.model.combat.CombatManager;
import com.rs2.model.combat.CombatType;
import com.rs2.model.combat.hit.HitDefinition;
import com.rs2.model.combat.hit.HitType;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;

/**
 * Verac's Defiler: real queued hit accuracy, prayer, skill isolation and
 * non-proc guards in both legacy and modern combat calculation modes.
 */
public final class VeracsDefilerCombatChecks {
    private static final AttackStyleDefinition MELEE =
            new AttackStyleDefinition(CombatType.MELEE, AttackXpMode.AGGRESSIVE,
                    AttackBonusType.CRUSH);
    private static final AttackStyleDefinition RANGED =
            new AttackStyleDefinition(CombatType.RANGED, AttackXpMode.RANGED_ACCURATE,
                    AttackBonusType.RANGED);

    public static void main(String[] args) {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        // Real hits are queued through the global combat manager, which is
        // not bootstrapped by detached Java regression fixtures by default.
        CombatManager.initialize();
        boolean initialMode = ServerSettings.modernCombatSystemEnabled;
        try {
            testDefilerEligibility();
            for (boolean mode : new boolean[]{true, false}) {
                ServerSettings.modernCombatSystemEnabled = mode;
                testNpcBypassesHighDefenceAndPrayer(mode);
                testPlayerSetBypassesHighDefenceAndPrayer(mode);
                testOrdinaryHitsStillRespectPrayer(mode);
                testFakeEffectIdsDoNotBypassPrayer(mode);
                testVeracCannotIgnoreNonMeleePrayer(mode);
                testDefilerCannotOverrideCombatRestrictions(mode);
                testDefenceRollRemainsUnmodified(mode);
            }
        } finally {
            ServerSettings.modernCombatSystemEnabled = initialMode;
        }
        System.out.println("Verac Defiler: player/NPC armour accuracy, prayer, modes, fake IDs and stat isolation passed.");
    }

    private static void testDefilerEligibility() {
        Npc verac = npc(2030);
        Npc nonVerac = npc(2027);
        Player player = player();
        HitDefinition proc = proc(MELEE, 10);
        require(CombatManager.isVeracsDefilerHit(verac, proc),
                "Verac's original brother NPC cannot activate Defiler");
        require(!CombatManager.isVeracsDefilerHit(nonVerac, proc),
                "Another Barrows brother incorrectly activated Defiler");
        require(!CombatManager.isVeracsDefilerHit(npc(2036), proc),
                "An ordinary tunnel NPC incorrectly activated Defiler");
        require(!CombatManager.isVeracsDefilerHit(player, proc),
                "Player without full Verac set incorrectly activated Defiler");
        player.setVeracSetEffectActive(true);
        require(CombatManager.isVeracsDefilerHit(player, proc),
                "Player with full Verac set cannot activate Defiler");
        require(!CombatManager.isVeracsDefilerHit(player,
                        new HitDefinition(MELEE, HitType.NORMAL, 10)),
                "The full set bypassed Defence without a proc");
        require(!CombatManager.isVeracsDefilerHit(verac, proc(RANGED, 10)),
                "Defiler activated on an NPC ranged hit");
        require(!CombatManager.isVeracsDefilerHit(player, proc(RANGED, 10)),
                "Defiler activated on a player ranged hit");
        require(!CombatManager.isVeracsDefilerHit(null, proc),
                "Missing attacker activated Defiler");
        require(!CombatManager.isVeracsDefilerHit(verac, null),
                "Missing hit definition activated Defiler");
        require(CombatManager.isVeracsDefilerHit(player, proc.copy()),
                "HitDefinition.copy() lost the proc's eligibility");
    }

    private static void testNpcBypassesHighDefenceAndPrayer(boolean mode) {
        Npc verac = npc(2030);
        Player protectedTarget = player();
        protectedTarget.getSkillManager().setCurrentLevel(1, 99);
        protectedTarget.getActivePrayers()[14] = true;
        runQueuedHit(verac, protectedTarget, proc(MELEE, 10));
        require(protectedTarget.getCurrentHitpoints() == 80,
                "Verac NPC did not bypass armour/Defence/prayer in " + mode);
        require(protectedTarget.getActivePrayers()[14],
                "Defiler should bypass, not deactivate, the target prayer");
        requireUnchangedSkills(protectedTarget,
                "Verac NPC proc drained unrelated skills in " + mode);
    }

    private static void testPlayerSetBypassesHighDefenceAndPrayer(boolean mode) {
        Player wearer = player();
        wearer.setVeracSetEffectActive(true);
        Player protectedTarget = player();
        protectedTarget.getSkillManager().setCurrentLevel(1, 99);
        protectedTarget.getActivePrayers()[14] = true;
        runQueuedHit(wearer, protectedTarget, proc(MELEE, 10));
        require(protectedTarget.getCurrentHitpoints() == 80,
                "Full Verac player set did not bypass high Defence and Protect from Melee in " + mode);
        requireUnchangedSkills(protectedTarget,
                "Full Verac player set drained skills in " + mode);
        require(protectedTarget.getActivePrayers()[14],
                "Player set proc incorrectly deactivated Protect from Melee");
    }

    private static void testOrdinaryHitsStillRespectPrayer(boolean mode) {
        // Use deferred-prayer hit definitions to exercise the impact-time
        // protection branch deterministically without any random proc rolls.
        Player npcVictim = player();
        npcVictim.getActivePrayers()[14] = true;
        HitDefinition standardHit = new HitDefinition(MELEE, HitType.NORMAL, 10, true)
                .setAlwaysHits(true);
        new CombatAction(npc(2030), npcVictim, standardHit).applyHit();
        require(npcVictim.getCurrentHitpoints() == 90,
                "Verac NPC hit without Defiler bypassed Protect from Melee in " + mode);

        Player playerVictim = player();
        playerVictim.getActivePrayers()[14] = true;
        Player attacker = player();
        attacker.setVeracSetEffectActive(true);
        new CombatAction(attacker, playerVictim,
                new HitDefinition(MELEE, HitType.NORMAL, 10, true)
                        .setAlwaysHits(true)).applyHit();
        require(playerVictim.getCurrentHitpoints() == 84,
                "Normal player hit ignored Protect from Melee despite no proc in " + mode);
    }

    private static void testFakeEffectIdsDoNotBypassPrayer(boolean mode) {
        Player target = player();
        target.getActivePrayers()[14] = true;
        runQueuedHit(npc(2036), target, proc(MELEE, 10));
        require(target.getCurrentHitpoints() == 90,
                "Non-Verac NPC with forged effect ID bypassed prayer in " + mode);

        Player attacker = player();
        Player other = player();
        other.getActivePrayers()[14] = true;
        // A bare effect ID is not sufficient to grant bypass to a player
        // who isn't wearing the complete Verac set.
        runQueuedHit(attacker, other, proc(MELEE, 10));
        require(other.getCurrentHitpoints() >= 84,
                "Unequipped player with effect ID bypassed protection in " + mode);
        requireUnchangedSkills(other, "Forged effect ID drained a player's skills");
    }

    private static void testVeracCannotIgnoreNonMeleePrayer(boolean mode) {
        Player target = player();
        target.getActivePrayers()[13] = true;
        runQueuedHit(npc(2030), target, proc(RANGED, 10));
        require(target.getCurrentHitpoints() == 90,
                "Verac wrongly bypassed Protect from Missiles with a ranged hit in " + mode);
    }

    private static void testDefilerCannotOverrideCombatRestrictions(boolean mode) {
        // Original queue restrictions stop attacks on characters with 1 HP
        // outside the tutorial; the proc must not reset a rejected hit.
        Player target = player();
        target.setCurrentHitpoints(1);
        target.getActivePrayers()[14] = true;
        runQueuedHit(npc(2030), target, proc(MELEE, 10));
        require(target.getCurrentHitpoints() == 1,
                "Verac proc overrode combat restrictions on the one-HP player in " + mode);
    }

    private static void testDefenceRollRemainsUnmodified(boolean mode) {
        Player target = player();
        target.getSkillManager().setCurrentLevel(1, 99);
        double plain = CombatManager.calculateDefenceRoll(target,
                new HitDefinition(MELEE, HitType.NORMAL, 10));
        double defiler = CombatManager.calculateDefenceRoll(target, proc(MELEE, 10));
        require(Double.compare(plain, defiler) == 0,
                "Legacy 25% defence-roll modifier remains in " + mode);
    }

    private static HitDefinition proc(AttackStyleDefinition style, int damage) {
        return new HitDefinition(style, HitType.NORMAL, damage, true)
                .enableAccuracyCheck().setSpecialEffectId(11);
    }

    private static void runQueuedHit(com.rs2.model.Entity attacker,
                                     com.rs2.model.Entity target, HitDefinition hit) {
        CombatAction action = new CombatAction(attacker, target, hit);
        action.queue();
        action.applyHit();
    }

    private static void requireUnchangedSkills(Player target, String message) {
        int[] current = target.getSkillManager().getCurrentLevels();
        require(current[0] == 70 && current[2] == 70 && current[4] == 70
                && current[5] == 70 && current[6] == 70 && current[16] == 70,
                message);
    }

    private static Player player() {
        Player player = new Player(null);
        player.isBot = true;
        // Entity.isPlayer() requires a high-bit encoded index in test fixtures.
        player.setEncodedIndex(32768);
        player.setPosition(new Position(3551, 9700, 0));
        int[] levels = player.getSkillManager().getCurrentLevels();
        for (int id : new int[]{0, 1, 2, 4, 5, 6, 16}) levels[id] = 70;
        player.setCurrentHitpoints(90);
        return player;
    }

    private static Npc npc(int id) {
        Npc npc = new Npc(id);
        npc.setPosition(new Position(3550, 9700, 0));
        return npc;
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
