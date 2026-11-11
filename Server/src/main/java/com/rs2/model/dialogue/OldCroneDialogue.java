package com.rs2.model.dialogue;

import com.rs2.model.World;
import com.rs2.model.npc.NpcDefinition;
import com.rs2.model.player.Player;

/**
 * Period-appropriate conversation for the Old Crone east of the Slayer Tower.
 * She is a Ghosts Ahoy / Animal Magnetism character. Those quests are not yet
 * in this server's QuestDefinition scripts, so this dialogue deliberately
 * provides her introduction and hints without handing out quest rewards or
 * pretending the unfinished quest stages have been completed.
 */
public final class OldCroneDialogue {
    private OldCroneDialogue() { }

    public static boolean isOldCrone(int npcId) {
        // Scripted dialogue IDs (e.g. Barrows repair dialogue 10089) are not NPC IDs.
        if (npcId < 0 || npcId >= World.getNpcDefinitions().length) return false;
        NpcDefinition npc = NpcDefinition.forId(npcId);
        return npc != null && "Old crone".equalsIgnoreCase(npc.getName());
    }

    public static boolean continueDialogue(Player player, int step, int option) {
        DialogueManager dialogue = player.getDialogueManager();
        switch (step) {
            case 1:
                dialogue.showPlayerOneLineDialogue("Hello there.", 591);
                return true;
            case 2:
                dialogue.showNpcTwoLineDialogue(
                        "Oh, a visitor! It's been a while since", "anyone came to see this old lady.", 591);
                return true;
            case 3:
                dialogue.showThreeOptions(
                        "Do you know anything about Necrovarus?",
                        "Do you know Velorina of Port Phasmatys?",
                        "Have you lived here long?");
                return true;
            case 4:
                switch (option) {
                    case 1:
                        dialogue.showNpcThreeLineDialogue(
                                "Necrovarus... I knew him long ago, I think.",
                                "My memory is not what it used to be.",
                                "A nice cup of nettle tea might help.", 591);
                        dialogue.finishDialogue();
                        return true;
                    case 2:
                        dialogue.showNpcThreeLineDialogue(
                                "Velorina? She was a good friend of mine.",
                                "We lived in Port Phasmatys together.",
                                "I fled before Necrovarus came to power.", 591);
                        dialogue.setNextDialogueStep(5);
                        return true;
                    case 3:
                        dialogue.showNpcThreeLineDialogue(
                                "Oh, many years. My son sailed away",
                                "to join the pirates when he was young.",
                                "I still keep his little model ship.", 591);
                        dialogue.finishDialogue();
                        return true;
                    default:
                        dialogue.showNpcOneLineDialogue("Do come back to visit, dear.", 591);
                        dialogue.finishDialogue();
                        return true;
                }
            case 5:
                dialogue.showNpcThreeLineDialogue(
                        "She may still need help. When I can remember",
                        "the old enchantment, a ghostspeak amulet",
                        "might let you command Necrovarus himself.", 591);
                dialogue.finishDialogue();
                return true;
            default:
                dialogue.finishDialogue();
                return true;
        }
    }
}
