package ds.R;

import ds.server.SimilarityResultSet;
import org.kchine.r.server.RServices;

public interface RSimilarityPackage {
	
	public SimilarityResultSet runSimilarity(String geneid, String netCDF,  int closestGenes, RServices r);
	
	public SimilarityResultSet getSimilarityResult();

}
