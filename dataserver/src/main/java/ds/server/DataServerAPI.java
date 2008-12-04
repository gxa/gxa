package ds.server;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Vector;

import uk.ac.ebi.microarray.pools.TimeoutException;
import ds.utils.DSConstants;

public class DataServerAPI {
	
	private static String netCDFlocation;


	/**
	 * 
	 * Draws plots in the specified filepath 
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.PLOT_TYPE_TIMESERIES , RConstants.TYPE_ATLAS or RConstants.TYPE_BOXPLOT)
	 * @param filepath where the images will be saved
	 */
	
	public static DataServerMonitor drawPlots(String[] deIds, String[] netcdfs, String plotSize, String plotType) {

		DataServer ds = new DataServer();
		ds.drawPlots(deIds, netcdfs, plotSize, plotType, DSConstants.IMAGES_PATH, null);
		return ds;
	}
	
	
	/**
	 * 
	 * Retrieves an ExpressionData containing the data in a netcdf file 
	 * 
	 * @param ncName
	 * @return
	 */
	
	public static ExpressionDataSet retrieveExpressionDataSet(String ncName) {
		
		DataServer ds = new DataServer();
		
		return ds.retrieveExpressionDataSet(ncName);
		
	}
	
	/**
	 * 
	 * Retrieves an ExpressionData containing the data in a netcdf file 
	 * 
	 * @param ncName
	 * @return
	 */
	
	public static ExpressionDataSet retrieveExpressionDataSet(String[]deIds, String ncName) {
		
		DataServer ds = new DataServer();
		
		return ds.retrieveExpressionDataSet(deIds, ncName);
		
	}
	
	/**
	 * 
	 * Retrieves an ExpressionData containing the data in a netcdf file 
	 * 
	 * @param ncName
	 * @return
	 */
	
	public static ExpressionDataSet retrieveExpressionDataSet(String geneIdentifier, String experimentIdentifier, String factor) throws Exception {
		
		DataServer ds = new DataServer();
		
		return ds.retrieveExpressionDataSet(geneIdentifier, experimentIdentifier, factor);
		
	}
	
	/**
	 * 
	 * Retrieves an ExpressionData containing the data in a netcdf file 
	 * 
	 * @param ncName
	 * @return
	 */
	
	public static double[][] retrieveExpressionMatrix(String[] deIds, String ncName) {
		
		DataServer ds = new DataServer();
		
		return ds.retrieveExpressionDataSet(deIds, ncName).getExpressionMatrix();
		
	}
	
	
	/**
	 * 
	 * Retrieve expression matrix for an ExpressionDataSet
	 * 
	 * @param eds ExpressionDataSet for which we want to retrieve the expression matrix
	 * @return a matrix of double containing the expression value
	 * @throws Exception
	 */
	
	public static double[][] retrieveExpressionMatrix (ExpressionDataSet eds) throws Exception{
		
		DataServer ds = new DataServer();
		
		return ds.retrieveExpressionMatrix(eds);
	}
	
	public static double[][] retrieveOrderedExpressionMatrix (String[]deIds, String ncName, String factor) throws Exception{
		
		DataServer ds = new DataServer();
		
		return ds.retrieveExpressionMatrix(deIds, ncName, factor);

	}
	
	
	
	public static double[][] retrieveOrderedExpressionMatrix (ExpressionDataSet eds, String factor) throws Exception{
		
		DataServer ds = new DataServer();
		
		return ds.retrieveExpressionMatrix(eds, factor);
	}
	
	public static String[] retrieveFactorsForNetCDF(String filepath) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {
		
		DataServer ds = new DataServer();
		return ds.retrieveFactorsForNetCDF(filepath);
		
	}
	
	public static String[] retrieveFactorValuesForNetCDF(String filepath, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {
		
		DataServer ds = new DataServer();
		return ds.retrieveFactorValuesForNetCDF(filepath, factor);
		
	}
	
	/**
	 * 
	 * Retrieve Similarity Data via a SimilarityResultSet object
	 * 
	 * @param netCDF
	 * @param deId
	 * @param topMatches
	 * @param method
	 * @return
	 * @throws Exception
	 */
	
	public static DataServerMonitor launchSimilarity(String deId, String netCDF,  int topMatches, String method) throws Exception {
		
		DataServer ds = new DataServer();
		
		ds.launchSimilarity(netCDF, deId, topMatches, method);
		
		return ds;
	}
	
	/**
	 * 
	 * Retrieve Similarity Data via a SimilarityResultSet object
	 * 
	 * @param netCDF
	 * @param deId
	 * @param topMatches
	 * @param method
	 * @return
	 * @throws Exception
	 */
	
	public static SimilarityResultSet launchSimilarityAndRetrieveResult(String deId, String netCDF, int topMatches, String method) throws Exception {
		
		DataServer ds = new DataServer();
		
		return ds.launchSimilarityAndRetrieveResult(deId, netCDF, topMatches, method);
		
	}
	
	/**
	 * 
	 * Draws plots to the specified filepath for a list of ids and a list of netcdfs file. The method will also returns the complete filepaths of all the images once they are drawn.
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.PLOT_TYPE_TIMESERIES , RConstants.TYPE_ATLAS or RConstants.TYPE_BOXPLOT)
	 */
	
	public static Vector<String> drawPlotsAndRetrieveImagePath (String[] deIds, String[] netcdfs, String plotSize, String plotType) {
		
		DataServer ds = new DataServer();
		
		return ds.drawPlotsAndRetrieveImagePath(deIds, netcdfs, plotSize, plotType, DSConstants.IMAGES_PATH, null);
		
	}
	
	/**
	 * 
	 * Draws plots to the specified filepath for a list of ids and one netcdf file. The method will also returns the complete filepaths of all the images once they are drawn.
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.PLOT_TYPE_TIMESERIES , RConstants.TYPE_ATLAS or RConstants.TYPE_BOXPLOT)
	 */
	
	public static DataServerMonitor drawPlots(String[] deIds, String netcdfs, String plotSize, String plotType,String factor) {
		
		DataServer ds = new DataServer();
		
		ds.drawPlots(deIds, new String[] {netcdfs}, plotSize, plotType, DSConstants.IMAGES_PATH, factor);
		return ds;
		
	}
	
	public static DataServerMonitor drawPlots(String[] deIds, String netcdfs, String plotSize, String plotType) {
		
		DataServer ds = new DataServer();
		
		ds.drawPlots(deIds, new String[] {netcdfs}, plotSize, plotType, DSConstants.IMAGES_PATH, null);
		return ds;
		
	}
	
	/**
	 * 
	 * Draws plots to the specified filepath for one id and a list of netcdfs file. The method will also returns the complete filepaths of all the images once they are drawn.
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.PLOT_TYPE_TIMESERIES , RConstants.TYPE_ATLAS or RConstants.TYPE_BOXPLOT)
	 */
	
	public static DataServerMonitor drawPlots(String deIds, String[] netcdfs, String plotSize, String plotType) {
		
		DataServer ds = new DataServer();
		
		ds.drawPlots(new String[] {deIds}, netcdfs, plotSize, plotType, DSConstants.IMAGES_PATH, null);
		return ds;
	}
	
	public static DataServerMonitor drawPlots(ExpressionDataSet eds, String plotSize, String plotType){
		
		DataServer ds = new DataServer();
		
		ds.drawPlots(eds.getAr_DE(), eds.getFilename(), plotSize, plotType, DSConstants.IMAGES_PATH, null);
		
		return ds;
		
	}
	
	public static DataServerMonitor drawPlots(ExpressionDataSet eds, String plotSize, String plotType, String factor){
		
		DataServer ds = new DataServer();
		
		ds.drawPlots(eds.getAr_DE(), eds.getFilename(), plotSize, plotType, DSConstants.IMAGES_PATH, factor);
		
		return ds;
		
	}
	
	/**
	 * 
	 * Will launch RPlotter threads for drawing plots. This method does not wait on the Plotting thread to be finished. 
	 * Caution is needed when accessing the created images as they might not be finished of drawn when accessed.
	 * 
	 * To actually wait on drawing process to be finished call method 'drawPlotsAndRetrieveImagePath' instead or method 'waitOnProcessToFinish' after this one.
	 * 
	 */
	
	public static DataServerMonitor drawPlots(String deIds, String netcdfs, String plotSize, String plotType, String factor) {
		
		DataServer ds = new DataServer();
		
		ds.drawPlots(new String[] {deIds}, new String[] {netcdfs}, plotSize, plotType, DSConstants.IMAGES_PATH, factor);
		return ds;
	}
	
	/**
	 * 
	 * 	 Will launch RPlotter threads for drawing plots but the main process will wait on all the RPlotter threads to be finished and will return
		 a vector of strings containing all the fullpaths to the images. This method might be useful to call when we need to wait on the images 
		 to be generated before continuing and when we also need to have to images' path. 
	 * 
	 * @param deIds list of ids
	 * @param netcdfs list of netcdfs (fullpath)
	 * @param plotSize size of the plot
	 * @param plotType type of plot
	 * @param filepath filepath where the images will be drawn
	 * @return
	 */
	
	public static Vector<String> drawPlotsAndRetrieveImagePath (String[] deIds, String netcdfs, String plotSize, String plotType, String factor) {
		
		DataServer ds = new DataServer();
		
		return ds.drawPlotsAndRetrieveImagePath (deIds, new String[] {netcdfs}, plotSize, plotType, DSConstants.IMAGES_PATH, factor);
		
	}
	
	public static Vector<String> drawPlotsAndRetrieveImagePath (String[] deIds, String netcdfs, String plotSize, String plotType) {
		
		DataServer ds = new DataServer();
		
		return ds.drawPlotsAndRetrieveImagePath (deIds, new String[] {netcdfs}, plotSize, plotType, DSConstants.IMAGES_PATH, null);
		
	}
	

	/**
	 * 
	 * 	 Draws plots but the main process will wait for process to be finished and will return
		 a vector of strings containing all the fullpaths to the images. This method might be useful to call when we need to wait on the images 
		 to be generated before continuing and when we also need to have to images' path. 
	 * 
	 * @param deIds an id
	 * @param netcdfs list of netcdfs (fullpath)
	 * @param plotSize size of the plot
	 * @param plotType type of plot
	 * @param filepath filepath where the images will be drawn
	 * @return
	 */
	
	public static Vector<String> drawPlotsAndRetrieveImagePath (String deIds, String netcdfs, String plotSize, String plotType, String factor) {
		
		DataServer ds = new DataServer();
		
		return ds.drawPlotsAndRetrieveImagePath (new String[] {deIds}, new String[] {netcdfs}, plotSize, plotType, DSConstants.IMAGES_PATH, factor);
		
	}
	
	public static Vector<String> drawPlotsAndRetrieveImagePath (String deIds, String netcdfs, String plotSize, String plotType) {
		
		DataServer ds = new DataServer();
		
		return ds.drawPlotsAndRetrieveImagePath (new String[] {deIds}, new String[] {netcdfs}, plotSize, plotType, DSConstants.IMAGES_PATH, null);
		
	}
	
	/**
	 * 
	 * Draw Large plots for a specific gene identifier and for a specific experiment identifier.
	 * 
	 * @param geneIdent gene name
	 * @param expIdent experiment identifier
	 * @param plotType type of plot to be plotted ('Atlas', 'TimeSeries')
	 * @param path fullpath where to draw the images
	 * @return
	 */
	public static Vector<String> drawLargePlotsByIdentifierAndRetrieveImagePath(String geneIdent, String expIdent, String plotType,String factor) {
		
		DataServer ds = new DataServer();
		
		return ds.drawLargePlotsAndRetrieveImagePath(geneIdent, expIdent, plotType, DSConstants.IMAGES_PATH, factor);
		
		
	}
	
	public static Vector<String> drawLargePlotsByIdentifierAndRetrieveImagePath(String geneIdent, String expIdent, String plotType) {
		
		DataServer ds = new DataServer();
		
		return ds.drawLargePlotsAndRetrieveImagePath(geneIdent, expIdent, plotType, DSConstants.IMAGES_PATH, null);
		
		
	}
	
	/**
	 * 
	 * Draw Thumbnail plots for a specific gene identifier and for a specific experiment identifier.
	 * 
	 * @param geneIdent gene name
	 * @param expIdent experiment identifier
	 * @param plotType type of plot to be plotted ('Atlas', 'TimeSeries')
	 * @param path fullpath where to draw the images
	 * @param factor for which to draw the plot, null means all the factors
	 * @return
	 */
	public static Vector<String> drawThumbnailPlotsByIdentifierAndRetrieveImagePath(String geneIdent, String expIdent, String plotType, String factor) {
		
		DataServer ds = new DataServer();
		
		return ds.drawThumbnailPlotsAndRetrieveImagePath(geneIdent, expIdent, plotType, DSConstants.IMAGES_PATH, factor);
		
		
	}
	
	/**
	 * 
	 * Draw Thumbnail plots for a specific gene identifier and for a specific experiment identifier.
	 * 
	 * @param geneIdent gene name
	 * @param expIdent experiment identifier
	 * @param plotType type of plot to be plotted ('Atlas', 'TimeSeries')
	 * @param path fullpath where to draw the images
	 * @return
	 */
	
	public static Vector<String> drawThumbnailPlotsByIdentifierAndRetrieveImagePath(String geneIdent, String expIdent, String plotType) {
		
		DataServer ds = new DataServer();
		
		return ds.drawThumbnailPlotsAndRetrieveImagePath(geneIdent, expIdent, plotType, DSConstants.IMAGES_PATH, null);
		
		
	}
	
	/**
	 * 
	 * Draw Large plots for a specific gene identifier and for a specific experiment identifier.
	 * 
	 * @param geneIdent gene name
	 * @param expIdent experiment identifier
	 * @param plotType type of plot to be plotted ('Atlas', 'TimeSeries')
	 * @param path fullpath where to draw the images
	 * @param factor factor for which to draw the plot
	 * @return an interface to the server object that allow to follow the status of the processes
	 */
	public static DataServerMonitor drawLargePlots(String geneIdent, String expIdent, String plotType, String factor) {
		
		DataServer ds = new DataServer();
		
		ds.drawLargePlots(geneIdent, expIdent, plotType, DSConstants.IMAGES_PATH, factor);
		return ds;
		
	}
	
	/**
	 * 
	 * Draw Large plots for a specific gene identifier and for a specific experiment identifier.
	 * 
	 * @param geneIdent gene name
	 * @param expIdent experiment identifier
	 * @param plotType type of plot to be plotted ('Atlas', 'TimeSeries')
	 * @param path fullpath where to draw the images
	 * @return an interface to the server object that allow to follow the status of the processes
	 */
	
	public static DataServerMonitor drawLargePlots(String geneIdent, String expIdent, String plotType) {
		
		DataServer ds = new DataServer();
		
		ds.drawLargePlots(geneIdent, expIdent, plotType, DSConstants.IMAGES_PATH, null);
		return ds;
		
	}

	
	/**
	 * 
	 * Retrieve the most similar genes to the gene for the experiment passed in parameters 
	 * 
	 * @param geneIdent gene name
	 * @param expIdent experiment identifier
	 * @param topMatches number of matches to retrieve
	 * @param method similarity method to use
	 * @return
	 * @throws Exception
	 */	
	public static SimilarityResultSet retrieveSimilarityByGeneIdent(String geneIdent, String expIdent, int topMatches, String method) throws Exception {
		
		DataServer ds = new DataServer();
		
		return ds.retrieveSimilarityByGeneIdent(geneIdent, expIdent, topMatches, method);
		
	}
	
	/**
	 * 
	 * Set up a Database Connection. Needed for retrieving annotations. Database configuration can be changed in RConstants.
	 * 
	 * @param databaseName
	 */

	public static void setDatabaseConnection(String databaseName) {
		
		if (databaseName.equals("DWC")){
			DSConstants.DEFAULT_DATABASE_NAME = databaseName;
		}
		else if (databaseName.equals("DWDEV")) {
			DSConstants.DEFAULT_DATABASE_NAME = databaseName;
		}
		else{
			DSConstants.DEFAULT_DATABASE_NAME = "DWDEV";
		}
		
	}

	/**
	 * 
	 * Get Name of the Warehouse Database to whic hthe DataServer is conencted to retrieve annotations
	 * 
	 * @return
	 */
	public static String getDataBaseConnectionName() {
		
		return DSConstants.DEFAULT_DATABASE_NAME;
		
	}
	
	/**
	 * 
	 * Setup path to where the NetCDFs a located
	 * 
	 * @param fullpath
	 */
	
	public static void setNetCDFPath(String fullpath) {
		
		netCDFlocation = fullpath;
		
	}
	public static String getNetCDFPath() {
		
		return netCDFlocation;
		
	}
	
	public static String getImagePath() {
		
		return DSConstants.IMAGES_PATH;
		
	}
	
	public static void setImagePath(String iPath) {
		
		DSConstants.IMAGES_PATH = iPath;
		
	}
	
	 

}
