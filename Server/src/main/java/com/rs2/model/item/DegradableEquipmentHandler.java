package com.rs2.model.item;

import com.rs2.model.Entity;
import com.rs2.model.npc.Npc;
import com.rs2.model.player.Player;
import com.rs2.util.GameUtil;

/**
 * Equipment degradation is split by its original trigger:
 * Barrows wears once per combat tick, crystal/legacy gear retains the
 * pre-existing per-attack charge behaviour.
 */
public final class DegradableEquipmentHandler {
    private DegradableEquipmentHandler() { }

    /**
     * One world tick = 600ms, so 90,000 engaged ticks exhaust 15 hours.
     * The equipment's metadata persists the *full remaining tick count*;
     * there is no session counter to reset on logging out or unequipping.
     */
    public static void processBarrowsCombatTick(Player player) {
        if (player == null || !isEngagedInCombat(player)) return;

        ItemContainer equipment = player.getEquipmentManager().getContainer();
        boolean changedItemId = false;
        boolean broke = false;
        for (int slot = 0; slot < 14; slot++) {
            ItemStack item = equipment.getItemAt(slot);
            if (item == null || !BarrowsEquipmentDurability.isBarrowsItem(item.getId())
                    || BarrowsEquipmentDurability.isBroken(item.getId())) continue;

            ItemStack worn = BarrowsEquipmentDurability.consumeCombatTicks(item, 1);
            if (worn != item) {
                equipment.setItem(slot, worn);
                changedItemId = true;
                if (BarrowsEquipmentDurability.isBroken(worn.getId())) broke = true;
            }
        }
        if (changedItemId) {
            player.getEquipmentManager().refresh();
            if (broke) {
                player.packetSender.sendGameMessage(
                        "Your Barrows equipment has broken and needs repairing.");
            }
        }
    }

    /**
     * Fighting includes engaging a live target and receiving attacks while
     * standing still, praying, or not making your own attacks. The incoming
     * combat timer is refreshed by CombatCycleEvent even for blocked hits.
     * Chasing a far-away target or staying logged in while idle does not count.
     */
    public static boolean isEngagedInCombat(Player player) {
        if (player == null || player.isDead() || player.getCurrentHitpoints() <= 0) {
            return false;
        }
        Entity target = player.getCombatTarget();
        if (target != null && !target.isDead()
                && GameUtil.getDistance(player.getPosition(), target.getPosition())
                    <= Math.max(1, player.getAttackRange())
                && (!target.isNpc() || ((Npc) target).isActive())) {
            return true;
        }
        Entity attacker = player.getSingleCombatTimer().getTarget();
        return attacker != null && !attacker.isDead()
                && !player.getSingleCombatTimer().hasElapsed()
                && (!attacker.isNpc() || ((Npc) attacker).isActive());
    }

    /**
     * Legacy per-attack degradation for non-Barrows items (notably crystal
     * equipment). Kept separate so changing Barrows does not stop crystal wear.
     */
    public static void degradeEquipmentAfterCombat(Player player) {
        if (player == null) return;
        ItemContainer equipment = player.getEquipmentManager().getContainer();
        boolean changed = false;
        for (int slot = 0; slot < 14; slot++) {
            ItemStack item = equipment.getItemAt(slot);
            if (item == null || BarrowsEquipmentDurability.isBarrowsItem(item.getId())) continue;

            String name = item.getDefinition().getName().toLowerCase();
            if (name.contains("crystal")) {
                if (item.getId() == 4212 || item.getId() == 4224) {
                    item = new ItemStack(item.getId() == 4212 ? 4214 : 4225);
                    item.setMetadata(250);
                    equipment.setItem(slot, item);
                    changed = true;
                } else if (item.getMetadata() == 0) {
                    if (item.getId() == 4223 || item.getId() == 4234) {
                        equipment.setItem(slot, null);
                        changed = true;
                        continue;
                    }
                    item = new ItemStack(item.getId() + 1);
                    item.setMetadata(250);
                    equipment.setItem(slot, item);
                    changed = true;
                }
            }
            // Preserve existing charge behaviour of other special items too.
            if (item.getMetadata() >= 0 && item.getId() != 11283
                    && item.getId() != 11284) {
                item.setMetadata(item.getMetadata() - 1);
                changed = true;
            }
        }
        if (changed) player.getEquipmentManager().refresh();
    }
}
