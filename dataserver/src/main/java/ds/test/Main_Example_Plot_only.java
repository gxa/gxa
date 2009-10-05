package ds.test;
import java.util.Vector;

import ds.server.*;
import ds.utils.DSConstants;


public class Main_Example_Plot_only {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		
		
		String[] deIds = {"182124046", "179466001", "178452205"}; // list of the ids of the genes of interest
		
		String[] ncdfs = {"/Volumes/Workspace/Projects/DWfiles/NetCDFs.DWDEV/330400871_175818769.nc",
						  "/Volumes/Workspace/Projects/DWfiles/NetCDFs.DWDEV/308971793_175818769.nc"}; // netcdf fullpath
		
		DataServerAPI.setImagePath("/Volumes/Workspace/Projects/AEdataserver/images/"); // set image path in DataServer
		
		try{
			
			DataServerAPI.drawPlots(deIds, ncdfs, DSConstants.PLOT_SIZE_LARGE, DSConstants.PLOT_TYPE_TIMESERIES);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
		
