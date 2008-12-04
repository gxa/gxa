package ae3.util;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class DBhandler {
	protected final Log log = LogFactory.getLog(getClass());
	// ArrayExpress (AEW/Atlas) RDBMS DataSource
	private DataSource theAEDS;

	// In-memory (local ArrayExpress AEW/Atlas helper) RDBMS Datasource
	private DataSource memAEDS;
	private DBhandler() {};
	private static DBhandler _instance = null;

	/**
	 * Returns the singleton instance.
	 *
	 * @return Singleton instance of Atlas DBhandler
	 */
	public static DBhandler instance() {
		if(null == _instance) {
			_instance = new DBhandler();
		}

		return _instance;
	}


    public void setAEDataSource(DataSource aeds) {
        this.theAEDS = aeds;
    }

    public void setMEMDataSource(DataSource memds) {
        this.memAEDS = memds;
    }
    /**
     * Gives a connection from the pool. Don't forget to close.
     * TODO: DbUtils
     *
     * @return a connection from the pool
     * @throws SQLException
     */
    public Connection getAE_ORAConnection() throws SQLException {
        if (theAEDS != null)
            return theAEDS.getConnection();

        return null;
    }
    /**
     * Gives a connection from the pool. Don't forget to close.
     * TODO: DbUtils
     *
     * @return a connection from the pool
     * @throws SQLException
     */
    public Connection getMEMConnection() throws SQLException {
        if (memAEDS != null)
            return memAEDS.getConnection();

        return null;
    }
}
