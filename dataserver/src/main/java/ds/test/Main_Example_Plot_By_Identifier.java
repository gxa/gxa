package ds.test;
import java.util.Vector;

import ds.server.*;
import ds.utils.DSConstants;


public class Main_Example_Plot_By_Identifier {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		

		// with this set up the DataServer use the database to fetch the corresponding ids 
		// matching to the gene and experiment name
		
		DataServerAPI.setImagePath("/Volumes/Workspace/Projects/AEdataserver/"); // set image path in DataServer
		DataServerAPI.setNetCDFPath("/Volumes/Workspace/Projects/DWfiles/NetCDFs.DWDEV/"); // set the netcdf path
		DataServerAPI.setDatabaseConnection(DSConstants.DATABASE_DWDEV);
		
		try{
			
			DataServerAPI.drawLargePlots("TP53", "E-TABM-147", DSConstants.PLOT_TYPE_ATLAS);
//			DataServerAPI.drawThumbnailPlotsByIdentifierAndRetrieveImagePath("TP53", "E-TABM-147", DSConstants.PLOT_TYPE_ATLAS);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
		
