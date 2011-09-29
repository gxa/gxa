/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.utils.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;

/**
 * User: nsklyar
 * Date: 11/08/2011
 */
public class BioMartDbDAO {
    protected JdbcTemplate template;

    public BioMartDbDAO(String url) {
        createTemplate(url);
    }

    public HashSet<Pair<String, String>> getSynonyms(String dbNameTemplate, String version) throws BioMartAccessException {
        String dbName = findDBName(dbNameTemplate, version);
        String query = "SELECT gene_stable_id.stable_id,  external_synonym.synonym \n" +
                "FROM " + dbName + ".gene_stable_id, " + dbName + ".gene, " + dbName + ".xref, " + dbName + ".external_synonym \n" +
                "WHERE gene_stable_id.gene_id = gene.gene_id \n" +
                "AND gene.display_xref_id = xref.xref_id \n" +
                "AND external_synonym.xref_id = xref.xref_id \n" +
                "ORDER BY gene_stable_id.stable_id; ";
        List<Pair<String, String>> result = template.query(query, new RowMapper<Pair<String, String>>() {
            @Override
            public Pair<String, String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                Pair<String, String> pair = Pair.create(rs.getString(1), rs.getString(2));

                return pair;
            }
        });
        return new HashSet<Pair<String, String>>(result);
    }


    protected String findDBName(String dbNameTemplate, String version) throws BioMartAccessException {
        String query = "SHOW DATABASES LIKE \"" + dbNameTemplate + "_core_" + version + "%\"";
        List<String> result = template.query(query, new RowMapper<String>() {
            @Override
            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                return rs.getString(1);
            }
        });

        //here it is important that only a single line is returned. In a case Biomart changes format it is potentially possible more then a single result.
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
