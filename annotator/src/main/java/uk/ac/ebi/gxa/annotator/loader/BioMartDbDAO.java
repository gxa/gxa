/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.loader;

import org.apache.commons.dbcp.BasicDataSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import uk.ac.ebi.gxa.annotator.model.connection.AnnotationSourceAccessException;
import uk.ac.ebi.gxa.utils.Pair;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * @author Nataliya Sklyar
 */
class BioMartDbDAO {
    private String url;

    BioMartDbDAO(String url) {
        this.url = url;
    }

    public Collection<Pair<String, String>> getSynonyms(String dbNameTemplate, String version) throws AnnotationSourceAccessException {
        final String dbName = findSynonymsDBName(dbNameTemplate, version);
        final JdbcTemplate template = createTemplate(dbName);
        return template.query(
                "SELECT DISTINCT gene_stable_id.stable_id, external_synonym.synonym \n" +
                        "FROM gene_stable_id, gene, xref, external_synonym \n" +
                        "WHERE gene_stable_id.gene_id = gene.gene_id \n" +
                        "AND gene.display_xref_id = xref.xref_id \n" +
                        "AND external_synonym.xref_id = xref.xref_id \n" +
                        "ORDER BY gene_stable_id.stable_id; ",
                new RowMapper<Pair<String, String>>() {
                    @Override
                    public Pair<String, String> mapRow(ResultSet rs, int rowNum) throws SQLException {
                        return Pair.create(rs.getString(1), rs.getString(2));
                    }
                });
    }


    String findSynonymsDBName(String dbNameTemplate, String version) throws AnnotationSourceAccessException {
        final JdbcTemplate template = createTemplate("");
        try {
            // here it is important that only a single line is returned.
            // In a case Biomart changes format it is potentially possible more then a single result.
            return template.queryForObject("SHOW DATABASES LIKE ?",
                    new SingleColumnRowMapper<String>(String.class),
                    dbNameTemplate + "_core_" + version + "%");
        } catch (DataAccessException e) {
            throw new AnnotationSourceAccessException("Cannot find database name to fetch synonyms. Please check Annotation Source configuration");
        }
    }

    JdbcTemplate createTemplate(String name) {
        BasicDataSource source = new BasicDataSource();
        source.setDriverClassName("com.mysql.jdbc.Driver");
        source.setUrl("jdbc:mysql://" + url + "/" + name);
        source.setUsername("anonymous");
        source.setPassword("");
        return new JdbcTemplate(source);
    }
}
