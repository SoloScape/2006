package com.rs2.model.gameplay.barrows;

import com.rs2.model.World;
import com.rs2.model.gameplay.barrows.BarrowsManager;
import com.rs2.model.gameplay.barrows.BarrowsPrayerDrainResetTask;
import com.rs2.model.player.Player;
import com.rs2.model.task.TickTask;
import com.rs2.util.GameUtil;

public final class BarrowsPrayerDrainTask
extends TickTask {
    private final Player player;

    // Item-model pairs are ordered Ahrim, Dharok, Guthan, Karil, Torag, Verac.
    // The killed-brother flags are ordered Verac, Torag, Karil, Guthan, Dharok, Ahrim.
    private static final int[] FACE_FIRST_MODEL_BY_BROTHER =
            {4771, 4769, 4767, 4765, 4763, 4761};

    /**
     * Select a ghostly face only from defeated brothers. The prayer loss
     * itself still happens if the player has not killed a brother.
     * 'selection' is the zero-based choice among killed brothers.
     */
    public static int faceModelForKilledBrother(boolean[] killed, int selection, boolean alternate) {
        if (killed == null || killed.length < FACE_FIRST_MODEL_BY_BROTHER.length) return -1;
        int count = 0;
        for (int i = 0; i < FACE_FIRST_MODEL_BY_BROTHER.length; i++) {
            if (killed[i]) count++;
        }
        if (count == 0 || selection < 0 || selection >= count) return -1;
        for (int i = 0; i < FACE_FIRST_MODEL_BY_BROTHER.length; i++) {
            if (!killed[i]) continue;
            if (selection-- == 0) return FACE_FIRST_MODEL_BY_BROTHER[i] + (alternate ? 1 : 0);
        }
        return -1;
    }

    public BarrowsPrayerDrainTask(int value2, Player player) {
        super(30);
        this.player = player;
    }

    @Override
    public final void execute() {
        if (!this.player.isRegistered()) {
            this.stop();
            return;
        }
        if (this.player.isInBarrows()) {
            int defeated = BarrowsManager.countKilledBrothers(this.player);
            if (defeated > 0) {
                int modelId = faceModelForKilledBrother(this.player.getBarrowsKilledBrothers(),
                        GameUtil.randomInt(defeated), GameUtil.randomInt(2) == 0);
                if (modelId != -1) {
                    int interfaceId = BarrowsManager.prayerDrainModelInterfaceIds[GameUtil.randomInt(6)];
                    this.player.packetSender.sendInterfaceModel(interfaceId, 100, modelId);
                    this.player.packetSender.sendInterfaceAnimation(interfaceId, 2085);
                    World.getTaskScheduler().schedule(
                            new BarrowsPrayerDrainResetTask(this, 3, this.player, interfaceId));
                }
            }
            int[] levels = this.player.getSkillManager().getCurrentLevels();
            levels[5] = Math.max(0, levels[5] - (defeated + 8));
            this.player.getSkillManager().refreshSkill(5);
            return;
        }
        this.player.activeRecurringEffectId = -1;
        this.stop();
    }
}

