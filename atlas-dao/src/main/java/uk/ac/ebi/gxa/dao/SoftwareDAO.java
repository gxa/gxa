package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SoftwareDAO {
    public static final String ENSEMBL = "Ensembl";
    public static final String MIRBASE = "miRBase";

    private JdbcTemplate template;

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public long getSoftwareId(final String name, final String version) {
        template.update("merge into a2_software sw\n" +
                "  using (select 1 from dual)\n" +
                "  on (sw.name = ? and sw.version = ?)\n" +
                "  when not matched then \n" +
                "  insert (name, version) values (?, ?)",
                new PreparedStatementSetter() {
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, name);
                        ps.setString(2, version);
                        ps.setString(3, name);
                        ps.setString(4, version);
                    }
                });

        return template.queryForLong("SELECT SOFTWAREid FROM a2_SOFTWARE " +
                "WHERE name = ? AND version = ?",
                name, version);
    }

    public long getLatestVersionOfSoftware(String name) {
        return template.queryForLong("SELECT SOFTWAREid \n" +
                "FROM a2_SOFTWARE \n" +
                "WHERE name = ? \n" +
                "AND version = (\n" +
                "SELECT MAX(version) FROM a2_SOFTWARE WHERE name = ?)",
                name, name);
    }
}
