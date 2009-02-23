package ae3.service.structuredquery;

/**
 * @author pashky
 */
public class AutoCompleteItem implements Comparable<AutoCompleteItem> {
    private final String property;
    private final String value;
    private final Long count;
    private final String comment;

    public AutoCompleteItem(String property, String value, Long count, final String comment) {
        this.property = property;
        this.value = value;
        this.count = count;
        this.comment = comment;
    }

    public String getProperty() {
        return property;
    }

    public String getValue() {
        return value;
    }

    public Long getCount() {
        return count;
    }

    public String getComment() {
        return comment;
    }

    public int compareTo(AutoCompleteItem o) {
        return value.toLowerCase().compareTo(o.getValue().toLowerCase());
    }
}
