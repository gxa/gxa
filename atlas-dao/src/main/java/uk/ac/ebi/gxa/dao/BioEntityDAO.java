package uk.ac.ebi.gxa.dao;

import com.google.common.collect.ArrayListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.*;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.BioEntity;
import uk.ac.ebi.microarray.atlas.model.DesignElement;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.google.common.collect.Iterables.partition;

/**
 * TODO: Rename me to JdbcBioEntityDAO
 *
 * @author Nataliya Sklyar
 */
public class BioEntityDAO {
    private static Logger log = LoggerFactory.getLogger(BioEntityDAO.class);

    public static final String ALL_GENE_DESIGN_ELEMENT_LINKED = "SELECT distinct " + GeneDesignElementMapper.FIELDS + "\n" +
            "  FROM a2_designelement de\n" +
            "          join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid\n" +
            "          join a2_designeltbioentity debe on debe.designelementid = de.designelementid and debe.softwareid = ad.mappingswid\n" +
            "          join a2_bioentity2bioentity be2be on be2be.bioentityidfrom = debe.bioentityid and be2be.softwareid = ?\n" +
            "          join a2_bioentity be on be.bioentityid = be2be.bioentityidto\n" +
            "          join a2_bioentitytype betype on betype.bioentitytypeid = be.bioentitytypeid and betype.ID_FOR_INDEX = 1";

    public static final String ALL_GENE_DESIGN_ELEMENT_DIRECT = "SELECT distinct " + GeneDesignElementMapper.FIELDS + "\n" +
            "  FROM a2_designelement de\n" +
            "  join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid\n" +
            "  join a2_designeltbioentity debe on debe.designelementid = de.designelementid and  debe.softwareid = ad.mappingswid\n" +
            "  join a2_bioentity be on be.bioentityid = debe.bioentityid\n" +
            "  join a2_bioentitytype bet on bet.bioentitytypeid = be.bioentitytypeid and bet.ID_FOR_INDEX = 1";

    public static final String BIOENTITYTYPE_ID = "SELECT bioentitytypeid FROM a2_bioentitytype WHERE name = ?";

    public static final String ARRAYDESIGN_ID = "SELECT a.arraydesignid FROM a2_arraydesign a WHERE a.accession = ?";

    public static final String ALL_PROPERTIES = "SELECT bioentitypropertyid, name FROM a2_bioentityproperty";


    public static final int MAX_QUERY_PARAMS = 15;
    public static final int SUB_BATCH_SIZE = 50;

    private final JdbcTemplate template;
    private final SoftwareDAO softwareDAO;

    public BioEntityDAO(JdbcTemplate template, SoftwareDAO softwareDAO) {
        this.template = template;
        this.softwareDAO = softwareDAO;
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
        long annotationsSW = softwareDAO.getLatestVersionOfSoftware(SoftwareDAO.ENSEMBL);


        ArrayListMultimap<Long, DesignElement> beToDe = ArrayListMultimap.create(350000, 200);

        GeneDesignElementMapper mapper = new GeneDesignElementMapper(beToDe);
        template.query(ALL_GENE_DESIGN_ELEMENT_LINKED,
                new Object[]{annotationsSW},
                mapper);

        template.query(ALL_GENE_DESIGN_ELEMENT_DIRECT,
                mapper);
        return beToDe;
    }

    public Map<String, Long> getAllBEProperties() {
        final Map<String, Long> result = new HashMap<String, Long>();
        template.query(ALL_PROPERTIES, new RowMapper<Object>() {
            public Object mapRow(ResultSet rs, int i) throws SQLException {
                result.put(rs.getString(2), rs.getLong(1));
                return rs.getLong(1);
            }
        });
        return result;
    }

    public long getBETypeIdByName(final String typeName) {
        String query = "merge into a2_bioentitytype t\n" +
                "  using (select  1 from dual)\n" +
                "  on (t.name = ?)\n" +
                "  when not matched then \n" +
                "  insert (name) values (?)";

        template.update(query, new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, typeName);
                ps.setString(2, typeName);
            }
        });

        return template.queryForLong(BIOENTITYTYPE_ID, typeName);
    }


    private long getArrayDesignIdByAccession(String arrayDesignAccession) {
        return template.queryForLong(ARRAYDESIGN_ID, arrayDesignAccession);
    }

    public List<String> getSpeciesForExperiment(long experimentId) {
        return template.query("select distinct o.name\n" +
                "  from A2_ORGANISM o\n" +
                "          join A2_SAMPLE s on s.organismid = o.organismid\n" +
                "          join A2_ASSAYSAMPLE ass on ass.SAMPLEID = s.SAMPLEID\n" +
                "          join A2_ASSAY a on ass.ASSAYID = a.ASSAYID\n" +
                "  where a.EXPERIMENTID = ?",
                new Object[]{experimentId},
                new SingleColumnRowMapper<String>());
    }

    /////////////////////////////////////////////////////////////////////////////
    //   Write methods
    /////////////////////////////////////////////////////////////////////////////
    public void writeBioentities(final Set<BioEntity> bioEntities) {
        String query = "merge into a2_bioentity p\n" +
                "  using (select  1 from dual)\n" +
                "  on (p.identifier = ? and p.bioentitytypeid = ?)\n" +
                "  when not matched then \n" +
                "  insert (identifier, organismid, bioentitytypeid)   \n" +
                "  values (?, (select o.organismid from a2_organism o where o.name = ?), ?) ";

        final List<BioEntity> bioEntityList = new ArrayList<BioEntity>(bioEntities);

        if (bioEntityList.size() == 0) {
            return;
        }

        ListStatementSetter<BioEntity> statementSetter = new ListStatementSetter<BioEntity>() {

            long typeId = getBETypeIdByName(bioEntityList.get(0).getType());

            //ToDo: might be optimized: check if all BE have the same organism then get organism id only once
            /*
               "We should forget about small efficiencies, say about 97% of the time:
               premature optimization is the root of all evil.
               Yet we should not pass up our opportunities in that critical 3%.
               A good programmer will not be lulled into complacency by such reasoning,
               he will be wise to look carefully at the critical code;
               but only after that code has been identified" - Donald Knuth
             */
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).getIdentifier());
                ps.setLong(2, typeId);
                ps.setString(3, list.get(i).getIdentifier());
                ps.setString(4, list.get(i).getSpecies());
                ps.setLong(5, typeId);
            }

        };

        int loadedRecordsNumber = writeBatchInChunks(query, bioEntityList, statementSetter);
        log.info("BioEntities merged: " + loadedRecordsNumber);

    }

    public synchronized void writeProperties(final Set<String> properties) {
        final List<String> propList = new ArrayList<String>(properties);

        int[] ints = template.batchUpdate("merge into a2_bioentityproperty p\n" +
                "  using (select  1 from dual)\n" +
                "  on (p.name = ?)\n" +
                "  when not matched then \n" +
                "  insert (name) values (?)",
                new BatchPreparedStatementSetter() {
                    public void setValues(PreparedStatement ps, int i) throws SQLException {
                        ps.setString(1, propList.get(i));
                        ps.setString(2, propList.get(i));
                    }

                    public int getBatchSize() {
                        return propList.size();
                    }
                });

        log.info("Properties merged : " + ints.length);
    }

    public synchronized void writePropertyValues(final Collection<BEPropertyValue> propertyValues) {
        String query = "merge into a2_bioentitypropertyvalue pv\n" +
                "  using (select  1 from dual)\n" +
                "  on (pv.value = ? and pv.bioentitypropertyid = ?)\n" +
                "  when not matched then \n" +
                "  insert (value, bioentitypropertyid) values (?, ?)";

        List<BEPropertyValue> fullPropList = new ArrayList<BEPropertyValue>(propertyValues);

        ListStatementSetter<BEPropertyValue> statementSetter = new ListStatementSetter<BEPropertyValue>() {
            Map<String, Long> properties = getAllBEProperties();

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).getValue());
                ps.setLong(2, properties.get(list.get(i).getName()));
                ps.setString(3, list.get(i).getValue());
                ps.setLong(4, properties.get(list.get(i).getName()));
            }
        };

        int loadedRecords = writeBatchInChunks(query, fullPropList, statementSetter);
        log.info("PropertieValues merged : " + loadedRecords);

    }

    /**
     * @param beProperties - a List of String array, which contains values:
     *                     [0] - BioEntity identifier
     *                     [1] - property name
     *                     [2] - property value
     * @param swName
     * @param swVersion
     */
    public void writeBioEntityToPropertyValues(final Set<List<String>> beProperties, final String beType,
                                               final String swName, final String swVersion) {

        String query = "insert into a2_bioentitybepv (bioentityid, bepropertyvalueid, softwareid) \n" +
                "  values (\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ? and be.bioentitytypeid = ?),\n" +
                "  (select pv.bepropertyvalueid from a2_bioentitypropertyvalue pv " +
                "where pv.VALUE = ? " +
                "  and pv.bioentitypropertyid = ?),\n" +
                "  ?)";

        List<List<String>> propertyValues = new ArrayList<List<String>>(beProperties);

        ListStatementSetter<List<String>> statementSetter = new ListStatementSetter<List<String>>() {
            long softwareId = softwareDAO.getSoftwareId(swName, swVersion);
            Map<String, Long> properties = getAllBEProperties();
            long typeId = getBETypeIdByName(beType);

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).get(0));
                ps.setLong(2, typeId);
                ps.setString(3, list.get(i).get(2));
                ps.setLong(4, properties.get(list.get(i).get(1)));
                ps.setLong(5, softwareId);
            }

        };

        writeBatchInChunks(query, propertyValues, statementSetter);
    }

    /**
     * @param relations - a List of String array, which contains values:
     *                  [0] - gene identifier
     *                  [1] - transcript identifier
     * @param swName
     * @param swVersion
     */
    public void writeGeneToTranscriptRelations(final List<String[]> relations,
                                               final String transcriptType,
                                               final String geneType,
                                               final String swName, final String swVersion) {

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


        ListStatementSetter<String[]> statementSetter = new ListStatementSetter<String[]>() {
            long softwareId = softwareDAO.getSoftwareId(swName, swVersion);
            public long geneTypeId = getBETypeIdByName(geneType);
            public long tnsTypeId = getBETypeIdByName(transcriptType);

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i)[0]);
                ps.setLong(2, geneTypeId);
                ps.setString(3, list.get(i)[1]);
                ps.setLong(4, tnsTypeId);
                ps.setLong(5, softwareId);
            }
        };

        writeBatchInChunks(query, relations, statementSetter);

    }

    public synchronized void writeArrayDesign(final ArrayDesign arrayDesign, final String swName, final String swVersion) {
        String query = "merge into a2_arraydesign a\n" +
                "  using (select  1 from dual)\n" +
                "  on (a.accession = ?)\n" +
                "  when matched then\n" +
                "            update set mappingswid = ?\n" +
                "  when not matched then \n" +
                "   insert (accession, name, type, provider, mappingswid) values (?, ?, ?, ?, ?)";

        final long swId = softwareDAO.getSoftwareId(swName, swVersion);

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

    public void writeDesignElements(final List<DesignElement> designElements, final String arrayDesignAccession) {
        String query = "MERGE INTO a2_designelement de\n" +
                "  USING (select  1 from dual)\n" +
                "  ON (de.arraydesignid = ? AND de.accession = ?)\n" +
                "  WHEN NOT MATCHED THEN\n" +
                "  INSERT (arraydesignid,\n" +
                "          accession,\n" +
                "          name)\n" +
                "  VALUES(?, ?, ?)";


        ListStatementSetter<DesignElement> setter = new ListStatementSetter<DesignElement>() {
            long adId = getArrayDesignIdByAccession(arrayDesignAccession);

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

    public void writeDesignElementBioentityMappings(final Collection<List<String>> deToBeMappings, final String beType,
                                                    final String swName, final String swVersion,
                                                    final String arrayDesignAccession) {

        String query = "INSERT INTO a2_designeltbioentity \n" +
                " (designelementid, bioentityid, softwareid)\n" +
                " VALUES\n" +
                " ((select de.designelementid from A2_DESIGNELEMENT de where de.accession = ? and de.arraydesignid = ?),\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ? and be.bioentitytypeid = ?),\n" +
                "  ?)";

        List<List<String>> mappings = new ArrayList<List<String>>(deToBeMappings);

        ListStatementSetter<List<String>> setter = new ListStatementSetter<List<String>>() {
            long swId = softwareDAO.getSoftwareId(swName, swVersion);
            long adId = getArrayDesignIdByAccession(arrayDesignAccession);
            long typeId = getBETypeIdByName(beType);

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).get(0));
                ps.setLong(2, adId);
                ps.setString(3, list.get(i).get(1));
                ps.setLong(4, typeId);
                ps.setLong(5, swId);
            }
        };

        writeBatchInChunks(query, mappings, setter);
    }

    private <T> int writeBatchInChunks(String query, final List<T> bioEntityList, ListStatementSetter<T> statementSetter) {
        int loadedRecordsNumber = 0;

        for (List<T> subList : partition(bioEntityList, SUB_BATCH_SIZE)) {
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

        long ensAnnSW = softwareDAO.getLatestVersionOfSoftware(SoftwareDAO.ENSEMBL);
        long mRNAannSW = softwareDAO.getLatestVersionOfSoftware(SoftwareDAO.MIRBASE);

        // if we have more than 'MAX_QUERY_PARAMS' genes, split into smaller queries
        List<Long> geneIDs = new ArrayList<Long>(genesByID.keySet());
        for (List<Long> geneIDsChunk : partition(geneIDs, MAX_QUERY_PARAMS)) {
            // now query for properties that map to one of these genes
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            Long[] sw = {ensAnnSW, mRNAannSW};
            propertyParams.addValue("swid", Arrays.asList(sw));
            propertyParams.addValue("geneids", geneIDsChunk);

            //ToDo: gets only properties which are directly linked with the queried bioentities
            namedTemplate.query("select " + GenePropertyMapper.FIELDS + "\n" +
                    "  from a2_bioentitybepv bebepv \n" +
                    "  join a2_bioentitypropertyvalue bepv on bepv.bepropertyvalueid = bebepv.bepropertyvalueid\n" +
                    "  join a2_bioentityproperty bep on bep.bioentitypropertyid = bepv.bioentitypropertyid\n" +
                    "  where bebepv.softwareid in (:swid)  " +
                    "  and bebepv.bioentityid in (:geneids)", propertyParams, genePropertyMapper);
        }
    }


    private static class GenePropertyMapper implements RowMapper<BEPropertyValue> {
        public static String FIELDS = "bebepv.bioentityid as id, bep.name as property, bepv.value as propertyvalue";

        private Map<Long, BioEntity> genesByID;

        public GenePropertyMapper(Map<Long, BioEntity> genesByID) {
            this.genesByID = genesByID;
        }

        public BEPropertyValue mapRow(ResultSet resultSet, int i) throws SQLException {
            BEPropertyValue property = new BEPropertyValue(resultSet.getString(2).toLowerCase(), resultSet.getString(3));

            long geneID = resultSet.getLong(1);

            genesByID.get(geneID).addProperty(property);

            return property;
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


    private static class GeneMapper implements RowMapper<BioEntity> {
        public static final String FIELDS_CLEAN = "bioentityid, identifier, species";
        public static final String FIELDS = "be.bioentityid, be.identifier, o.name AS species";

        public BioEntity mapRow(ResultSet resultSet, int i) throws SQLException {
            BioEntity gene = new BioEntity(resultSet.getString(2));

            gene.setId(resultSet.getLong(1));
            gene.setSpecies(resultSet.getString(3));

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
