package uk.ac.ebi.gxa.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;

/**
 * The actual executor part extracted from {@link com.carbonfive.db.jdbc.ScriptRunner#doExecute}
 */
class SqlStatementExecutor {
    private final static Logger log = LoggerFactory.getLogger(SqlStatementExecutor.class);
    private Connection conn;

    SqlStatementExecutor(Connection conn) {
        this.conn = conn;
    }

    public void executeStatement(String command) throws SQLException {
        Statement statement = null;
        ResultSet rs = null;
        try {
            statement = conn.createStatement();

            log.info(command);

            boolean hasResults = statement.execute(command);

            rs = statement.getResultSet();

            if (hasResults && rs != null) {
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();
                for (int i = 1; i <= cols; i++) {
                    String name = md.getColumnName(i);
                    log.debug(name + "\t");
                }
                while (rs.next()) {
                    for (int i = 1; i <= cols; i++) {
                        String value = rs.getString(i);
                        log.debug(value + "\t");
                    }
                }
            }
        } finally {
            cleanup(statement, rs);
        }

        Thread.yield();
    }

    private void cleanup(Statement statement, ResultSet rs) {
        if (rs != null)
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Cannot close result set", e);
            }
        if (statement != null)
            try {
                statement.close();
            } catch (Exception e) {
                // Ignore to workaround a bug in Jakarta DBCP
            }
    }
}
