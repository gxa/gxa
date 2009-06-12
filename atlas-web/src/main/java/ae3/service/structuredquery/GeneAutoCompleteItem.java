package ae3.service.structuredquery;

import java.util.Collection;

/**
 * @author pashky
 */
public class GeneAutoCompleteItem extends AutoCompleteItem {
    private String species;
    private Collection<String> otherNames;
    
    public GeneAutoCompleteItem(String property, String value, Long count, final String species, final String geneId, final Collection<String> otherNames) {
        super(property, geneId != null ? geneId : value, value, count);
        this.species = species;
        this.otherNames = otherNames;
    }

    public String getSpecies() {
        return species;
    }

    public Collection<String> getOtherNames() {
        return otherNames;
    }
}
