package uk.ac.ebi.gxa.dao.bioentity;

import com.google.common.collect.ArrayListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntity;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.collect.Iterables.partition;

/**
 * @author Nataliya Sklyar
 */
public class BioEntityDAO {
    public static final String ALL_GENE_DESIGN_ELEMENT_DIRECT = "SELECT distinct " + GeneDesignElementMapper.FIELDS + "\n" +
            "  FROM a2_designelement de\n" +
            "  join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid\n" +
            "  join a2_designeltbioentity debe on debe.designelementid = de.designelementid and  debe.softwareid = ad.mappingswid\n" +
            "  join a2_bioentity be on be.bioentityid = debe.bioentityid\n" +
            "  join a2_bioentitytype bet on bet.bioentitytypeid = be.bioentitytypeid and bet.ID_FOR_INDEX = 1";


    public static final int MAX_QUERY_PARAMS = 15;
    public static final int SUB_BATCH_SIZE = 50;

    private static Logger log = LoggerFactory.getLogger(BioEntityDAO.class);
    private SoftwareDAO softwareDAO;
    private BioEntityPropertyDAO propertyDAO;
    private BioEntityTypeDAO typeDAO;
    protected JdbcTemplate template;

    private static Map<String, BioEntityType> beTypeCache = new HashMap<String, BioEntityType>();

    public BioEntityDAO(SoftwareDAO softwareDAO, JdbcTemplate template, BioEntityPropertyDAO propertyDAO, BioEntityTypeDAO typeDAO) {
        this.template = template;
        this.softwareDAO = softwareDAO;
        this.propertyDAO = propertyDAO;
        this.typeDAO = typeDAO;
    }

    /**
     * Same as getAllGenes(), but doesn't do design elements. Sometime we just don't need them.
     *
     * @return list of all genes
     */
    public List<BioEntity> getAllGenesFast() {
        // do the query to fetch genes without design elements
        return template.query("SELECT " + GeneMapper.FIELDS + " \n" +
                "FROM a2_bioentity be \n" +
                "JOIN a2_organism o ON o.organismid = be.organismid\n" +
                "JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid\n" +
                "WHERE bet.id_for_index = 1",
                new GeneMapper());
    }

    public List<BioEntity> getGenes(String prefix, int offset, int limit) {
        return template.query("SELECT " + GeneMapper.FIELDS_CLEAN + "\n" +
                " FROM ( " +
                "   SELECT ROW_NUMBER() OVER(ORDER BY be.identifier) LINENUM, " + GeneMapper.FIELDS + "\n" +
                "     FROM a2_bioentity be \n" +
                "     JOIN a2_organism o ON o.organismid = be.organismid \n" +
                "     JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid \n" +
                "    WHERE bet.id_for_index = 1 \n" +
                "      AND LOWER(be.identifier) LIKE ? \n" +
                "    ORDER BY be.identifier \n" +
                ") WHERE LINENUM BETWEEN ? AND ?",
                new Object[]{prefix.toLowerCase() + "%", offset, offset + limit - 1},
                new GeneMapper());
    }

    public int getGeneCount() {
        return template.queryForInt("select count(be.bioentityid) \n" +
                "from a2_bioentity be \n" +
                "join a2_bioentitytype bet on bet.bioentitytypeid = be.bioentitytypeid\n" +
                "where bet.id_for_index = 1");
    }

    public void getPropertiesForGenes(List<BioEntity> bioEntities) {
        // populate the other info for these genes
        if (bioEntities.size() > 0) {
            fillOutGeneProperties(bioEntities);
        }
    }

    public ArrayListMultimap<Long, DesignElement> getAllDesignElementsForGene() {

        ArrayListMultimap<Long, DesignElement> beToDe = ArrayListMultimap.create(350000, 200);

        GeneDesignElementMapper mapper = new GeneDesignElementMapper(beToDe);

        //ToDo: querying for linked bioentities might be skipped for now, while we have all mappings directly to genes
//        template.query(ALL_GENE_DESIGN_ELEMENT_LINKED,
//                new Object[]{annotationsSW},
//                mapper);

        template.query(ALL_GENE_DESIGN_ELEMENT_DIRECT,
                mapper);
        return beToDe;
    }

    public BioEntityType findOrCreateBioEntityType(final String name) {
        if (beTypeCache.containsKey(name)) {
            return beTypeCache.get(name);
        }
        BioEntityType type = typeDAO.findOrCreate(name);
        beTypeCache.put(name, type);
        return type;
    }

//    private long getArrayDesignIdByAccession(String arrayDesignAccession) {
//        return template.queryForLong(ARRAYDESIGN_ID, arrayDesignAccession);
//    }

    /////////////////////////////////////////////////////////////////////////////
    //   Write methods
    /////////////////////////////////////////////////////////////////////////////
    public void writeBioentities(final Collection<BioEntity> bioEntities) {
        String query = "merge into a2_bioentity p\n" +
                "  using (select  1 from dual)\n" +
                "  on (p.identifier = ? and p.bioentitytypeid = ?)\n" +
                "  when not matched then \n" +
                "  insert (identifier, organismid, bioentitytypeid)   \n" +
                "  values (?, ?, ?) ";
        //ToDo(4ns): add BE name

        ListStatementSetter<BioEntity> statementSetter = new ListStatementSetter<BioEntity>() {

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).getIdentifier());
                ps.setLong(2, list.get(i).getType().getId());
                ps.setString(3, list.get(i).getIdentifier());
                ps.setLong(4, list.get(i).getOrganism().getId());
                ps.setLong(5, list.get(i).getType().getId());
            }

        };

        int loadedRecordsNumber = writeBatchInChunks(query, bioEntities, statementSetter);
        log.info("BioEntities merged: " + loadedRecordsNumber);

    }

    public void writePropertyValues(final Collection<BEPropertyValue> propertyValues) {

        String query = "merge into a2_bioentitypropertyvalue pv\n" +
                "  using (select  1 from dual)\n" +
                "  on (pv.value = ? and pv.bioentitypropertyid = ?)\n" +
                "  when not matched then \n" +
                "  insert (value, bioentitypropertyid) values (?, ?)";


        ListStatementSetter<BEPropertyValue> statementSetter = new ListStatementSetter<BEPropertyValue>() {

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).getValue());
                ps.setLong(2, list.get(i).getProperty().getBioentitypropertyid());
                ps.setString(3, list.get(i).getValue());
                ps.setLong(4, list.get(i).getProperty().getBioentitypropertyid());
            }
        };

        int loadedRecords = writeBatchInChunks(query, propertyValues, statementSetter);
        log.info("PropertieValues merged : " + loadedRecords);

    }

    /**
     * @param beProperties - a List of String array, which contains values:
     *                     [0] - BioEntity identifier
     *                     [1] - property name
     *                     [2] - property value
     * @param beType
     * @param software
     */
    public void writeBioEntityToPropertyValues(final Set<List<String>> beProperties, final BioEntityType beType,
                                               final Software software) {

        String query = "insert into a2_bioentitybepv (bioentityid, bepropertyvalueid, softwareid) \n" +
                "  values (\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ? and be.bioentitytypeid = ?),\n" +
                "  (select pv.bepropertyvalueid from a2_bioentitypropertyvalue pv " +
                "where pv.VALUE = ? " +
                "  and pv.bioentitypropertyid = ?),\n" +
                "  ?)";


        ListStatementSetter<List<String>> statementSetter = new ListStatementSetter<List<String>>() {
            long softwareId = software.getSoftwareid();
            List<BioEntityProperty> properties = getAllBEProperties();

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).get(0));
                ps.setLong(2, beType.getId());
                ps.setString(3, list.get(i).get(2));
                ps.setLong(4, properties.get(i).getBioentitypropertyid());
                ps.setLong(5, softwareId);
            }

        };

        writeBatchInChunks(query, beProperties, statementSetter);
    }

    /**
     * @param relations - a List of StrinBioEntitis, which contains values:
     *                  [0] - gene
     *                  [1] - transcript
     * @param software
     */
    public void writeGeneToBioentityRelations(final Set<List<BioEntity>> relations,
                                              final Software software) {

        String query = "INSERT INTO a2_bioentity2bioentity "
                + "            (bioentityidto, "
                + "             bioentityidfrom, "
                + "             softwareid) "
                + "VALUES      ( (SELECT be.bioentityid "
                + "              FROM   a2_bioentity be "
                + "              WHERE  be.identifier = ? and be.bioentitytypeid = ?), "
                + "             (SELECT be.bioentityid "
                + "              FROM   a2_bioentity be "
                + "              WHERE  be.identifier = ? and be.bioentitytypeid = ?), "
                + "             ?) ";


        ListStatementSetter<List<BioEntity>> statementSetter = new ListStatementSetter<List<BioEntity>>() {
            long softwareId = software.getSoftwareid();


            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).get(0).getIdentifier());
                ps.setLong(2, list.get(i).get(0).getType().getId());
                ps.setString(3, list.get(i).get(1).getIdentifier());
                ps.setLong(4, list.get(i).get(1).getType().getId());
                ps.setLong(5, softwareId);
            }
        };

        writeBatchInChunks(query, relations, statementSetter);

    }

    public void writeArrayDesign(final ArrayDesign arrayDesign, Software software) {
        String query = "merge into a2_arraydesign a\n" +
                "  using (select  1 from dual)\n" +
                "  on (a.accession = ?)\n" +
                "  when matched then\n" +
                "            update set mappingswid = ?\n" +
                "  when not matched then \n" +
                "   insert (accession, name, type, provider, mappingswid) values (?, ?, ?, ?, ?)";

        final long swId = software.getSoftwareid();

        template.update(query, new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, arrayDesign.getAccession());
                ps.setLong(2, swId);
                ps.setString(3, arrayDesign.getAccession());
                ps.setString(4, arrayDesign.getName());
                ps.setString(5, arrayDesign.getType());
                ps.setString(6, arrayDesign.getProvider());
                ps.setLong(7, swId);
            }
        });

        log.info("Loaded/Updated ArrayDisign : " + arrayDesign.getAccession());
    }

    public void writeDesignElements(final Collection<DesignElement> designElements, final ArrayDesign arrayDesign) {
        String query = "MERGE INTO a2_designelement de\n" +
                "  USING (select  1 from dual)\n" +
                "  ON (de.arraydesignid = ? AND de.accession = ?)\n" +
                "  WHEN NOT MATCHED THEN\n" +
                "  INSERT (arraydesignid,\n" +
                "          accession,\n" +
                "          name)\n" +
                "  VALUES(?, ?, ?)";


        ListStatementSetter<DesignElement> setter = new ListStatementSetter<DesignElement>() {
            long adId = arrayDesign.getArrayDesignID();

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, adId);
                ps.setString(2, list.get(i).getAccession());
                ps.setLong(3, adId);
                ps.setString(4, list.get(i).getAccession());
                ps.setString(5, list.get(i).getName());
            }
        };
        writeBatchInChunks(query, designElements, setter);

    }

    public void writeDesignElementBioentityMappings(final Collection<List<String>> deToBeMappings, final BioEntityType beType,
                                                    final Software software,
                                                    final ArrayDesign arrayDesign) {

        String query = "INSERT INTO a2_designeltbioentity \n" +
                " (designelementid, bioentityid, softwareid)\n" +
                " VALUES\n" +
                " ((select de.designelementid from A2_DESIGNELEMENT de where de.accession = ? and de.arraydesignid = ?),\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ? and be.bioentitytypeid = ?),\n" +
                "  ?)";

        ListStatementSetter<List<String>> setter = new ListStatementSetter<List<String>>() {
            long swId = software.getSoftwareid();
            long adId = arrayDesign.getArrayDesignID();
            long typeId = beType.getId();

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).get(0));
                ps.setLong(2, adId);
                ps.setString(3, list.get(i).get(1));
                ps.setLong(4, typeId);
                ps.setLong(5, swId);
            }
        };

        writeBatchInChunks(query, deToBeMappings, setter);
    }

    private <T> int writeBatchInChunks(String query, final Collection<T> entityList, ListStatementSetter<T> statementSetter) {
        int loadedRecordsNumber = 0;

        for (List<T> subList : partition(entityList, SUB_BATCH_SIZE)) {
            statementSetter.setList(subList);
            int[] rowsAffectedArray = template.batchUpdate(query, statementSetter);
            loadedRecordsNumber += rowsAffectedArray.length;
            log.info("Number of rows loaded to the DB = " + loadedRecordsNumber);
        }

        return loadedRecordsNumber;
    }

    private void fillOutGeneProperties(List<BioEntity> bioEntities) {
        // map genes to gene id
        Map<Long, BioEntity> genesByID = new HashMap<Long, BioEntity>();
        for (BioEntity gene : bioEntities) {
            // index this assay
            genesByID.put(gene.getId(), gene);
        }

        // map of genes and their properties
        GenePropertyMapper genePropertyMapper = new GenePropertyMapper(genesByID);

        // query template for genes
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        //find all recent software
        List<Software> softwares = softwareDAO.getActiveSoftwares();
        Set<Long> swIds = new HashSet<Long>(softwares.size());
        for (Software software : softwares) {
            swIds.add(software.getSoftwareid());
        }

        // if we have more than 'MAX_QUERY_PARAMS' genes, split into smaller queries
        List<Long> geneIDs = new ArrayList<Long>(genesByID.keySet());
        for (List<Long> geneIDsChunk : partition(geneIDs, MAX_QUERY_PARAMS)) {
            // now query for properties that map to one of these genes
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("swid", swIds);
            propertyParams.addValue("geneids", geneIDsChunk);

            //ToDo: now gets only properties which are directly linked with the queried bioentities, we might need also to get properties of connected bioentities
            namedTemplate.query("select " + GenePropertyMapper.FIELDS + "\n" +
                    "  from a2_bioentitybepv bebepv \n" +
                    "  join a2_bioentitypropertyvalue bepv on bepv.bepropertyvalueid = bebepv.bepropertyvalueid\n" +
                    "  join a2_bioentityproperty bep on bep.bioentitypropertyid = bepv.bioentitypropertyid\n" +
                    "  where bebepv.softwareid in (:swid)  " +
                    "  and bebepv.bioentityid in (:geneids)", propertyParams, genePropertyMapper);
        }
    }

    private List<BioEntityProperty> getAllBEProperties() {
        return propertyDAO.getAll();
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    private static class GenePropertyMapper implements RowMapper<BEPropertyValue> {
        public static String FIELDS = "bebepv.bioentityid, bep.name, bep.bioentitypropertyid, bepv.value , bepv.bepropertyvalueid ";

        private Map<Long, BioEntity> genesByID;

        public GenePropertyMapper(Map<Long, BioEntity> genesByID) {
            this.genesByID = genesByID;
        }

        public BEPropertyValue mapRow(ResultSet resultSet, int i) throws SQLException {
            BioEntityProperty property = new BioEntityProperty(resultSet.getLong(3), resultSet.getString(2));
            BEPropertyValue propertyValue = new BEPropertyValue(resultSet.getLong(5), property, resultSet.getString(4));

            long geneID = resultSet.getLong(1);
            genesByID.get(geneID).addProperty(propertyValue);

            return propertyValue;
        }
    }


    private static class GeneDesignElementMapper implements RowCallbackHandler {
        public static final String FIELDS = "be.bioentityid, de.accession, de.name";
        private ArrayListMultimap<Long, DesignElement> designElementsByBeID;

        public GeneDesignElementMapper(ArrayListMultimap<Long, DesignElement> designElementsByBeID) {
            this.designElementsByBeID = designElementsByBeID;
        }

        @Override
        public void processRow(ResultSet rs) throws SQLException {
            long geneID = rs.getLong(1);
            designElementsByBeID.put(geneID, new DesignElement(rs.getString(2), rs.getString(3)));

            if (designElementsByBeID.size() % 10000 == 0)
                log.debug("designElementsByBeID = " + designElementsByBeID.size());
        }
    }


    private class GeneMapper implements RowMapper<BioEntity> {
        public static final String FIELDS_CLEAN = "bioentityid, identifier, name, species, speciesid, typename";
        public static final String FIELDS = "be.bioentityid, be.identifier, be.name, o.name AS species, o.organismid AS speciesid, bet.name as typename";

        private String intern(String str) {
            return str != null ? str.intern() : null;
        }

        public BioEntity mapRow(ResultSet resultSet, int i) throws SQLException {
            BioEntityType type = findOrCreateBioEntityType(resultSet.getString(6));
            BioEntity gene = new BioEntity(resultSet.getString(2), type);

            gene.setId(resultSet.getLong(1));
            gene.setName(resultSet.getString(3));
            Organism organism = new Organism(resultSet.getLong(5), intern(resultSet.getString(4)));
            gene.setOrganism(organism);

            return gene;
        }
    }


    private abstract static class ListStatementSetter<T> implements BatchPreparedStatementSetter {
        protected List<T> list;

        public int getBatchSize() {
            return list.size();
        }

        public void setList(List<T> list) {
            this.list = list;
        }
    }
}
