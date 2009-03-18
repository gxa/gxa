package ae3.service.webservices;

import ae3.service.AtlasResultSet;
import ae3.service.ArrayExpressSearchService;
import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.dao.MultipleGeneException;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.rmi.RemoteException;

public class AtlasWebServiceImpl implements AtlasWebService {
    private Log log = LogFactory.getLog(getClass());

    public List<HashMap> query(String q_gene, String q_expt, String q_orgn, String q_updn) {
        if(null != q_expt && q_expt.endsWith("*")) q_expt = q_expt.replaceAll("[*]$","?*");

        QueryResponse exptHitsResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(q_expt);

        if(null!= q_gene && q_gene.endsWith("*")) q_gene= q_gene.replaceAll("[*]$","?*");
        QueryResponse geneHitsResponse = ArrayExpressSearchService.instance().fullTextQueryGenes(q_gene);

        AtlasResultSet arset = null;
        try {
            arset = ArrayExpressSearchService.instance().doAtlasQuery(geneHitsResponse, exptHitsResponse, q_updn, q_orgn);
        } catch (IOException e) {
            log.error(e);
        }

        if(null == arset) return null;

        return arset.getAllAtlasResults(null);
    }

    public List<HashMap> batchQuery(String[] q_genes, String[] q_expts, String q_orgn, String q_updn) throws RemoteException {
        String q_gene = null;
        if (q_genes.length > 500) throw new RemoteException("Too many genes in query; must be under 500.");
        if (q_genes.length > 0 && !q_genes[0].equals("")) q_gene = "gene_ids:(" + StringUtils.join(q_genes, " ") + ")";

        String q_expt = null;
        if (q_expts.length > 500) throw new RemoteException("Too many experiments in query; must be under 500.");
        if (q_expts.length > 0 && !q_expts[0].equals("")) q_expt = "dwe_txt_accession:(" + StringUtils.join(q_expts, " ") + ")";

        if((null == q_gene && null == q_expt) || ("".equals(q_gene) && "".equals(q_expt))) throw new RemoteException("No arguments supplied!");

        return query(q_gene, q_expt, q_orgn, q_updn);
    }

    public HashMap getAtlasGene(String geneIdentifier) throws RemoteException {
        try {
            return AtlasDao.getGeneByIdentifier(geneIdentifier).serializeForWebServices();
        } catch (AtlasObjectNotFoundException e) {
            throw new RemoteException(e.getMessage());
        } catch (MultipleGeneException e) {
        	throw new RemoteException(e.getMessage());
        }
    }

    public HashMap getAtlasExperiment(String exptAccession) throws RemoteException {
        try {
            return AtlasDao.getExperimentByAccession(exptAccession).serializeForWebServices();
        } catch (AtlasObjectNotFoundException e) {
            throw new RemoteException(e.getMessage());
        }
    }
}
