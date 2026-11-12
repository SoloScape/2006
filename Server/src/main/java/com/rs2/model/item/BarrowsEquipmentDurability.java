package com.rs2.model.item;

/**
 * Original-era Barrows wear: 15 hours of active combat, independent of
 * weapon attack speed, outgoing damage, or protection-prayer use.
 *
 * The item metadata is saved in character files and in the database. A tagged
 * metadata value records every remaining 600ms combat tick, so logout,
 * unequipping, and trading a partially worn item cannot refill the timer.
 * Untagged 0..4500 values from the old per-attack implementation are migrated
 * in place by converting each old charge to five combat ticks.
 */
public final class BarrowsEquipmentDurability {
    public static final int COMBAT_TICKS_PER_QUARTER = 22500;
    public static final int FULL_COMBAT_TICKS = COMBAT_TICKS_PER_QUARTER * 4;
    private static final int METADATA_TICK_FLAG = 1 << 20;
    private static final int LEGACY_CHARGES_PER_QUARTER = 4500;

    private static final int[] REPAIRED_IDS = {
            4708, 4710, 4712, 4714,
            4716, 4718, 4720, 4722,
            4724, 4726, 4728, 4730,
            4732, 4734, 4736, 4738,
            4745, 4747, 4749, 4751,
            4753, 4755, 4757, 4759
    };
    private static final int[] DEGRADED_BASE_IDS = {
            4856, 4862, 4868, 4874,
            4880, 4886, 4892, 4898,
            4904, 4910, 4916, 4922,
            4928, 4934, 4940, 4946,
            4952, 4958, 4964, 4970,
            4976, 4982, 4988, 4994
    };

    private BarrowsEquipmentDurability() { }

    private static int entryIndex(int itemId) {
        for (int i = 0; i < REPAIRED_IDS.length; i++) {
            if (itemId == REPAIRED_IDS[i]
                    || itemId >= DEGRADED_BASE_IDS[i]
                    && itemId <= DEGRADED_BASE_IDS[i] + 4) return i;
        }
        return -1;
    }

    public static boolean isBarrowsItem(int itemId) {
        return entryIndex(itemId) >= 0;
    }

    /**
     * In 2006, dropping or losing *any* Barrows piece on death turned it into
     * the tradeable 0% version, including a completely repaired piece.
     * Returns -1 for unrelated items so normal drop/death rules are unchanged.
     */
    public static int fullyDegradedItemId(int itemId) {
        int index = entryIndex(itemId);
        return index < 0 ? -1 : DEGRADED_BASE_IDS[index] + 4;
    }

    public static boolean isBroken(int itemId) {
        int i = entryIndex(itemId);
        return i >= 0 && itemId == DEGRADED_BASE_IDS[i] + 4;
    }

    public static boolean isTickMetadata(int metadata) {
        return metadata >= 0 && (metadata & METADATA_TICK_FLAG) != 0;
    }

    public static int remainingCombatTicks(ItemStack item) {
        if (item == null) return -1;
        int i = entryIndex(item.getId());
        if (i < 0) return -1;
        if (item.getId() == REPAIRED_IDS[i]) return FULL_COMBAT_TICKS;
        int stage = item.getId() - DEGRADED_BASE_IDS[i];
        if (stage == 4) return 0;
        int metadata = item.getMetadata();
        if (isTickMetadata(metadata)) {
            return Math.min(FULL_COMBAT_TICKS, metadata & ~METADATA_TICK_FLAG);
        }
        // Old saves stored 4,500 deductions per 25%-of-life item ID.
        int oldCharges = metadata < 0 ? LEGACY_CHARGES_PER_QUARTER
                : Math.max(0, Math.min(LEGACY_CHARGES_PER_QUARTER, metadata));
        return (3 - stage) * COMBAT_TICKS_PER_QUARTER + oldCharges * 5;
    }

    /**
     * Deplete exact elapsed combat ticks. Mutates metadata when an item's ID
     * does not change and returns a replacement ItemStack at a 25% boundary.
     */
    public static ItemStack consumeCombatTicks(ItemStack item, int ticks) {
        if (item == null || !isBarrowsItem(item.getId()) || ticks <= 0) return item;
        int remaining = remainingCombatTicks(item);
        if (remaining <= 0) return item;
        remaining = Math.max(0, remaining - ticks);

        int entry = entryIndex(item.getId());
        int stage;
        if (remaining == 0) stage = 4;
        else if (remaining <= COMBAT_TICKS_PER_QUARTER) stage = 3;
        else if (remaining <= COMBAT_TICKS_PER_QUARTER * 2) stage = 2;
        else if (remaining <= COMBAT_TICKS_PER_QUARTER * 3) stage = 1;
        else stage = 0;
        int id = DEGRADED_BASE_IDS[entry] + stage;
        int metadata = METADATA_TICK_FLAG | remaining;
        if (id != item.getId()) return new ItemStack(id, item.getAmount(), metadata);
        item.setMetadata(metadata);
        return item;
    }
}
