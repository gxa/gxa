package ae3.service.structuredquery;

import ae3.model.AtlasGene;
import ae3.util.DecoratedSolrDocument;

import java.util.List;
import java.util.Map;

import org.apache.solr.common.SolrDocument;

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
