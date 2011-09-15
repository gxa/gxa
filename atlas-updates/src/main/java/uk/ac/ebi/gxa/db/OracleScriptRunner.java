package uk.ac.ebi.gxa.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.sql.*;

import static org.apache.commons.lang.StringUtils.startsWithIgnoreCase;

/**
 * Oracle-aware script runner
 * <p/>
 * Hugely based on {@link com.carbonfive.db.jdbc.ScriptRunner} - the only significant difference is
 * that we now support Oracle's dirty hack with slash, <tt>/</tt>, meaning literally
 * "now just send to the database whatever you've got in your
 * <a href="http://download.oracle.com/docs/cd/B19306_01/server.102/b14357/ch4.htm#i1039357">SQL buffer</a>."
 * <p/>
 * For more details, refer to
 * <a href="http://download.oracle.com/docs/cd/B19306_01/server.102/b14357/ch4.htm#i1039663">Oracle documentation</a>
 * on PL/SQL scripts.
 * <p/>
 * Also addresses <a href="http://code.google.com/p/c5-db-migration/issues/detail?id=31&q=oracle">Issue 31:	Running Oracle 11g SQL*plus script generates ORA-06650 "end-of-file"</a>
 * of <tt>c5-db-migrations</tt>
 *
 * @author alf
 */
public class OracleScriptRunner {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private static final String DEFAULT_DELIMITER = ";";
    private static final String ORACLE_HACK = "/";

    public void execute(Connection connection, Reader reader) throws IOException, SQLException {
        try {
            final boolean originalAutoCommit = connection.getAutoCommit();
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(false);
                }
                doExecute(connection, reader);
            } finally {
                connection.setAutoCommit(originalAutoCommit);
            }
        } catch (IOException e) {
            throw e;
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error running script.  Cause: " + e, e);
        }
    }

    private void doExecute(Connection connection, Reader reader) throws IOException, SQLException {
        StringBuffer command = null;
        try {
            boolean ignoreDelimiter = false;
            LineNumberReader lineReader = new LineNumberReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                if (command == null) {
                    command = new StringBuffer();
                }

                line = line.trim();

                if (line.length() < 1) {
                    // Do nothing, it's an empty line.
                } else if (line.startsWith("--") || line.startsWith("#") || line.startsWith("//")) {
                    logger.debug(line);
                } else if ("/".equals(line)) {
                    executeStatement(connection, command.toString());
                    ignoreDelimiter = false;
                    command = null;
                } else {
                    if (startsWithIgnoreCase(line, "begin") || startsWithIgnoreCase(line, "declare")) {
                        ignoreDelimiter = true;
                        command.append(line);
                        command.append(" ");
                    } else if (!ignoreDelimiter && line.contains(DEFAULT_DELIMITER)) {
                        if (line.endsWith(DEFAULT_DELIMITER)) {
                            command.append(line.substring(0, line.lastIndexOf(DEFAULT_DELIMITER)));
                            executeStatement(connection, command.toString());
                            command = null;
                        }
                    } else {
                        command.append(line);
                        command.append(" \n");
                    }
                }
            }

            // Check to see if we have an unexecuted statement in command.
            if (command != null && command.length() > 0) {
                logger.info("Last statement in script is missing a terminating delimiter, executing anyway.");
                executeStatement(connection, command.toString());
            }
        } catch (SQLException e) {
            e.fillInStackTrace();
            logger.error("Error executing: " + command, e);
            throw e;
        } catch (IOException e) {
            e.fillInStackTrace();
            logger.error("Error executing: " + command, e);
            throw e;
        }
    }

    private void executeStatement(Connection conn, String command) throws SQLException {
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
