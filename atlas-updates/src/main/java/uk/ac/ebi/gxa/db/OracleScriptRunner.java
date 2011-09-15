package uk.ac.ebi.gxa.db;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.SQLException;

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
 * Also addresses <a href="http://code.google.com/p/c5-db-migration/issues/detail?id=31">Issue 31</a>:
 * Running Oracle 11g SQL*plus script generates ORA-06650 "end-of-file"
 * of <tt>c5-db-migrations</tt>
 *
 * @author alf
 */
public class OracleScriptRunner {
    public void execute(Connection connection, Reader reader) throws IOException, SQLException {
        try {
            final boolean originalAutoCommit = connection.getAutoCommit();
            try {
                if (originalAutoCommit) {
                    connection.setAutoCommit(false);
                }
                new OracleScriptSplitter().parse(reader, new SqlStatementExecutor(connection));
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
}
