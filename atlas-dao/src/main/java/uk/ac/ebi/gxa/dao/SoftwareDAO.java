package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class SoftwareDAO {
    public static final String ENSEMBL = "Ensembl";
    public static final String MIRBASE = "miRBase";

    private JdbcTemplate template;

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public long getSoftwareId(final Software software) {
        template.update("merge into a2_software sw\n" +
                "  using (select 1 from dual)\n" +
                "  on (sw.name = ? and sw.version = ?)\n" +
                "  when not matched then \n" +
                "  insert (name, version) values (?, ?)",
                new PreparedStatementSetter() {
                    public void setValues(PreparedStatement ps) throws SQLException {
                        ps.setString(1, software.getName());
                        ps.setString(2, software.getVersion());
                        ps.setString(3, software.getName());
                        ps.setString(4, software.getVersion());
                    }
                });

        return template.queryForLong("SELECT SOFTWAREid FROM a2_SOFTWARE " +
                "WHERE name = ? AND version = ?",
                software.getName(), software.getVersion());
    }

    public Software getLatestVersionOfSoftware(String name) {
        List<Software> answer = template.query("SELECT softwareid, name, version, url \n" +
                "FROM a2_SOFTWARE \n" +
                "WHERE name = ? \n" +
                "AND version = (\n" +
                "SELECT MAX(version) FROM a2_SOFTWARE WHERE name = ?)",
                new Object[]{name, name}, new RowMapper<Software>() {
                    public Software mapRow(ResultSet rs, int rowNum) throws SQLException {
                        Software sw = new Software(rs.getString(2), rs.getString(3));
                        sw.setSoftwareid(rs.getLong(1));
                        sw.setUrl(rs.getString(4));
                        return sw;
                    }
                });
        if (answer.size() == 1)
            return answer.get(0);
        else
            return null;
    }

//    public long getLatestVersionOfSoftwareId(String name) {
//        List<Long> answer = template.query("SELECT SOFTWAREid \n" +
//                "FROM a2_SOFTWARE \n" +
//                "WHERE name = ? \n" +
//                "AND version = (\n" +
//                "SELECT MAX(version) FROM a2_SOFTWARE WHERE name = ?)",
//                new Object[]{name, name}, new RowMapper<Long>() {
//                    public Long mapRow(ResultSet rs, int rowNum) throws SQLException {
//                        return rs.getLong(1);
//                    }
//                });
//        if (answer.size() == 1)
//            return answer.get(0);
//        else
//            return -1;
//    }

//    public String getLatestVersionOfSoftware(String name) {
//        List<String> answer = template.query("SELECT version \n" +
//                "FROM a2_SOFTWARE \n" +
//                "WHERE name = ? \n" +
//                "AND version = (\n" +
//                "SELECT MAX(version) FROM a2_SOFTWARE WHERE name = ?)",
//                new Object[]{name, name}, new RowMapper<String>() {
//                    public String mapRow(ResultSet rs, int rowNum) throws SQLException {
//                        return rs.getString(1);
//                    }
//                });
//        if (answer.size() == 1)
//            return answer.get(0);
//        else
//            return null;
//    }
}
