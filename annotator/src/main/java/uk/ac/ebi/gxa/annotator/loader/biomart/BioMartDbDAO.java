package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * User: nsklyar
 * Date: 11/08/2011
 */
public class BioMartDbDAO {
    protected JdbcTemplate template;

    public BioMartDbDAO(String url) {
        createTemplate(url);
    }

    public Set<List<String>> getSynonyms(String dbNameTemplate, String version) throws BioMartAccessException {
        String dbName = findDBName(dbNameTemplate, version);
        String query = "SELECT gene_stable_id.stable_id,  external_synonym.synonym \n" +
                "FROM " + dbName + ".gene_stable_id, " + dbName + ".gene, " + dbName + ".xref, " + dbName + ".external_synonym \n" +
                "WHERE gene_stable_id.gene_id = gene.gene_id \n" +
                "AND gene.display_xref_id = xref.xref_id \n" +
                "AND external_synonym.xref_id = xref.xref_id \n" +
                "ORDER BY gene_stable_id.stable_id; ";
        List<List<String>> result = template.query(query, new RowMapper<List<String>>() {
            @Override
            public List<String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                List<String> answer = new ArrayList<String>(2);
                answer.add(rs.getString(1));
                answer.add(rs.getString(2));

                return answer;
            }
        });
        return new HashSet<List<String>>(result);
    }

//    protected List<List<String>> getSynonymsByDbName(String dbName) {
//        String query = "SELECT gene_stable_id.stable_id,  external_synonym.synonym \n" +
//                "FROM " + dbName + ".gene_stable_id, " + dbName + ".gene, " + dbName + ".xref, " + dbName + ".external_synonym \n" +
//                "WHERE gene_stable_id.gene_id = gene.gene_id \n" +
//                "AND gene.display_xref_id = xref.xref_id \n" +
//                "AND external_synonym.xref_id = xref.xref_id \n" +
//                "ORDER BY gene_stable_id.stable_id; ";
//        List<List<String>> result = template.query(query, new RowMapper<List<String>>() {
//            @Override
//            public List<String> mapRow(ResultSet rs, int rowNum) throws SQLException {
//                List<String> answer = new ArrayList<String>(2);
//                answer.add(rs.getString(1));
//                answer.add(rs.getString(2));
//
//                return answer;
//            }
//        });
//        return result;
//    }

    protected String findDBName(String dbNameTemplate, String version) throws BioMartAccessException {
        String query = "SHOW DATABASES LIKE \"" + dbNameTemplate + "_core_" + version + "%\"";
        List<String> result = template.query(query, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        });

        if (result.size() != 1) {
            throw new BioMartAccessException("Cannot find database name to fetch synonyms. Please check Annotation Source configuration");
        }
        return result.get(0);
    }

    protected void createTemplate(String url) {
        BasicDataSource source = new BasicDataSource();
        source.setDriverClassName("com.mysql.jdbc.Driver");
        source.setUrl("jdbc:mysql://" + url);
        source.setUsername("anonymous");
        source.setPassword("");


       this.template = new JdbcTemplate(source);
    }
}
