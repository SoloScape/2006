import com.rs2.cache.js5.Interfaces;
import java.util.Map;

/** Verify that the native 443 book has the expected text layout. */
public final class BarrowsBookWidgetAudit {
    public static void main(String[] args) throws Exception {
        Interfaces.load();
        Map<Integer, Interfaces.Component> widgets = Interfaces.group(49);
        if (widgets.isEmpty()) throw new AssertionError("Missing native book group 49");
        for (int i = 83; i <= 112; i++) {
            Interfaces.Component c = widgets.get(i);
            if (c == null) continue;
            System.out.println("book49:"+i+" type="+c.type+" action="+c.actionType
                    +" xy="+c.x+","+c.y+" size="+c.width+"x"+c.height
                    +" parent="+c.parentId);
        }
    }
}
