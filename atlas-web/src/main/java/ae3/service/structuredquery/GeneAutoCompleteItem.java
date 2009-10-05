package ae3.service.structuredquery;

import java.util.Collection;

/**
 * @author pashky
 */
public class GeneAutoCompleteItem extends AutoCompleteItem {
    private String species;
    private Collection<String> otherNames;
    private String valueSource;

    public GeneAutoCompleteItem(String property, String value, Long count, final String species, final String geneId, final Collection<String> otherNames, String valueSource) {
        super(property, geneId != null ? geneId : value, value, count);
        this.species = species;
        this.otherNames = otherNames;
        this.valueSource = valueSource;
    }

    public String getSpecies() {
        return species;
    }

    public Collection<String> getOtherNames() {
        return otherNames;
    }

    public String getValueSource() {
        return valueSource;
    }
}
