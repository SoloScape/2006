package com.rs2.model.player;

import com.rs2.model.combat.hit.HitType;
import com.rs2.model.player.Player;
import com.rs2.model.task.TickTask;
import com.rs2.util.GameUtil;

public final class BarrowsChestDamageTask
extends TickTask {
    // Preserved collapse values; 2006 sources confirm falling-rock damage
    // but do not independently establish these exact interval/model values.
    public static final int COLLAPSE_INTERVAL_TICKS = 50;
    public static final int MAX_COLLAPSE_DAMAGE = 5;
    public static final int ROCKFALL_GRAPHIC_ID = 60;
    private Player player;

    public BarrowsChestDamageTask(Player player, int value2) {
        super(COLLAPSE_INTERVAL_TICKS);
        this.player = player;
    }

    @Override
    public final void execute() {
        if (!this.player.isRegistered()) {
            this.stop();
            return;
        }
        // Climbing into the crypt leaves the Barrows X/Y region but changes
        // plane. Collapse damage must not continue in the crypt above.
        if (!this.player.barrowsChestOpened
                || !com.rs2.model.gameplay.barrows.BarrowsManager.isInBarrowsTunnels(this.player)) {
            this.stop();
            return;
        }
        this.player.getUpdateState().setGraphic(ROCKFALL_GRAPHIC_ID, 0);
        this.player.applyDirectHit(GameUtil.randomInclusive(MAX_COLLAPSE_DAMAGE), HitType.NORMAL);
    }
}

