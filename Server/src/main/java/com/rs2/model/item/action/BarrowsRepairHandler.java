package com.rs2.model.item.action;

import com.rs2.model.dialogue.DialogueManager;
import com.rs2.model.item.BarrowsEquipmentDurability;
import com.rs2.model.item.ItemStack;
import com.rs2.model.player.InventoryManager;
import com.rs2.model.player.Player;

public final class BarrowsRepairHandler {
    // Bob, Dunstan, Tindel Marchant, and the Void Knight repair Squire.
    // Native revision-443 Squire 3797 maps to the original Void Knights' smith
    // (older/OSRS ID 1766). Squire 3795 has no Talk-to action or world spawn;
    // NPC 1766 in this 443 cache is a Cow calf.
    private static final int[] repairNpcIds = {519, 1082, 1799, 3797};
    // Original revision-443 cache object; OSRS/317 uses object 6802 instead.
    public static final int ARMOUR_STAND_OBJECT_ID = 13715;
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
        return repairItemForCost(player, itemStack, calculateRepairCost(itemStack));
    }

    /**
     * 2006 POH workshop armour stand (revision-443 object 13715). A validated, reachable
     * real object is supplied by ItemOnObjectTask; no standalone POH system is
     * assumed. Using a friend's stand does not require 55 Construction.
     */
    public static boolean handleItemOnArmourStand(Player player, int itemId, int objectId) {
        if (objectId != ARMOUR_STAND_OBJECT_ID || !BarrowsEquipmentDurability.isBarrowsItem(itemId))
            return false;
        int slot = player.getSelectedItemSlot();
        if (slot < 0 || slot >= player.getInventoryManager().getContainer().getCapacity()) return true;
        ItemStack item = player.getInventoryManager().getContainer().getItemAt(slot);
        if (item == null || item.getId() != itemId) return true;
        int smithing = player.getSkillManager().getCurrentLevels()[13];
        int cost = calculateArmourStandRepairCost(item, smithing);
        if (cost < 0) {
            player.packetSender.sendGameMessage("This equipment does not need repairing.");
            return true;
        }
        if (repairItemForCost(player, item, cost)) {
            player.packetSender.sendGameMessage("You repair your Barrows equipment for "
                    + cost + " coins.");
        }
        return true;
    }

    /** Each two Smithing levels discount the ordinary repair by one percent. */
    public static int calculateArmourStandRepairCost(ItemStack item, int smithingLevel) {
        int npcCost = calculateRepairCost(item);
        if (npcCost < 0) return -1;
        int smithing = Math.max(1, Math.min(200, smithingLevel));
        return (int) ((long) npcCost * (200 - smithing) / 200);
    }

    private static boolean repairItemForCost(Player player, ItemStack item, int cost) {
        BarrowsRepairHandler definition = forItem(item);
        if (definition == null || cost < 0) return false;
        int slot = player.getSelectedItemSlot();
        if (slot < 0 || slot >= player.getInventoryManager().getContainer().getCapacity()) return false;
        ItemStack selected = player.getInventoryManager().getContainer().getItemAt(slot);
        if (selected == null || selected.getId() != item.getId()
                || selected.getMetadata() != item.getMetadata()) return false;
        InventoryManager inventory = player.getInventoryManager();
        if (cost > 0 && !inventory.containsItemStack(new ItemStack(995, cost))) {
            player.packetSender.sendGameMessage("You don't have enough coins to fix that.");
            return false;
        }
        if (!inventory.removeItemFromSlot(item, slot)) return false;
        if (cost > 0) inventory.removeItem(new ItemStack(995, cost));
        inventory.addItem(new ItemStack(definition.repairedItemId, 1));
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

