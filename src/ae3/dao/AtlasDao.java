package ae3.dao;

import ae3.model.AtlasExperiment;
import ae3.model.AtlasGene;
import ae3.service.ArrayExpressSearchService;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrDocument;

import java.util.Map;
import java.util.Collection;

/**
 * Created by IntelliJ IDEA.
 * User: ostolop, mdylag
 * Date: Apr 17, 2008
 * Time: 9:32:22 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDao {
    public static AtlasExperiment getExperiment(String experiment_id_key) throws AtlasObjectNotFoundException {
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryExpts("exp_id:" + experiment_id_key);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(experiment_id_key);

        SolrDocument exptDoc = documentList.get(0);

        return new AtlasExperiment(exptDoc);
    }

    public static AtlasExperiment getExperiment(SolrDocument solrExptDoc, QueryResponse exptHitsResponse) {
        AtlasExperiment expt = new AtlasExperiment(solrExptDoc);
        expt.setExperimentHighlights(exptHitsResponse.getHighlighting().get(expt.getExperimentId()));

        return expt;
    }

    public static AtlasGene getGene(String gene_id_key) throws AtlasObjectNotFoundException {
        QueryResponse queryResponse = ArrayExpressSearchService.instance().fullTextQueryGenes("gene_id:" + gene_id_key);

        SolrDocumentList documentList = queryResponse.getResults();

        if (documentList == null || documentList.size() == 0)
            throw new AtlasObjectNotFoundException(gene_id_key);

        SolrDocument geneDoc = documentList.get(0);

        return new AtlasGene(geneDoc);
    }

    public static AtlasGene getGene(SolrDocument solrGeneDoc, QueryResponse geneHitsResponse) {
        AtlasGene gene = new AtlasGene(solrGeneDoc);
        gene.setGeneHighlights(geneHitsResponse.getHighlighting().get(gene.getGeneId()));
        
        return gene;
    }
}
