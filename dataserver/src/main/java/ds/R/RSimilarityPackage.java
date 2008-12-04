package ds.R;

import remoting.RServices;
import ds.server.SimilarityResultSet;

public interface RSimilarityPackage {
	
	public SimilarityResultSet runSimilarity(String geneid, String netCDF,  int closestGenes, RServices r);
	
	public SimilarityResultSet getSimilarityResult();

}
