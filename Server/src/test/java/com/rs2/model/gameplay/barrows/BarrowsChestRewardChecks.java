package com.rs2.model.gameplay.barrows;

import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.ground.GroundItem;
import com.rs2.model.ground.GroundItemManager;
import com.rs2.model.ground.GroundItemVisibility;
import com.rs2.model.item.ItemDefinition;
import com.rs2.model.item.ItemStack;
import com.rs2.model.player.Player;
import com.rs2.model.quest.QuestDefinition;
import com.rs2.net.packet.InterfaceBridge;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Historical Barrows chest loot delivery for revision 443.
 * Reward popup was added in September 2015, not present in 2006:
 * https://forums.rs/en/380,381,203,65658217.html
 */
public final class BarrowsChestRewardChecks {
    private static final int BARROWS_HELM = 4708;
    private static final int BRONZE_DAGGER = 1205;
    private static final int COINS = 995;
    private static final int RUNES = 560;

    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        QuestDefinition.loadDefinitions();
        ItemDefinition.loadDefinitions();

        require(InterfaceBridge.translateGroup(19550) == InterfaceBridge.UNMAPPED,
                "The later chest rewards screen must not be mapped in revision 443");
        require(InterfaceBridge.translate(19555) == InterfaceBridge.UNMAPPED,
                "The later chest reward container must not be mapped in revision 443");
        assertEmptyChest();
        assertInventoryDelivery();
        assertFullInventoryGroundDelivery();
        assertFullInventoryWithExistingStack();
        assertPartialNonStackableOverflow();
        assertRepeatChestClickCannotAwardAgain();

        System.out.println("Barrows 2006 chest: no reward window, inventory delivery, "
                + "full/partial overflow ownership, stacked preservation and duplicate-click guard passed.");
    }

    private static void assertEmptyChest() {
        Player player = player();
        BarrowsManager.deliverChestRewards(player, new ArrayList<ItemStack>());
        require(player.barrowsRunsCompleted == 0, "Empty chest counted as a rewarded run");
        require(player.getInventoryManager().getContainer().getUsedSlots() == 0,
                "Empty chest added items");
        require(player.getOpenInterfaceId() != 19550, "Empty chest opened post-2006 interface");
    }

    private static void assertInventoryDelivery() {
        Player player = player();
        ArrayList<ItemStack> rewards = items(new ItemStack(BARROWS_HELM),
                new ItemStack(COINS, 1293), new ItemStack(RUNES, 86));
        BarrowsManager.deliverChestRewards(player, rewards);
        require(player.getInventoryManager().getItemAmount(BARROWS_HELM) == 1,
                "Barrows armour missing from inventory");
        require(player.getInventoryManager().getItemAmount(COINS) == 1293,
                "Coin reward missing or changed");
        require(player.getInventoryManager().getItemAmount(RUNES) == 86,
                "Rune reward missing or changed");
        require(player.barrowsRunsCompleted == 1, "Collected chest run not counted");
        require(player.getOpenInterfaceId() != 19550,
                "Chest opened 2015 reward window rather than using inventory");
    }

    private static void assertFullInventoryGroundDelivery() throws Exception {
        Player player = player();
        fillInventory(player, 28);
        List<GroundItem> original = new ArrayList<GroundItem>(groundItems());
        try {
            BarrowsManager.deliverChestRewards(player,
                    items(new ItemStack(BARROWS_HELM), new ItemStack(COINS, 277)));
            require(player.getInventoryManager().getItemAmount(BARROWS_HELM) == 0,
                    "Full inventory unexpectedly accepted armour");
            require(player.getInventoryManager().getItemAmount(COINS) == 0,
                    "Full inventory unexpectedly accepted coin stack");
            require(groundAmountSince(original, player, BARROWS_HELM) == 1,
                    "Full inventory lost the Barrows armour or duplicated it");
            require(groundAmountSince(original, player, COINS) == 277,
                    "Full inventory lost the coin stack or duplicated it");
            require(player.getInventoryManager().getContainer().getUsedSlots() == 28,
                    "Full inventory replaced an existing item");
        } finally {
            restoreGround(original);
        }
    }

    private static void assertFullInventoryWithExistingStack() throws Exception {
        Player player = player();
        fillInventory(player, 28);
        player.getInventoryManager().getContainer().setItem(0, new ItemStack(COINS, 50));
        List<GroundItem> original = new ArrayList<GroundItem>(groundItems());
        try {
            BarrowsManager.deliverChestRewards(player,
                    items(new ItemStack(COINS, 450), new ItemStack(BARROWS_HELM)));
            require(player.getInventoryManager().getItemAmount(COINS) == 500,
                    "Full inventory failed to merge the existing coin stack");
            require(groundAmountSince(original, player, COINS) == 0,
                    "Coins dropped despite a matching inventory stack");
            require(groundAmountSince(original, player, BARROWS_HELM) == 1,
                    "Unstackable reward lost while inventory was full");
            require(player.getInventoryManager().getContainer().getUsedSlots() == 28,
                    "Existing inventory slots were overwritten");
        } finally {
            restoreGround(original);
        }
    }

    private static void assertPartialNonStackableOverflow() throws Exception {
        Player player = player();
        fillInventory(player, 27);
        List<GroundItem> original = new ArrayList<GroundItem>(groundItems());
        try {
            BarrowsManager.deliverChestRewards(player, items(new ItemStack(BARROWS_HELM, 3)));
            int inventory = player.getInventoryManager().getItemAmount(BARROWS_HELM);
            int ground = groundAmountSince(original, player, BARROWS_HELM);
            require(inventory == 1 && ground == 2,
                    "27-slot inventory must keep one helmet and privately drop two, got "
                            + inventory + " in inventory and " + ground + " on ground");
            require(inventory + ground == 3,
                    "Partially fitting rewards were lost or duplicated");
        } finally {
            restoreGround(original);
        }
    }

    private static void assertRepeatChestClickCannotAwardAgain() throws Exception {
        Player player = player();
        BarrowsManager.deliverChestRewards(player, items(new ItemStack(COINS, 125)));
        player.barrowsChestOpened = true; // Set by startBarrowsChestDamage after first award.
        player.setBarrowsKillCount(6);
        player.configStates[453] = 1 << 16;
        int coins = player.getInventoryManager().getItemAmount(COINS);
        int runs = player.barrowsRunsCompleted;
        List<GroundItem> original = new ArrayList<GroundItem>(groundItems());
        try {
            for (int chestObjectId : new int[]{10284, 6774, 6775}) {
                require(BarrowsManager.handleFirstObjectAction(player, chestObjectId),
                        "Repeat chest interaction was not consumed for object " + chestObjectId);
                require(player.getInventoryManager().getItemAmount(COINS) == coins,
                        "Repeat chest click awarded coins again");
                require(player.barrowsRunsCompleted == runs,
                        "Repeat chest click counted another successful run");
                require(groundItems().size() == original.size(),
                        "Repeat chest click spawned further rewards on the ground");
            }
        } finally {
            restoreGround(original);
        }
    }

    private static ArrayList<ItemStack> items(ItemStack... rewards) {
        ArrayList<ItemStack> items = new ArrayList<ItemStack>();
        for (ItemStack item : rewards) items.add(item);
        return items;
    }

    private static void fillInventory(Player player, int count) {
        for (int slot = 0; slot < count; slot++) {
            player.getInventoryManager().getContainer().setItem(slot, new ItemStack(BRONZE_DAGGER));
        }
    }

    private static Player player() {
        Player player = new Player(null);
        player.isBot = true;
        player.setEncodedIndex(32768);
        // EntityReference resolves detached test players by a live index.
        player.setIndex(1);
        player.setPosition(new Position(3552, 9694, 0));
        return player;
    }

    @SuppressWarnings("unchecked")
    private static List<GroundItem> groundItems() throws Exception {
        Field field = GroundItemManager.class.getDeclaredField("groundItems");
        field.setAccessible(true);
        return (List<GroundItem>) field.get(GroundItemManager.getInstance());
    }

    private static int groundAmountSince(List<GroundItem> before, Player player, int itemId)
            throws Exception {
        int total = 0;
        for (GroundItem item : groundItems()) {
            if (before.contains(item) || item.getItem().getId() != itemId) continue;
            require(item.getVisibility() == GroundItemVisibility.PRIVATE,
                    "Overflow reward was not privately owned");
            require(item.getOwner() != null && item.getOwner().resolve() == player,
                    "Overflow reward was not owned by the chest looter");
            total += item.getItem().getAmount();
        }
        return total;
    }

    private static void restoreGround(List<GroundItem> before) throws Exception {
        groundItems().retainAll(before);
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
