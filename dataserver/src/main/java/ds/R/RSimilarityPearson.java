package ds.R;


import org.bioconductor.packages.rservices.RMatrix;

import remoting.RServices;


import ds.server.SimilarityResultSet;
import ds.utils.DSConstants;

/**
 * 
 * Class that takes care of calculating the Pearson Similarity.
 * 
 * @author hugo
 *
 */

public class RSimilarityPearson extends Thread implements RSimilarityPackage{


	private static final long serialVersionUID = -8959215136767562220L;
	private String method  ="Pearson"; // method name used
	SimilarityResultSet srs; // object that stores the similarity result
	
	private String rSourcePath = DSConstants.R_SOURCES_PATH;
	private boolean currentProcessFinished = false; // Flag to represent whether or not the process is finished
	
	public RSimilarityPearson() {
		
		srs = new SimilarityResultSet();
		
	}
	
	public SimilarityResultSet runSimilarity(String geneid, String netCDF,  int closestGenes, RServices r) {
		
		try{
			r.evaluate("source('" + rSourcePath + "/Similarity_Pearson.R');"); // load R source related to Euclidean Similarity Search
				RMatrix simRes = (RMatrix) r.getObject("aew.sim.pear('"+ netCDF + "', '" + geneid + "', " + closestGenes + ");"); //run similarity
	
			srs.loadResult(simRes); // retrieve result from R object and store it into a Similarity Result Set
			srs.setMethodUsed(this.method);
			srs.setColNames(new String[] { "target designelement id","correlation", "p-value", "adjusted p-value [fdr]" }); // column names related to the result object

			currentProcessFinished = true;

		}
		catch(Exception e){
			e.printStackTrace();
		}
		
		return srs;
		
	}
	
	
	public boolean isCurrentProcessFinished() {
		return currentProcessFinished;
	}

	public void setCurrentProcessFinished(boolean currentProcessFinished) {
		this.currentProcessFinished = currentProcessFinished;
	}

	public String getMethod() {
		return method;
	}
	
	public SimilarityResultSet getSimilarityResult() {
		return srs;
	}


}
