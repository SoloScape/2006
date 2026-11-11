package com.rs2.model.gameplay.barrows;

import com.rs2.ServerSettings;
import com.rs2.model.player.Player;

/**
 * The Barrows crumbling tome (4707), also known as Legend of the Brothers.
 * Uses the existing two-page book UI, with the revision 443 native book bridge.
 * This is a retelling of the 2005 Barrows legend, not a quest-state change.
 */
public final class BarrowsTomeHandler {
    public static final int TOME_ITEM_ID = 4707;
    private static final int BOOK_INTERFACE = 837;
    private static final int TITLE_TEXT = 903;
    private static final int FIRST_TEXT = 843;
    private static final int LEFT_PAGE = 14165;
    private static final int RIGHT_PAGE = 14166;

    // Eleven short lines per page, left page followed by right page.
    private static final String[][] SPREADS = {
        {
            "The Legend of the", "Brothers", "",
            "During the God Wars,", "six brothers fought as", "one against an evil", "army in Morytania.", "",
            "Their valour and skill", "became legendary.", "",
            "The eldest wielded", "mighty battle magic.", "Four of his brothers",
            "charged into battle", "with sword and axe.", "",
            "The youngest stayed", "out of sight, guarding", "his brothers with", "deadly crossbow fire.", ""
        },
        {
            "For a time their army", "pressed onwards, but", "a silent stranger", "followed their camp.", "",
            "As the brothers fought,", "a dark fate awaited", "them in the east.", "",
            "The six grew weaker.",
            "One by one the", "brothers were lost.", "Their army could no", "longer advance without",
            "the six commanders.", "",
            "At last the soldiers", "began their retreat,", "carrying the bodies", "of their fallen heroes.", "", ""
        },
        {
            "To honour the six,", "their comrades built", "six stone crypts and", "raised earthen mounds", "above their graves.", "",
            "Each night the enemy", "returned and the army", "hid behind its camp", "defences.", "",
            "The soldiers laid", "each brother to rest", "beneath the soil,", "hoping their tombs", "would stay undisturbed.", "",
            "The stranger was seen", "again atop the hills,", "arms raised towards", "the darkening sky.", ""
        },
        {
            "Few of the soldiers", "made it home alive.", "",
            "Those who survived", "spoke of strange light", "over the six barrows", "after nightfall.", "",
            "Travellers still", "tell of sounds that",
            "rise from the tombs.", "Perhaps the warriors", "rest there still.", "",
            "And perhaps the old", "stories are a warning", "not to disturb", "their sleeping dead.", "", "", "", ""
        }
    };

    private BarrowsTomeHandler() { }

    public static boolean read(Player player, int itemId) {
        if (itemId != TOME_ITEM_ID) return false;
        if (!player.getInventoryManager().getContainer().containsItem(TOME_ITEM_ID)) return true;
        player.activeBookItemId = TOME_ITEM_ID;
        player.activeBookPageIndex = 0;
        render(player);
        return true;
    }

    public static boolean handleBookButton(Player player, int buttonId) {
        if (player == null || player.activeBookItemId != TOME_ITEM_ID
                || player.getOpenInterfaceId() != BOOK_INTERFACE) return false;
        switch (buttonId) {
            case 10162:
                player.activeBookItemId = 0;
                player.getPacketSender().closeInterfaces();
                return true;
            case 839:
            case 3071:
                if (player.activeBookPageIndex > 0) --player.activeBookPageIndex;
                render(player);
                return true;
            case 841:
            case 3073:
                if (player.activeBookPageIndex + 1 < SPREADS.length) ++player.activeBookPageIndex;
                render(player);
                return true;
            default:
                return false;
        }
    }

    private static void render(Player player) {
        // Native 443 book arrows need an interface-open ACK on every click,
        // including an attempted turn at the first or last page.
        if (ServerSettings.clientBuild == 443 || player.getOpenInterfaceId() != BOOK_INTERFACE) {
            player.getPacketSender().showInterface(BOOK_INTERFACE);
        }
        String[] lines = SPREADS[player.activeBookPageIndex];
        for (int i = 0; i < 22; i++) {
            player.getPacketSender().sendInterfaceText(i < lines.length ? lines[i] : "", FIRST_TEXT + i);
        }
        player.getPacketSender().sendInterfaceText("Legend of the Brothers", TITLE_TEXT);
        player.getPacketSender().sendInterfaceText("Page " + (player.activeBookPageIndex * 2 + 1), LEFT_PAGE);
        player.getPacketSender().sendInterfaceText("Page " + (player.activeBookPageIndex * 2 + 2), RIGHT_PAGE);
    }
}
