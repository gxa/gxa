package ds.server;

import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.TreeMap;
import java.util.Vector;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import ucar.ma2.Array;
import ucar.ma2.ArrayChar;
import ucar.ma2.ArrayObject;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;
import uk.ac.ebi.microarray.pools.TimeoutException;

import ds.R.RUtilities;

import ds.utils.DSConstants;
import ds.utils.DS_DBconnection;


public class DataServer implements DataServerMonitor {

	DataProcessing[] dpProcess;
	private String netCDFsPath = DataServerAPI.getNetCDFPath();
	private final Log log = LogFactory.getLog(getClass());
	
	/**
	 * 
	 * Draws plots. Since no filepath is specified thte iamges will be drawn in the user directory.
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.PLOT_TYPE_TIMESERIES, RConstants.PLOT_TYPE_ATLAS or RConstants.PLOT_TYPE_BOXPLOT)
	 */
	public void drawPlots(String[] deIds, String[] netcdfs, String plotSize, String plotType){
		
		String filepath = System.getProperty("user.dir");
		drawPlots(deIds, netcdfs, plotSize, plotType, filepath, null);
		
	}
	
	
	
	
	/**
	 * 
	 * Draws plots in the specified filepath 
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn (RConstants.PLOT_SIZE_LARGE, RConstants.PLOT_SIZE_SMALL)
	 * @param plotType type of plot drawn (RConstants.PLOT_TYPE_TIMESERIES, RConstants.PLOT_TYPE_ATLAS or RConstants.PLOT_TYPE_BOXPLOT)
	 * @param filepath where the images will be saved
	 */
	
	public void drawPlots(String[] deIds, String[] netcdfs, String plotSize, String plotType, String filepath, String factor) {
		
		dpProcess = new DataProcessing[netcdfs.length];

		for (int a = 0; a < netcdfs.length; a++) {
			
			DataProcessing dp = new DataProcessing();
			dpProcess[a] = dp;
			
			ExpressionDataSet curr_eds = new ExpressionDataSet();
			
			curr_eds.setAr_DE(deIds);
			curr_eds.setFilename(netcdfs[a]);

			if (plotSize.equalsIgnoreCase(DSConstants.PLOT_SIZE_SMALL)) {

				if (plotType.equalsIgnoreCase(DSConstants.PLOT_TYPE_TIMESERIES))
					dp.drawThumbnailTimeSeries(curr_eds, RUtilities.DRAWINGPACKAGE_MATPLOT, filepath, factor);
				else if (plotType.equalsIgnoreCase(DSConstants.PLOT_TYPE_BOXPLOT))
					dp.drawThumbnailBoxPlot(curr_eds, RUtilities.DRAWINGPACKAGE_GGPLOT, filepath, factor);
				else // is Atlas plot then
					dp.drawThumbnailAtlas(curr_eds, filepath, factor);

			} else {

				if (plotType.equalsIgnoreCase(DSConstants.PLOT_TYPE_TIMESERIES))
					dp.drawLargePlotTimeSeries(curr_eds, RUtilities.DRAWINGPACKAGE_MATPLOT, filepath, factor);
				else if (plotType.equalsIgnoreCase(DSConstants.PLOT_TYPE_BOXPLOT))
					dp.drawLargePlotBoxPlot(curr_eds, RUtilities.DRAWINGPACKAGE_GGPLOT, filepath, factor);
				else // is Atlas plot then
					dp.drawLargePlotAtlas(curr_eds, filepath, factor);

			}

		}

	}
	
	/**
	 * 
	 * Returns whether this process is finished or not.
	 * 
	 */
	
	public boolean isProcessFinished(){
		
		return isProcessFinished(this.dpProcess);
		
	}
	
	/**
	 * 
	 * Methods that tells whether process(es) launched are finished
	 * 
	 * @param status
	 * @return boolean representing whether or not the processes are finished
	 */
	
	private boolean isProcessFinished(DataProcessing[] status){
		
		boolean result = true;
	
		for(int b = 0; b < status.length; b++){
			result = result && status[b].isCurrentProcessFinished(); 
		}
		
		return result;
		
	}
	
	/**
	 * 
	 * Wait current process until processes are finished. May be useful if wanting to be sure process is finished before displaying images.
	 * 
	 */
	
	public void waitOnProcessToFinish(){
		
		while (!isProcessFinished(dpProcess)){
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * 
	 * Retrieves images fullpath from all the R Plotting processes launched by this instance of the data server. Caution is need when calling this 
	 * method as R processes might not be finished of drawing. It is a good idea to call the 'waitOnProcessToFinish' method before calling this one.
	 * 
	 * @return
	 */
	
	public Vector<String> retrieveImagePath(){
		
		Vector<String> imagePath = new Vector<String>();
		
		if (!isProcessFinished())
			waitOnProcessToFinish();
		
		for (int a = 0; a < dpProcess.length; a++) {

			imagePath.addAll(dpProcess[a].retrievePlotPaths());

		}
		dpProcess = null;
		return imagePath;
		
	}
	
	/**
	 * 
	 * Retrieves images fullpath from all the R Plotting processes launched by this instance of the data server. Caution is need when calling this 
	 * method as R processes might not be finished of drawing. It is a good idea to call the 'waitOnProcessToFinish' method before calling this one.
	 * 
	 * @return
	 */
	
	public SimilarityResultSet retrieveSimilarityResult(){
		
		return dpProcess[0].retrieveSimilarityResult();
		
	}
	
	/**
	 * 
	 * Retrieves an ExpressionData containing the data in a netcdf file 
	 * 
	 * @param ncName
	 * @return
	 */
	
	public ExpressionDataSet retrieveExpressionDataSet(String ncName) {
		
		DataProcessing dp = new DataProcessing();
		
		return dp.retrieveExpressionDataSet(ncName);
		
	}
	
	public ExpressionDataSet retrieveExpressionDataSet(String geneIdentifier, String expIdentifier, String factor) throws Exception{
		
		
		//String ADid = netCDF.substring(netCDF.indexOf("_")+1, netCDF.indexOf(".nc"));
		String deId_ADid = getDEforGene(geneIdentifier,expIdentifier,factor);
		String[] ids = deId_ADid.split("_");
		String deId = ids[0];
		String adId = ids[1];
		String netCDF;
		//String netCDF = getNetCDF(geneIdentifier, expIdentifier);
		if(isExpRatio(expIdentifier,adId))
			netCDF = netCDFsPath+"/"+expIdentifier+"_"+adId+"_ratios.nc";	
		else
			netCDF = netCDFsPath+"/"+expIdentifier+"_"+adId+".nc";
		Vector deIds = new Vector<String>(); deIds.add(deId);
		ExpressionDataSet eds = getDataFromNetCDF(netCDF,deIds,factor);
//		DataProcessing dp = new DataProcessing();
//		dpProcess = new DataProcessing[] {dp};
//		ExpressionDataSet eds = dp.retrieveExpressionDataForDE(netCDF,deIds,factor);
//		this.waitOnProcessToFinish();
		return eds;
		
	}
	
	private ExpressionDataSet getDataFromNetCDF(String filename, Vector<String> deIds, String factor){
		 NetcdfFile ncfile = null;
		 ExpressionDataSet eds = new ExpressionDataSet();
		  try {
		    ncfile = NetcdfFile.open(filename);
		    Variable bdc = ncfile.findVariable("BDC");
		    Variable de = ncfile.findVariable("DE");
		    Variable bs = ncfile.findVariable("BS");
		    Variable efv = ncfile.findVariable("EFV");
		    Variable ef = ncfile.findVariable("EF");
		    

		    //Get index of current EF
		    Object[] EFarr = (Object[])((ArrayChar)ef.read()).make1DStringArray().get1DJavaArray(String.class); //there should be a better way to read array of strings from netcdf
		    int EFindex = Arrays.asList(EFarr).indexOf(factor);
		    
		    //Get FVs for the current EF
		    int[] shapeEFV = efv.getShape();      int[] originEFV = new int[ efv.getRank()];
		    originEFV[0] = EFindex; 
		    shapeEFV[0] = 1;   
		    Object[] EFVarr= (Object[])((ArrayChar)efv.read(originEFV,shapeEFV)).make1DStringArray().get1DJavaArray(String.class); 
		    
		    
		    //Get index of DE(s)
		    Array deArray = de.read();
		    int[] des = (int[])deArray.copyTo1DJavaArray();		   
		    List DElist = Arrays.asList(ArrayUtils.toObject(des));
		    ArrayList<Integer> DEindices = new ArrayList<Integer>();
		    for(String DEid : deIds){
		    	int DEindex = DElist.indexOf(Integer.parseInt(DEid));
		    	DEindices.add(DEindex);
		    }
		    
		    //Get data for this DE index
		    int[] shapeBDC = bdc.getShape(); int[] originBDC = new int[bdc.getRank()];
		    double[][] DEsData = new double[deIds.size()][];
		    for(int i=0; i< DEindices.size(); i++){
		    	originBDC[0]=DEindices.get(i);
			    shapeBDC[0]=1;
			    DEsData[i] =  (double[])bdc.read(originBDC,shapeBDC).reduce().get1DJavaArray(double.class);
		    }
		    
		      
		    TreeMap<String, ArrayList[] > dataPerFV_map = new TreeMap<String, ArrayList[]>();
		    for(int i=0; i<EFVarr.length; i++){
		    	String fv = EFVarr[i].toString();
		    	ArrayList[] dataPerFV;
		    	if(dataPerFV_map.containsKey(fv)){
		    		dataPerFV  = dataPerFV_map.get(fv);
//		    		dataPerFV_map.get(fv).add(DEdata[i]);
		    	}
		    	else{
		    		dataPerFV = new ArrayList[deIds.size()];
		    		for(int k=0; k<dataPerFV.length; k++)
		    			dataPerFV[k] = new ArrayList();
		    		dataPerFV_map.put(fv, dataPerFV);	
		    	}
		    	for(int j=0; j<DEindices.size(); j++){
	    			dataPerFV[j].add(DEsData[j][i]);
	    		}
		    		
		    }
		    
		    //Populate Expression dataset
		    
		    eds.addEFdata(factor,dataPerFV_map);
		    eds.setFactors(EFarr);
		    eds.setFactorValues(factor, dataPerFV_map.keySet());
		    
		  } catch (Exception ioe) {
			  log.error("trying to open " + filename, ioe);
		  } finally { 
		    if (null != ncfile) try {
		      ncfile.close();
		    } catch (IOException ioe) {
		      log.error("trying to close " + filename, ioe);
		    }
		  }
		  return eds;
	}
	
	/**
	 * 
	 * Retrieves an ExpressionData containing the data in a netcdf file 
	 * 
	 * @param ncName
	 * @return
	 */
	
	public ExpressionDataSet retrieveExpressionDataSet(String[] deIds, String ncName) {
		
		DataProcessing dp = new DataProcessing();
		
		return dp.retrieveExpressionDataSet(deIds, ncName);
		
	}
	
	/**
	 * 
	 * Returns the expression matrix for this ExpressionDataSet
	 * 
	 * @param eds
	 * @return
	 * @throws Exception
	 */
	
	public double[][] retrieveExpressionMatrix (ExpressionDataSet eds) throws Exception{
		
		DataProcessing dp = new DataProcessing();
		dpProcess = new DataProcessing[] {dp};
		
		return dp.retrieveExpressionMatrix(eds);
	}
	
	/**
	 * 
	 * Returns the expression matrix for this ExpressionDataSet order by a particular factor
	 * 
	 * @param eds
	 * @return
	 * @throws Exception
	 */
	
	public double[][] retrieveExpressionMatrix (ExpressionDataSet eds, String factor) throws Exception{
		
		DataProcessing dp = new DataProcessing();
		dpProcess = new DataProcessing[] {dp};
		
		return dp.retrieveExpressionMatrix(eds, factor);
	}
	
	/**
	 * 
	 * Returns the expression matrix for this ExpressionDataSet order by a particular factor
	 * 
	 * @param ncName fullpath + filename
	 * @param deIds ids of the design element
	 * @param factor name of the factor on which the data will be sorted
	 * @return
	 * @throws Exception
	 */
	
	public double[][] retrieveExpressionMatrix (String[]deIds, String ncName, String factor) throws Exception{
		
		DataProcessing dp = new DataProcessing();
		dpProcess = new DataProcessing[] {dp};
		
		return dp.retrieveExpressionMatrix(deIds, ncName, factor);
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
	
	public void launchSimilarity(String deId, String netCDF,  int topMatches, String method) throws Exception {
		
		DataProcessing dp = new DataProcessing();
		dpProcess = new DataProcessing[] {dp};
		
		dp.launchSimilarity(netCDF, deId, topMatches, method);
		
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
	
	public SimilarityResultSet launchSimilarityAndRetrieveResult(String deId, String netCDF, int topMatches, String method) throws Exception {
		
		DataProcessing dp = new DataProcessing();
		
		dpProcess = new DataProcessing[] {dp};
		dp.launchSimilarity(deId, netCDF, topMatches, method);
		
		waitOnProcessToFinish();
		
		return dp.retrieveSimilarityResult();
		
		
	}
	
	/**
	 * 
	 * Returns a list of Factors for the current file.
	 * 
	 * @param filepath
	 * @return
	 * @throws MalformedURLException
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws TimeoutException
	 */
	
	public String[] retrieveFactorsForNetCDF(String filepath) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {
		
		DataProcessing dp = new DataProcessing();
		return dp.retrieveFactorsForNetCDF(filepath);
		
	}
	
	/**
	 * 
	 * Returns list of all factor values for all samples in the file for the current factor.
	 * 
	 * @param filepath
	 * @param factor
	 * @return
	 * @throws MalformedURLException
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws TimeoutException
	 */
	
	public String[] retrieveFactorValuesForNetCDF(String filepath, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {
		
		DataProcessing dp = new DataProcessing();
		return dp.retrieveFactorValuesForNetCDF(filepath, factor);
		
	}
	
	
	
	/**
	 * 
	 * Draws plots to the specified filepath for a list of ids and a list of netcdfs file. The method will also returns the complete filepaths of all the images once they are drawn.
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.TYPE_TIMESERIES, "Atlas" or RConstants.TYPE_BOXPLOT)
	 */
	
	public Vector<String> drawPlotsAndRetrieveImagePath (String[] deIds, String[] netcdfs, String plotSize, String plotType, String filepath, String factor) {
		
		drawPlots(deIds, netcdfs, plotSize, plotType, filepath, factor);
		
		waitOnProcessToFinish();
		
		return retrieveImagePath();
		
	}
	
	/**
	 * 
	 * Draws plots to the specified filepath for a list of ids and one netcdf file. The method will also returns the complete filepaths of all the images once they are drawn.
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.TYPE_TIMESERIES, "Atlas" or RConstants.TYPE_BOXPLOT)
	 */
	
	public void drawPlots(String[] deIds, String netcdfs, String plotSize, String plotType, String filepath, String factor) {
		
		drawPlots(deIds, new String[] {netcdfs}, plotSize, plotType, filepath, factor);
		
	}
	
	/**
	 * 
	 * Draws plots to the specified filepath for one id and a list of netcdfs file. The method will also returns the complete filepaths of all the images once they are drawn.
	 * 
	 * @param deIds list of ids 
	 * @param netcdfs list of netcdfs
	 * @param plotSize size of the plot to be drawn 
	 * @param plotType type of plot drawn (RConstants.TYPE_TIMESERIES, "Atlas" or RConstants.TYPE_BOXPLOT)
	 */
	
	public void drawPlots(String deIds, String[] netcdfs, String plotSize, String plotType, String filepath, String factor) {
		
		drawPlots(new String[] {deIds}, netcdfs, plotSize, plotType, filepath, factor);
		
	}
	
	/**
	 * 
	 * Will launch RPlotter threads for drawing plots. This method does not wait on the Plotting thread to be finished. 
	 * Caution is needed when accessing the created images as they might not be finished of drawn when accessed.
	 * 
	 * To actually wait on drawing process to be finished call method 'drawPlotsAndRetrieveImagePath' instead or method 'waitOnProcessToFinish' after this one.
	 * 
	 */
	
	public void drawPlots(String deIds, String netcdfs, String plotSize, String plotType, String filepath, String factor) {
		
		drawPlots(new String[] {deIds}, new String[] {netcdfs}, plotSize, plotType, filepath, factor);
		
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
	
	public Vector<String> drawPlotsAndRetrieveImagePath (String[] deIds, String netcdfs, String plotSize, String plotType, String filepath, String factor) {
		
		return drawPlotsAndRetrieveImagePath (deIds, new String[] {netcdfs}, plotSize, plotType, filepath, factor);
		
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
	
	public Vector<String> drawPlotsAndRetrieveImagePath (String deIds, String netcdfs, String plotSize, String plotType, String filepath, String factor) {
		
		return drawPlotsAndRetrieveImagePath (new String[] {deIds}, new String[] {netcdfs}, plotSize, plotType, filepath, factor);
		
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
	
	public SimilarityResultSet retrieveSimilarityByGeneIdent(String geneIdent, String expIdent, int topMatches, String method) throws Exception {
		
		DataProcessing dp = new DataProcessing();
		dpProcess = new DataProcessing[] {dp};
		
		String expId = new String();
		String arID = new String();
		String deId = new String();
		String ratio = new String();
		//String absolute = new String();
		String netCDFFile = new String();
		Statement stmt;
		Vector<String> deIds = new Vector<String>();
		
		try {
			stmt = DS_DBconnection.instance().getConnection().createStatement();

			String sql = "SELECT EV.EXPERIMENT_ID, EV.ARRAYDESIGN_ID, EV.DESIGNELEMENT_ID_KEY,RATIO,ABSOLUTE "
					+ "FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, AE2__GENE_ALL__DM G, "
					+ "AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='"
					+ expIdent
					+ "' "
					+ "AND E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = '"
					+ geneIdent.toUpperCase()
					+ "' AND "
					+ "G.GENE_ID_KEY = D.GENE_ID_KEY AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY AND ROWNUM < 2 ";

			
			
			
			
			ResultSet rset = stmt.executeQuery(sql);
			if (rset.next()) {
				expId = rset.getString(1);
				arID = rset.getString(2);
				deId = rset.getString(3);
				ratio = rset.getString(4);
				//absolute = rset.getString(5);
				deIds.add(deId);
			}

			if (ratio ==null){
				netCDFFile = expId+"_"+arID+".nc";
			}
			else {
				netCDFFile = expId+"_"+arID+"_ratio.nc";
			}
			
			rset.close();

			stmt.close();
			netCDFFile = netCDFsPath + netCDFFile;

			

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		SimilarityResultSet srs = launchSimilarityAndRetrieveResult(deId, netCDFFile, topMatches, method);
		srs.setSourceNetCDF(netCDFFile);
		return srs;
		
	}
	
	/**
	 * 
	 * Draw large plots by giving gene and experiment identifier instead of ids. This require querying the database in order to fetch the ids.
	 * 
	 * @param geneIdent
	 * @param expIdent
	 * @param plotType
	 * @param path
	 * @param factor
	 */
	
	public void drawLargePlots(String geneIdent, String expIdent, String plotType, String path, String factor) {
		
		/*
		 * SELECT EV.EXPERIMENT_ID, EV.ARRAYDESIGN_ID, EV.DESIGNELEMENT_ID_KEY,RATIO,ABSOLUTE 
		 * FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, AE2__GENE_ALL__DM G, 
		 * AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='E-AFMX-6' 
		 * AND E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = 'P53' AND 
		 * G.GENE_ID_KEY = D.GENE_ID_KEY AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY AND ROWNUM < 2
		 */
		
		//ExpressionDataSet ers = new ExpressionDataSet();
		String expId = new String();
		String arID = new String();
		String deId = new String();
		String ratio = new String();
		//String absolute = new String();
		String netCDFFile = new String();
		Statement stmt;
		Vector<String> deIds = new Vector<String>();
		
		try {
			stmt = DS_DBconnection.instance().getConnection().createStatement();

			String sql = "SELECT EV.EXPERIMENT_ID, EV.ARRAYDESIGN_ID, EV.DESIGNELEMENT_ID_KEY,RATIO,ABSOLUTE "
					+ "FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, AE2__GENE_ALL__DM G, "
					+ "AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='"
					+ expIdent
					+ "' "
					+ "AND E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = '"
					+ geneIdent.toUpperCase()
					+ "' AND "
					+ "G.GENE_ID_KEY = D.GENE_ID_KEY AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY AND ROWNUM < 2 ";

			
			
			
			
			ResultSet rset = stmt.executeQuery(sql);
			if (rset.next()) {
				expId = rset.getString(1);
				arID = rset.getString(2);
				deId = rset.getString(3);
				ratio = rset.getString(4);
				//absolute = rset.getString(5);
				deIds.add(deId);
			}

			if (ratio ==null){
				netCDFFile = expId+"_"+arID+".nc";
			}
			else {
				netCDFFile = expId+"_"+arID+"_ratio.nc";
			}
			
			rset.close();

			stmt.close();
			

			

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		drawPlots(deIds.toArray(new String[deIds.size()]), netCDFsPath+netCDFFile, DSConstants.PLOT_SIZE_LARGE, plotType, path, factor);
		
	}
		
	public Vector<String> drawLargePlotsAndRetrieveImagePath(String geneIdent, String expIdent, String plotType, String path, String factor) {
		
		/*
		 * SELECT EV.EXPERIMENT_ID, EV.ARRAYDESIGN_ID, EV.DESIGNELEMENT_ID_KEY,RATIO,ABSOLUTE 
		 * FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, AE2__GENE_ALL__DM G, 
		 * AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='E-AFMX-6' 
		 * AND E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = 'P53' AND 
		 * G.GENE_ID_KEY = D.GENE_ID_KEY AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY AND ROWNUM < 2
		 */
		
		//ExpressionDataSet ers = new ExpressionDataSet();
		
		Vector<String> expIds = new Vector<String>();
		Vector<String> arIDs = new Vector<String>();
		String deId = new String();
		String ratio = new String();
		//String absolute = new String();
		Vector<String> netCDFFiles = new Vector<String>();
		Statement stmt;
		Vector<String> deIds = new Vector<String>();
		
		try {
			
			
			
			stmt = DS_DBconnection.instance().getConnection().createStatement();

			/*
			String sql = "SELECT EV.EXPERIMENT_ID, EV.ARRAYDESIGN_ID, EV.DESIGNELEMENT_ID_KEY,RATIO,ABSOLUTE "
					+ "FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, AE2__GENE_ALL__DM G, "
					+ "AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='"
					+ expIdent
					+ "' "
					+ "AND E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = '"
					+ geneIdent.toUpperCase()
					+ "' AND "
					+ "G.GENE_ID_KEY = D.GENE_ID_KEY AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY AND ROWNUM < 2 ";
			*/
			/*
			String sql ="SELECT EV.DESIGNELEMENT_ID_KEY, EV.EXPERIMENT_ID FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, " +
					"AE2__GENE_ALL__DM G, AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='"+expIdent+"' AND " +
					"E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = '"+geneIdent.toUpperCase()+"' AND G.GENE_ID_KEY = D.GENE_ID_KEY " +
					"AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY GROUP BY EV.DESIGNELEMENT_ID_KEY, EV.EXPERIMENT_ID";
			*/
			
			String sql = "SELECT D.DESIGNELEMENT_ID_KEY FROM  " +
					"AE2__GENE_ALL__DM G, AE2__DESIGNELEMENT__MAIN D " +
					"WHERE G.VALUE = '"+geneIdent.toUpperCase()+"' AND G.GENE_ID_KEY = D.GENE_ID_KEY";
			
			
			
			ResultSet rset = stmt.executeQuery(sql);
			while (rset.next()) {
				deId = rset.getString(1);
				//expId = rset.getString(2);
				deIds.add(deId);
			}
			
			rset.close();

			stmt.close();

			stmt = DS_DBconnection.instance().getConnection().createStatement();
			
			sql = "SELECT ARRAYDESIGN_ID, E.EXPERIMENT_ID_KEY FROM AE1__ASSAY__MAIN A,  " +
					"AE1__EXPERIMENT__MAIN E WHERE E.EXPERIMENT_IDENTIFIER = '"+expIdent+"' AND " +
					"E.EXPERIMENT_ID_KEY = A.EXPERIMENT_ID_KEY AND ARRAYDESIGN_ID IS NOT " +
					"NULL group BY ARRAYDESIGN_ID, E.EXPERIMENT_ID_KEY"; 
			
			rset = stmt.executeQuery(sql);
			
			while (rset.next()) {
				arIDs.add(rset.getString(1));
				expIds.add(rset.getString(2));
			}
			
			rset.close();

			stmt.close();
			
			for (int a = 0 ; a < expIds.size(); a++)
			{
			
				stmt = DS_DBconnection.instance().getConnection().createStatement();
	
				sql = "SELECT RATIO FROM AE2__EXPRESSIONVALUE__MAIN " +
						"WHERE EXPERIMENT_ID = '"+expIds.get(a)+"' AND ARRAYDESIGN_ID = '"+arIDs.get(a)+"' AND ROWNUM < 2";
	
				
				rset = stmt.executeQuery(sql);
				if (rset.next()) {
					ratio = rset.getString(1);
					//absolute = rset.getString(2);
					//deIds.add(deId);
				}
	
	
				if (ratio ==null){
					netCDFFiles.add(netCDFsPath+expIds.get(a)+"_"+arIDs.get(a)+".nc");
				}
				else {
					netCDFFiles.add(netCDFsPath+expIds.get(a)+"_"+arIDs.get(a)+"_ratio.nc");
				}
				
				rset.close();
	
				stmt.close();
		}
			

			

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		drawPlots(deIds.toArray(new String[deIds.size()]), netCDFFiles.toArray(new String[netCDFFiles.size()]), DSConstants.PLOT_SIZE_LARGE, plotType, path, factor);
		
		waitOnProcessToFinish();
		
		return retrieveImagePath();
		
	}
	
	public Vector<String> drawThumbnailPlotsAndRetrieveImagePath(String geneIdent, String expIdent, String plotType, String path, String factor) {
		
		/*
		 * SELECT EV.EXPERIMENT_ID, EV.ARRAYDESIGN_ID, EV.DESIGNELEMENT_ID_KEY,RATIO,ABSOLUTE 
		 * FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, AE2__GENE_ALL__DM G, 
		 * AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='E-AFMX-6' 
		 * AND E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = 'P53' AND 
		 * G.GENE_ID_KEY = D.GENE_ID_KEY AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY AND ROWNUM < 2
		 */
		
		//ExpressionDataSet ers = new ExpressionDataSet();
		
		Vector<String> expIds = new Vector<String>();
		Vector<String> arIDs = new Vector<String>();
		String deId = new String();
		String ratio = new String();
		//String absolute = new String();
		Vector<String> netCDFFiles = new Vector<String>();
		Statement stmt;
		Vector<String> deIds = new Vector<String>();
		
		try {
			
			
			
			stmt = DS_DBconnection.instance().getConnection().createStatement();

			/*
			String sql = "SELECT EV.EXPERIMENT_ID, EV.ARRAYDESIGN_ID, EV.DESIGNELEMENT_ID_KEY,RATIO,ABSOLUTE "
					+ "FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, AE2__GENE_ALL__DM G, "
					+ "AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='"
					+ expIdent
					+ "' "
					+ "AND E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = '"
					+ geneIdent.toUpperCase()
					+ "' AND "
					+ "G.GENE_ID_KEY = D.GENE_ID_KEY AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY AND ROWNUM < 2 ";
			*/
			/*
			String sql ="SELECT EV.DESIGNELEMENT_ID_KEY, EV.EXPERIMENT_ID FROM AE2__EXPRESSIONVALUE__MAIN EV, AE1__EXPERIMENT__MAIN E, " +
					"AE2__GENE_ALL__DM G, AE2__DESIGNELEMENT__MAIN D WHERE E.EXPERIMENT_IDENTIFIER ='"+expIdent+"' AND " +
					"E.EXPERIMENT_ID_KEY=EV.EXPERIMENT_ID AND G.VALUE = '"+geneIdent.toUpperCase()+"' AND G.GENE_ID_KEY = D.GENE_ID_KEY " +
					"AND D.DESIGNELEMENT_ID_KEY = EV.DESIGNELEMENT_ID_KEY GROUP BY EV.DESIGNELEMENT_ID_KEY, EV.EXPERIMENT_ID";
			*/
			
			String sql = "SELECT D.DESIGNELEMENT_ID_KEY FROM  " +
					"AE2__GENE_ALL__DM G, AE2__DESIGNELEMENT__MAIN D " +
					"WHERE G.VALUE = '"+geneIdent.toUpperCase()+"' AND G.GENE_ID_KEY = D.GENE_ID_KEY";
			
			System.out.println(sql);
			
			ResultSet rset = stmt.executeQuery(sql);
			while (rset.next()) {
				deId = rset.getString(1);
				//expId = rset.getString(2);
				deIds.add(deId);
			}
			
			rset.close();

			stmt.close();

			stmt = DS_DBconnection.instance().getConnection().createStatement();
			
			sql = "SELECT ARRAYDESIGN_ID, E.EXPERIMENT_ID_KEY FROM AE1__ASSAY__MAIN A,  " +
					"AE1__EXPERIMENT__MAIN E WHERE E.EXPERIMENT_IDENTIFIER = '"+expIdent+"' AND " +
					"E.EXPERIMENT_ID_KEY = A.EXPERIMENT_ID_KEY AND ARRAYDESIGN_ID IS NOT " +
					"NULL group BY ARRAYDESIGN_ID, E.EXPERIMENT_ID_KEY"; 
			
			System.out.println(sql);
			
			rset = stmt.executeQuery(sql);
			
			while (rset.next()) {
				arIDs.add(rset.getString(1));
				expIds.add(rset.getString(2));
			}
			
			rset.close();

			stmt.close();
			
			for (int a = 0 ; a < expIds.size(); a++)
			{
			
				stmt = DS_DBconnection.instance().getConnection().createStatement();
	
				sql = "SELECT RATIO FROM AE2__EXPRESSIONVALUE__MAIN " +
						"WHERE EXPERIMENT_ID = '"+expIds.get(a)+"' AND ARRAYDESIGN_ID = '"+arIDs.get(a)+"' AND ROWNUM < 2";
	
				System.out.println(sql);
				
				rset = stmt.executeQuery(sql);
				if (rset.next()) {
					ratio = rset.getString(1);
					//absolute = rset.getString(2);
					//deIds.add(deId);
				}
	
	
				if (ratio ==null){
					netCDFFiles.add(netCDFsPath+expIds.get(a)+"_"+arIDs.get(a)+".nc");
				}
				else {
					netCDFFiles.add(netCDFsPath+expIds.get(a)+"_"+arIDs.get(a)+"_ratio.nc");
				}
				
				rset.close();
	
				stmt.close();
		}
			

			

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		drawPlots(deIds.toArray(new String[deIds.size()]), netCDFFiles.toArray(new String[netCDFFiles.size()]), DSConstants.PLOT_SIZE_SMALL, plotType, path, factor);
		
		waitOnProcessToFinish();
		
		return retrieveImagePath();
		
	}

	public String getNetCDFsPath() {
		return netCDFsPath;
	}
	
	/**
	 * 
	 * Set up NetCDFs full path on the fly. 
	 * 
	 * @param netCDFsPath
	 */

	public void setNetCDFsPath(String netCDFsPath) {
		this.netCDFsPath = netCDFsPath;
	}

	
	private boolean isExpRatio(String exp_id_key, String AD_id_key){
		Connection connection=null;
		boolean isRatio = false;
		try {
			connection = DS_DBconnection.instance().getConnection();
			Statement stmt = connection.createStatement();
			String sql = "SELECT RATIO FROM AE2__EXPRESSIONVALUE__MAIN " +
					     "WHERE EXPERIMENT_ID = "+exp_id_key+" AND ARRAYDESIGN_ID = "+AD_id_key+" AND ROWNUM < 2";
		ResultSet rset = stmt.executeQuery(sql);
		
		if (rset.next()) {
			rset.getString(1);
			isRatio = !rset.wasNull();
		}
		rset.close();
		stmt.close();
		}catch (Exception ex){
			log.error("Unable to check experiment is ratio or not. "+ex.getMessage());
		}finally{
			if(connection!=null)
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		return isRatio;
	}

	public String getNetCDF(String geneIdKey, String expIdKey){
		String netCDF="";
		Vector<String> expIds = new Vector<String>();
		Vector<String> arIDs = new Vector<String>();
		String deId = new String();
		String ratio = new String();
		//String absolute = new String();
		Vector<String> netCDFFiles = new Vector<String>();
		Statement stmt;
		Vector<String> deIds = new Vector<String>();
		Connection connection=null;
		try {
			
			
			connection = DS_DBconnection.instance().getConnection();
			stmt = connection.createStatement();

			
			
//		String	sql = "SELECT ARRAYDESIGN_ID, E.EXPERIMENT_ID_KEY FROM AE1__ASSAY__MAIN A,  " +
//					"AE1__EXPERIMENT__MAIN E WHERE E.EXPERIMENT_IDENTIFIER = '"+expAcc+"' AND " +
//					"E.EXPERIMENT_ID_KEY = A.EXPERIMENT_ID_KEY AND ARRAYDESIGN_ID IS NOT " +
//					"NULL group BY ARRAYDESIGN_ID, E.EXPERIMENT_ID_KEY"; 
			
			String sql = "SELECT distinct A.ARRAYDESIGN_ID "+
						 "FROM AE1__ASSAY__MAIN A, AE2__DESIGNELEMENT__MAIN DE "+
						 "WHERE A.experiment_id_key = " +expIdKey+
						 " AND DE.arraydesign_id = A.arraydesign_id " +
						 " AND DE.gene_id_key = "+geneIdKey;
			
////			System.out.println(sql);
//			
			ResultSet rset = stmt.executeQuery(sql);
			
			while (rset.next()) {
				arIDs.add(rset.getString(1));
//				expIds.add(rset.getString(2));
			}
//			
			rset.close();

			stmt.close();
			
			for (int a = 0 ; a < arIDs.size(); a++)
			{
			
				stmt = connection.createStatement();
	
				sql = "SELECT RATIO FROM AE2__EXPRESSIONVALUE__MAIN " +
						"WHERE EXPERIMENT_ID = "+expIdKey+" AND ARRAYDESIGN_ID = '"+arIDs.get(a)+"' AND ROWNUM < 2";
	
//				System.out.println(sql);
				
				rset = stmt.executeQuery(sql);
				if (rset.next()) {
					ratio = rset.getString(1);
					//absolute = rset.getString(2);
					//deIds.add(deId);
				}
	
	
				if (ratio ==null){
					netCDFFiles.add(netCDFsPath+"/"+expIdKey+"_"+arIDs.get(a)+".nc");
				}
				else {
					netCDFFiles.add(netCDFsPath+"/"+expIdKey+"_"+arIDs.get(a)+"_ratio.nc");
				}
				
				rset.close();
	
				stmt.close();
		}
			

			

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(connection!=null)
				try {
					connection.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
		}
		return netCDFFiles.get(0);
	}

	private String getDEforGene(String gene_id_key, String exp_id_key, String factor){

		String sqlEF = "Select distinct EF "+
						" From atlas " +
						" where experiment_id_key = "+exp_id_key+
						" and gene_id_key = "+gene_id_key+
						" and ef = '"+factor.substring(3)+"'";
		
		
		Vector<String> deIds= new Vector<String>();
		Connection connection=null;
		boolean inAtlas = false;
		String selectedId="";
		try {
			connection = DS_DBconnection.instance().getConnection();
			Statement stmt = connection.createStatement();
			ResultSet rset = stmt.executeQuery(sqlEF);
			if (rset.next()) {
				inAtlas = true;
			}
			rset.close();
			
			
			String sql = "select * from( " +
			"select /*+ INDEX (ATLAS ATLAS_EF_EFV)*/ atlas.EF, atlas.EFV, atlas.UPDN, atlas.UPDN_PVALADJ, atlas.UPDN_TSTAT, atlas.DESIGNELEMENT_ID_KEY, atlas.ARRAYDESIGN_ID_KEY, " +
			"row_number() OVER(" +
			"PARTITION BY atlas.EXPERIMENT_ID_KEY, atlas.GENE_ID_KEY, atlas.ef,  atlas.EFV " +
			"ORDER BY atlas.updn_pvaladj asc, UPDN_TSTAT desc) TopN " +
			"from ATLAS " +
			"where gene_id_key = "+gene_id_key + 
			" and experiment_id_key = "+exp_id_key;
			
			if(inAtlas)
				sql+= " and EF= '"+factor.substring(3)+"' ";
			
			sql+= ") where topn=1 "+
			      "order by updn_pvaladj asc";
			
			
			
			ResultSet rs = stmt.executeQuery(sql);

			String deId,adId;
			HashMap<String, Integer> deTopCounts = new HashMap<String, Integer>(); 
			while(rs.next()){
				
				if(rs.getInt("TopN") != 1)
					continue;
				else{
					deId = rs.getString(6);
					adId = rs.getString(7);
					if(deTopCounts.containsKey(deId+"_"+adId))
						deTopCounts.put(deId+"_"+adId,deTopCounts.get(deId+"_"+adId)+1);
					else
						deTopCounts.put(deId+"_"+adId, 1);

				}
			}
			int count=0;int maxCount=0;
			
			for(String deId_ADid:deTopCounts.keySet()){
				count= deTopCounts.get(deId_ADid);
				if(count>maxCount){
					maxCount = count;
					selectedId = deId_ADid;
				}
			}
			
			rs.close();
			stmt.close();
			connection.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		return selectedId;
//		if(!deIds.isEmpty())
//			return deIds;
//		else
//			return null;
	}
	
	
	
	

}
