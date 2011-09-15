package uk.ac.ebi.gxa.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.sql.*;

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
 * Also addresses <a href="http://code.google.com/p/c5-db-migration/issues/detail?id=31">Issue 31:	Running Oracle 11g SQL*plus script generates ORA-06650 "end-of-file"</a>
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
        StringBuilder sqlBuffer = null;
        try {
            boolean plsqlMode = false;
            LineNumberReader lineReader = new LineNumberReader(reader);
            String line;
            while ((line = lineReader.readLine()) != null) {
                if (sqlBuffer == null) {
                    sqlBuffer = new StringBuilder();
                }

                line = line.trim();

                if (line.length() < 1) {
                    // Do nothing, it's an empty line.
                } else if (line.startsWith("--") || line.startsWith("#") || line.startsWith("//")) {
                    logger.debug(line);
                } else if (line.matches("[/.]")) {
                    /*
                    http://download.oracle.com/docs/cd/B19306_01/server.102/b14357/ch4.htm#i1039663
                    Terminate PL/SQL subprograms by entering a period (.) by itself on a new line.
                    You can also terminate and execute a PL/SQL subprogram by entering a slash (/) by itself on a new line.
                     */
                    executeStatement(connection, sqlBuffer.toString());
                    plsqlMode = false;
                    sqlBuffer = null;
                } else if (!plsqlMode && line.toLowerCase().matches("(" +
                        "begin|declare" +
                        ")\\s.*")) {
                    /*
                    * TODO: support also
                        CREATE FUNCTION
                        CREATE LIBRARY
                        CREATE PACKAGE
                        CREATE PACKAGE BODY
                        CREATE PROCEDURE
                        CREATE TRIGGER
                        CREATE TYPE

                        e.g.

CREATE OR REPLACE PACKAGE ATLASMGR IS
  PROCEDURE DisableConstraints;
  PROCEDURE EnableConstraints;
  PROCEDURE DisableTriggers;
  PROCEDURE EnableTriggers;
  PROCEDURE RebuildSequence(seq_name varchar2);
  PROCEDURE RebuildSequences;
  PROCEDURE RebuildIndex;
  PROCEDURE fix_sequence(tbl VARCHAR2, field VARCHAR2, seq VARCHAR2);
END ATLASMGR;
/

CREATE OR REPLACE PACKAGE BODY ATLASMGR AS

etc.
                    */
                    plsqlMode = true;
                    sqlBuffer.append(line);
                    sqlBuffer.append(" ");
                } else if (!plsqlMode && line.contains(DEFAULT_DELIMITER)) {
                    if (line.endsWith(DEFAULT_DELIMITER)) {
                        sqlBuffer.append(line.substring(0, line.lastIndexOf(DEFAULT_DELIMITER)));
                        executeStatement(connection, sqlBuffer.toString());
                        sqlBuffer = null;
                    }
                } else {
                    sqlBuffer.append(line);
                    sqlBuffer.append(" \n");
                }
            }

            // Check to see if we have an unexecuted statement in command.
            if (sqlBuffer != null && sqlBuffer.length() > 0) {
                logger.info("Last statement in script is missing a terminating delimiter, executing anyway.");
                executeStatement(connection, sqlBuffer.toString());
            }
        } catch (SQLException e) {
            e.fillInStackTrace();
            logger.error("Error executing: " + sqlBuffer, e);
            throw e;
        } catch (IOException e) {
            e.fillInStackTrace();
            logger.error("Error executing: " + sqlBuffer, e);
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
