import com.rs2.model.gameplay.barrows.BarrowsTomeHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * No text may be dropped or reordered while fitting all fourteen source
 * pages onto the native revision-443 11-row-per-side book. Preserves each
 * original page's start even if a continuation page is necessary.
 *
 * Native widget limitations still preclude claiming pixel-identical 2006
 * pagination or animation without a reference client capture.
 */
public final class BarrowsTomeAuthenticityChecks {
    public static void main(String[] args) {
        require(BarrowsTomeHandler.originalPageCount() == 14,
                "The 2006 crumbling tome has fourteen original page boundaries");
        require(BarrowsTomeHandler.displayPageCount()
                        >= BarrowsTomeHandler.originalPageCount(),
                "Native book pages cannot be fewer than historical source pages");
        int displayIndex = 0;
        int continuations = 0;
        for (int sourceIndex = 0; sourceIndex < BarrowsTomeHandler.originalPageCount(); sourceIndex++) {
            List<String> originalWords = words(BarrowsTomeHandler.originalPage(sourceIndex));
            List<String> renderedWords = new ArrayList<String>();
            int pagesUsed = 0;
            while (renderedWords.size() < originalWords.size()) {
                require(displayIndex < BarrowsTomeHandler.displayPageCount(),
                        "Page " + (sourceIndex + 1) + " lost text off the end of the native book");
                String[] page = BarrowsTomeHandler.displayPage(displayIndex++);
                pagesUsed++;
                require(page.length == 11, "Native revision-443 page must have 11 rows");
                for (String line : page) {
                    require(line != null && line.length() <= 30,
                            "443 text row overflow at display page " + displayIndex);
                    renderedWords.addAll(words(line));
                }
                require(renderedWords.size() <= originalWords.size(),
                        "An original page spilled into a different source page");
            }
            require(renderedWords.equals(originalWords),
                    "Original 2006 page " + (sourceIndex + 1)
                            + " changed or omitted words in native widget rendering");
            if (pagesUsed > 1) continuations += pagesUsed - 1;
        }
        require(displayIndex == BarrowsTomeHandler.displayPageCount(),
                "Extra native pages appeared after the full original transcript");
        require(BarrowsTomeHandler.displaySpreadCount()
                        == (displayIndex + 1) / 2,
                "Native two-sided book spread count disagrees with display pages");
        System.out.println("Barrows tome: all fourteen 2006 source pages' wording "
                + "and boundaries preserved without clipping, native continuations="
                + continuations + "; full pixel-identical layout still unverified.");
    }

    private static List<String> words(String source) {
        if (source == null || source.trim().isEmpty()) return new ArrayList<String>();
        return Arrays.asList(source.trim().split("\\s+"));
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
