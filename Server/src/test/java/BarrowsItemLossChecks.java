import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.ground.GroundItem;
import com.rs2.model.ground.GroundItemManager;
import com.rs2.model.item.BarrowsEquipmentDurability;
import com.rs2.model.item.ItemDefinition;
import com.rs2.model.item.ItemStack;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;
import com.rs2.model.quest.QuestDefinition;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 2006 death rule: fully repaired as well as worn Barrows equipment drops as 0%.
 * Uses a detached bot player so the death handler performs no character save.
 */
public final class BarrowsItemLossChecks {
    private static final int[] REPAIRED = {
            4708, 4710, 4712, 4714, 4716, 4718, 4720, 4722,
            4724, 4726, 4728, 4730, 4732, 4734, 4736, 4738,
            4745, 4747, 4749, 4751, 4753, 4755, 4757, 4759
    };

    public static void main(String[] args) throws Exception {
        ServerSettings.clientBuild = 443;
        NpcDefinition.loadDefinitions();
        QuestDefinition.loadDefinitions();
        ItemDefinition.loadDefinitions();
        checkDeathDrops(false);
        checkDeathDrops(true);
        System.out.println("Barrows death drops: all 24 pristine and 24 worn pieces become 0%.");
    }

    private static void checkDeathDrops(boolean worn) throws Exception {
        Player victim = new Player(null);
        victim.isBot = true;
        victim.setEncodedIndex(32768);
        victim.setIndex(1);
        victim.setPosition(new Position(3552, 9694, 0));
        victim.setDeathPosition(victim.getPosition().copy());
        victim.refreshLocalViewArea();

        // Prevent the normal three kept-on-death slots from masking a broken
        // conversion; game mode 2 keeps no items on death.
        Field mode = Player.class.getDeclaredField("gameMode");
        mode.setAccessible(true);
        mode.setInt(victim, 2);
        for (int i = 0; i < REPAIRED.length; i++) {
            ItemStack gear = new ItemStack(REPAIRED[i]);
            if (worn) gear = BarrowsEquipmentDurability.consumeCombatTicks(gear, 45000);
            victim.getInventoryManager().getContainer().setItem(i, gear);
        }

        List<GroundItem> before = new ArrayList<GroundItem>(groundItems());
        try {
            victim.dropDeathItems(null);
            Set<Integer> found = new HashSet<Integer>();
            for (GroundItem ground : groundItems()) {
                if (before.contains(ground)) continue;
                int id = ground.getItem().getId();
                if (!BarrowsEquipmentDurability.isBarrowsItem(id)) continue;
                require(found.add(id), "Duplicate Barrows death drop " + id);
                require(BarrowsEquipmentDurability.isBroken(id),
                        "Death dropped non-broken gear " + id + " (worn=" + worn + ")");
            }
            require(found.size() == REPAIRED.length,
                    "Expected " + REPAIRED.length + " broken pieces; found " + found.size());
            for (int repaired : REPAIRED) {
                require(found.contains(BarrowsEquipmentDurability.fullyDegradedItemId(repaired)),
                        "Missing broken death drop for " + repaired);
            }
        } finally {
            groundItems().retainAll(before);
        }
    }

    @SuppressWarnings("unchecked")
    private static List<GroundItem> groundItems() throws Exception {
        Field f = GroundItemManager.class.getDeclaredField("groundItems");
        f.setAccessible(true);
        return (List<GroundItem>) f.get(GroundItemManager.getInstance());
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
