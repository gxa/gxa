package uk.ac.ebi.gxa.requesthandlers.api.result;

import ae3.model.AtlasGene;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;

import java.util.*;

import static uk.ac.ebi.gxa.utils.CollectionUtil.makeMap;

/**
 * Adaptor for a single gene
 *
 */
public class GeneResultAdapter implements ApiQueryResults<GeneResultAdapter.GeneResult>{
    final private AtlasProperties atlasProperties;
    final private AtlasGene gene;
    final private Set<String> geneIgnoreProp;

    public GeneResultAdapter(final AtlasGene gene, final AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
        this.gene = gene;
        this.geneIgnoreProp = new HashSet<String>(atlasProperties.getGeneApiIgnoreFields());
    }

    public long getTotalResults() {
        return 1;
    }

    public long getNumberOfResults() {
        return 1;
    }

    public long getStartingFrom() {
        return 0;
    }

    public Iterator<GeneResult> getResults() {
        return Collections.singletonList(new GeneResult(gene)).iterator();
    }

    @RestOut(name = "result")
    public class GeneResult {
        private AtlasGene gene_;

        public GeneResult(final AtlasGene gene) {
            this.gene_ = gene;
        }

        public Map getGene() {
            Map<String, Object> gene = makeMap(
                    "id", gene_.getGeneIdentifier(),
                    "name", gene_.getGeneName(),
                    "organism", gene_.getGeneSpecies(),
                    "orthologs", gene_.getOrthologs());
            for(Map.Entry<String, Collection<String>> prop : gene_.getGeneProperties().entrySet()) {
                if(!geneIgnoreProp.contains(prop.getKey()) && !prop.getValue().isEmpty()) {
                    String field = atlasProperties.getGeneApiFieldName(prop.getKey());
                    gene.put(field, field.endsWith("s") ? prop.getValue() : prop.getValue().iterator().next());
                }
            }

            return gene;
            
        }

        public Iterable getExpressionAnalyses() {
            return gene_.getExpressionAnalyticsTable().getAll();
        }
    }
}
