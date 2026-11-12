import com.rs2.ServerSettings;
import com.rs2.model.item.BarrowsEquipmentDurability;
import com.rs2.model.item.ItemDefinition;
import com.rs2.model.item.ItemStack;
import com.rs2.model.item.action.BarrowsRepairHandler;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;
import com.rs2.model.quest.QuestDefinition;

/**
 * Regression coverage for the 2005-2006 Barrows NPC repair rules.
 *
 * Historical reference: Jagex's "Barrows Changes and Other Tweaks" update,
 * corroborated by the Tip.It discussion of 15 June 2006:
 * https://forum.tip.it/topic/47140-howwhere-do-i-go-to-repair-barrows-items/
 */
public final class BarrowsRepairChecks {
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
    // Each brother's array is helm, weapon, body, legs.
    private static final int[] FULL_COSTS = {60000, 100000, 90000, 80000};
    private static final int[] REPAIR_NPCS = {519, 1082, 1799, 1766};

    public static void main(String[] args) {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        // Item definitions include quest requirements and need the quest count initialized.
        QuestDefinition.loadDefinitions();
        ItemDefinition.loadDefinitions();
        checkRepairers();
        checkPricesAcrossEveryPiece();
        checkLegacyMetadata();
        checkPaymentAndItemExchange();
        checkArmourStandRepair();
        System.out.println("Barrows repair: all 24 pieces, four 2006 NPCs, POH stand discounts, "
                + "partial/full wear, legacy metadata and coin/item exchange passed.");
    }

    private static void checkRepairers() {
        System.out.println("Repair NPC definitions: Bob(519)="
                + NpcDefinition.forId(519).getName() + ", Dunstan(1082)="
                + NpcDefinition.forId(1082).getName() + ", Tindel(1799)="
                + NpcDefinition.forId(1799).getName());
        require("Bob".equalsIgnoreCase(NpcDefinition.forId(519).getName()),
                "NPC 519 must be Bob");
        require("Dunstan".equalsIgnoreCase(NpcDefinition.forId(1082).getName()),
                "NPC 1082 must be Dunstan");
        require("Tindel Marchant".equalsIgnoreCase(NpcDefinition.forId(1799).getName()),
                "NPC 1799 must be Tindel Marchant");

        require(NpcDefinition.forId(1766).getName().toLowerCase().contains("squire"),
                "NPC 1766 must be the Void Knights' Squire");

        Player player = player();
        ItemStack damaged = new ItemStack(DEGRADED[0] + 4, 1, -1);
        for (int npcId : REPAIR_NPCS) {
            require(BarrowsRepairHandler.handleItemOnNpc(player, npcId, damaged),
                    "Repair NPC " + npcId + " rejected broken Barrows gear");
            require(player.sharedActionValue == npcId,
                    "Dialogue lost the selected repairer " + npcId);
            require(player.pendingDialogueItem == damaged,
                    "Dialogue lost the worn item for repairer " + npcId);
        }
        require(!BarrowsRepairHandler.handleItemOnNpc(player, 0, damaged),
                "Unknown NPC accepted Barrows gear for repair");
        require(!BarrowsRepairHandler.handleItemOnNpc(player, 519, new ItemStack(995)),
                "Repair NPC accepted non-Barrows item");
    }

    private static void checkPricesAcrossEveryPiece() {
        int[] usedTicks = {1, 11250, 22499, 22500, 33750, 45000,
                56250, 67500, 89999, 90000};
        int fullSetCost = 0;
        for (int i = 0; i < REPAIRED.length; i++) {
            final int fullCost = FULL_COSTS[i % FULL_COSTS.length];
            if (i < 4) fullSetCost += fullCost;
            require(BarrowsRepairHandler.calculateRepairCost(new ItemStack(REPAIRED[i])) == -1,
                    "A pristine item is not itself a degraded repair target");

            // Test the actual 90,000 combat-tick metadata and ID transitions.
            for (int used : usedTicks) {
                ItemStack worn = BarrowsEquipmentDurability.consumeCombatTicks(
                        new ItemStack(REPAIRED[i]), used);
                int expected = (int) ((long) fullCost * used
                        / BarrowsEquipmentDurability.FULL_COMBAT_TICKS);
                assertCost(worn, expected, "tick wear " + used + " on " + REPAIRED[i]);
            }

            // A broken (0%) ID must cost exactly 100%, never 125%.
            assertCost(new ItemStack(DEGRADED[i] + 4, 1, -1), fullCost,
                    "legacy broken " + REPAIRED[i]);
            assertCost(new ItemStack(DEGRADED[i] + 4, 1, 0), fullCost,
                    "zero-metadata broken " + REPAIRED[i]);
        }
        require(fullSetCost == 330000,
                "Fully depleted historical NPC repair price must be 330,000 gp per set");
    }

    private static void checkLegacyMetadata() {
        for (int i = 0; i < REPAIRED.length; i++) {
            int fullCost = FULL_COSTS[i % FULL_COSTS.length];
            for (int stage = 0; stage < 4; stage++) {
                for (int oldCharges : new int[]{0, 2250, 4500}) {
                    int remaining = (3 - stage)
                            * BarrowsEquipmentDurability.COMBAT_TICKS_PER_QUARTER
                            + oldCharges * 5;
                    int expected = (int) ((long) fullCost
                            * (BarrowsEquipmentDurability.FULL_COMBAT_TICKS - remaining)
                            / BarrowsEquipmentDurability.FULL_COMBAT_TICKS);
                    assertCost(new ItemStack(DEGRADED[i] + stage, 1, oldCharges),
                            expected, "legacy stage " + stage
                                    + " charges " + oldCharges + " on " + REPAIRED[i]);
                }
            }
            // Unset legacy metadata denotes a complete remaining quarter.
            assertCost(new ItemStack(DEGRADED[i], 1, -1), 0,
                    "unset legacy first quarter " + REPAIRED[i]);
        }
        require(BarrowsRepairHandler.calculateRepairCost(new ItemStack(995)) == -1,
                "Coins must not be treated as repairable equipment");
        require(BarrowsRepairHandler.calculateRepairCost(null) == -1,
                "Null item must not be repairable");
    }

    private static void checkPaymentAndItemExchange() {
        for (int slot = 0; slot < 4; slot++) {
            for (int used : new int[]{45000, 90000}) {
                Player player = player();
                ItemStack worn = BarrowsEquipmentDurability.consumeCombatTicks(
                        new ItemStack(REPAIRED[slot]), used);
                int expected = (int) ((long) FULL_COSTS[slot] * used
                        / BarrowsEquipmentDurability.FULL_COMBAT_TICKS);
                player.getInventoryManager().getContainer().setItem(0, worn);
                player.getInventoryManager().getContainer().setItem(1,
                        new ItemStack(995, 200000));
                player.setSelectedItemSlot(0);
                require(BarrowsRepairHandler.repairItem(player, worn),
                        "Paid repair failed for slot " + slot + ", ticks " + used);
                require(player.getInventoryManager().containsItem(REPAIRED[slot]),
                        "Repair did not return pristine item for slot " + slot);
                require(!player.getInventoryManager().containsItem(worn.getId()),
                        "Repair left worn item in inventory for slot " + slot);
                require(player.getInventoryManager().getItemAmount(995)
                                == 200000 - expected,
                        "Repair debited wrong number of coins for slot " + slot);
            }
        }

        Player player = player();
        require(!BarrowsRepairHandler.repairItem(player, new ItemStack(995)),
                "Invalid item should fail repair without changing inventory");
    }

    private static void checkArmourStandRepair() {
        require(BarrowsRepairHandler.ARMOUR_STAND_OBJECT_ID == 6802,
                "Original 2006 POH armour stand object must be 6802");
        for (int i = 0; i < REPAIRED.length; i++) {
            ItemStack damaged = BarrowsEquipmentDurability.consumeCombatTicks(
                    new ItemStack(REPAIRED[i]), 45000);
            int normal = FULL_COSTS[i % FULL_COSTS.length] / 2;
            require(BarrowsRepairHandler.calculateArmourStandRepairCost(damaged, 1)
                            == normal * 199 / 200,
                    "Level 1 Smithing armour stand discount is wrong for " + REPAIRED[i]);
            require(BarrowsRepairHandler.calculateArmourStandRepairCost(damaged, 60)
                            == normal * 140 / 200,
                    "Level 60 Smithing armour stand discount is wrong for " + REPAIRED[i]);
            require(BarrowsRepairHandler.calculateArmourStandRepairCost(damaged, 99)
                            == normal * 101 / 200,
                    "Level 99 Smithing armour stand discount is wrong for " + REPAIRED[i]);
            require(BarrowsRepairHandler.calculateArmourStandRepairCost(
                            new ItemStack(REPAIRED[i]), 60) == -1,
                    "Unworn equipment became a repair target");
        }
        Player p = player();
        p.getSkillManager().setCurrentLevel(13, 60);
        ItemStack damaged = BarrowsEquipmentDurability.consumeCombatTicks(
                new ItemStack(REPAIRED[0]), 45000);
        p.getInventoryManager().getContainer().setItem(0, damaged);
        p.getInventoryManager().getContainer().setItem(1, new ItemStack(995, 30000));
        p.setSelectedItemSlot(0);
        require(!BarrowsRepairHandler.handleItemOnArmourStand(p, damaged.getId(), 6803),
                "Other objects must not offer stand repair");
        require(BarrowsRepairHandler.handleItemOnArmourStand(p, damaged.getId(), 6802),
                "The armour stand did not handle worn Barrows equipment");
        require(p.getInventoryManager().containsItem(REPAIRED[0]),
                "Armour stand did not restore the repaired helm");
        require(p.getInventoryManager().getItemAmount(995) == 9000,
                "Armour stand failed to apply the 30% Smithing discount");

        Player broke = player();
        ItemStack broken = new ItemStack(DEGRADED[1] + 4);
        broke.getInventoryManager().getContainer().setItem(0, broken);
        broke.setSelectedItemSlot(0);
        require(BarrowsRepairHandler.handleItemOnArmourStand(broke, broken.getId(), 6802),
                "Broken gear item-on-stand interaction was not consumed");
        require(broke.getInventoryManager().containsItem(broken.getId()),
                "Failed armour-stand payment lost worn gear");
        require(broke.getInventoryManager().getItemAmount(995) == 0,
                "Failed armour-stand payment created or removed money");
    }

    private static Player player() {
        Player player = new Player(null);
        player.isBot = true;
        return player;
    }

    private static void assertCost(ItemStack item, int expected, String context) {
        require(BarrowsRepairHandler.forItem(item) != null,
                "Missing repair mapping: " + context);
        int actual = BarrowsRepairHandler.calculateRepairCost(item);
        require(actual == expected,
                context + ": expected " + expected + " gp but got " + actual);
    }

    private static void require(boolean result, String message) {
        if (!result) throw new AssertionError(message);
    }
}
