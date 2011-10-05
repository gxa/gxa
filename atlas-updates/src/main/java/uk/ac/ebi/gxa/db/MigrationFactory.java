package uk.ac.ebi.gxa.db;

import com.carbonfive.db.migration.Migration;
import org.springframework.core.io.Resource;

import static org.apache.commons.io.FilenameUtils.getExtension;

/**
 * @author alf
 */
public class MigrationFactory extends com.carbonfive.db.migration.MigrationFactory {
    @Override
    public Migration create(String version, Resource resource) {
        final String extension = getExtension(resource.getFilename()).toLowerCase();

        if ("sql".equals(extension)) {
            return new OracleScriptMigration(version, resource);
        }

        return super.create(version, resource);
    }
}
