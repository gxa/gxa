package uk.ac.ebi.gxa.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.sql.SQLException;
import java.util.regex.Pattern;

/**
 * Hugely based on the code from {@link com.carbonfive.db.jdbc.ScriptRunner#doExecute}
 * - the only significant difference is that we support Oracle's dirty hack with slash, <tt>/</tt>,
 * meaning literally "now just send to the database whatever you've got in your
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
class OracleScriptSplitter {
    private final static Logger log = LoggerFactory.getLogger(OracleScriptSplitter.class);
    private final Pattern pattern = Pattern.compile("(^|(\\s|\\n)+)" +
            "create(\\s|\\n)+" +
            "(or(\\s|\\n)+replace(\\s|\\n)+)?" +
            "(function|library|package((\\s|\\n)+body)?|procedure|trigger|type)(\\s|\\n)+" +
            "(\\S+|\"[^\"]+\")(\\s|\\n)+.*");

    void parse(Reader reader, SqlStatementExecutor executor) throws SQLException, IOException {
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
                if (line.length() == 0) {
                    continue;
                }

                if (line.startsWith("--") || line.startsWith("#") || line.startsWith("//")) {
                    log.debug(line);
                } else if (line.matches("[/.]")) {
                    /*
                   http://download.oracle.com/docs/cd/B19306_01/server.102/b14357/ch4.htm#i1039663
                   Terminate PL/SQL subprograms by entering a period (.) by itself on a new line.
                   You can also terminate and execute a PL/SQL subprogram by entering a slash (/)
                   by itself on a new line.
                    */
                    execute(executor, sqlBuffer);
                    plsqlMode = false;
                    sqlBuffer = null;
                } else if (!plsqlMode && (enteredPlSqlDeclaration(sqlBuffer.toString().toLowerCase()) ||
                        "begin".equalsIgnoreCase(line) ||
                        "declare".equalsIgnoreCase(line))) {
                    plsqlMode = true;
                    sqlBuffer.append(line);
                    sqlBuffer.append("\n");
                } else if (!plsqlMode && line.endsWith(";")) {
                    sqlBuffer.append(line.substring(0, line.lastIndexOf(";")));
                    execute(executor, sqlBuffer);
                    sqlBuffer = null;
                } else {
                    sqlBuffer.append(line);
                    sqlBuffer.append("\n");
                }
            }

            // Check to see if we have an unexecuted statement in command.
            if (sqlBuffer != null && sqlBuffer.length() > 0) {
                log.info("Last statement in script is missing a terminating delimiter, executing anyway.");
                execute(executor, sqlBuffer);
            }
        } catch (SQLException e) {
            e.fillInStackTrace();
            log.error("Error executing: " + sqlBuffer, e);
            throw e;
        } catch (IOException e) {
            e.fillInStackTrace();
            log.error("Error executing: " + sqlBuffer, e);
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error running script.  Cause: " + e, e);
        }
    }

    private boolean enteredPlSqlDeclaration(String prefix) {
        return pattern.matcher(prefix).find();
    }

    private void execute(SqlStatementExecutor executor, StringBuilder sqlBuffer) throws SQLException {
        executor.executeStatement(sqlBuffer.toString().trim());
    }
}
