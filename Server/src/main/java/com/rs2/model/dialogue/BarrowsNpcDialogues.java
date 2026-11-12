package com.rs2.model.dialogue;

import com.rs2.model.item.ItemStack;
import com.rs2.model.player.Player;
import com.rs2.util.GameUtil;

/**
 * Barrows-area conversations available during 2006, without later quest,
 * diary, banking or multi-item repair features.
 *
 * Strange Old Man conversation variants are based on surviving historical
 * transcripts; wording apart from short recognizable phrases is reconstructed.
 * Repair Squire conversations are a period-appropriate interaction, not a
 * verified word-for-word capture.
 */
public final class BarrowsNpcDialogues {
    public static final int STRANGE_OLD_MAN_ID = 2024;
    public static final int VOID_KNIGHT_REPAIR_SQUIRE_ID = 3797;
    public static final int CRUMBLING_TOME_ID = 4707;

    private BarrowsNpcDialogues() { }

    public static boolean supports(int npcId) {
        return npcId == STRANGE_OLD_MAN_ID || npcId == VOID_KNIGHT_REPAIR_SQUIRE_ID;
    }

    public static boolean continueDialogue(Player player, int npcId, int step, int option) {
        if (player == null || !supports(npcId)) return false;
        if (npcId == VOID_KNIGHT_REPAIR_SQUIRE_ID) {
            return squire(player, step, option);
        }
        return oldMan(player, step, option);
    }

    private static boolean squire(Player player, int step, int option) {
        DialogueManager dialogue = player.getDialogueManager();
        switch (step) {
            case 1:
                dialogue.showNpcTwoLineDialogue(
                        "Welcome to the Void Knights' workshop.",
                        "I can help mend damaged armour.", 591);
                return true;
            case 2:
                dialogue.showTwoOptions(
                        "Can you repair my damaged equipment?",
                        "No thanks.");
                return true;
            case 3:
                if (option == 1) {
                    dialogue.showNpcThreeLineDialogue(
                            "Use a damaged piece of armour on me,",
                            "and I'll tell you what it costs",
                            "before I repair it.", 591);
                } else {
                    dialogue.showPlayerOneLineDialogue("No thanks.", 591);
                }
                dialogue.finishDialogue();
                return true;
            default:
                dialogue.finishDialogue();
                return true;
        }
    }

    private static boolean oldMan(Player player, int step, int option) {
        DialogueManager dialogue = player.getDialogueManager();
        switch (step) {
            case 1:
                if (!player.getInventoryManager().containsItem(952)) {
                    // The surviving no-spade transcript mentions spades at
                    // every mound, a convenience added after 2006. Do not
                    // promise those later world spawns in this dialogue.
                    dialogue.showNpcTwoLineDialogue(
                            "Dig, dig, DIG! You can't dig",
                            "without bringing a spade!", 591);
                    dialogue.setNextDialogueStep(5);
                } else {
                    dialogue.showNpcOneLineDialogue("Dig, dig, dig!", 591);
                }
                return true;
            case 5:
                dialogue.showPlayerOneLineDialogue(
                        "Where can I find a spade?", 591);
                return true;
            case 6:
                dialogue.showNpcTwoLineDialogue(
                        "A spade is what you need!",
                        "Find one before digging here.", 591);
                dialogue.finishDialogue();
                return true;
            case 2:
                // Four surviving regular conversation branches. The precise
                // frequency and verbatim 2006 wording are not authenticated.
                // Never expose later quest, diary or warning-menu dialogue.
                int branch = GameUtil.randomInt(4);
                dialogue.setNextDialogueStep(10 + branch * 10);
                dialogue.showNpcOneLineDialogue(branch == 0
                        ? "Pst, wanna hear a secret?"
                        : branch == 1 ? "Knock, knock."
                        : branch == 2 ? "What? I didn't ask for a book!"
                        : "AAAAAARRRRGGHHHH!", 591);
                if (branch == 2) {
                    grantTome(player);
                    dialogue.finishDialogue();
                }
                return true;
            case 10:
                dialogue.showTwoOptions("Sure!", "No, thanks.");
                return true;
            case 11:
                if (option == 1) {
                    dialogue.showPlayerOneLineDialogue("Sure!", 591);
                } else {
                    dialogue.showPlayerOneLineDialogue("No, thanks.", 591);
                    dialogue.finishDialogue();
                }
                return true;
            case 12:
                dialogue.showNpcOneLineDialogue("They're not normal!", 591);
                dialogue.finishDialogue();
                return true;
            case 20:
                dialogue.showPlayerOneLineDialogue("Who's there?", 591);
                return true;
            case 21:
                dialogue.showNpcOneLineDialogue(
                        "A big scary monster, HAHAHAHAHAHAHAHAHAHA!", 591);
                return true;
            case 22:
                dialogue.showPlayerOneLineDialogue("Okay...", 591);
                dialogue.finishDialogue();
                return true;
            case 40:
                dialogue.showTwoOptions(
                        "What's wrong?", "I'll leave you to it, then.");
                return true;
            case 41:
                if (option == 1) {
                    dialogue.showPlayerOneLineDialogue("What's wrong?", 591);
                } else {
                    dialogue.showPlayerOneLineDialogue(
                            "I'll leave you to it, then.", 591);
                    dialogue.finishDialogue();
                }
                return true;
            case 42:
                // Asking what's wrong repeats the scream and returns to the
                // same choices, until the player elects to leave.
                dialogue.showNpcOneLineDialogue("AAAAAARRRRGGHHHH!", 591);
                dialogue.setNextDialogueStep(40);
                return true;
            default:
                dialogue.finishDialogue();
                return true;
        }
    }

    /** The tome can also be picked up from its original entrance-table spawn. */
    private static void grantTome(Player player) {
        if (!player.getInventoryManager().containsItem(CRUMBLING_TOME_ID)) {
            player.getInventoryManager().addOrDropItem(new ItemStack(CRUMBLING_TOME_ID));
        }
    }
}
