package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * User: nsklyar
 * Date: 23/02/2011
 */
public class ArrayDesignDAO extends AbstractAtlasDAO {

    public static final String ARRAY_DESIGN_SELECT =
            "SELECT accession, type, name, provider, arraydesignid, mappingswid " +
                    "FROM a2_arraydesign ORDER BY accession";

    public static final String ARRAY_DESIGN_BY_ACC_SELECT =
            "SELECT accession, type, name, provider, arraydesignid, mappingswid FROM a2_arraydesign WHERE accession=?";

    public static final String DESIGN_ELEMENTS_AND_GENES_BY_RELATED_ARRAY =
            "SELECT degn.arraydesignid, degn.designelementid, degn.accession, degn.name, degn.bioentityid\n" +
                    "from VWDESIGNELEMENTGENE degn \n" +
                    "JOIN a2_arraydesign ad ON ad.arraydesignid = degn.arraydesignid\n" +
                    "WHERE degn.arraydesignid = ?\n" +
                    "AND degn.annotationswid = ?\n" +
                    "AND degn.mappingswid = ad.mappingswid";

    public static final String ARRAYDESIGN_IDS_BY_EXPERIMENT_ACCESSION =
            "SELECT distinct ad.accession, ad.type, ad.name, ad.provider, ad.arraydesignid, ad.mappingswid \n" +
                    "FROM a2_arraydesign ad \n" +
                    "JOIN a2_assay ass ON ass.arraydesignid = ad.arraydesignid\n" +
                    "JOIN a2_experiment e ON e.experimentid = ass.experimentid\n" +
                    "WHERE e.accession = ?";
    private SoftwareDAO softwareDAO;

    /**
     * Returns all array designs in the underlying datasource.  Note that, to reduce query times, this method does NOT
     * prepopulate ArrayDesigns with their associated design elements (unlike other methods to retrieve array designs
     * more specifically).
     *
     * @return the list of array designs, not prepopulated with design elements.
     */
    public List<ArrayDesign> getAllArrayDesigns() {
        List results = template.query(ARRAY_DESIGN_SELECT,
                new ArrayDesignMapper());

        return (List<ArrayDesign>) results;
    }

    public List<ArrayDesign> getArrayDesignsForExperiment(String experimentAcc) {
        List results = template.query(ARRAYDESIGN_IDS_BY_EXPERIMENT_ACCESSION,
                new Object[]{experimentAcc},
                new ArrayDesignMapper());

        return (List<ArrayDesign>) results;
    }

    public ArrayDesign getArrayDesignByAccession(String accession) {
        List<ArrayDesign> results = template.query(ARRAY_DESIGN_BY_ACC_SELECT,
                new Object[]{accession},
                new ArrayDesignMapper());

        // get first result only
        ArrayDesign arrayDesign = first(results);

        if (arrayDesign != null) {
            fillOutArrayDesigns(arrayDesign);
        }

        return arrayDesign;
    }

    /**
     * @param accession Array design accession
     * @return Array design (with no design element and gene ids filled in) corresponding to accession
     */
    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        List<ArrayDesign> results = template.query(ARRAY_DESIGN_BY_ACC_SELECT,
                new Object[]{accession},
                new ArrayDesignMapper());

        return first(results);
    }

    private void fillOutArrayDesigns(ArrayDesign arrayDesign) {

        long annotationsSW = getSoftwareDAO().getLatestVersionOfSoftware(SoftwareDAO.ENSEMBL);

        template.query(DESIGN_ELEMENTS_AND_GENES_BY_RELATED_ARRAY,
                new Object[]{arrayDesign.getArrayDesignID(), annotationsSW},
                new ArrayDesignElementMapper(arrayDesign));
    }

    private static <T> T first(List<T> results) {
        return results.size() > 0 ? results.get(0) : null;
    }

    ////////////////////////////////////////
    // Mappers
    // ////////////////////////////////////////
    private static class ArrayDesignElementMapper implements RowMapper {
        private ArrayDesign arrayDesign;

        public ArrayDesignElementMapper(ArrayDesign arrayDesign) {
            this.arrayDesign = arrayDesign;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {

            long deid = resultSet.getLong(2);
            String acc = resultSet.getString(3);
            String name = resultSet.getString(4);
            long geneId = resultSet.getLong(5);

            arrayDesign.addDesignElement(acc, deid);
            arrayDesign.addDesignElement(name, deid);
            arrayDesign.addGene(deid, geneId);

            return arrayDesign;
        }

    }

    private static class ArrayDesignMapper implements RowMapper {
        public Object mapRow(ResultSet resultSet, int i)
                throws SQLException {
            ArrayDesign array = new ArrayDesign();

            array.setAccession(resultSet.getString(1));
            array.setType(resultSet.getString(2));
            array.setName(resultSet.getString(3));
            array.setProvider(resultSet.getString(4));
            array.setArrayDesignID(resultSet.getLong(5));
            array.setMappingSoftwareId(resultSet.getLong(6));

            return array;
        }
    }

    public SoftwareDAO getSoftwareDAO() {
        if (softwareDAO == null) {
            softwareDAO = new SoftwareDAO();
            softwareDAO.setJdbcTemplate(template);
        }
        return softwareDAO;
    }
}
