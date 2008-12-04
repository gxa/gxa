package ds.utils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;



public class DS_DBconnection {
	
		protected final Log log = LogFactory.getLog(getClass());
		// ArrayExpress (AEW/Atlas) RDBMS DataSource
		private DataSource theAEDS;

	
		
		private DS_DBconnection() {};
		private static DS_DBconnection _instance = null;

		/**
		 * Returns the singleton instance.
		 *
		 * @return Singleton instance of Atlas DBhandler
		 */
		public static DS_DBconnection instance() {
			if(null == _instance) {
				_instance = new DS_DBconnection();
			}

			return _instance;
		}


	    public void setAEDataSource(DataSource aeds) {
	        this.theAEDS = aeds;
	    }

	    
	    /**
	     * Gives a connection from the pool. Don't forget to close.
	     * TODO: DbUtils
	     *
	     * @return a connection from the pool
	     * @throws SQLException
	     */
	    public Connection getConnection() throws SQLException {
	        if (theAEDS != null)
	            return theAEDS.getConnection();

	        return null;
	    }
	   
	

}
