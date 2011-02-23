package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 * User: nsklyar
 * Date: 23/02/2011
 * Time: 16:13
 */
 class SoftwareDAO extends AbstractAtlasDAO {
    public static final String SOFTWARE_ID = "SELECT SOFTWAREid FROM a2_SOFTWARE " +
            "WHERE name = ? AND version = ?";

    public static final String LATEST_SOFTWARE_ID = "SELECT SOFTWAREid \n" +
            "FROM a2_SOFTWARE \n" +
            "WHERE name = ? \n" +
            "AND version = (\n" +
            "SELECT MAX(version) FROM a2_SOFTWARE WHERE name = ?)";

    public static final String ENSEMBL = "Ensembl";

    public long getSoftwareId(final String name, final String version) {
        String query = "merge into a2_software sw\n" +
                "  using (select 1 from dual)\n" +
                "  on (sw.name = ? and sw.version = ?)\n" +
                "  when not matched then \n" +
                "  insert (name, version) values (?, ?)";
        template.update(query, new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, name);
                ps.setString(2, version);
                ps.setString(3, name);
                ps.setString(4, version);

            }
        });

        return template.queryForLong(SOFTWARE_ID,
                new Object[]{name, version});

    }

    public long getLatestVersionOfSoftware(String name) {
        return template.queryForLong(LATEST_SOFTWARE_ID, new Object[]{name, name});
    }
}
