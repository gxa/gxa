package ds.R;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Vector;

import org.bioconductor.packages.rservices.RArray;
import org.bioconductor.packages.rservices.RChar;
import org.bioconductor.packages.rservices.RInteger;
import org.bioconductor.packages.rservices.RList;
import org.bioconductor.packages.rservices.RLogical;
import org.bioconductor.packages.rservices.RMatrix;
import org.bioconductor.packages.rservices.RNumeric;
import org.bioconductor.packages.rservices.RObject;
import org.bioconductor.packages.rservices.RVector;

import ds.server.ExpressionDataSet;
import ds.server.SimilarityResultSet;
import ds.utils.DSConstants;

import remoting.RServices;
import uk.ac.ebi.microarray.pools.RemoteLogListener;
import uk.ac.ebi.microarray.pools.ServantProviderFactory;
import uk.ac.ebi.microarray.pools.TimeoutException;

/**
 * 
 * Class that deals with request that can be done to the R Framework
 * 
 * @author hugo
 * 
 */

public class RUtilities implements Serializable {

	private static final long serialVersionUID = -9188720544227505219L;
	private Vector<String> plotPaths; // Full paths of where the images are drawn.
	private boolean currentProcessFinished = false; // Flag to represent whether or not the process is finished
	private RServices r = null;
	private int drawingPackage = RPlotter.GGPLOT; // default Drawing Package
	private String rSourcePath = DSConstants.R_SOURCES_PATH; // Full path of where to load the R Source code needed for plotting, reading netcdf, etc...
	public static final int DRAWINGPACKAGE_GGPLOT = 2;
	public static final int DRAWINGPACKAGE_MATPLOT = 3;

	SimilarityResultSet srs;

	String thumbnails_filepath = DSConstants.IMAGES_PATH;

	public RUtilities() {
		plotPaths = new Vector<String>();
	}

	public RUtilities(String path) {
		plotPaths = new Vector<String>();
		this.thumbnails_filepath = path;
	}

	/**
	 * 
	 * Request an R Service from the Servant Provider Factory
	 * 
	 * @throws MalformedURLException
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws TimeoutException
	 */

	private void fetchServices() throws MalformedURLException, RemoteException,
	NotBoundException, TimeoutException {

		r = (RServices) uk.ac.ebi.microarray.pools.ServantProviderFactory.getFactory()
		.getServantProvider().borrowServantProxy(); // Request a R Resource from the ServantProviderFactory

	}

	/**
	 * 
	 * Return an R Service to the Servant Provider Factory
	 * 
	 * @throws MalformedURLException
	 * @throws RemoteException
	 * @throws NotBoundException
	 * @throws TimeoutException
	 */

	private void returnServices() throws MalformedURLException,
	RemoteException, NotBoundException, TimeoutException {

		r.evaluate("rm(list=ls());"); // clears object in memory
		r.reset();
		r.freeAllReferences();
		System.out.println("Resetting RUtilities Servant");
		ServantProviderFactory.getFactory().getServantProvider().returnServantProxy(r); // Return a R Ressource from the ServantProviderFactory

		r = null;
	}

	/**
	 * 
	 * Method that create a RPlotter object and draws thumbnails in a thread
	 * context
	 * 
	 * @param deIds
	 * @param ncdfs
	 * @throws Exception
	 */


	public void drawThumbnailBoxPlot(ExpressionDataSet eds, String factor) throws Exception {

		RPlotter rp = new RPlotter(eds, plotPaths); // create an RPlotter object
		rp.setPlotType(RPlotter.BOXPLOT);
		rp.setDrawingPackage(drawingPackage);

		if (factor != null)		
			rp.setFactorToDraw(factor);

		rp.start(); // Launch the Thumbnail drawing as a Thread
	}

	public void drawThumbnailTimeSeries(ExpressionDataSet eds, String factor) throws Exception {

		RPlotter rp = new RPlotter(eds, plotPaths); // create an RPlotter object
		rp.setPlotType(RPlotter.TIMESERIES);
		rp.setDrawingPackage(drawingPackage);

		if (factor != null)		
			rp.setFactorToDraw(factor);

		rp.start(); // Launch the Thumbnail drawing as a Thread
	}

	public void drawLargePlotBoxPlot(ExpressionDataSet eds, String factor) throws Exception {

		RPlotter rp = new RPlotter(eds, plotPaths); // create an RPlotter object
		rp.setPlotType(RPlotter.BOXPLOT);
		rp.setDrawingPackage(drawingPackage);
		rp.setDrawingThumbnails(false);

		if (factor != null)		
			rp.setFactorToDraw(factor);

		rp.start(); // Launch the Thumbnail drawing as a Thread
	}

	public void drawLargePlotTimeSeries(ExpressionDataSet eds, String factor) throws Exception {

		RPlotter rp = new RPlotter(eds, plotPaths); // create an RPlotter object
		rp.setPlotType(RPlotter.TIMESERIES);
		rp.setDrawingPackage(drawingPackage);
		rp.setDrawingThumbnails(false);

		if (factor != null)		
			rp.setFactorToDraw(factor);

		rp.start(); // Launch the Thumbnail drawing as a Thread
	}

	public void drawThumbnailAtlas(ExpressionDataSet eds, String factor) throws Exception {

		RPlotter rp = new RPlotter(eds, plotPaths); // create an RPlotter object
		rp.setAtlas(true);
		rp.setPlotType(RPlotter.ATLAS);
		rp.setDrawingPackage(RUtilities.DRAWINGPACKAGE_GGPLOT);

		if (factor != null)		
			rp.setFactorToDraw(factor);

		rp.start(); // Launch the Thumbnail drawing as a Thread
	}

	public void drawLargePlotAtlas(ExpressionDataSet eds, String factor) throws Exception {

		RPlotter rp = new RPlotter(eds, plotPaths); // create an RPlotter object
		rp.setAtlas(true);
		rp.setPlotType(RPlotter.ATLAS);
		rp.setDrawingPackage(RUtilities.DRAWINGPACKAGE_GGPLOT);
		rp.setDrawingThumbnails(false);

		if (factor != null)		
			rp.setFactorToDraw(factor);

		rp.start(); // Launch the Thumbnail drawing as a Thread
	}

	public void getRankings() {

	}

	/**
	 * 
	 * Runs the Similarity on the R Framework and returns a SimilarityResultSet
	 * object
	 * 
	 * @param netCDF netcdf on which to run the similarity
	 * @param deId the id on which to run the similarity
	 * @param closestGenes the number of closest gene to retrieve
	 * @param method the similarity method to use
	 * @return a SimilarityResultSet object
	 * @throws Exception
	 */

	public void getSimilarity(String deId,String netCDF, 
			int closestGenes, String method) throws Exception {

		RSimilarity rs = new RSimilarity(deId, netCDF, closestGenes, method);

		rs.start();

	}

	private class RSimilarity extends Thread implements Serializable {


		private static final long serialVersionUID = 789124037880411597L;

		private RSimilarityPackage rSimilarityPackage;
		private RServices r = null;
		//private SimilarityResultSet srs = null;

		private String netCDF;
		private String geneid;
		private int closestGenes;
		private String method;

		public RSimilarity(String geneid, String netCDF, int closestGenes, String method){

			this.netCDF = netCDF;
			this.geneid = geneid;
			this.closestGenes = closestGenes;
			this.method = method;

			setRSimilarityPackage(method);

		}

		/**
		 * 
		 * Request an R Service from the Servant Provider Factory
		 * 
		 * @throws MalformedURLException
		 * @throws RemoteException
		 * @throws NotBoundException
		 * @throws TimeoutException
		 */
		private void fetchServices() throws MalformedURLException,
		RemoteException, NotBoundException, TimeoutException {

			r = (RServices) uk.ac.ebi.microarray.pools.ServantProviderFactory.getFactory()
			.getServantProvider().borrowServantProxy(); // Request a R Ressource from the ServantProviderFactory

		}

		/**
		 * 
		 * Return an R Service to the Servant Provider Factory
		 * 
		 * @throws MalformedURLException
		 * @throws RemoteException
		 * @throws NotBoundException
		 * @throws TimeoutException
		 */
		private void returnServices() throws MalformedURLException,
		RemoteException, NotBoundException, TimeoutException {

			r.evaluate("rm(list=ls());"); // clears object in memory
			r.reset();
			r.freeAllReferences();
			System.out.println("Resetting Similarity Servant");
			ServantProviderFactory.getFactory().getServantProvider().returnServantProxy(r); // Return a R Ressource from the ServantProviderFactory
			r = null;

		}

		public RSimilarityPackage getRSimilarityPackage() {
			return rSimilarityPackage;
		}

		public void setRSimilarityPackage(RSimilarityPackage similarityPackage) {
			rSimilarityPackage = similarityPackage;
		}

		public void setRSimilarityPackage(String similarityPackage) {

			if (similarityPackage.equals(DSConstants.SIMILARITY_EUCLIDEAN)){
				rSimilarityPackage = new RSimilarityEuclidean();			
			}else if (similarityPackage.equals(DSConstants.SIMILARITY_PEARSON)) {
				rSimilarityPackage = new RSimilarityPearson();	
			}

		}

		/**
		 * 
		 * Method that is called when the Thread method start() is called
		 * 
		 */

		public void run() {
			System.out.println("Launching Thread !");
			try {

				// if drawing thumbnails call appropriate method depending on
				// drawing package

				if (rSimilarityPackage != null){
					fetchServices();
					srs = rSimilarityPackage.runSimilarity(geneid, netCDF,  closestGenes, r);
					returnServices();

				}

				currentProcessFinished = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Stopping Thread !");
		}

		public String getMethod() {
			return method;
		}

		public int getClosestGenes() {
			return closestGenes;
		}



	}

	/**
	 * 
	 * This class takes care of drawing thumbnails for the given designelement
	 * ids and the specified NetCDF file
	 * 
	 * @author hugo
	 * 
	 */

	private class RPlotter extends Thread implements Serializable {

		private static final long serialVersionUID = -9204078312825247269L;

		ExpressionDataSet eds;
		Vector<String> plotPaths;

		boolean drawingThumbnails = true; // Whether or not the plotter is currently drawing a thumbnail
		boolean benchmarkTest;
		boolean atlas = false;

		public static final int TIMESERIES = 0;
		public static final int BOXPLOT = 1;
		public static final int ATLAS = 4;
		public static final int GGPLOT = 2;
		public static final int MATPLOT = 3;

		private int plotType;
		private int drawingPackage = MATPLOT;

		private String factorToDraw = null;

		private RPlotPackage plotPackage = null;

		public RPlotter(ExpressionDataSet eds, Vector<String> plotPaths) {
			this.eds = eds;
			this.plotPaths = plotPaths;
			benchmarkTest = false;
			this.plotType = TIMESERIES;

		}

		/**
		 * 
		 * Request an R Service from the Servant Provider Factory
		 * 
		 * @throws MalformedURLException
		 * @throws RemoteException
		 * @throws NotBoundException
		 * @throws TimeoutException
		 */
		private void fetchServices() throws MalformedURLException,
		RemoteException, NotBoundException, TimeoutException {

			r = (RServices) uk.ac.ebi.microarray.pools.ServantProviderFactory.getFactory()
			.getServantProvider().borrowServantProxy(); // Request a R Ressource from the ServantProviderFactory

		}

		/**
		 * 
		 * Load NetCDF data into an expression dataset
		 * 
		 * 
		 * @throws RemoteException
		 */
		private void loadNetCDFData() throws RemoteException {

			r.evaluate("source('" + rSourcePath + "/DS_CoreLib.R');"); // load main R library needed for the DataServer

			if (this.getPlotType()== RPlotter.TIMESERIES){

				r.evaluate("library(ncdf)"); // load the netcdf R library
				r.evaluate("netcdf<-'" + eds.getFilename() + "'; "); // store the netcdf filename in variable
				r.evaluate("ncinfo = unlist(strsplit (netcdf,'_|[.]'));"); // split the path to retrieve info related to the filename
				r.evaluate("morencinfo = unlist(strsplit (ncinfo[2],'/'));");
				r.evaluate("expt_id = morencinfo[2];"); // retrieve experiment id
				r.evaluate("arraydesign_id = ncinfo[3]; "); // retrieve arraydesign id
				r.evaluate("nc = open.ncdf(netcdf); "); // read netcdf file
				r.evaluate("bdc  = get.var.ncdf(nc,'BDC'); "); // retrieve DE variable
				r.evaluate("de  = get.var.ncdf(nc,'DE'); ");	// retrieve DE variable (Design Element Ids vector)
				r.evaluate("bs  = get.var.ncdf(nc,'BS'); ");	// retrieve BS variable (BioSample Ids vector)

				r.evaluate("if ( length(bs) == 1){bdc=as.matrix(bdc);} else{bdc = t(bdc);}"); // transpose bdc matrix if only one biosample
				r.evaluate("rownames(bdc) = de; ");				// attach design element ids to rownames of bdc
				r.evaluate("colnames(bdc) = bs; ");		
				r.evaluate("ef  = get.var.ncdf(nc,'EF'); "); // retrieve EF variable
				r.evaluate("efv  = get.var.ncdf(nc,'EFV'); "); // retrieve EFV variable
			}
			else {
				r.evaluate("eset<-read.aew.nc('" + eds.getFilename() + "');");
			}

		}

		/**
		 * 
		 * Return an R Service to the Servant Provider Factory
		 * 
		 * @throws MalformedURLException
		 * @throws RemoteException
		 * @throws NotBoundException
		 * @throws TimeoutException
		 */
		private void returnServices() throws MalformedURLException,
		RemoteException, NotBoundException, TimeoutException {

			r.evaluate("rm(list=ls());"); // clears object in memory
			r.reset();
			r.freeAllReferences();
			System.out.println("Resetting Plotter Servant");

			ServantProviderFactory.getFactory().getServantProvider().returnServantProxy(r); // Return a R Ressource from the ServantProviderFactory
			r = null;
		}

		/**
		 * 
		 * Method that is called when the Thread method start() is called
		 * 
		 */
		public void run() {
			System.out.println("Launching Thread !");
			try {

				if (factorToDraw == null) {

					// if drawing thumbnails call appropriate method depending on drawing package
					if (drawingThumbnails) {

						if (plotType == RPlotter.BOXPLOT) {
							setPlotPackage(new RPlotBoxPlot(this.plotPaths));
						} else if (plotType == RPlotter.TIMESERIES) {
							setPlotPackage(new RPlotTimeSeries(this.plotPaths));
						} else if (plotType == RPlotter.ATLAS) {
							setPlotPackage(new RPlotAtlas(this.plotPaths));
						}

						drawThumbnail(eds);

					}
					// if drawing large plot call appropriate method depending on drawing package
					else {

						if (plotType == RPlotter.BOXPLOT) {
							setPlotPackage(new RPlotBoxPlot(this.plotPaths));
						} else if (plotType == RPlotter.TIMESERIES) {
							setPlotPackage(new RPlotTimeSeries(this.plotPaths));
						} else if (plotType == RPlotter.ATLAS) {
							setPlotPackage(new RPlotAtlas(this.plotPaths));
						}

						drawLargePlot(eds);

					}
				}
				else {

					// if drawing thumbnails call appropriate method depending on drawing package
					if (drawingThumbnails) {

						if (plotType == RPlotter.BOXPLOT) {
							setPlotPackage(new RPlotBoxPlot(this.plotPaths));
						} else if (plotType == RPlotter.TIMESERIES) {
							setPlotPackage(new RPlotTimeSeries(this.plotPaths));
						} else if (plotType == RPlotter.ATLAS) {
							setPlotPackage(new RPlotAtlas(this.plotPaths));
						}

						drawThumbnail(eds, factorToDraw);

					}
					// if drawing large plot call appropriate method depending on drawing package
					else {

						if (plotType == RPlotter.BOXPLOT) {
							setPlotPackage(new RPlotBoxPlot(this.plotPaths));
						} else if (plotType == RPlotter.TIMESERIES) {
							setPlotPackage(new RPlotTimeSeries(this.plotPaths));
						} else if (plotType == RPlotter.ATLAS) {
							setPlotPackage(new RPlotAtlas(this.plotPaths));
						}

						drawLargePlot(eds, factorToDraw);

					}

				}

				currentProcessFinished = true;
				factorToDraw = null;
			} catch (Exception e) {
				e.printStackTrace();
			}
			System.out.println("Stopping Thread !");
		}

		public boolean isBenchmarkTest() {
			return benchmarkTest;
		}

		public void setBenchmarkTest(boolean benchmarkTest) {
			this.benchmarkTest = benchmarkTest;
		}

		public boolean isDrawingThumbnails() {
			return drawingThumbnails;
		}

		public void setDrawingThumbnails(boolean drawingThumbnails) {
			this.drawingThumbnails = drawingThumbnails;
		}

		private void drawLargePlot(ExpressionDataSet eds) throws Exception {

			fetchServices();
			loadNetCDFData();
			plotPackage.drawLargePlot(eds, r, thumbnails_filepath, null);
			returnServices();
		}

		private void drawThumbnail(ExpressionDataSet eds) throws Exception {

			fetchServices();
			loadNetCDFData();
			plotPackage.drawThumbnail(eds, r, thumbnails_filepath, null);
			returnServices();
		}

		private void drawLargePlot(ExpressionDataSet eds, String factor) throws Exception {

			fetchServices();
			loadNetCDFData();
			plotPackage.drawLargePlot(eds, r, thumbnails_filepath, factor);
			returnServices();
		}

		private void drawThumbnail(ExpressionDataSet eds, String factor) throws Exception {

			fetchServices();
			loadNetCDFData();
			plotPackage.drawThumbnail(eds, r, thumbnails_filepath, factor);
			returnServices();
		}

		public int getPlotType() {
			return plotType;
		}

		public void setPlotType(int plotType) {

			this.plotType = plotType;

		}

		public int getDrawingPackage() {
			return drawingPackage;
		}

		public void setDrawingPackage(int drawingPackage) {
			this.drawingPackage = drawingPackage;
		}

		public boolean isAtlas() {
			return atlas;
		}

		public void setAtlas(boolean atlas) {
			this.atlas = atlas;
		}

		public RPlotPackage getPlotPackage() {
			return plotPackage;
		}

		public void setPlotPackage(RPlotPackage plotPackage) {
			this.plotPackage = plotPackage;
		}

		public String getFactorToDraw() {
			return factorToDraw;
		}

		public void setFactorToDraw(String factorToDraw) {
			this.factorToDraw = factorToDraw;
		}

	}

	public String getThumbnails_filepath() {
		return thumbnails_filepath;
	}

	public void setThumbnails_filepath(String thumbnails_filepath) {
		this.thumbnails_filepath = thumbnails_filepath;
	}

	public String[] retrieveFactorsForNetCDF(String filepath) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {

		fetchServices();

		r.evaluate("library(ncdf)"); // load the netcdf R library
		r.evaluate("netcdf<-'" + filepath + "'; "); // store the netcdf filename in variable
		r.evaluate("ncinfo = unlist(strsplit (netcdf,'_|[.]'));"); // split the path to retrieve info related to the filename
		r.evaluate("morencinfo = unlist(strsplit (ncinfo[2],'/'));");
		r.evaluate("expt_id = morencinfo[2];"); // retrieve experiment id
		r.evaluate("arraydesign_id = ncinfo[3]; "); // retrieve arraydesign id
		r.evaluate("nc = open.ncdf(netcdf); "); // read netcdf file
		r.evaluate("ef  = get.var.ncdf(nc,'EF'); "); // retrieve EF variable

		RArray ra_EF = (RArray) r.getObject("ef");
		RChar rv_EF = (RChar) ra_EF.getValue();
		String[] ar_EF = rv_EF.getValue();

		returnServices();

		return ar_EF;
	}

	public String[] retrieveFactorValuesForNetCDF(String filepath, String factor) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {

		fetchServices();

		r.evaluate("library(ncdf)"); // load the netcdf R library
		r.evaluate("netcdf<-'" + filepath + "'; "); // store the netcdf filename in variable
		r.evaluate("nc = open.ncdf(netcdf); "); // read netcdf file
		r.evaluate("ef  = get.var.ncdf(nc,'EF'); "); // retrieve EF variable
		r.evaluate("efv  = get.var.ncdf(nc,'EFV'); ");

		r.evaluate("currFactorIt<-which(ef=='"+factor+"');");
		r.evaluate("oefv<-efv[order(efv[,currFactorIt])];");

		RChar ra_EF = (RChar) r.getObject("oefv");
		//RChar rv_EF = (RChar) ra_EF.getValue();
		String[] ar_EF = ra_EF.getValue();

		returnServices();

		return ar_EF;
	}

	public ExpressionDataSet retrieveExpressionDataSet(String filepath) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {

		ExpressionDataSet eds = new ExpressionDataSet();
		eds.setFilename(filepath);

		fetchServices();

		r.evaluate("library(ncdf)"); // load the netcdf R library
		r.evaluate("netcdf<-'" + filepath + "'; "); // store the netcdf filename in variable
		r.evaluate("ncinfo = unlist(strsplit (netcdf,'_|[.]'));"); // split the path to retrieve info related to the filename
		r.evaluate("morencinfo = unlist(strsplit (ncinfo[2],'/'));");
		r.evaluate("expt_id = morencinfo[2];"); // retrieve experiment id
		r.evaluate("arraydesign_id = ncinfo[3]; "); // retrieve arraydesign id
		r.evaluate("nc = open.ncdf(netcdf); "); // read netcdf file
		r.evaluate("de  = get.var.ncdf(nc,'DE'); "); // retrieve DE variable (Design Element Ids vector)
		r.evaluate("bs  = get.var.ncdf(nc,'BS'); "); // retrieve BS variable (BioSample Ids vector)
		r.evaluate("ef  = get.var.ncdf(nc,'EF'); "); // retrieve EF variable
		r.evaluate("efv  = get.var.ncdf(nc,'EFV'); "); // retrieve EFV variable

		RChar exp = (RChar) r.getObject("expt_id");
		RChar array_id = (RChar) r.getObject("arraydesign_id");

		RArray ra_DE = (RArray) r.getObject("de");
		RInteger rv_DE = (RInteger) ra_DE.getValue();
		int[] i_ar_DE = rv_DE.getValue();
		String[] ar_DE = new String[i_ar_DE.length];

		for (int a = 0; a < i_ar_DE.length; a++) {
			ar_DE[a] = ((Integer) i_ar_DE[a]).toString();
		}

		RArray ra_BS = (RArray) r.getObject("bs");
		RInteger rv_BS = (RInteger) ra_BS.getValue();
		int[] ar_BS = rv_BS.getValue();

		RArray ra_EF = (RArray) r.getObject("ef");
		RChar rv_EF = (RChar) ra_EF.getValue();
		String[] ar_EF = rv_EF.getValue();

		RArray ra_EFV = (RArray) r.getObject("efv");
		int[] dim = ra_EFV.getDim();
		String[][] ar_EFV = new String[dim[0]][dim[1]];
		RChar rv_EFV = (RChar) ra_EFV.getValue();
		String[] temp_EFV = rv_EFV.getValue();
		int c = 0;

		for (int a = 0; a < dim[1]; a++) {

			for (int b = 0; b < dim[0]; b++) {

				ar_EFV[b][a] = temp_EFV[c];
				c++;

			}

		}

		eds.setStudyId(exp.getValue()[0]);
		eds.setAssayId(array_id.getValue()[0]);
		eds.setSampleList(ar_BS);
		eds.setAr_DE(ar_DE);
		eds.setAr_EF(ar_EF);
		eds.setAr_EFV(ar_EFV);
		eds.updatedeMap();
		eds.setFilename(filepath);

		returnServices(); // returns R Service to ServantProviderFactory

		return eds;

	}

	@Deprecated 
	public ExpressionDataSet retrieveExpressionDataSet(String[] deIds, String filepath) throws MalformedURLException, RemoteException, NotBoundException, TimeoutException {

		ExpressionDataSet eds = retrieveExpressionDataSet(filepath);

		eds = eds.getSliceByDE(deIds);

		return eds;

	}

	public ExpressionDataSet retrieveExpressionMatrix(ExpressionDataSet eds) throws MalformedURLException, RemoteException, NotBoundException,
	TimeoutException  {

		fetchServices();

		r.evaluate("library(ncdf)"); // load the netcdf R
		// library
		r.evaluate("netcdf<-'" + eds.getFilename() + "'; "); // store the netcdf filename
		// in variable
		r.evaluate("ncinfo = unlist(strsplit (netcdf,'_|[.]'));"); 
		r.evaluate("nc = open.ncdf(netcdf); "); // read netcdf file
		r.evaluate("bdc  = get.var.ncdf(nc,'BDC'); "); // retrieve DE variable
		r.evaluate("de  = get.var.ncdf(nc,'DE'); ");	// retrieve DE variable (Design Element Ids vector)
		r.evaluate("bs  = get.var.ncdf(nc,'BS'); ");	// retrieve BS variable (BioSample Ids vector)
		r.evaluate("ef  = get.var.ncdf(nc,'EF'); ");
		r.evaluate("efv  = get.var.ncdf(nc,'EFV'); ");
		r.evaluate("if ( length(bs) == 1){bdc=as.matrix(bdc);} else{bdc = t(bdc);}"); // transpose bdc matrix if only one biosample
		r.evaluate("rownames(bdc) = de; ");				// attach design element ids to rownames of bdc
		r.evaluate("colnames(bdc) = bs; ");		

		String bdcIds = new String();

		for (int a = 0; a < eds.getAr_DE().length; a++) {

			RLogical rl = (RLogical) r.getObject("any(row.names(bdc)=='"+eds.getAr_DE()[a] + "')");

			boolean brl = rl.getValue()[0];

			if (brl) {

				bdcIds += "'" + eds.getAr_DE()[a] + "',";

			}

		}



		// if no Design Elements Ids are in matrix bdc then we return from teh
		// method without drawing the plot
		if (bdcIds.length() == 0)
			return null;

		bdcIds = "c(" + bdcIds.substring(0, bdcIds.length() - 1) + ")";

		RArray ra_BDC = (RArray) r.getObject("bdc["+bdcIds+",];");
		int[] dim = ra_BDC.getDim();
		double[][] ar_BDC = new double[dim[0]][dim[1]];
		RNumeric rv_BDC = (RNumeric) ra_BDC.getValue();
		double[] temp_BDC = rv_BDC.getValue();
		int c = 0;

		for (int a = 0; a < dim[1]; a++) {

			for (int b = 0; b < dim[0]; b++) {

				ar_BDC[b][a] = temp_BDC[c];
				c++;

			}

		}

		eds.setExpressionMatrix(ar_BDC);

		RArray ra_EF = (RArray) r.getObject("ef");
		RChar rv_EF = (RChar) ra_EF.getValue();
		String[] ar_EF = rv_EF.getValue();
		eds.setFactors(ar_EF);

		for (int a = 0; a < ar_EF.length; a++) {

			r.evaluate("currFactorIt<-" + (a+1) + ";");
			r.evaluate("efvd<-levels(factor(efv[,currFactorIt]));");
			r.evaluate("efvj<-efv[,currFactorIt];"); // retrieve matrix of
			// factor values
			r.evaluate("mj<-matrix(c(efvj)); "); // create matrix from one
			// column of factor value
			r.evaluate("oj<-order(mj); "); // get order of sorted factor values

			RChar currentFactor = (RChar) r.getObject("ef[currFactorIt]");

			ra_BDC = (RArray) r.getObject("bdc["+bdcIds+",oj];");
			dim = ra_BDC.getDim();
			ar_BDC = new double[dim[0]][dim[1]];
			rv_BDC = (RNumeric) ra_BDC.getValue();
			temp_BDC = rv_BDC.getValue();
			c = 0;

			for (int d = 0; d < dim[1]; d++) {

				for (int e = 0; e < dim[0]; e++) {

					ar_BDC[e][d] = temp_BDC[c];
					c++;

				}

			}

			RChar ra_EFVs = (RChar) r.getObject("efv[oj,currFactorIt];");
			String[] ar_EFVs = ra_EFVs.getValue();
			eds.setFactorValues(currentFactor.getValue()[0], null);

			RInteger ra_OJ = (RInteger) r.getObject("oj");
			int[] ar_OJ = ra_OJ.getValue();

			RChar ra_EFVJ = (RChar) r.getObject("efvj[oj]");
			String[] ar_EFVJ = ra_EFVJ.getValue();
			eds.setFactorValues(currentFactor.getValue()[0], null);

			eds.addSortOrder(currentFactor.getValue()[0], ar_OJ);

			eds.setSortedExpressionMatrix(ar_BDC, currentFactor.getValue()[0]);

		}




		//eds.setExpressionMatrix(ar_BDC);
		currentProcessFinished = true;
		returnServices(); // returns R Service to ServantProviderFactory

		return eds;

	}


	/*
	public ExpressionDataSet retrieveExpressionMatrix(ExpressionDataSet eds)
			throws MalformedURLException, RemoteException, NotBoundException,
			TimeoutException {

			eds.setExpressionMatrix(this.retrieveExpressionMatrix(eds.getFilename(), eds.getAr_DE()));

			return eds;


	}*/

	public ExpressionDataSet retrieveExpressionDataForDE(String netcdf, Vector<String> deIds, String factor)  {
		ExpressionDataSet eds = new ExpressionDataSet();
		eds.setFilename(netcdf);
		try{
			fetchServices();

			r.evaluate("library(ncdf)"); // load the netcdf R
			// library
			r.evaluate("netcdf<-'" + netcdf + "'; "); // store the netcdf filename
			// in variable
			r.evaluate("ncinfo = unlist(strsplit (netcdf,'_|[.]'));"); 
			r.evaluate("nc = open.ncdf(netcdf); "); // read netcdf file
			r.evaluate("bdc  = get.var.ncdf(nc,'BDC'); "); // retrieve DE variable
			r.evaluate("de  = get.var.ncdf(nc,'DE'); ");	// retrieve DE variable (Design Element Ids vector)
			r.evaluate("bs  = get.var.ncdf(nc,'BS'); ");	// retrieve BS variable (BioSample Ids vector)
			r.evaluate("ef  = get.var.ncdf(nc,'EF'); ");
			r.evaluate("efv  = get.var.ncdf(nc,'EFV'); ");
			r.evaluate("if ( length(bs) == 1){bdc=as.matrix(bdc);} else{bdc = t(bdc);}"); // transpose bdc matrix if only one biosample
			r.evaluate("rownames(bdc) = de; ");				// attach design element ids to rownames of bdc
			//r.evaluate("colnames(bdc) = efv; ");		

			String bdcIds = new String();

			for (int a = 0; a < deIds.size(); a++) {
				RLogical rl = (RLogical) r.getObject("any(row.names(bdc)=='"+deIds.get(a) + "')");
				boolean brl = rl.getValue()[0];	
				if (brl) {
					bdcIds += "'" + deIds.get(a) + "',";
				}
			}
			bdcIds = "c(" + bdcIds.substring(0, bdcIds.length() - 1) + ")";
			String[] ar_EF = ((RChar)((RArray) r.getObject("ef")).getValue()).getValue();
			eds.setFactors(ar_EF);

//			for (int i = 0; i < ar_EF.length; i++) {

//				r.evaluate("currFactorIt<-" + (i+1) + ";");
				r.evaluate("colnames(efv)=ef");
//				r.evaluate("colnames(bdc)=efv[,currFactorIt]"); // set column names to current factor values
				r.evaluate("colnames(bdc)=efv[,colnames=\""+factor+"\"]");

				r.evaluate("dataPerFV<-lapply(levels(factor(efv[,colnames=\""+factor+"\"])),function(i)bdc["+bdcIds+",colnames(bdc)==i])");

				RList dataByFV =  (RList)r.getObject("dataPerFV");
				RObject[] FVdata = (RObject[])dataByFV.getValue();
				HashMap dataByFV_map = new HashMap();
				
				double[][] ar_BDC;
				double[] temp_BDC;
				int[] dim = new int[2];
				if(deIds.size()>1){
					for(int i=1;i<=FVdata.length; i++){
						String[] currFVarr;String currFV="";double[] data=null;HashMap<String, double[]> deData=new HashMap<String, double[]>();
						RMatrix fv_matrix = (RMatrix)r.getObject("dataPerFV[["+i+"]]");
						temp_BDC = ((RNumeric)fv_matrix.getValue()).getValue();
						dim = fv_matrix.getDim();
						ar_BDC = new double[dim[0]][dim[1]];	
						int c = 0;
						for (int d = 0; d < dim[1]; d++) {
							for (int e = 0; e < dim[0]; e++) {
								ar_BDC[e][d] = temp_BDC[c];
								c++;
							}
						}
						currFV = ((RChar)fv_matrix.getDimnames().getValue()[1]).getValue()[0];
						dataByFV_map.put(currFV,ar_BDC);
					}
				}
				else{
					for(int j=0;j<FVdata.length; j++){
						double[] data = ((RNumeric)FVdata[j]).getValue();
						String[] currFVarr = ((RNumeric)FVdata[j]).getNames();
						String currFV = currFVarr!=null? currFVarr[0] :"undefined"; 
						ar_BDC = new double[1][data.length];
						ar_BDC[0] = data;
						dataByFV_map.put(currFV,ar_BDC);
					}
				}

				eds.addEFdata(factor,null);

				String[] ar_EFVs = ((RChar) r.getObject("levels(factor(efv[,colnames=\""+factor+"\"]))")).getValue();
				eds.setFactorValues(factor, null);

		}catch(Exception e){
				System.out.println(e);
		}finally{
			currentProcessFinished = true;
			try {
				returnServices();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} 
		}

		return eds;


	}


	public double[][] retrieveExpressionMatrix(String[] deIds, String filename, String factor)
	throws MalformedURLException, RemoteException, NotBoundException,
	TimeoutException {


		fetchServices();

		r.evaluate("library(ncdf)"); // load the netcdf R
		// library
		r.evaluate("netcdf<-'" + filename + "'; "); // store the netcdf filename
		// in variable
		r.evaluate("ncinfo = unlist(strsplit (netcdf,'_|[.]'));"); 
		r.evaluate("nc = open.ncdf(netcdf); "); // read netcdf file
		r.evaluate("bdc  = get.var.ncdf(nc,'BDC'); "); // retrieve DE variable
		r.evaluate("de  = get.var.ncdf(nc,'DE'); ");	// retrieve DE variable (Design Element Ids vector)
		r.evaluate("bs  = get.var.ncdf(nc,'BS'); ");	// retrieve BS variable (BioSample Ids vector)
		r.evaluate("ef  = get.var.ncdf(nc,'EF'); ");	
		r.evaluate("efv  = get.var.ncdf(nc,'EFV'); ");

		r.evaluate("if ( length(bs) == 1){bdc=as.matrix(bdc);} else{bdc = t(bdc);}"); // transpose bdc matrix if only one biosample
		r.evaluate("rownames(bdc) = de; ");				// attach design element ids to rownames of bdc
		r.evaluate("colnames(bdc) = bs; ");		

		String bdcIds = new String();

		for (int a = 0; a < deIds.length; a++) {

			RLogical rl = (RLogical) r.getObject("any(row.names(bdc)=='"+deIds[a] + "')");

			boolean brl = rl.getValue()[0];

			if (brl) {

				bdcIds += "'" + deIds[a] + "',";

			}

		}



		// if no Design Elements Ids are in matrix bdc then we return from teh
		// method without drawing the plot
		if (bdcIds.length() == 0)
			return null;

		bdcIds = "c(" + bdcIds.substring(0, bdcIds.length() - 1) + ")";
		RInteger dimEf = (RInteger) r.getObject("length(ef);");

		RArray ra_EF = (RArray) r.getObject("ef");
		RChar rv_EF = (RChar) ra_EF.getValue();
		String[] ar_EF = rv_EF.getValue();

		int i_dimEf = dimEf.getValue()[0];
		boolean factorFound =false;

		for (int a = 0; a < ar_EF.length; a++) {

			if (!factor.equalsIgnoreCase(ar_EF[a])){
				factorFound = true;
				continue;
			}

			r.evaluate("currFactorIt<-" + (a+1) + ";");
			r.evaluate("efvd<-levels(factor(efv[,currFactorIt]));");
			r.evaluate("efvj<-efv[,currFactorIt];"); // retrieve matrix of
			// factor values
			r.evaluate("mj<-matrix(c(efvj)); "); // create matrix from one
			// column of factor value
			r.evaluate("oj<-order(mj); "); // get order of sorted factor values

			RChar currentFactor = (RChar) r.getObject("ef[currFactorIt]");

		}

		if (!factorFound)
			return null;

		RArray ra_BDC = (RArray) r.getObject("bdc["+bdcIds+",oj];");
		int[] dim = ra_BDC.getDim();
		double[][] ar_BDC = new double[dim[0]][dim[1]];
		RNumeric rv_BDC = (RNumeric) ra_BDC.getValue();
		double[] temp_BDC = rv_BDC.getValue();
		int c = 0;

		for (int a = 0; a < dim[1]; a++) {

			for (int b = 0; b < dim[0]; b++) {

				ar_BDC[b][a] = temp_BDC[c];
				c++;

			}

		}


		currentProcessFinished = true;
		returnServices(); // returns R Service to ServantProviderFactory

		return ar_BDC;

	}

	public ExpressionDataSet retrieveExpressionMatrix(ExpressionDataSet eds, String factor)
	throws MalformedURLException, RemoteException, NotBoundException,
	TimeoutException {

		eds.setExpressionMatrix(this.retrieveExpressionMatrix(eds.getAr_DE(),eds.getFilename(), factor));

		return eds;

	}



	public int getDrawingPackage() {
		return drawingPackage;
	}

	public void setDrawingPackage(int drawingPackage) {

		// PoolUtils

		if (drawingPackage == RPlotter.MATPLOT)
			this.drawingPackage = RPlotter.MATPLOT;
		else if (drawingPackage == RPlotter.GGPLOT)
			this.drawingPackage = RPlotter.GGPLOT;
	}

	public String getRSourcePath() {
		return rSourcePath;
	}

	public void setRSourcePath(String sourcePath) {
		rSourcePath = sourcePath;
	}

	public Vector<String> getPlotPaths() {
		return plotPaths;
	}

	public void setPlotPaths(Vector<String> plotPaths) {
		this.plotPaths = plotPaths;
	}

	public boolean isCurrentProcessFinished() {
		return currentProcessFinished;
	}

	public SimilarityResultSet retrieveSimilarityResult() {
		return srs;
	}

}
