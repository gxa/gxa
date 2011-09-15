package uk.ac.ebi.gxa.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * @author alf
 */
class SqlStatementExecutor {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private Connection conn;

    SqlStatementExecutor(Connection conn) {
        this.conn = conn;
    }

    public void executeStatement(String command) throws SQLException {
        Statement statement = conn.createStatement();

        logger.debug(command);

        boolean hasResults = statement.execute(command);

        ResultSet rs = statement.getResultSet();

        if (hasResults && rs != null) {
            ResultSetMetaData md = rs.getMetaData();
            int cols = md.getColumnCount();
            for (int i = 1; i <= cols; i++) {
                String name = md.getColumnName(i);
                logger.debug(name + "\t");
            }
            while (rs.next()) {
                for (int i = 1; i <= cols; i++) {
                    String value = rs.getString(i);
                    logger.debug(value + "\t");
                }
            }
        }

        try {
            statement.close();
        } catch (Exception e) {
            // Ignore to workaround a bug in Jakarta DBCP
        }
        Thread.yield();
    }
}
