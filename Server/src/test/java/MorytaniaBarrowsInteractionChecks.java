import com.rs2.ServerSettings;
import com.rs2.model.Position;
import com.rs2.model.dialogue.OldCroneDialogue;
import com.rs2.model.gameplay.barrows.BarrowsTomeHandler;
import com.rs2.model.ground.GroundItem;
import com.rs2.model.interaction.TableItemPlacement;
import com.rs2.model.item.ItemStack;
import com.rs2.model.player.Player;

/** Regression checks for Barrows tome, Old Crone and table placement. */
public final class MorytaniaBarrowsInteractionChecks {
    public static void main(String[] args) {
        ServerSettings.clientBuild = 443;

        require(TableItemPlacement.isOrdinaryTable("Table"), "Generic tables must accept items");
        require(TableItemPlacement.isOrdinaryTable("Wooden table"), "Wooden tables must accept items");
        require(!TableItemPlacement.isOrdinaryTable("Crafting bench"), "Crafting benches must not become item tables");
        require(!TableItemPlacement.isOrdinaryTable("Anvil"), "Anvils must retain their item uses");
        require(TableItemPlacement.TABLE_LIFETIME_TICKS == 1000,
                "A table item should remain for 600 seconds at 600ms per game tick");

        GroundItem tableItem = new GroundItem(new ItemStack(995, 25), new Position(3562, 3311, 0), false, true);
        tableItem.setLifetimeTicks(TableItemPlacement.TABLE_LIFETIME_TICKS);
        require(tableItem.getLifetimeTicks() == TableItemPlacement.TABLE_LIFETIME_TICKS,
                "Table item lifetime was not stored on the ground item");

        Player reader = new Player(null);
        reader.isBot = true; // No session/network writes in this detached test.
        require(reader.getInventoryManager().addItem(new ItemStack(BarrowsTomeHandler.TOME_ITEM_ID)),
                "Unable to give the Barrows tome to the test player");
        require(BarrowsTomeHandler.read(reader, BarrowsTomeHandler.TOME_ITEM_ID),
                "Reading the Barrows tome was not handled");
        require(reader.getOpenInterfaceId() == 837, "The 443 book interface did not open");
        require(reader.activeBookItemId == BarrowsTomeHandler.TOME_ITEM_ID,
                "The tome reader did not record the active book");

        for (int i = 1; i < 4; i++) {
            require(BarrowsTomeHandler.handleBookButton(reader, 841), "Next-page click not handled");
            require(reader.activeBookPageIndex == i, "Next-page click did not advance the book");
        }
        require(BarrowsTomeHandler.handleBookButton(reader, 841), "Last-page boundary not handled");
        require(reader.activeBookPageIndex == 3, "Next went past the final page");
        require(BarrowsTomeHandler.handleBookButton(reader, 839), "Previous-page click not handled");
        require(reader.activeBookPageIndex == 2, "Previous-page click did not turn back");
        require(BarrowsTomeHandler.handleBookButton(reader, 10162), "Close click not handled");
        require(reader.activeBookItemId == 0, "Closing the tome did not clear its state");

        Player visitor = new Player(null);
        visitor.isBot = true;
        visitor.setUsername("Dialogue Tester");
        // In a detached fixture the normal NPC definition bootstrap has not run.
        // The renderer still needs a non-null definition for the NPC portrait.
        com.rs2.model.World.getNpcDefinitions()[2996] =
                com.rs2.model.npc.NpcDefinition.forId(2996);
        visitor.getDialogueManager().setDialogueNpcId(2996);
        require(OldCroneDialogue.continueDialogue(visitor, 1, 0), "Old Crone greeting not handled");
        require(OldCroneDialogue.continueDialogue(visitor, 2, 0), "Old Crone introduction not handled");
        require(OldCroneDialogue.continueDialogue(visitor, 3, 0), "Old Crone conversation choices missing");
        require(OldCroneDialogue.continueDialogue(visitor, 4, 1), "Old Crone Necrovarus option not handled");

        System.out.println("Barrows tome, Old Crone and table placement checks passed.");
    }

    private static void require(boolean valid, String message) {
        if (!valid) throw new AssertionError(message);
    }
}
