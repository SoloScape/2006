import com.rs2.model.gameplay.barrows.BarrowsPrayerDrainTask;

/** Six original brother faces, paired models, and first-crypt drain behaviour. */
public final class BarrowsPrayerDrainChecks {
    private static final int[] FIRST_MODEL_BY_BROTHER = {
            4771, 4769, 4767, 4765, 4763, 4761
    };

    public static void main(String[] args) {
        require(BarrowsPrayerDrainTask.DRAIN_INTERVAL_TICKS == 30,
                "Preserved Prayer drain interval changed; recheck period evidence");
        require(BarrowsPrayerDrainTask.BASE_DRAIN_POINTS == 8,
                "2006 Prayer drain base amount changed");
        require(BarrowsPrayerDrainTask.FACE_ANIMATION_ID == 2085,
                "443 Barrows face animation changed");
        boolean[] slain = new boolean[6];
        require(BarrowsPrayerDrainTask.faceModelForKilledBrother(slain, 0, false) == -1,
                "A ghostly face appeared before any brother was killed");
        for (int i = 0; i < slain.length; i++) {
            slain[i] = true;
            require(BarrowsPrayerDrainTask.faceModelForKilledBrother(slain, i, false)
                            == FIRST_MODEL_BY_BROTHER[i],
                    "Wrong slain brother's primary model at index " + i);
            require(BarrowsPrayerDrainTask.faceModelForKilledBrother(slain, i, true)
                            == FIRST_MODEL_BY_BROTHER[i] + 1,
                    "Wrong slain brother's alternate model at index " + i);
        }

        slain = new boolean[] {false, true, false, false, true, false};
        require(BarrowsPrayerDrainTask.faceModelForKilledBrother(slain, 0, false) == 4769,
                "A live brother's face was chosen instead of Torag");
        require(BarrowsPrayerDrainTask.faceModelForKilledBrother(slain, 1, true) == 4764,
                "A live brother's face was chosen instead of Dharok");
        require(BarrowsPrayerDrainTask.faceModelForKilledBrother(slain, 2, false) == -1,
                "A nonexistent third slain brother was selected");
        require(BarrowsPrayerDrainTask.faceModelForKilledBrother(slain, -1, false) == -1,
                "Negative face selection must be rejected");
        require(BarrowsPrayerDrainTask.faceModelForKilledBrother(null, 0, false) == -1,
                "Missing kill-state array must not select an arbitrary face");
        System.out.println("Barrows prayer apparitions: all six slain-only model pairs passed.");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
