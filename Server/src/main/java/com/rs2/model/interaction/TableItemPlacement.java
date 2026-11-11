package com.rs2.model.interaction;

import com.rs2.model.Position;
import com.rs2.model.gameplay.castlewars.CastleWarsManager;
import com.rs2.model.ground.GroundItem;
import com.rs2.model.ground.GroundItemManager;
import com.rs2.model.item.ItemDefinition;
import com.rs2.model.item.ItemStack;
import com.rs2.model.objects.ObjectDefinition;
import com.rs2.model.player.Player;
import com.rs2.util.GameUtil;
import java.util.Locale;

/**
 * 2006-style item-on-table placement. Items are moved from the selected
 * inventory slot to the table tile, and remain recoverable for ten minutes.
 * Object-specific quest/crafting uses retain priority in ItemOnObjectTask.
 */
public final class TableItemPlacement {
    public static final int TABLE_LIFETIME_TICKS = (int) GameUtil.secondsToTicks(600L);

    private TableItemPlacement() { }

    public static boolean isOrdinaryTable(String name) {
        if (name == null) return false;
        String lower = name.trim().toLowerCase(Locale.ROOT);
        return lower.equals("table")
                || lower.equals("wooden table")
                || lower.equals("dining table")
                || lower.equals("broken table")
                || lower.equals("stone table")
                || lower.equals("small table")
                || lower.equals("large table")
                || lower.equals("round table")
                || lower.equals("long table")
                || lower.equals("book table");
    }

    public static boolean use(Player player, int itemId, int objectId,
                              int x, int y, int plane) {
        ObjectDefinition object = ObjectDefinition.forId(objectId);
        if (object == null || !isOrdinaryTable(object.getName())) return false;

        int slot = player.getSelectedItemSlot();
        if (slot < 0 || slot >= player.getInventoryManager().getContainer().getCapacity()) return true;
        ItemStack selected = player.getInventoryManager().getContainer().getItemAt(slot);
        if (selected == null || selected.getId() != itemId || !selected.isValid()) return true;

        ItemDefinition definition = selected.getDefinition();
        // A destroy-only or activity-specific item must not bypass its
        // ordinary restrictions by placing it on shared scenery.
        if (definition.hasDestroyOption()
                || (CastleWarsManager.isInGame(player)
                    && CastleWarsManager.isFlagItemId(itemId))) {
            player.getPacketSender().sendGameMessage("You cannot put that on the table.");
            return true;
        }

        int amount = definition.isStackable() ? selected.getAmount() : 1;
        ItemStack placed = new ItemStack(itemId, amount, selected.getMetadata());
        if (!player.getInventoryManager().removeItemFromSlot(placed, slot)) {
            return true;
        }
        // Ground items are positioned on the table's tile, not the player's
        // feet. The native client renders them on the scenery surface.
        GroundItem groundItem = new GroundItem(placed, new Position(x, y, plane), false, true);
        groundItem.setLifetimeTicks(TABLE_LIFETIME_TICKS);
        GroundItemManager.getInstance().spawn(groundItem);
        player.getEquipmentManager().refreshCarriedValue();
        player.getPacketSender().sendGameMessage("You put the item on the table.");
        return true;
    }
}
