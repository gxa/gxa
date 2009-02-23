package ae3.service.structuredquery;

import java.util.List;
import java.util.ArrayList;

/**
     * Gene epxression option
 */
public enum Expression {
    UP_DOWN("up/down"),
    UP("up"),
    DOWN("down");

    private String description;
    Expression(String description) { this.description = description; }

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
        for(Expression r : values())
        {
           result.add(new String[] { r.name(), r.getDescription() });
        }
        return result;
    }
}
