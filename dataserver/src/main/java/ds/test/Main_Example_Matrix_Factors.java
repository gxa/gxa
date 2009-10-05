package ds.test;
import java.util.Vector;

import ds.server.*;
import ds.utils.DSConstants;


public class Main_Example_Matrix_Factors {

	/**
	 * @param args
	 */

	public static void main(String[] args) {
		
		
		String[] deIds = {"182124046", "179466001", "178452205"}; // list of the ids of the genes of interest
		
		String[] ncdfs = {"/Volumes/Workspace/Projects/DWfiles/NetCDFs.DWDEV/330400871_175818769.nc"}; // netcdf fullpath
		
		DataServerAPI.setImagePath("/Volumes/Work/aedsImages/"); // set image path in DataServer
		
		try{
			
			ExpressionDataSet eds = DataServerAPI.retrieveExpressionDataSet(ncdfs[0]);
			DataServerAPI.retrieveExpressionMatrix(eds);
			
			// retrieve factors for this exp
//			String ef[] = DataServerAPI.retrieveFactorsForNetCDF(ncdfs[0]);
//			// retrieve expression matrix order by factor values of factor[0] -> "ba_compound"
//			double mat[][] = DataServerAPI.retrieveOrderedExpressionMatrix(deIds, ncdfs[0], ef[0]);
//			// retrieve factor values for this factor
//			String[] efval = DataServerAPI.retrieveFactorValuesForNetCDF(ncdfs[0], ef[0]);
//			
//			// printing out the matrix along with factor values			
//			System.out.println("Factor: "+ef[0]);
//			// Going over matrix and printing out values
//			for (int a = 0 ; a < mat.length; a++){
//				for(int b = 0; b < mat[0].length; b++){
//					System.out.print(mat[a][b]+"("+efval[b]+"), ");
//				}
//				System.out.print("\n");
//			}
			
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
}
		
