package ds.test;
import java.util.Vector;

import ds.server.*;
import ds.utils.DSConstants;


public class MainForTest {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		
		//other Main_Example call to main methods can be added here for a more complete testing of all functionnalities
		
		String[] deIds = {"182124046", "179466001", "178452205"}; // list of the ids of the genes of interest
		
		String[] ncdfs = {"/ebi/ArrayExpress-files/NetCDFs.DWDEV/330400871_175818769.nc",
						  "/ebi/ArrayExpress-files/NetCDFs.DWDEV/308971793_175818769.nc"}; // netcdf fullpath
		
		DataServerAPI.setImagePath("/Volumes/Work/aedsImages/"); // set image path in DataServer
		
		try{
			
			DataServerAPI.drawPlots(deIds, ncdfs, DSConstants.PLOT_SIZE_LARGE, DSConstants.PLOT_TYPE_TIMESERIES);
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
		
