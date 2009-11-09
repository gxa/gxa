package ae3.service.webservices;

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.List;

public interface AtlasWebService {
    List<HashMap> query(String q_gene, String q_expt, String q_orgn, String q_updn);

    List<HashMap> batchQuery(String[] q_genes, String[] q_expts, String q_orgn, String q_updn) throws RemoteException;

    HashMap getAtlasGene(String geneIdentifier) throws RemoteException;

    HashMap getAtlasExperiment(String exptAccession) throws RemoteException;
}