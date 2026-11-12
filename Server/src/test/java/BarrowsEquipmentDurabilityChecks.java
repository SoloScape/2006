import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.World;
import com.rs2.model.item.BarrowsEquipmentDurability;
import com.rs2.model.item.DegradableEquipmentHandler;
import com.rs2.model.item.ItemStack;
import com.rs2.model.item.ItemDefinition;
import com.rs2.model.item.action.BarrowsRepairHandler;
import com.rs2.model.npc.Npc;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;
import com.rs2.model.quest.QuestDefinition;

/**
 * Exhaustive 24-piece Barrows wear regression: 90,000 x 600ms ticks = 15h.
 * Exercises id transitions, JSON/SQL-compatible item metadata, logout,
 * legacy metadata conversion, combat status, and crystal-wear isolation.
 */
public final class BarrowsEquipmentDurabilityChecks {
    private static final int[] REPAIRED = {
            4708, 4710, 4712, 4714,
            4716, 4718, 4720, 4722,
            4724, 4726, 4728, 4730,
            4732, 4734, 4736, 4738,
            4745, 4747, 4749, 4751,
            4753, 4755, 4757, 4759
    };
    private static final int[] DEGRADED = {
            4856, 4862, 4868, 4874,
            4880, 4886, 4892, 4898,
            4904, 4910, 4916, 4922,
            4928, 4934, 4940, 4946,
            4952, 4958, 4964, 4970,
            4976, 4982, 4988, 4994
    };

    public static void main(String[] args) {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        // Item definitions include quest requirements and need the quest count initialized.
        QuestDefinition.loadDefinitions();
        ItemDefinition.loadDefinitions();
        assertAllTwentyFourPieces();
        assertAllItemLossConversions();
        assertLoginPersistence();
        assertMigrationFromAttackCharges();
        assertCombatTickConditions();
        assertCrystalStillDegradesOnAttacks();
        assertBrokenSetCannotProvideEffect();
        assertBrokenItemCannotBeReequipped();
        System.out.println("Barrows durability: 24/24 pieces, 15h transitions, persistence, combat-only wear, crystal and repairs passed.");
    }

    private static void assertAllItemLossConversions() {
        for (int i = 0; i < REPAIRED.length; i++) {
            int broken = DEGRADED[i] + 4;
            require(BarrowsEquipmentDurability.fullyDegradedItemId(REPAIRED[i]) == broken,
                    "Dropping pristine Barrows item " + REPAIRED[i] + " did not break it");
            for (int stage = 0; stage <= 4; stage++) {
                require(BarrowsEquipmentDurability.fullyDegradedItemId(DEGRADED[i] + stage) == broken,
                        "Degradation stage " + stage + " of item " + REPAIRED[i]
                        + " is not converted to the right broken form");
            }
        }
        require(BarrowsEquipmentDurability.fullyDegradedItemId(995) == -1,
                "Unrelated items must not be converted to Barrows 0%");
    }

    private static void assertAllTwentyFourPieces() {
        require(BarrowsEquipmentDurability.FULL_COMBAT_TICKS == 90000,
                "Barrows lifetime must be exactly fifteen hours of 600ms combat ticks");
        require(BarrowsEquipmentDurability.COMBAT_TICKS_PER_QUARTER == 22500,
                "Each Barrows degradation level should last 3h45m of combat");
        for (int i = 0; i < REPAIRED.length; i++) {
            ItemStack item = new ItemStack(REPAIRED[i]);
            require(BarrowsEquipmentDurability.isBarrowsItem(item.getId()),
                    "Missing repaired item mapping for " + REPAIRED[i]);
            require(BarrowsEquipmentDurability.remainingCombatTicks(item) == 90000,
                    "Fresh gear should have 15h of combat left");
            require(BarrowsEquipmentDurability.consumeCombatTicks(item, 0) == item,
                    "Idle zero-tick update unexpectedly converted an item");
            item = BarrowsEquipmentDurability.consumeCombatTicks(item, 1);
            verify(item, DEGRADED[i], 89999, "Initial 100% conversion");
            item = BarrowsEquipmentDurability.consumeCombatTicks(item, 22498);
            verify(item, DEGRADED[i], 67501, "Last tick of 100% stage");
            item = BarrowsEquipmentDurability.consumeCombatTicks(item, 1);
            verify(item, DEGRADED[i] + 1, 67500, "75% transition");
            item = BarrowsEquipmentDurability.consumeCombatTicks(item, 22500);
            verify(item, DEGRADED[i] + 2, 45000, "50% transition");
            item = BarrowsEquipmentDurability.consumeCombatTicks(item, 22500);
            verify(item, DEGRADED[i] + 3, 22500, "25% transition");
            item = BarrowsEquipmentDurability.consumeCombatTicks(item, 22499);
            verify(item, DEGRADED[i] + 3, 1, "Last remaining combat tick");
            item = BarrowsEquipmentDurability.consumeCombatTicks(item, 1);
            verify(item, DEGRADED[i] + 4, 0, "0% broken transition");
            require(BarrowsEquipmentDurability.isBroken(item.getId()),
                    "Fully depleted item not marked broken");
            require(BarrowsEquipmentDurability.consumeCombatTicks(item, 200000) == item,
                    "Broken gear changed after additional combat");

            // Every stage remains repairable; historical NPC prices are
            // checked exhaustively by BarrowsRepairChecks.
            require(BarrowsRepairHandler.forItem(item) != null,
                    "Broken gear cannot be found by repair handler: " + item.getId());
            require(BarrowsRepairHandler.calculateRepairCost(item) > 0,
                    "Broken item should remain repairable: " + item.getId());
        }
    }

    private static void assertLoginPersistence() {
        ItemStack item = BarrowsEquipmentDurability.consumeCombatTicks(
                new ItemStack(4708), 25317);
        int before = BarrowsEquipmentDurability.remainingCombatTicks(item);
        // This is exactly what both character JSON and SQL save backends
        // persist: itemId, amount, metadata (not a session-only timer).
        ItemStack loaded = new ItemStack(item.getId(), item.getAmount(), item.getMetadata());
        require(BarrowsEquipmentDurability.isTickMetadata(loaded.getMetadata()),
                "Tick metadata is not distinguishable from old attack charges");
        require(BarrowsEquipmentDurability.remainingCombatTicks(loaded) == before,
                "Logout/login reset an item's remaining combat time");
        ItemStack continued = BarrowsEquipmentDurability.consumeCombatTicks(loaded, 731);
        require(BarrowsEquipmentDurability.remainingCombatTicks(continued) == before - 731,
                "Combat wear did not continue after reloading saved gear");
        ItemStack unequipped = new ItemStack(continued.getId(), 1, continued.getMetadata());
        require(BarrowsEquipmentDurability.remainingCombatTicks(unequipped) == before - 731,
                "Unequip/re-equip reset stored durability");
        require(BarrowsEquipmentDurability.consumeCombatTicks(unequipped, 1).getMetadata()
                    != continued.getMetadata(),
                "Equipment metadata did not advance after re-equipping");
    }

    private static void assertMigrationFromAttackCharges() {
        ItemStack old = new ItemStack(4868 + 2, 1, 2250);
        require(BarrowsEquipmentDurability.remainingCombatTicks(old) == 33750,
                "Legacy 4500-charge metadata was not migrated at the right rate");
        old = BarrowsEquipmentDurability.consumeCombatTicks(old, 1);
        verify(old, 4870, 33749, "Legacy charge migration");
        ItemStack boundary = new ItemStack(4856, 1, 0);
        boundary = BarrowsEquipmentDurability.consumeCombatTicks(boundary, 1);
        verify(boundary, 4857, 67499, "Legacy exhausted quarter boundary");
        require(BarrowsEquipmentDurability.remainingCombatTicks(
                new ItemStack(4860, 1, -1)) == 0,
                "Legacy already-broken items must not become repaired");
        require(!BarrowsEquipmentDurability.isBarrowsItem(11283),
                "Non-Barrows item unexpectedly mapped as degradable Barrows");
    }

    private static void assertCombatTickConditions() {
        int initialWorldTick = World.tickCount;
        try {
            Player player = player();
            Npc opponent = new Npc(2030);
            opponent.setPosition(new Position(3552, 9700, 0));
            opponent.setActive(true);
            player.getEquipmentManager().getContainer().setItem(0, new ItemStack(4708));
            World.tickCount = 1000;

            for (int i = 0; i < 100; i++) {
                DegradableEquipmentHandler.processBarrowsCombatTick(player);
            }
            require(player.getEquipmentManager().getItemIdAtSlot(0) == 4708,
                    "Idle gear degraded without combat");

            // The target is close and alive; no attack packets are required.
            player.setCombatTarget(opponent);
            for (int i = 0; i < 25; i++) {
                World.tickCount++;
                DegradableEquipmentHandler.processBarrowsCombatTick(player);
            }
            ItemStack worn = player.getEquipmentManager().getContainer().getItemAt(0);
            verify(worn, 4856, 89975, "Combat-tick wear independent of outgoing attacks");

            // Only chasing a distant monster must not spend combat time.
            opponent.setPosition(new Position(3600, 9700, 0));
            for (int i = 0; i < 50; i++) {
                World.tickCount++;
                DegradableEquipmentHandler.processBarrowsCombatTick(player);
            }
            require(BarrowsEquipmentDurability.remainingCombatTicks(
                    player.getEquipmentManager().getContainer().getItemAt(0)) == 89975,
                    "Following distant target depleted Barrows equipment");

            // Being attacked while standing still, e.g. while using prayer,
            // counts even though the player is not attacking anything.
            player.setCombatTarget(null);
            opponent.setPosition(new Position(3552, 9700, 0));
            player.getSingleCombatTimer().setTargetDelay(opponent, 8);
            World.tickCount++;
            DegradableEquipmentHandler.processBarrowsCombatTick(player);
            require(BarrowsEquipmentDurability.remainingCombatTicks(
                    player.getEquipmentManager().getContainer().getItemAt(0)) == 89974,
                    "Receiving attacks did not degrade worn Barrows equipment");
            World.tickCount += 10;
            DegradableEquipmentHandler.processBarrowsCombatTick(player);
            require(BarrowsEquipmentDurability.remainingCombatTicks(
                    player.getEquipmentManager().getContainer().getItemAt(0)) == 89974,
                    "Expired incoming combat timer kept degrading idle gear");

            player.setCombatTarget(opponent);
            player.setDead(true);
            DegradableEquipmentHandler.processBarrowsCombatTick(player);
            require(BarrowsEquipmentDurability.remainingCombatTicks(
                    player.getEquipmentManager().getContainer().getItemAt(0)) == 89974,
                    "Dead players should not wear equipment");
        } finally {
            World.tickCount = initialWorldTick;
        }
    }

    private static void assertCrystalStillDegradesOnAttacks() {
        Player player = player();
        player.getEquipmentManager().getContainer().setItem(3, new ItemStack(4214, 1, 10));
        player.getEquipmentManager().getContainer().setItem(0, new ItemStack(4708));
        DegradableEquipmentHandler.degradeEquipmentAfterCombat(player);
        require(player.getEquipmentManager().getContainer().getItemAt(3).getMetadata() == 9,
                "Crystal gear lost its per-attack wear");
        require(player.getEquipmentManager().getItemIdAtSlot(0) == 4708,
                "Barrows equipment still degraded when making an attack");
        DegradableEquipmentHandler.degradeEquipmentAfterCombat(player);
        require(player.getEquipmentManager().getContainer().getItemAt(3).getMetadata() == 8,
                "Crystal gear wear does not follow attack count");
    }

    private static void assertBrokenSetCannotProvideEffect() {
        Player player = player();
        player.getEquipmentManager().getContainer().setItem(0, new ItemStack(4753));
        player.getEquipmentManager().getContainer().setItem(3, new ItemStack(4755));
        player.getEquipmentManager().getContainer().setItem(4, new ItemStack(4757));
        player.getEquipmentManager().getContainer().setItem(7, new ItemStack(4759));
        player.getEquipmentManager().refreshBarrowsSetEffects();
        require(player.isVeracSetEffectActive(),
                "A valid full Verac set was not recognised; names="
                        + new ItemStack(4753).getDefinition().getName() + ", "
                        + new ItemStack(4755).getDefinition().getName() + ", "
                        + new ItemStack(4757).getDefinition().getName() + ", "
                        + new ItemStack(4759).getDefinition().getName());
        player.getEquipmentManager().getContainer().setItem(0, new ItemStack(4980));
        player.getEquipmentManager().refreshBarrowsSetEffects();
        require(!player.isVeracSetEffectActive(),
                "Fully broken Barrows helm still granted set effect");
    }

    private static void assertBrokenItemCannotBeReequipped() {
        Player player = player();
        ItemStack broken = new ItemStack(4980);
        player.getInventoryManager().getContainer().setItem(0, broken);
        player.getEquipmentManager().equipFromInventorySlot(0);
        require(player.getEquipmentManager().getContainer().getItemAt(0) == null,
                "Fully broken Verac helm was equipped without repair");
        require(player.getInventoryManager().getContainer().getItemAt(0) == broken,
                "A rejected equip attempt consumed the broken armour");
    }

    private static Player player() {
        Player player = new Player(null);
        player.isBot = true;
        player.setEncodedIndex(32768);
        player.setPosition(new Position(3551, 9700, 0));
        player.getSkillManager().setCurrentLevel(3, 90);
        return player;
    }

    private static void verify(ItemStack item, int id, int ticks, String context) {
        require(item.getId() == id,
                context + ": expected item " + id + " but got " + item.getId());
        require(BarrowsEquipmentDurability.remainingCombatTicks(item) == ticks,
                context + ": expected " + ticks + " combat ticks, got "
                        + BarrowsEquipmentDurability.remainingCombatTicks(item));
    }

    private static void require(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
