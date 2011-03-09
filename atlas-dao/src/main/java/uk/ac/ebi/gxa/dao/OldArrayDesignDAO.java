package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.getFirst;

public class OldArrayDesignDAO implements ArrayDesignDAOInterface {

    public static final String ARRAY_DESIGN_SELECT =
            "SELECT accession, type, name, provider, arraydesignid " +
                    "FROM a2_arraydesign ORDER BY accession";
    public static final String ARRAY_DESIGN_BY_ACC_SELECT =
            "SELECT accession, type, name, provider, arraydesignid FROM a2_arraydesign WHERE accession=?";

    public static final String DESIGN_ELEMENTS_AND_GENES_BY_RELATED_ARRAY =
            "SELECT de.arraydesignid, de.designelementid, de.accession, de.name, de.geneid " +
                    "FROM a2_designelement de " +
                    "WHERE de.arraydesignid IN (:arraydesignids)";

    protected JdbcTemplate template;

    public List<ArrayDesign> getAllArrayDesigns() {
        return template.query(ARRAY_DESIGN_SELECT, new ArrayDesignMapper());
    }


    public ArrayDesign getArrayDesignByAccession(String accession) {
        List<ArrayDesign> results = template.query(ARRAY_DESIGN_BY_ACC_SELECT,
                new Object[]{accession},
                new ArrayDesignMapper());

        // get first result only
        ArrayDesign arrayDesign = getFirst(results, null);

        if (arrayDesign != null) {
            fillOutArrayDesigns(Collections.singletonList(arrayDesign));
        }

        return arrayDesign;
    }

    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        List<ArrayDesign> results = template.query(ARRAY_DESIGN_BY_ACC_SELECT,
                new Object[]{accession},
                new ArrayDesignMapper());

        return getFirst(results, null);
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    private void fillOutArrayDesigns(List<ArrayDesign> arrayDesigns) {
        // map array designs to array design id
        Map<Long, ArrayDesign> arrayDesignsByID = new HashMap<Long, ArrayDesign>();
        for (ArrayDesign array : arrayDesigns) {
            // index this array
            arrayDesignsByID.put(array.getArrayDesignID(), array);
        }

        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // now query for design elements that map to one of these array designs
        ArrayDesignElementMapper arrayDesignElementMapper = new ArrayDesignElementMapper(arrayDesignsByID);
        MapSqlParameterSource arrayParams = new MapSqlParameterSource();
        arrayParams.addValue("arraydesignids", arrayDesignsByID.keySet());
        namedTemplate.query(DESIGN_ELEMENTS_AND_GENES_BY_RELATED_ARRAY, arrayParams, arrayDesignElementMapper);
    }

    private static class ArrayDesignMapper implements RowMapper<ArrayDesign> {
        public ArrayDesign mapRow(ResultSet resultSet, int i)
                throws SQLException {
            ArrayDesign array = new ArrayDesign();

            array.setAccession(resultSet.getString(1));
            array.setType(resultSet.getString(2));
            array.setName(resultSet.getString(3));
            array.setProvider(resultSet.getString(4));
            array.setArrayDesignID(resultSet.getLong(5));

            return array;
        }
    }

    private static class ArrayDesignElementMapper implements RowCallbackHandler {
        private Map<Long, ArrayDesign> arrayByID;

        public ArrayDesignElementMapper(Map<Long, ArrayDesign> arraysByID) {
            this.arrayByID = arraysByID;
        }

        public void processRow(ResultSet rs) throws SQLException {
            long arrayID = rs.getLong(1);

            long id = rs.getLong(2);
            String acc = rs.getString(3);
            String name = rs.getString(4);
            long geneId = rs.getLong(5);

            ArrayDesign ad = arrayByID.get(arrayID);
            ad.addDesignElement(acc, id);
            ad.addDesignElement(name, id);
            ad.addGene(id, geneId);
        }
    }
}
