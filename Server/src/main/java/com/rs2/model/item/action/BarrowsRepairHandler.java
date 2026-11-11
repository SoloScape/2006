package com.rs2.model.item.action;

import com.rs2.model.dialogue.DialogueManager;
import com.rs2.model.item.BarrowsEquipmentDurability;
import com.rs2.model.item.ItemStack;
import com.rs2.model.player.InventoryManager;
import com.rs2.model.player.Player;

public final class BarrowsRepairHandler {
    // Bob (Lumbridge), Dunstan (Burthorpe), Tindel Marchant (Port Khazard).
    private static final int[] repairNpcIds = {519, 4105, 1358};
    private int repairedItemId;
    private int baseDegradedItemId;
    private static BarrowsRepairHandler[][] repairDefinitions = new BarrowsRepairHandler[][]{{new BarrowsRepairHandler(4708, 4856), new BarrowsRepairHandler(4710, 4862), new BarrowsRepairHandler(4712, 4868), new BarrowsRepairHandler(4714, 4874)}, {new BarrowsRepairHandler(4716, 4880), new BarrowsRepairHandler(4718, 4886), new BarrowsRepairHandler(4720, 4892), new BarrowsRepairHandler(4722, 4898)}, {new BarrowsRepairHandler(4724, 4904), new BarrowsRepairHandler(4726, 4910), new BarrowsRepairHandler(4728, 4916), new BarrowsRepairHandler(4730, 4922)}, {new BarrowsRepairHandler(4732, 4928), new BarrowsRepairHandler(4734, 4934), new BarrowsRepairHandler(4736, 4940), new BarrowsRepairHandler(4738, 4946)}, {new BarrowsRepairHandler(4745, 4952), new BarrowsRepairHandler(4747, 4958), new BarrowsRepairHandler(4749, 4964), new BarrowsRepairHandler(4751, 4970)}, {new BarrowsRepairHandler(4753, 4976), new BarrowsRepairHandler(4755, 4982), new BarrowsRepairHandler(4757, 4988), new BarrowsRepairHandler(4759, 4994)}};

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

    private BarrowsRepairHandler(int repairedItemId, int baseDegradedItemId) {
        this.repairedItemId = repairedItemId;
        this.baseDegradedItemId = baseDegradedItemId;
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

        // The broken item definition may have no equipment slot.
        int slot = new ItemStack(repairDefinition.repairedItemId)
                .getDefinition().getEquipmentSlot();
        int fullRepairCost;
        switch (slot) {
            case 0: fullRepairCost = 60000; break;  // Helmet
            case 3: fullRepairCost = 100000; break; // Weapon
            case 4: fullRepairCost = 90000; break;  // Body
            case 7: fullRepairCost = 80000; break;  // Legs
            default: return -1;
        }

        int remaining = BarrowsEquipmentDurability.remainingCombatTicks(itemStack);
        if (remaining < 0) return -1;
        int total = BarrowsEquipmentDurability.FULL_COMBAT_TICKS;
        return (int) ((long) fullRepairCost * (total - remaining) / total);
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

