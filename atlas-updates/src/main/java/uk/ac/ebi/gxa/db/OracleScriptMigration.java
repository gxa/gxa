package uk.ac.ebi.gxa.db;

import com.carbonfive.db.jdbc.DatabaseType;
import com.carbonfive.db.migration.AbstractMigration;
import com.carbonfive.db.migration.MigrationException;
import com.carbonfive.db.migration.SQLScriptMigration;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;

/**
 * Oracle-aware migration, based on {@link SQLScriptMigration}
 * <p/>
 * The only difference with {@link SQLScriptMigration} is that we use our own script runner. Would its creation be
 * extracted to a factory method, we'd just override it; it wasn't, so we needed to create a brand new Migration instead.
 *
 * @author alf
 */
public class OracleScriptMigration extends AbstractMigration {
    private final Resource script;

    public OracleScriptMigration(String version, Resource script) {
        super(version, script.getFilename());
        this.script = script;
    }

    @Override
    public void migrate(DatabaseType dbType, Connection connection) {
        InputStream inputStream = null;
        try {
            inputStream = script.getInputStream();
            OracleScriptRunner scriptRunner = new OracleScriptRunner();
            scriptRunner.execute(connection, new InputStreamReader(inputStream, "UTF-8"));
            Validate.isTrue(!connection.isClosed(), "JDBC Connection should not be closed.");
        } catch (IOException e) {
            throw new MigrationException("Error while reading script input stream.", e);
        } catch (SQLException e) {
            throw new MigrationException("Error while executing migration script.", e);
        } finally {
            IOUtils.closeQuietly(inputStream);
        }
    }
}
