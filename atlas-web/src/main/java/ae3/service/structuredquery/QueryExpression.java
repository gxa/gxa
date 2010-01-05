package ae3.service.structuredquery;

import java.util.ArrayList;
import java.util.List;

/**
     * Gene epxression option
 */
public enum QueryExpression {
    UP_DOWN("up/down"),
    UP("up"),
    DOWN("down");

    private String description;
    QueryExpression(String description) { this.description = description; }

    /**
     * Get human-readable option description
     * @return description string
     */
    public String getDescription() { return description; }

    /**
     * Lists all available options and their human-readable representation
     * @return list of gene expression options
     */
    static public List<String[]> getOptionsList() {
        List<String[]> result = new ArrayList<String[]>();
        for(QueryExpression r : values())
        {
           result.add(new String[] { r.name(), r.getDescription() });
        }
        return result;
    }

    static public QueryExpression parseFuzzyString(String s) {
        s = s.toLowerCase();
        boolean hasUp = s.contains("up");
        boolean hasDn = s.contains("dn") || s.contains("down");
        if(!(hasUp ^ hasDn))
            return UP_DOWN;
        return hasUp ? UP : DOWN;
    }
}
