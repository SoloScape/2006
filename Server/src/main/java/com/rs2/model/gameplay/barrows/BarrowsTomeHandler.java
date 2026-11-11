package com.rs2.model.gameplay.barrows;

import com.rs2.ServerSettings;
import com.rs2.model.player.Player;
import java.util.ArrayList;
import java.util.List;

/**
 * The Barrows crumbling tome (4707), also known as Legend of the Brothers.
 * Uses the existing two-page book UI, with the revision 443 native book bridge.
 * The original wording and fourteen 2006 page boundaries were supplied by
 * the player. The 443 client's native book (group 49) has only eleven
 * single-line text components per side, so longer original pages require
 * continuation pages instead of silently clipping the authentic wording.
 */
public final class BarrowsTomeHandler {
    public static final int TOME_ITEM_ID = 4707;
    private static final int BOOK_INTERFACE = 837;
    private static final int TITLE_TEXT = 903;
    private static final int FIRST_TEXT = 843;
    private static final int LEFT_PAGE = 14165;
    private static final int RIGHT_PAGE = 14166;

    private static final int ROWS_PER_PAGE = 11;
    private static final int MAX_LINE_CHARS = 30;
    // Source pages 1-14, split at the exact screenshot page boundaries.
    private static final String[] ORIGINAL_PAGES = {
        "During the great God Wars there were six brothers who fought side by side against the forces of evil. Their fervor in battle was awed and feared by all who saw them. Nothing is known of their origins, only that they were first seen when the legions of evil were pressing their hardest into civilised lands.\n\nIt was the eldest who struck the first blow in that battle, hurling powerful battle magic ",
        "into the enemies' ranks, opening a hole for four of the other brothers to charge through and into the heart of their forces. Little was seen or known of the sixth brother at the time, only that when one of the other brothers looked in danger his foe would be felled by a shower of crossbow bolts. It wasn't until the dust had settled that those left could look on their saviours, the six brothers who stood proud with the bodies of the fallen at their feet.\n\n",
        "For years these brothers fought hard against the enemy, throwing themselves into battle with little regard for their own safety and relying only on each other for aid. Stories were told around camp fires by soldiers of the feats they'd witnessed, they became legend long before the end.\n\nSome say he'd always been ",
        "there, but no-one really knows for sure. The first real tale was after an incursion deep into the dark lands, where once again the brothers proved their worth. It was after the battle that a bent and robed figure was seen amongst the bodies of the dead. Most took him for a scavenger, but he moved towards the camp fire of the brothers with such directness that some at first thought him another companion. The ",
        "brothers however reacted as with suspicion at his approach, drawing their weapons and rising from their places. The stranger merely stood before them, as though sizing them up before turning and walking back into the mists. Some say that it's the first time they'd ever seen the brothers unsure.\n\nAs the campaign moved deeper into the dark lands the stranger appeared with ",
        "greater frequency, often appearing on a rise near a battle and watching over it. The most disturbing thing was how his presence started to affect the brothers, they became distracted and on occasion even received injuries that required attention.\n\nIt was deep in the heart of the enemies' territory, in the failing light of dusk, that the enemy struck back. Great ",
        "monstrosities swept through the camps tearing at all in their path, soldiers scattered like chaff in the wind before the onslaught. The brothers, valiant as ever, stood back to back before the enemy and fought with all their might. As dawn broke the enemy fled the battle, moving faster than any man could give chase. It was as the soldiers went about rebuilding the camp that the brothers collapsed at their camp fire, they lay as ",
        "though dead, yet breathing, muttering in strange tongues. Their injuries were found swollen and infected, none of the physicians knew what to do, it was like no infection they had ever seen before. All through the day the brothers were tended and cared for while the soldiers waited nervously for their heroes to rise once again. It was then, just as the sun's last rays vanished behind the horizon, that all hope was lost. ",
        "As if on cue all the six brothers sighed one last breath and died. It was only then that the sentries noticed the stranger once again, standing on a rise just outside the camp.\n\nThat night the camp was surrounded by the great beasts of the enemy once again, but they didn't attack, they merely baited the soldiers to come out and meet them. Once again at ",
        "the break of dawn the enemy fled without a trace, leaving the soldiers tired from a restless night of worry and fear. That day the commanders decided to turn back, they could advance no further without the brothers' aid. As was the custom at the time, out of honour and respect for the mighty fallen, they constructed six great barrows for the brothers. A crypt for each brother covered in a small hill of ",
        "earth that would hold their remains undisturbed by predators. The work took many days, for each night the enemy returned and the soldiers had to flee back inside their camp to wait away the night to the sounds of howling and scratching at the fences. But each day they resumed their work, their sense of duty to the brothers surmounting their fear of the dark forces in the land.\n\n",
        "On the last day the assembled troops buried the brothers inside their stone tombs, then with great reverence, but little ceremony, they covered the tombs with earth. As the army broke camp and turned to march back to their lands the stranger was seen once again, standing atop the central barrow... standing tall against the failing light with his arms outstretched to the ",
        "sky.\nNot many of the original army made it back to these lands, many more had perished on the journey home, but tales were told of how each night an unnatural glow could be seen on the horizon where they had buried the brothers. Even today there are stories told about those mounds of earth, and of the sights and sounds that have been witnessed ",
        "nearby."
    };
    private static final String[][] DISPLAY_PAGES = createDisplayPages();
    private static final int DISPLAY_SPREAD_COUNT = (DISPLAY_PAGES.length + 1) / 2;

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
                if (player.activeBookPageIndex + 1 < DISPLAY_SPREAD_COUNT) ++player.activeBookPageIndex;
                render(player);
                return true;
            default:
                return false;
        }
    }

    public static int originalPageCount() { return ORIGINAL_PAGES.length; }
    public static int displayPageCount() { return DISPLAY_PAGES.length; }
    public static int displaySpreadCount() { return DISPLAY_SPREAD_COUNT; }

    public static String originalPage(int pageIndex) { return ORIGINAL_PAGES[pageIndex]; }
    public static String[] displayPage(int pageIndex) { return DISPLAY_PAGES[pageIndex].clone(); }

    private static void render(Player player) {
        // 443 book arrows require an interface-open ACK, even at boundaries.
        if (ServerSettings.clientBuild == 443 || player.getOpenInterfaceId() != BOOK_INTERFACE) {
            player.getPacketSender().showInterface(BOOK_INTERFACE);
        }
        int firstPage = player.activeBookPageIndex * 2;
        for (int i = 0; i < ROWS_PER_PAGE * 2; i++) {
            int page = firstPage + i / ROWS_PER_PAGE;
            int row = i % ROWS_PER_PAGE;
            String text = page < DISPLAY_PAGES.length ? DISPLAY_PAGES[page][row] : "";
            player.getPacketSender().sendInterfaceText(text, FIRST_TEXT + i);
        }
        player.getPacketSender().sendInterfaceText("Legend of the Brothers", TITLE_TEXT);
        player.getPacketSender().sendInterfaceText("Page " + (firstPage + 1), LEFT_PAGE);
        player.getPacketSender().sendInterfaceText(
                firstPage + 1 < DISPLAY_PAGES.length ? "Page " + (firstPage + 2) : "", RIGHT_PAGE);
    }

    private static String[][] createDisplayPages() {
        List<String[]> display = new ArrayList<String[]>();
        for (String sourcePage : ORIGINAL_PAGES) {
            List<String> rows = new ArrayList<String>();
            // Paragraph breaks, including the single newline before "Not many",
            // remain visible. Content itself is never rewritten or omitted.
            for (String paragraph : sourcePage.trim().split("\\n+")) {
                if (!rows.isEmpty()) rows.add("");
                StringBuilder line = new StringBuilder();
                for (String word : paragraph.trim().split("\\s+")) {
                    if (word.length() == 0) continue;
                    if (line.length() != 0 && line.length() + 1 + word.length() > MAX_LINE_CHARS) {
                        rows.add(line.toString());
                        line.setLength(0);
                    }
                    if (line.length() > 0) line.append(' ');
                    line.append(word);
                }
                if (line.length() > 0) rows.add(line.toString());
            }
            // Start each original screenshot page on a fresh native page;
            // spill text into continuation pages rather than truncate it.
            for (int from = 0; from < rows.size(); from += ROWS_PER_PAGE) {
                String[] page = new String[ROWS_PER_PAGE];
                for (int i = 0; i < ROWS_PER_PAGE; i++) {
                    int index = from + i;
                    page[i] = index < rows.size() ? rows.get(index) : "";
                }
                display.add(page);
            }
        }
        return display.toArray(new String[display.size()][]);
    }
}
