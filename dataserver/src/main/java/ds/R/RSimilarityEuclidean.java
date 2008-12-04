package ds.R;

import org.bioconductor.packages.rservices.RMatrix;
import remoting.RServices;


import ds.server.SimilarityResultSet;
import ds.utils.DSConstants;

/**
 * 
 * Class that takes care of calculating the Euclidean Similarity.
 * 
 * @author hugo
 *
 */

public class RSimilarityEuclidean implements RSimilarityPackage{

	private String method = "Euclidean"; // method name used
	SimilarityResultSet srs; // object that stores the similarity result
	
	private String rSourcePath = DSConstants.R_SOURCES_PATH;
	private boolean currentProcessFinished = false; // Flag to represent whether or not the process is finished
	
	public RSimilarityEuclidean() {

		srs = new SimilarityResultSet();
		
	}
	
	public SimilarityResultSet runSimilarity(String geneid, String netCDF,  int closestGenes, RServices r) {
		
		try{
			r.evaluate("source('" + rSourcePath + "/Similarity_Euclidean.R');"); // load R source related to Euclidean Similarity Search
			
			RMatrix simRes = (RMatrix) r.getObject("aew.sim.euc('"+netCDF + "', '" + geneid + "', " + closestGenes + ");"); //run similarity
	
			srs.loadResult(simRes); // retrieve result from R object and store it into a Similarity Result Set
			srs.setColNames(new String[] { "target designelement id","correlation", "p-value" }); // column names related to the result object
			srs.setMethodUsed(this.method);

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

	public SimilarityResultSet getSrs() {
		return srs;
	}

	public void setSrs(SimilarityResultSet srs) {
		this.srs = srs;
	}


}
