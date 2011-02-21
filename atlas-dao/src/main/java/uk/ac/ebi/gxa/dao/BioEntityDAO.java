package uk.ac.ebi.gxa.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import uk.ac.ebi.gxa.utils.ChunkedSublistIterator;
import uk.ac.ebi.microarray.atlas.model.*;

import java.io.Writer;
import java.sql.*;
import java.util.*;

/**
 * User: nsklyar
 * Date: 03/02/2011
 */
public class BioEntityDAO extends AbstractAtlasDAO {
    // gene queries
    public static final String GENES_SELECT =
            "SELECT DISTINCT be.bioentityid, be.identifier, o.name AS species \n" +
                    "FROM a2_bioentity be \n" +
                    "JOIN a2_organism o ON o.organismid = be.organismid\n" +
                    "JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid\n" +
                    "WHERE bet.id_for_index = 1";

//    public static final String BE_SELECT =
//            "SELECT DISTINCT be.bioentityid, be.identifier " +
//                    "FROM a2_bioentity be  \n" +
//                    "JOIN a2_organism o ON o.organismid = be.organismid\n" +
//                    "JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid\n" +
//                    "WHERE bet.name = ? " +
//                    "AND o.name = ?";
//
//    public static final String BE_WITH_PROP_SELECT =
//            "SELECT  be.identifier, be.properties FROM test_clob be ";

    public static final String GENE_BY_ID =
            "SELECT DISTINCT be.bioentityid, be.identifier, o.name AS species " +
                    "FROM a2_bioentity be " +
                    "JOIN a2_organism o ON o.organismid = be.organismid " +
                    "JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid " +
                    "WHERE bet.id_for_index = 1\n" +
                    "AND be.bioentityid=?";


    public static final String GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT  distinct degn.bioentityid, degn.identifier, o.name AS species \n" +
                    "FROM VWDESIGNELEMENTGENE degn\n" +
                    "JOIN a2_bioentitytype betype on betype.bioentitytypeid = degn.bioentitytypeid\n" +
                    "JOIN a2_organism o ON o.organismid = degn.organismid\n" +
                    "JOIN a2_assay ass ON ass.arraydesignid = degn.arraydesignid\n" +
                    "JOIN a2_experiment e ON e.experimentid = ass.experimentid\n" +
                    "WHERE betype.id_for_index = 1 " +
                    "AND e.accession=?";

    public static final String PROPERTIES_BY_RELATED_GENES =
            "select distinct frombe.bioentityid as id, bep.name as property, bepv.value as propertyvalue\n" +
                    "  from \n" +
                    "  a2_bioentity frombe \n" +
                    "  join a2_be2be_unfolded be2be on be2be.beidfrom = frombe.bioentityid\n" +
                    "  join a2_bioentity tobe on tobe.bioentityid = be2be.beidto\n" +
                    "  join a2_bioentitytype betype on betype.bioentitytypeid = tobe.bioentitytypeid\n" +
                    "  join a2_bioentitybepv bebepv on bebepv.bioentityid = tobe.bioentityid\n" +
                    "  join a2_bioentitypropertyvalue bepv on bepv.bepropertyvalueid = bebepv.bepropertyvalueid\n" +
                    "  join a2_bioentityproperty bep on bep.bioentitypropertyid = bepv.bioentitypropertyid \n" +
                    "  \n" +
                    "  where betype.prop_for_index = '1' \n" +
                    "  and frombe.bioentityid in (:geneids)";

    public static final String DESIGN_ELEMENTS_BY_GENEID =
            "SELECT  degn.accession, degn.name \n" +
                    "FROM VWDESIGNELEMENTGENE degn \n" +
                    "JOIN a2_bioentitytype betype on betype.bioentitytypeid = degn.bioentitytypeid\n" +
                    "WHERE \n" +
                    "betype.id_for_index = 1 \n" +
                    "AND degn.bioentityid = ?";

    public static final String BIOENTITIES_BY_ORAGANISM =
            "select be.bioentityid, be.identifier, be.bioentitytypeid, be.properties \n" +
                    "from a2_bioentity_P be\n" +
                    "join a2_organism o on o.organismid = be.organismid\n" +
                    "join a2_bioentitytype t on t.bioentitytypeid = be.bioentitytypeid\n" +
                    "where o.name = ? and be.bioentityid <1000 ";

    public static final String ORGANISM_ID = "SELECT organismid FROM a2_organism WHERE name = ?";

    public static final String SOFTWARE_ID = "SELECT SOFTWAREid FROM a2_SOFTWARE " +
            "WHERE name = ? AND version = ?";

    public static final String BIOENTITYTYPE_ID = "SELECT bioentitytypeid FROM a2_bioentitytype WHERE name = ?";

    public static final String ARRAYDESIGN_ID = "SELECT a.arraydesignid FROM a2_arraydesign a WHERE a.accession = ?";

    public static final String ALL_PROPERTIES = "SELECT bioentitypropertyid, name FROM a2_bioentityproperty";

    private static final String INSERT_INTO_TMP_BIOENTITY_VALUES = "INSERT INTO TMP_BIOENTITY VALUES (?, ?, ?)";

    private static final String INSERT_INTO_BIOENTITY = "INSERT INTO A2_BIOENTITY_P (IDENTIFIER, ORGANISMID, BIOENTITYTYPEID, properties) " +
            "VALUES (?, ?, ?, ?)";
    private static final String UPDATE_BIOENTITY = "UPDATE A2_BIOENTITY_P SET BIOENTITYTYPEID = ?, properties = ? WHERE bioentityid = ? ";

    private static final String INSERT_INTO_TEST_CLOB = "INSERT INTO TEST_CLOB VALUES (?, ?)";

    private static final String INSERT_INTO_TMP_DESIGNELEMENTMAP_VALUES = "INSERT INTO TMP_BIOENTITY " +
            "(accession, name) VALUES (?, ?)";

    private int maxQueryParams = 500;

    final int subBatchSize = 5000;

    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Same as getAllGenes(), but doesn't do design elements. Sometime we just don't need them.
     *
     * @return list of all genes
     */
    public List<Gene> getAllGenesFast() {
        // do the query to fetch genes without design elements
        return (List<Gene>) template.query(GENES_SELECT,
                new GeneMapper());
    }

    /**
     * Fetches one gene from the database. Note that genes are not automatically prepopulated with property information,
     * to keep query time down.  If you require this data, you can fetch it for the list of genes you want to obtain
     * properties for by calling {@link #getPropertiesForGenes(java.util.List)}.
     *
     * @param id gene's ID
     * @return the gene found
     */
    public Gene getGeneById(Long id) {
        // do the query to fetch gene without design elements
        List results = template.query(GENE_BY_ID,
                new Object[]{id},
                new GeneMapper());

        fillOutGeneProperties(results);

        if (results.size() > 0) {
            return (Gene) results.get(0);
        }
        return null;
    }


    /**
     * Fetches all genes for the given experiment accession.  Note that genes are not automatically prepopulated with
     * property information, to keep query time down.  If you require this data, you can fetch it for the list of genes
     * you want to obtain properties for by calling {@link #getPropertiesForGenes(java.util.List)}.  Genes <b>are</b>
     * prepopulated with design element information, however.
     *
     * @param exptAccession the accession number of the experiment to query for
     * @return the list of all genes in the database for this experiment accession
     */
    public List<Gene> getGenesByExperimentAccession(String exptAccession) {
        // do the first query to fetch genes without design elements
        log.debug("Querying for genes by experiment " + exptAccession);
        List results = template.query(GENES_BY_EXPERIMENT_ACCESSION,
                new Object[]{exptAccession},
                new GeneMapper());
        log.debug("Genes for " + exptAccession + " acquired");

        return (List<Gene>) results;
    }

    public void getPropertiesForGenes(List<Gene> genes) {
        // populate the other info for these genes
        if (genes.size() > 0) {
            fillOutGeneProperties(genes);
        }
    }

    public List<DesignElement> getDesignElementsByGeneID(long geneID) {
        return (List<DesignElement>) template.query(DESIGN_ELEMENTS_BY_GENEID,
                new Object[]{geneID},
                new RowMapper() {
                    public Object mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        return new DesignElement(
                                rs.getString(1), rs.getString(2));
                    }
                });
    }

//    public Map<String, BioEntity> getBioentities(String organism) {
//        Map<String, BioEntity> result = new HashMap<String, BioEntity>(250000);
//
//        BeByIdentifierMapper mapper = new BeByIdentifierMapper(result);
//        template.query(BIOENTITIES_BY_ORAGANISM,
//                new Object[]{organism},
//                mapper);
//        return result;
//    }

    public Map<String, Long> getAllProperties() {
        final Map<String, Long> result = new HashMap<String, Long>();
        template.query(ALL_PROPERTIES, new RowMapper<Object>() {
            public Object mapRow(ResultSet rs, int i) throws SQLException {
                result.put(rs.getString(2), rs.getLong(1));
                return rs.getLong(1);
            }
        });
        return result;
    }

    public long getOrganismIdByName(final String organismName) {
        String query = "merge into a2_organism o\n" +
                "  using (select  1 from dual)\n" +
                "  on (o.name = ?)\n" +
                "  when not matched then \n" +
                "  insert (name) values (?) ";

        template.update(query, new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, organismName);
                ps.setString(2, organismName);

            }
        });

        return template.queryForLong(ORGANISM_ID,
                new Object[]{organismName});

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

        return template.queryForLong(BIOENTITYTYPE_ID,
                new Object[]{typeName});
    }

    public long getSoftwareId(final String name, final String version) {
        String query = "merge into a2_software sw\n" +
                "  using (select 1 from dual)\n" +
                "  on (sw.name = ? and sw.version = ?)\n" +
                "  when not matched then \n" +
                "  insert (name, version) values (?, ?)";
        template.update(query, new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, name);
                ps.setString(2, version);
                ps.setString(3, name);
                ps.setString(4, version);

            }
        });

        return template.queryForLong(SOFTWARE_ID,
                new Object[]{name, version});

    }

    private long getArrayDesignIdByAccession(String arrayDesignAccession) {
        return template.queryForLong(ARRAYDESIGN_ID,
                new Object[]{arrayDesignAccession});
    }

    /////////////////////////////////////////////////////////////////////////////
    //   Write methods
    /////////////////////////////////////////////////////////////////////////////
    public void writeBioentities(final Collection<BioEntity> bioEntities) {
        String query = "merge into a2_bioentity p\n" +
                "  using (select  1 from dual)\n" +
                "  on (p.identifier = ?)\n" +
                "  when not matched then \n" +
                "  insert (identifier, organismid, bioentitytypeid) " +
                "  values (?, ?, ?) ";

        final List<BioEntity> bioEntityList = new ArrayList<BioEntity>(bioEntities);

        if (bioEntityList.size() == 0) {
            return;
        }

//        final long organismId = getOrganismIdByName(bioEntityList.get(0).getOrganism());
//        final long typeId = getBETypeIdByName(bioEntityList.get(0).getType());
        ListStatementSetter<BioEntity> statementSetter = new ListStatementSetter<BioEntity>() {

                long organismId;
                long typeId;

                @Override
                protected void init() {
                    organismId = getOrganismIdByName(bioEntityList.get(0).getOrganism());
                    typeId = getBETypeIdByName(bioEntityList.get(0).getType());
                }

                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, list.get(i).getIdentifier());
                    ps.setString(2, list.get(i).getIdentifier());
                    ps.setLong(3, organismId);
                    ps.setLong(4, typeId);
                }

            };

        int loadedRecordsNumber = writeBatchInChunks(query, bioEntityList, statementSetter);


        System.out.println("loadedRecordsNumber = " + loadedRecordsNumber);
    }

    private <T> int writeBatchInChunks(String query, final List<T> bioEntityList, ListStatementSetter<T> statementSetter) {
        int iterations = bioEntityList.size() % subBatchSize == 0 ? bioEntityList.size() / subBatchSize : (bioEntityList.size() / subBatchSize) + 1;
        int loadedRecordsNumber = 0;
        for (int i = 0; i < iterations; i++) {

            final int maxLength = ((i + 1) * subBatchSize > bioEntityList.size()) ? bioEntityList.size() : (i + 1) * subBatchSize;
            final List<T> subList = bioEntityList.subList(i * subBatchSize, maxLength);

            statementSetter.setList(subList);

            int[] ints = template.batchUpdate(query, statementSetter);


            loadedRecordsNumber += ints.length;
            log.info("Number of raws loaded to the DB = " + loadedRecordsNumber);
        }
        return loadedRecordsNumber;
    }


    public void writeProperties(final Collection<String> properties) {
        final List<String> propList = new ArrayList<String>(properties);
        String query = "merge into a2_bioentityproperty p\n" +
                "  using (select  1 from dual)\n" +
                "  on (p.name = ?)\n" +
                "  when not matched then \n" +
                "  insert (name) values (?)";

        template.batchUpdate(query, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, propList.get(i));
                ps.setString(2, propList.get(i));
            }

            public int getBatchSize() {
                return propList.size();
            }
        });
    }

    public void writePropertyValues(final Collection<BEPropertyValue> propertyValues) {
        String query = "merge into a2_bioentitypropertyvalue pv\n" +
                "  using (select  1 from dual)\n" +
                "  on (pv.value = ? and pv.bioentitypropertyid = ?)\n" +
                "  when not matched then \n" +
                "  insert (value, bioentitypropertyid) values (?, ?)";

        final List<BEPropertyValue> fullPropList = new ArrayList<BEPropertyValue>(propertyValues);
        final Map<String, Long> properties = getAllProperties();


        int iterations = fullPropList.size() % subBatchSize == 0 ? fullPropList.size() / subBatchSize : (fullPropList.size() / subBatchSize) + 1;
        int loadedRecordsNumber = 0;
        for (int i = 0; i < iterations; i++) {

            final int maxLength = ((i + 1) * subBatchSize > fullPropList.size()) ? fullPropList.size() : (i + 1) * subBatchSize;
            final List<BEPropertyValue> propList = fullPropList.subList(i * subBatchSize, maxLength);

            int[] ints = template.batchUpdate(query, new BatchPreparedStatementSetter() {
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, propList.get(i).getValue());
                    ps.setLong(2, properties.get(propList.get(i).getName()));
                    ps.setString(3, propList.get(i).getValue());
                    ps.setLong(4, properties.get(propList.get(i).getName()));
                }

                public int getBatchSize() {
                    return propList.size();
                }
            });

            loadedRecordsNumber += ints.length;
            log.info("Number of raws loaded to the DB = " + loadedRecordsNumber);
        }


    }

    /**
     * @param beProperties - a List of String array, which contains values:
     *                     [0] - BioEntity identifier
     *                     [1] - property name
     *                     [2] - property value
     * @param swName
     * @param swVersion
     */
    public void writeBioEntityToPropertyValues(final Set<List<String>> beProperties, String swName, String swVersion) {
        String query = "insert into a2_bioentitybepv (bioentityid, bepropertyvalueid, softwareid) \n" +
                "  values (\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ?),\n" +
                "  (select pv.bepropertyvalueid from a2_bioentitypropertyvalue pv " +
                "where pv.VALUE = ? " +
                "  and pv.bioentitypropertyid = ?),\n" +
                "  ?)";

        final long softwareId = getSoftwareId(swName, swVersion);
        final Map<String, Long> properties = getAllProperties();

        List<List<String>> propertyValues = new ArrayList<List<String>>(beProperties);
        int iterations = propertyValues.size() % subBatchSize == 0 ? propertyValues.size() / subBatchSize : (propertyValues.size() / subBatchSize) + 1;
        int loadedRecordsNumber = 0;
        for (int i = 0; i < iterations; i++) {

            final int maxLength = ((i + 1) * subBatchSize > propertyValues.size()) ? propertyValues.size() : (i + 1) * subBatchSize;
            final List<List<String>> propList = propertyValues.subList(i * subBatchSize, maxLength);


            int[] ints = template.batchUpdate(query, new BatchPreparedStatementSetter() {
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, propList.get(i).get(0));
                    ps.setString(2, propList.get(i).get(2));
                    ps.setLong(3, properties.get(propList.get(i).get(1)));
                    ps.setLong(4, softwareId);
                }

                public int getBatchSize() {
                    return propList.size();
                }
            });

            loadedRecordsNumber += ints.length;
            log.info("Number of raws loaded to the DB = " + loadedRecordsNumber);
        }

        System.out.println("loaded BioEntityToPropertyValues = " + loadedRecordsNumber);
    }

    /**
     * @param relations - a List of String array, which contains values:
     *                  [0] - gene identifier
     *                  [1] - transcript identifier
     * @param swName
     * @param swVersion
     */
    public void writeGeneToTranscriptRelations(final List<String[]> relations, String swName, String swVersion) {
        String query = "INSERT INTO a2_bioentity2bioentity "
                + "            (bioentityidto, "
                + "             bioentityidfrom, "
                + "             berelationtypeid, "
                + "             softwareid) "
                + "VALUES      ( (SELECT be.bioentityid "
                + "              FROM   a2_bioentity be "
                + "              WHERE  be.identifier = ?), "
                + "             (SELECT be.bioentityid "
                + "              FROM   a2_bioentity be "
                + "              WHERE  be.identifier = ?), "
                + "             ?, "
                + "             ?) ";


        final long softwareId = getSoftwareId(swName, swVersion);

        int iterations = relations.size() % subBatchSize == 0 ? relations.size() / subBatchSize : (relations.size() / subBatchSize) + 1;
        int loadedRecordsNumber = 0;
        for (int i = 0; i < iterations; i++) {

            final int maxLength = ((i + 1) * subBatchSize > relations.size()) ? relations.size() : (i + 1) * subBatchSize;
            final List<String[]> subList = relations.subList(i * subBatchSize, maxLength);

            int[] ints = template.batchUpdate(query, new BatchPreparedStatementSetter() {
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    ps.setString(1, subList.get(i)[0]);
                    ps.setString(2, subList.get(i)[1]);
                    ps.setLong(3, 2);
                    ps.setLong(4, softwareId);
                }

                public int getBatchSize() {
                    return subList.size();
                }
            });
            loadedRecordsNumber += ints.length;
            log.info("Number of raws loaded to the DB = " + loadedRecordsNumber);
        }

        System.out.println("loaded  GeneToTranscriptRelations = " + loadedRecordsNumber);
    }

    public void writeArrayDesign(final ArrayDesign arrayDesign) {
        String query = "merge into a2_arraydesign a\n" +
                "  using (select  1 from dual)\n" +
                "  on (a.accession = ? and a.name = ?)\n" +
                "  when not matched then \n" +
                "  insert (accession, name, type, provider) values (?, ?, ?, ?)";

        template.update(query, new PreparedStatementSetter() {
            public void setValues(PreparedStatement ps) throws SQLException {
                ps.setString(1, arrayDesign.getAccession());
                ps.setString(2, arrayDesign.getName());
                ps.setString(3, arrayDesign.getAccession());
                ps.setString(4, arrayDesign.getName());
                ps.setString(5, arrayDesign.getType());
                ps.setString(6, arrayDesign.getProvider());
            }
        });
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

//        final long adId = getArrayDesignIdByAccession(arrayDesignAccession);

        ListStatementSetter<DesignElement> setter = new ListStatementSetter<DesignElement>() {
            long adId;

            @Override
            protected void init() {
                adId = getArrayDesignIdByAccession(arrayDesignAccession);
            }

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

    public void writeDesignElementBioentityMappings(final Collection<List<String>> deToBeMappings,
                                                    final String swName, final String swVersion,
                                                    final String arrayDesignAccession) {

        String query = "INSERT INTO a2_designeltbioentity \n" +
                " (designelementid, bioentityid, softwareid)\n" +
                " VALUES\n" +
                " ((select de.designelementid from A2_DESIGNELEMENT de where de.accession = ? and de.arraydesignid = ?),\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ?),\n" +
                "  ?)";

        List<List<String>> mappings = new ArrayList<List<String>>(deToBeMappings);

        ListStatementSetter<List<String>> setter = new ListStatementSetter<List<String>>(){
            long swId;
            long adId;

            @Override
            protected void init() {
                swId = getSoftwareId(swName, swVersion);
                adId = getArrayDesignIdByAccession(arrayDesignAccession);
            }

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).get(0));
                ps.setLong(2, adId);
                ps.setString(3, list.get(i).get(1));
                ps.setLong(4, swId);

                System.out.println(list.get(i).get(0) + " : " + list.get(i).get(1));
            }
        };

        writeBatchInChunks(query, mappings, setter);
    }

    public void writeVirtualArrayDesign(DesignElementMappingBundle bundle, String elementType) {
        log.info("Start virtual array design loading procedure");
        SimpleJdbcCall procedure = new SimpleJdbcCall(template)
                .withProcedureName("ATLASBELDR.A2_VIRTUALDESIGNSET")
                .withoutProcedureColumnMetaDataAccess()
                .useInParameterNames("ADaccession")
                .useInParameterNames("ADname")
                .useInParameterNames("Typename")
                .useInParameterNames("adprovider")
                .useInParameterNames("SWname")
                .useInParameterNames("SWversion")
                .useInParameterNames("DEtype")
                .declareParameters(
                        new SqlParameter("ADaccession", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("ADname", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("Typename", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("adprovider", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("SWname", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("SWversion", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("DEtype", Types.VARCHAR));

        MapSqlParameterSource params = new MapSqlParameterSource();
        setParametersFromBundle(params, bundle)
                .addValue("DEtype", elementType);

        procedure.execute(params);
        log.info("DONE");
    }

    public void writeDesignElementMappings(DesignElementMappingBundle bundle) {
        prepareTempTable();
        SimpleJdbcCall procedure;
        log.info("Load design elements mappings into temp table");

        writeBatch(INSERT_INTO_TMP_DESIGNELEMENTMAP_VALUES, bundle.getBatch());

        log.info("Start design elements mapping loading procedure");
        procedure = new SimpleJdbcCall(template)
                .withProcedureName("ATLASBELDR.A2_DESIGNELEMENTMAPPINGSET")
                .withoutProcedureColumnMetaDataAccess()
                .useInParameterNames("ADaccession")
                .useInParameterNames("ADname")
                .useInParameterNames("Typename")
                .useInParameterNames("adprovider")
                .useInParameterNames("SWname")
                .useInParameterNames("SWversion")
                .useInParameterNames("DEtype")
                .declareParameters(
                        new SqlParameter("ADaccession", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("ADname", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("Typename", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("adprovider", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("SWname", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("SWversion", Types.VARCHAR))
                .declareParameters(
                        new SqlParameter("DEtype", Types.VARCHAR));

        MapSqlParameterSource params = new MapSqlParameterSource();
        setParametersFromBundle(params, bundle)
                .addValue("DEtype", bundle.getAdType());

        procedure.execute(params);
        log.info("DONE");
    }

    private void fillOutGeneProperties(List<Gene> genes) {
        // map genes to gene id
        Map<Long, Gene> genesByID = new HashMap<Long, Gene>();
        for (Gene gene : genes) {
            // index this assay
            genesByID.put(gene.getGeneID(), gene);
        }

        // map of genes and their properties
        GenePropertyMapper genePropertyMapper = new GenePropertyMapper(genesByID);

        // query template for genes
        NamedParameterJdbcTemplate namedTemplate = new NamedParameterJdbcTemplate(template);

        // if we have more than 'maxQueryParams' genes, split into smaller queries
        List<Long> geneIDs = new ArrayList<Long>(genesByID.keySet());
        for (ChunkedSublistIterator<List<Long>> i = new ChunkedSublistIterator(geneIDs, maxQueryParams); i.hasNext();) {
            List<Long> geneIDsChunk = i.next();
            // now query for properties that map to one of these genes
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("geneids", geneIDsChunk);
            namedTemplate.query(PROPERTIES_BY_RELATED_GENES, propertyParams, genePropertyMapper);
        }
    }

    private MapSqlParameterSource setParametersFromBundle(MapSqlParameterSource params, DesignElementMappingBundle bundle) {
        return params.addValue("ADaccession", bundle.getAdAccession())
                .addValue("ADname", bundle.getAdName())
                .addValue("Typename", bundle.getAdType())
                .addValue("adprovider", bundle.getAdProvider())
                .addValue("SWname", bundle.getSwName())
                .addValue("SWversion", bundle.getSwVersion());
    }

    private void writeBatch(String insertQuery, List<Object[]> batch) {
        try {
            //ToDO: maybe no need to get connection every time
            Connection singleConn = template.getDataSource().getConnection();
            singleConn.setAutoCommit(true);
            SingleConnectionDataSource singleDs = new SingleConnectionDataSource(singleConn, true);

            SimpleJdbcTemplate simpleJdbcTemplate = new SimpleJdbcTemplate(singleDs);

            int subBatchSize = 5;
            int iterations = batch.size() % subBatchSize == 0 ? batch.size() / subBatchSize : (batch.size() / subBatchSize) + 1;
            int loadedRecordsNumber = 0;
            for (int i = 0; i < iterations; i++) {

                int maxLength = ((i + 1) * subBatchSize > batch.size()) ? batch.size() : (i + 1) * subBatchSize;
                int[] ints = simpleJdbcTemplate.batchUpdate(insertQuery, batch.subList(i * subBatchSize, maxLength));
                loadedRecordsNumber += ints.length;
                log.info("Number of raws loaded to the DB = " + loadedRecordsNumber);
            }

            singleDs.destroy();
            log.info("Number of rows loaded to the DB = " + loadedRecordsNumber);
        } catch (SQLException e) {
            log.error("Cannot get connection to the DB");
            throw new CannotGetJdbcConnectionException("Cannot get connection", e);
        }
    }

    private void prepareTempTable() {
        log.info("Prepare temp table");
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASBELDR.A2_BIOENTITYSETPREPARE").withoutProcedureColumnMetaDataAccess();
        procedure.execute();
    }

    private static class GenePropertyMapper implements RowMapper {
        private Map<Long, Gene> genesByID;

        public GenePropertyMapper(Map<Long, Gene> genesByID) {
            this.genesByID = genesByID;
        }

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            long geneID = resultSet.getLong(1);

            property.setName(resultSet.getString(2).toLowerCase());
            property.setValue(resultSet.getString(3));
            property.setFactorValue(false);

            genesByID.get(geneID).addProperty(property);

            return property;
        }
    }


//    private static class BeByIdentifierMapper implements RowMapper {
//        private Map<String, BioEntity> idBydentifier;
//
//        public BeByIdentifierMapper(Map<String, BioEntity> idBydentifier) {
//            this.idBydentifier = idBydentifier;
//        }
//
//        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
//            BioEntity be = new BioEntity(resultSet.getString(2));
//            be.setId(resultSet.getLong(1));
//            be.setTypeid(resultSet.getLong(3));
//            be.setPropertyString(resultSet.getString(4));
//            idBydentifier.put(resultSet.getString(2), be);
//            return be;
//        }
//    }

//    private static class PropertyMapper implements RowMapper {
//            private Map<String, Long> idBydentifier;
//
//            public PropertyMapper(Map<String, Long> idBydentifier) {
//                this.idBydentifier = idBydentifier;
//            }
//
//            public Object mapRow(ResultSet resultSet, int i) throws SQLException {
//                BioEntity be = new BioEntity(resultSet.getString(2));
//                be.setId(resultSet.getLong(1));
//                be.setTypeid(resultSet.getLong(3));
//                be.setPropertyString(resultSet.getString(4));
//                idBydentifier.put(resultSet.getString(2), be);
//                return be;
//            }
//        }


    private static class GeneMapper implements RowMapper {
        public Gene mapRow(ResultSet resultSet, int i) throws SQLException {
            Gene gene = new Gene();

            gene.setGeneID(resultSet.getLong(1));
            gene.setIdentifier(resultSet.getString(2));
            gene.setSpecies(resultSet.getString(3));

            return gene;
        }
    }


    private static class BEMapper implements RowMapper {
        public BioEntity mapRow(ResultSet resultSet, int i) throws SQLException {
            BioEntity be = new BioEntity(resultSet.getString(2));

            be.setId(resultSet.getLong(1));

            return be;
        }
    }

    private abstract static class ListStatementSetter<T> implements BatchPreparedStatementSetter {

        protected List<T> list;

        public int getBatchSize() {
           return list.size();
        }

        protected abstract void init();

        public void setList(List<T> list) {
            this.list = list;
            init();
        }
    }


}
