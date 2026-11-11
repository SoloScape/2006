package com.rs2.model.item.action;

import com.rs2.model.dialogue.DialogueManager;
import com.rs2.model.item.BarrowsEquipmentDurability;
import com.rs2.model.item.ItemStack;
import com.rs2.model.player.InventoryManager;
import com.rs2.model.player.Player;

public final class BarrowsRepairHandler {
    // Bob (Lumbridge), Dunstan (Burthorpe), Tindel Marchant (Port Khazard).
    private static final int[] repairNpcIds = {519, 1082, 1799};
    private final int repairedItemId;
    private final int baseDegradedItemId;
    private final int fullRepairCost;
    private static BarrowsRepairHandler[][] repairDefinitions = new BarrowsRepairHandler[][]{{new BarrowsRepairHandler(4708, 4856, 60000), new BarrowsRepairHandler(4710, 4862, 100000), new BarrowsRepairHandler(4712, 4868, 90000), new BarrowsRepairHandler(4714, 4874, 80000)}, {new BarrowsRepairHandler(4716, 4880, 60000), new BarrowsRepairHandler(4718, 4886, 100000), new BarrowsRepairHandler(4720, 4892, 90000), new BarrowsRepairHandler(4722, 4898, 80000)}, {new BarrowsRepairHandler(4724, 4904, 60000), new BarrowsRepairHandler(4726, 4910, 100000), new BarrowsRepairHandler(4728, 4916, 90000), new BarrowsRepairHandler(4730, 4922, 80000)}, {new BarrowsRepairHandler(4732, 4928, 60000), new BarrowsRepairHandler(4734, 4934, 100000), new BarrowsRepairHandler(4736, 4940, 90000), new BarrowsRepairHandler(4738, 4946, 80000)}, {new BarrowsRepairHandler(4745, 4952, 60000), new BarrowsRepairHandler(4747, 4958, 100000), new BarrowsRepairHandler(4749, 4964, 90000), new BarrowsRepairHandler(4751, 4970, 80000)}, {new BarrowsRepairHandler(4753, 4976, 60000), new BarrowsRepairHandler(4755, 4982, 100000), new BarrowsRepairHandler(4757, 4988, 90000), new BarrowsRepairHandler(4759, 4994, 80000)}};

    public static boolean handleItemOnNpc(Player player, int npcId, ItemStack itemStack) {
        int initialValue = -1;
        int[] integerValues = repairNpcIds;
        int length = repairNpcIds.length;
        int index = 0;
        while (index < length) {
            int value = integerValues[index];
            if (npcId == value) {
                initialValue = value;
                break;
            }
            ++index;
        }
        if (initialValue != -1 && BarrowsRepairHandler.calculateRepairCost(itemStack) != -1) {
            player.pendingDialogueItem = itemStack;
            player.sharedActionValue = initialValue;
            DialogueManager.startDialogue(player, 10089);
            return true;
        }
        return false;
    }

    public static boolean repairItem(Player player, ItemStack itemStack) {
        int repairCost = BarrowsRepairHandler.calculateRepairCost(itemStack);
        if (repairCost < 0) return false;
        if (!player.getInventoryManager().containsItemStack(new ItemStack(995, repairCost)) && repairCost > 0) {
            player.packetSender.sendGameMessage("You don't have enough coins to fix that.");
            return false;
        }
        player.getInventoryManager().removeItem(new ItemStack(995, repairCost));
        player.getInventoryManager().removeItemFromSlot(itemStack, player.getSelectedItemSlot());
        InventoryManager inventoryManager = player.getInventoryManager();
        BarrowsRepairHandler repairDefinition = BarrowsRepairHandler.forItem(itemStack);
        inventoryManager.addItem(new ItemStack(repairDefinition == null ? -1 : repairDefinition.repairedItemId, 1));
        return true;
    }

    private BarrowsRepairHandler(int repairedItemId, int baseDegradedItemId, int fullRepairCost) {
        this.repairedItemId = repairedItemId;
        this.baseDegradedItemId = baseDegradedItemId;
        this.fullRepairCost = fullRepairCost;
    }

    public final int getFullyDegradedItemId() {
        return this.baseDegradedItemId + 4;
    }

    /**
     * NPC repair prices from the 2005 Barrows changes (also recorded in 2006):
     * helm 60k, body 90k, legs 80k, weapon 100k. Early repairs are
     * proportional to combat life already spent, including partial quarters.
     *
     * Use the shared durability decoder for both legacy 4500-charge metadata
     * and flagged 90,000-tick metadata; a broken (0%) item has no life left.
     */
    public static int calculateRepairCost(ItemStack itemStack) {
        BarrowsRepairHandler repairDefinition = forItem(itemStack);
        if (repairDefinition == null) return -1;

        int remaining = BarrowsEquipmentDurability.remainingCombatTicks(itemStack);
        if (remaining < 0) return -1;
        int total = BarrowsEquipmentDurability.FULL_COMBAT_TICKS;
        return (int) ((long) repairDefinition.fullRepairCost * (total - remaining) / total);
    }

    public static BarrowsRepairHandler forItem(ItemStack itemStack) {
        if (itemStack == null) return null;
        // Broken (0%) items may intentionally have no equipment slot.
        // Match all original item ID ranges instead of looking up a slot
        // that no longer exists on an unequippable item definition.
        int id = itemStack.getId();
        for (BarrowsRepairHandler[] set : repairDefinitions) {
            for (BarrowsRepairHandler part : set) {
                if (id >= part.baseDegradedItemId
                        && id <= part.baseDegradedItemId + 4) return part;
            }
        }
        return null;
    }
}

