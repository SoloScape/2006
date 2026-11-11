package com.rs2.model.player;

import com.rs2.model.combat.hit.HitType;
import com.rs2.model.player.Player;
import com.rs2.model.task.TickTask;
import com.rs2.util.GameUtil;

public final class BarrowsChestDamageTask
extends TickTask {
    private Player player;

    public BarrowsChestDamageTask(Player player, int value2) {
        super(50);
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
        this.player.getUpdateState().setGraphic(60, 0);
        this.player.applyDirectHit(GameUtil.randomInclusive(5), HitType.NORMAL);
    }
}

