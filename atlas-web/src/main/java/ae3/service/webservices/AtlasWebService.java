package ae3.service.webservices;

import java.util.HashMap;
import java.util.List;

import java.rmi.RemoteException;

public interface AtlasWebService {
    public List<HashMap> query(String q_gene, String q_expt, String q_orgn, String q_updn);
    public List<HashMap> batchQuery(String[] q_genes, String[] q_expts, String q_orgn, String q_updn) throws RemoteException;
    public HashMap getAtlasGene(String geneIdentifier) throws RemoteException;
    public HashMap getAtlasExperiment(String exptAccession) throws RemoteException;              
}