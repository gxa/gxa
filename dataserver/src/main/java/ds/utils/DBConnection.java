package ds.utils;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


/**
 * 
 * 
 * Class that provides access to the database. Currently used to retrieve annotation.
 * 
 * @author hugo
 *
 */

@Deprecated
public class DBConnection {

	static Connection connection = null;

	static String mappingsFile;
	static String martName;
	static String jdbcString;
	static String username;
	static String password;
	
	static String currentConnection = null;
	
	public static void init () {
		
	    try {
	      
	      if (DSConstants.DEFAULT_DATABASE_NAME.equals("DWC")){
	    	  System.out.println("switching to DWC");
	    	  init_DWC();
	    	  
	      }
	      else if (DSConstants.DEFAULT_DATABASE_NAME.equals("DWDEV")){
	    	  
	    	  init_DWDEV();
	    	  
	      }
	      else
	    	  init_DWDEV();
	      
	      currentConnection = DSConstants.DEFAULT_DATABASE_NAME;
	      
	      DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver ());
	      connection = DriverManager.getConnection (jdbcString, username, password);
	      connection.setAutoCommit (false);
	      
	    } catch (Exception e) { e.printStackTrace (System.out);}
	  }
	
	 public static Connection getConnection(){
		 
		 if (connection == null)
			 init();
		 else if (!currentConnection.equals(DSConstants.DEFAULT_DATABASE_NAME)){
			try {
				connection.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			 init();
		 }
		 
		 return connection;
		 
	 }
	 
	 public static void init (String dbName) {
			
		    try {

		      DriverManager.registerDriver (new oracle.jdbc.driver.OracleDriver ());
		      connection = DriverManager.getConnection (jdbcString, username, password);
		      connection.setAutoCommit (false);
		      
		    } catch (Exception e) { e.printStackTrace (System.out);}
	}
	 
	 private static void init_DWDEV() {
		 
		 martName = "DWDEV";
	     jdbcString = "jdbc:oracle:thin:@moe.ebi.ac.uk:1521:AEDWDEV";
	     username = "aemart";
	     password = "marte";
		 
	 }
	 
	 private static void init_DWC() {
		 
		 martName = "DWC";
	     jdbcString = "jdbc:oracle:thin:@moe.ebi.ac.uk:1521:AEDWC";
	     username = "aemart";
	     password = "marte";
		 
	 }


}
