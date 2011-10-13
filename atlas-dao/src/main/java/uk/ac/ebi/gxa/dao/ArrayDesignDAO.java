package uk.ac.ebi.gxa.dao;

import org.hibernate.SessionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.hibernate3.HibernateTemplate;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static com.google.common.collect.Iterables.getFirst;

/**
 * @author Nataliya Sklyar
 */
public class ArrayDesignDAO {
    private static final String ARRAY_DESIGN_SELECT =
            "SELECT " + ArrayDesignMapper.FIELDS + " FROM a2_arraydesign ad ORDER BY ad.accession";

    private static final String ARRAY_DESIGN_BY_ACC_SELECT =
            "SELECT " + ArrayDesignMapper.FIELDS + " FROM a2_arraydesign ad WHERE ad.accession=?";

    private JdbcTemplate template;
    private HibernateTemplate ht;

    public ArrayDesignDAO(JdbcTemplate template,  SessionFactory sessionFactory) {
        this.template = template;
        this.ht = new HibernateTemplate(sessionFactory);
    }

    /**
     * Returns all array designs in the underlying datasource.  Note that, to reduce query times, this method does NOT
     * prepopulate ArrayDesigns with their associated design elements (unlike other methods to retrieve array designs
     * more specifically).
     *
     * @return the list of array designs, not prepopulated with design elements.
     */
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
            fillOutArrayDesigns(arrayDesign);
        }

        return arrayDesign;
    }

    /**
     * @param accession Array design accession
     * @return Array design (with no design element and gene ids filled in) corresponding to accession
     */
    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        @SuppressWarnings("unchecked")
        List<ArrayDesign> results = ht.find("from ArrayDesign where accession = ?", accession);
        return getFirst(results, null);
    }

    public void save(ArrayDesign ad) {
        ht.saveOrUpdate(ad);   
        ht.flush();
    }

    private void fillOutArrayDesigns(ArrayDesign arrayDesign) {

        if (!arrayDesign.hasGenes()) {
            template.query("SELECT " + ArrayDesignElementCallback.FIELDS +
                    " FROM a2_designelement de\n" +
                    "  join a2_designeltbioentity debe on debe.designelementid = de.designelementid\n" +
                    "  join a2_bioentity indexedbe on indexedbe.bioentityid = debe.bioentityid\n" +
                    "  join a2_bioentitytype betype on betype.bioentitytypeid = indexedbe.bioentitytypeid\n" +
                    "  JOIN A2_SOFTWARE SW ON SW.SOFTWAREID = DEBE.SOFTWAREID\n" +
                    "  where sw.isactive = 'T'\n" +
                    "  AND BETYPE.ID_FOR_INDEX = 1\n" +
                    "  and de.arraydesignid = ?",
                    new Object[]{arrayDesign.getArrayDesignID()},
                    new ArrayDesignElementCallback(arrayDesign));
        }
    }

    ////////////////////////////////////////
    // Mappers
    // ////////////////////////////////////////
    private static class ArrayDesignElementCallback implements RowCallbackHandler {
        private static final String FIELDS = "distinct de.designelementid, de.accession, de.name, indexedbe.bioentityid ";
        private ArrayDesign arrayDesign;

        public ArrayDesignElementCallback(ArrayDesign arrayDesign) {
            this.arrayDesign = arrayDesign;
        }

        public void processRow(ResultSet resultSet) throws SQLException {
            long deid = resultSet.getLong(1);
            String acc = resultSet.getString(2);
            String name = resultSet.getString(3);
            long geneId = resultSet.getLong(4);

            arrayDesign.addDesignElement(acc, deid);
            arrayDesign.addDesignElement(name, deid);
            arrayDesign.addGene(deid, geneId);
        }
    }

    private static class ArrayDesignMapper implements RowMapper<ArrayDesign> {
        private static final String FIELDS = "ad.accession, ad.type, ad.name, ad.provider, ad.arraydesignid, ad.mappingswid";

        public ArrayDesign mapRow(ResultSet resultSet, int i) throws SQLException {
            ArrayDesign array = new ArrayDesign(resultSet.getString(1));

            array.setType(resultSet.getString(2));
            array.setName(resultSet.getString(3));
            array.setProvider(resultSet.getString(4));
            array.setArrayDesignID(resultSet.getLong(5));
            array.setMappingSoftwareId(resultSet.getLong(6));

            return array;
        }
    }
}
