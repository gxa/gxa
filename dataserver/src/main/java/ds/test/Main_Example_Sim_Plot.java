package ds.test;
import java.util.Vector;

import ds.server.*;
import ds.utils.DSConstants;


public class Main_Example_Sim_Plot {

	/**
	 * @param args
	 */

	// Class for testing purpose only **********
	
	public static void main(String[] args) {
		
		
		String[] deIds = {"182124046", "179466001", "178452205"}; // list of the ids of the genes of interest
		
		String[] ncdfs = {"/ebi/ArrayExpress-files/NetCDFs.DWDEV/330400871_175818769.nc",
						  "/ebi/ArrayExpress-files/NetCDFs.DWDEV/308971793_175818769.nc"}; // netcdf fullpath
		

		String outputPath = "/Volumes/Workspace/Projects/AEdataserver/images"; // path to where the images will be drawn
		
		DataServerAPI.setNetCDFPath("/Volumes/Workspace/Projects/DWfiles/NetCDFs.DWDEV/"); // set the netcdf path
		DataServerAPI.setDatabaseConnection(DSConstants.DATABASE_DWDEV);
		DataServerAPI.setImagePath(outputPath); // set image path in DataServer
		
		try{
			
			SimilarityResultSet ssrs = DataServerAPI.retrieveSimilarityByGeneIdent("ENSG00000100906", "E-GEOD-6400", 3, DSConstants.SIMILARITY_EUCLIDEAN);
			ssrs.size();
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
		
