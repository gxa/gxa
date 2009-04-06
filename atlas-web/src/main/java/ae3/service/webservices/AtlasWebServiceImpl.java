package ae3.service.webservices;

import ae3.service.ArrayExpressSearchService;
import ae3.service.AtlasResult;
import ae3.dao.AtlasDao;
import ae3.dao.AtlasObjectNotFoundException;
import ae3.dao.MultipleGeneException;

import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.rmi.RemoteException;

@SuppressWarnings("unchecked")
public class AtlasWebServiceImpl implements AtlasWebService {
    private Logger log = LoggerFactory.getLogger(getClass());

    public List<HashMap> query(String q_gene, String q_expt, String q_orgn, String q_updn) {
        log.info("Atlas Web Service Query. Gene: {}, Expt: {}, Organism: {}, UpDn: {}", new String[] {q_gene, q_expt, q_orgn, q_updn});
        if(null != q_expt && q_expt.endsWith("*")) q_expt = q_expt.replaceAll("[*]$","?*");

        QueryResponse exptHitsResponse = ArrayExpressSearchService.instance().fullTextQueryExpts(q_expt);

        if(null!= q_gene && q_gene.endsWith("*")) q_gene= q_gene.replaceAll("[*]$","?*");
        QueryResponse geneHitsResponse = ArrayExpressSearchService.instance().fullTextQueryGenes(q_gene);

        List<AtlasResult> arset = ArrayExpressSearchService.instance().doAtlasQuery(geneHitsResponse, exptHitsResponse, q_updn, q_orgn);

        if(null == arset) return null;

        List<HashMap> res = new Vector<HashMap>();
        for(AtlasResult ar : arset) {
            HashMap h = new HashMap();
            h.put("experiment_id",          ar.getExperiment().getDwExpId());
            h.put("experiment_accession",   ar.getExperiment().getDwExpAccession());
            h.put("experiment_description", ar.getExperiment().getDwExpDescription());

            h.put("gene_id",                ar.getGene().getGeneId());
            h.put("gene_name",              ar.getGene().getGeneName());
            h.put("gene_identifier",        ar.getGene().getGeneIdentifier());
            h.put("gene_species",           ar.getGene().getGeneSpecies());
            h.put("gene_highlights",        ar.getGene().getGeneHighlightStringForHtml());

            h.put("ef",                     ar.getAtuple().getEf());
            h.put("efv",                    ar.getAtuple().getEfv());
            h.put("updn",                   ar.getAtuple().getUpdn());
            h.put("updn_pvaladj",           ar.getAtuple().getPval());

            res.add(h);
        }

        return res;
    }

    public List<HashMap> batchQuery(String[] q_genes, String[] q_expts, String q_orgn, String q_updn) throws RemoteException {
        log.info("Atlas Web Service Batch Query. Genes: {}, Expts: {}, Organism: {}, UpDn: {}", new String[] {StringUtils.join(q_genes, " "), StringUtils.join(q_expts, "; "), q_orgn, q_updn});

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
        log.info("Atlas Web Service Gene Query for {}", geneIdentifier);
        try {
            return AtlasDao.getGeneByIdentifier(geneIdentifier).serializeForWebServices();
        } catch (AtlasObjectNotFoundException e) {
            throw new RemoteException(e.getMessage());
        } catch (MultipleGeneException e) {
        	throw new RemoteException(e.getMessage());
        }
    }

    public HashMap getAtlasExperiment(String exptAccession) throws RemoteException {
        log.info("Atlas Web Service Experiment Query for {}", exptAccession);

        try {
            return AtlasDao.getExperimentByAccession(exptAccession).serializeForWebServices();
        } catch (AtlasObjectNotFoundException e) {
            throw new RemoteException(e.getMessage());
        }
    }
}
