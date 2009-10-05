package ds.server;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Hashtable;
import java.util.Vector;


import ds.R.RUtilities;
import ds.utils.DS_DBconnection;

import org.kchine.rpf.TimeoutException;

/**
 * 
 *  The Data Processing class takes care launching the appropriate data analysis process. 
 *  Each instance of DataProcessing consist of a single R Process and provide information
 *  as to whether or not the process is finished or not.
 *  
 * @author hugo
 *
 */

public class DataProcessing{

	private RUtilities ru; 

	public DataProcessing() {

		ru = new RUtilities(); //initiating RUtilities
		

	}
	
	/**
	 * 
	 * Returns if the RUtilities process is finished or still running.
	 * 
	 * @return
	 */
	
	public boolean isCurrentProcessFinished() {
		return ru.isCurrentProcessFinished();
	}

	/**
	 * 
	 * Draw a thumbnail for an ExpressionDataSet object
	 * 
	 * @param deIds Vector of String containing the design elements ids
	 * @param ncName String containing the netcdfs filename with path
	 */

	public Vector<String> drawThumbnailBoxPlot(ExpressionDataSet eds, String path, String factor) {
		
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.drawThumbnailBoxPlot(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	public Vector<String> drawThumbnailTimeSeries(ExpressionDataSet eds, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.drawThumbnailTimeSeries(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	/**
	 * 
	 * Draw a large plot for an ExpressionDataSet object
	 * 
	 * @param deIds Vector of String containing the design elements ids
	 * @param ncName String containing the netcdfs filename with path
	 */

	public Vector<String> drawLargePlotTimeSeries(ExpressionDataSet eds, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.drawLargePlotTimeSeries(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	public Vector<String> drawLargePlotBoxPlot(ExpressionDataSet eds, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.drawLargePlotBoxPlot(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	/**
	 * 
	 * Will draw a BoxPlots-type large plot for each factor contained in the dataset with the specified package
	 * 
	 * @param eds expression dataset
	 */

	public Vector<String> drawLargePlotBoxPlot(ExpressionDataSet eds, int drawingPackage, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.setDrawingPackage(drawingPackage);
			ru.drawLargePlotBoxPlot(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	/**
	 * 
	 * Will draw a BoxPlots-type thumbnail plot for each factor contained in the dataset with the specified package
	 * 
	 * @param eds expression dataset
	 */

	public Vector<String> drawThumbnailBoxPlot(ExpressionDataSet eds, int drawingPackage, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.setDrawingPackage(drawingPackage);
			ru.drawThumbnailBoxPlot(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	/**
	 * 
	 * Will draw a Atlas-type large plot for each factor contained in the dataset
	 * 
	 * @param eds expression dataset
	 */

	public Vector<String> drawLargePlotAtlas(ExpressionDataSet eds, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.drawLargePlotAtlas(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	/**
	 * 
	 * Will draw a Atlas-type large thumbnail for each factor contained in the dataset
	 * 
	 * @param eds expression dataset
	 */

	public Vector<String> drawThumbnailAtlas(ExpressionDataSet eds, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.drawThumbnailAtlas(eds, factor);
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	/**
	 * 
	 * Will draw time series type tumbnail with the specied R drawing package
	 * 
	 * @param eds Dataset to be drawn
	 * @param drawingPackage R package to be used for drawing
	 */

	public Vector<String> drawThumbnailTimeSeries(ExpressionDataSet eds, int drawingPackage, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.setDrawingPackage(drawingPackage);
			ru.drawThumbnailTimeSeries(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	/**
	 * 
	 * Draw a large plot for an ExpressionDataSet object
	 * 
	 * @param deIds Vector of String containing the design elements ids
	 * @param ncName String containing the netcdfs filename with path
	 */

	public Vector<String> drawLargePlotTimeSeries(ExpressionDataSet eds, int drawingPackage, String path, String factor) {
		eds.setDeAnn(fetchDEAnn(eds.getAr_DE())); //fetch Gene Annotation from database
		ru.setThumbnails_filepath(path);

		try {
			ru.setDrawingPackage(drawingPackage);
			ru.drawLargePlotTimeSeries(eds, factor);
		} catch (Exception e) {
			e.printStackTrace();
		}

		return ru.getPlotPaths();

	}

	public static RankingResultSet getRanking(Vector<String> deIds, Vector<String> studyIds) {
		// TODO Auto-generated method stub
		return null;
	}

	public static SimilarityResultSet getSimilarity(Vector<String> deIds, String studyIds) {
		// TODO Auto-generated method stub
		return null;
	}

	

	public Vector<String> retrievePlotPaths() {
		return ru.getPlotPaths();
	}

	public/*static*/ExpressionDataSet joinDataSet(ExpressionDataSet eds1,
			ExpressionDataSet eds2) {

		ExpressionDataSet eds3 = eds1.addExpressionDataSet(eds2);

		return eds3;

	}

	public/*static*/Hashtable<String, String> fetchDEAnn(String[] ar_DE) {

		Hashtable<String, String> deAnn = new Hashtable<String, String>();

		Statement stmt;
		try {
			stmt = DS_DBconnection.instance().getConnection().createStatement();

			for (int a = 0; a < ar_DE.length; a++) {
				String sql = "SELECT GENE_NAME,GENE_IDENTIFIER FROM AE2__DESIGNELEMENT__MAIN WHERE DESIGNELEMENT_ID_KEY = "
						+ ar_DE[a];

				ResultSet rset = stmt.executeQuery(sql);
				while (rset.next()) {
					String geneName = rset.getString(1);
					String geneIdent = rset.getString(2);

					if (geneName != null) {
						deAnn.put(ar_DE[a], geneName);
					} else if (geneIdent != null) {
						deAnn.put(ar_DE[a], geneIdent);
					} else
						deAnn.put(ar_DE[a], ar_DE[a]);
				}

				rset.close();

			}
			stmt.close();

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return deAnn;

	}

	public ExpressionDataSet retrieveExpressionDataSet(String ncName) {

		ExpressionDataSet eds = new ExpressionDataSet();

		RUtilities ru = new RUtilities();

		try {
			eds = ru.retrieveExpressionDataSet(ncName);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return eds;

	}
	
	@Deprecated
	public ExpressionDataSet retrieveExpressionDataSet(String[] deIds, String ncName) {

		ExpressionDataSet eds = new ExpressionDataSet();

		RUtilities ru = new RUtilities();

		try {
			eds = ru.retrieveExpressionDataSet(deIds, ncName);
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RemoteException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NotBoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TimeoutException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return eds;

	}
	
	public ExpressionDataSet retrieveExpressionDataForDE(String netcdf, Vector<String> deIds, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException{
		ExpressionDataSet eds = new ExpressionDataSet();

//		RUtilities ru = new RUtilities();
		eds = ru.retrieveExpressionDataForDE(netcdf, deIds, factor);
		
		return eds;
	}
	
	
		
	public void launchSimilarity(String deId, String netCDF,  int topMatches, String method) throws Exception {
		
		ru.getSimilarity(deId, netCDF, topMatches, method);
		
	}
	
	public double[][] retrieveExpressionMatrix (ExpressionDataSet eds) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException{
		
		eds = loadExpressionMatrix(eds);
		
		return eds.getExpressionMatrix();
	}
	
	public double[][] retrieveExpressionMatrix (ExpressionDataSet eds, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException{
		
		eds = loadExpressionMatrix(eds, factor);
		
		return eds.getExpressionMatrix();
	}
	
	public double[][] retrieveExpressionMatrix (String[]deIds, String filename, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException{
		
		RUtilities ru = new RUtilities();
		
		return ru.retrieveExpressionMatrix(deIds, filename, factor);
	}
	
	public ExpressionDataSet loadExpressionMatrix(ExpressionDataSet eds) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException{
		
		RUtilities ru = new RUtilities();
		
		return ru.retrieveExpressionMatrix(eds);
		
	}
	
	public ExpressionDataSet loadExpressionMatrix(ExpressionDataSet eds, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException{
		
		RUtilities ru = new RUtilities();
		return ru.retrieveExpressionMatrix(eds, factor);
		
	}
	
	public SimilarityResultSet retrieveSimilarityResult(){
		
		return ru.retrieveSimilarityResult();
		
	}
	
	public String[] retrieveFactorsForNetCDF(String filepath) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {
		
		return ru.retrieveFactorsForNetCDF(filepath);
	}
	
	public String[] retrieveFactorValuesForNetCDF(String filepath, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {
		
		return ru.retrieveFactorValuesForNetCDF(filepath, factor);
	}

}
