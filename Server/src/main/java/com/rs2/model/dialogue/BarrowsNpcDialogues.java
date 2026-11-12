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
                dialogue.showNpcOneLineDialogue("Dig, dig, dig!", 591);
                return true;
            case 2:
                // Four period-documented conversation branches. Never start
                // later miniquest, diary or the post-2021 warnings menu.
                int branch = GameUtil.randomInt(4);
                dialogue.setNextDialogueStep(10 + branch * 10);
                dialogue.showNpcOneLineDialogue(branch == 0
                        ? "Psst. Do you want to know a secret?"
                        : branch == 1 ? "Knock, knock."
                        : branch == 2 ? "What? I didn't ask for a book!"
                        : "AAAAAARRRRGGHHHH!", 591);
                if (branch == 2) {
                    grantTome(player);
                    dialogue.finishDialogue();
                }
                return true;
            case 10:
                dialogue.showTwoOptions("Yes, tell me.", "No, thanks.");
                return true;
            case 11:
                if (option == 1) {
                    dialogue.showNpcOneLineDialogue("Those brothers aren't ordinary men!", 591);
                } else {
                    dialogue.showPlayerOneLineDialogue("Perhaps another time.", 591);
                }
                dialogue.finishDialogue();
                return true;
            case 20:
                dialogue.showPlayerOneLineDialogue("Who's there?", 591);
                return true;
            case 21:
                dialogue.showNpcTwoLineDialogue(
                        "Something dreadful beneath the ground!",
                        "Hah! You should have seen your face.", 591);
                dialogue.finishDialogue();
                return true;
            case 40:
                dialogue.showTwoOptions("What's wrong?", "I'll leave you to it.");
                return true;
            case 41:
                dialogue.showPlayerOneLineDialogue(
                        option == 1 ? "What's wrong?" : "I'll leave you to it.", 591);
                dialogue.finishDialogue();
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
