package ds.utils;

public final class DSConstants {

	public final static String R_SOURCES_PATH = "/Volumes/Workspace/Projects/atlas2/dataserver/R.Sources"; // Full path to the R Sources. Must be accessible for R Servants.

	public static String IMAGES_PATH = System.getProperty("user.dir") + "/images"; // path where the images are drawn. Must be accessible for WebServer.
	public final static String NETCDF_PATH_DWDEV = "/Volumes/Workspace/Projects/DWfiles/NetCDFs.DWDEV/";
	public final static String NETCDF_PATH_DWC = "/ebi/ArrayExpress-files/NetCDFs.DWC/";
	public static String DEFAULT_DATABASE_NAME = "DWDEV";
	public static String DEFAULT_NETCDF_PATH = "/Volumes/Workspace/Projects/DWfiles/NetCDFs.DWDEV/";
	
	public final static String SIMILARITY_EUCLIDEAN = "Euclidean";
	public final static String SIMILARITY_PEARSON = "Pearson";
	
	public final static String PLOT_SIZE_LARGE = "large";
	public final static String PLOT_SIZE_SMALL = "thb";
	
	public final static String PLOT_TYPE_TIMESERIES = "TimeSeries";
	public final static String PLOT_TYPE_ATLAS = "Atlas";
	public final static String PLOT_TYPE_BOXPLOT = "Boxplot";
	
	public final static String DATABASE_DWDEV = "DWDEV";
	public final static String DATABASE_DWC = "DWC";
	
	
}
