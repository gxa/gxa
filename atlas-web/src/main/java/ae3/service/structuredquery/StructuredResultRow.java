package ae3.service.structuredquery;

import ae3.model.AtlasGene;

import java.util.List;

/**
 * @author pashky
*/
public class StructuredResultRow {
    private AtlasGene gene;

    private List<UpdownCounter> updownCounters;

    public StructuredResultRow(AtlasGene gene, List<UpdownCounter> updownCounters) {
        this.gene = gene;
        this.updownCounters = updownCounters;
    }

    public AtlasGene getGene() {
        return gene;
    }

    public List<UpdownCounter> getCounters() {
        return updownCounters;
    }
}
