package ae3.service.structuredquery;

/**
 * @author pashky
 */
public class EfoAutoCompleteItem extends AutoCompleteItem {
    private int depth;

    public EfoAutoCompleteItem(String property, String id, String value, Long count, final int depth) {
        super(property, id, value, count);
        this.depth = depth;
    }

    public int getDepth() {
        return depth;
    }
}
