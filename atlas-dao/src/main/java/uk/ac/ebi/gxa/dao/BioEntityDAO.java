package uk.ac.ebi.gxa.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import uk.ac.ebi.gxa.utils.ChunkedSublistIterator;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.*;
import java.util.*;

/**
 * User: nsklyar
 * Date: 03/02/2011
 */
public class BioEntityDAO extends AbstractAtlasDAO{
    // gene queries
    public static final String GENES_SELECT =
            "SELECT DISTINCT be.bioentityid, be.identifier, o.name AS species \n" +
                    "FROM a2_bioentity be \n" +
                    "JOIN a2_organism o ON o.organismid = be.organismid\n" +
                    "JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid\n" +
                    "WHERE bet.id_for_index = 1";

    public static final String BE_SELECT =
            "SELECT DISTINCT be.bioentityid, be.identifier " +
                    "FROM a2_bioentity be  \n" +
                    "JOIN a2_organism o ON o.organismid = be.organismid\n" +
                    "JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid\n" +
                    "WHERE bet.name = ? " +
                    "AND o.name = ?";

    public static final String BE_WITH_PROP_SELECT =
            "SELECT  be.identifier, be.properties FROM test_clob be ";

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

    private static final String INSERT_INTO_TMP_BIOENTITY_VALUES = "INSERT INTO TMP_BIOENTITY VALUES (?, ?, ?)";

    private static final String INSERT_INTO_TEST_CLOB = "INSERT INTO TEST_CLOB VALUES (?, ?)";

    private static final String INSERT_INTO_TMP_DESIGNELEMENTMAP_VALUES = "INSERT INTO TMP_BIOENTITY " +
            "(accession, name) VALUES (?, ?)";

    private int maxQueryParams = 500;

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

    /**
         * Writes bioentities and associated annotations back to the database.
         *
         * @param bundle an object encapsulating the array design data that must be written to the database
     */
    public void writeBioentityBundle1(BioentityBundle bundle) {

//        List<BioEntity> bes = template.query(BE_SELECT, new Object[]{"enstranscript", "homo sapiens"},
//                new BEMapper());
//
//        System.out.println("bes.size() = " + bes.size());

//            writeBatch(INSERT_INTO_TEST_CLOB, bundle.getBatchWithProp());

        System.err.println("bundle.getGeneField() = " + bundle.getGeneField());
        List<BioEntity> bewithprop = template.query("SELECT  be.identifier, be.properties FROM test_clob be where rownum <1000", new RowMapper() {

            public Object mapRow(ResultSet resultSet, int i) throws SQLException {
                DefaultLobHandler lobHandler = new DefaultLobHandler();
                BioEntity be = new BioEntity();
                be.setIdentifier(resultSet.getString(1));
                be.setPropertyString(lobHandler.getClobAsString(resultSet, 2));


                return be;
            }
        });

        System.err.println("bewithprop = " + bewithprop);

//            prepareTempTable();
//
//            log.info("Load bioentities with annotations into temp table");
//
//            writeBatch(INSERT_INTO_TMP_BIOENTITY_VALUES, bundle.getBatch());
//
//            log.info("Start loading procedure");
//            SimpleJdbcCall procedure =
//                    new SimpleJdbcCall(template)
//                            .withProcedureName("ATLASBELDR.A2_BIOENTITYSET")
//                            .withoutProcedureColumnMetaDataAccess()
//                            .useInParameterNames("ORGANISM")
//                            .useInParameterNames("swname")
//                            .useInParameterNames("swversion")
//                            .useInParameterNames("genepropertyname")
//                            .useInParameterNames("transcripttypename")
//                            .declareParameters(
//                                    new SqlParameter("ORGANISM", Types.VARCHAR))
//                            .declareParameters(
//                                    new SqlParameter("swname", Types.VARCHAR))
//                            .declareParameters(
//                                    new SqlParameter("swversion", Types.VARCHAR))
//                            .declareParameters(
//                                    new SqlParameter("genepropertyname", Types.VARCHAR))
//                            .declareParameters(
//                                    new SqlParameter("transcripttypename", Types.VARCHAR));
//            MapSqlParameterSource params = new MapSqlParameterSource();
//            params.addValue("ORGANISM", bundle.getOrganism())
//                    .addValue("swname", bundle.getSource())
//                    .addValue("swversion", bundle.getVersion())
//                    .addValue("genepropertyname", bundle.getGeneField())
//                    .addValue("transcripttypename", bundle.getBioentityField());
//            procedure.execute(params);
//            log.info("DONE");
        }


    /**
     * Writes bioentities and associated annotations back to the database.
     *
     * @param bundle an object encapsulating the array design data that must be written to the database
     */
    public void writeBioentityBundle(BioentityBundle bundle) {
        prepareTempTable();

        log.info("Load bioentities with annotations into temp table");

        writeBatch(INSERT_INTO_TMP_BIOENTITY_VALUES, bundle.getBatch());

        log.info("Start loading procedure");
        SimpleJdbcCall procedure =
                new SimpleJdbcCall(template)
                        .withProcedureName("ATLASBELDR.A2_BIOENTITYSET")
                        .withoutProcedureColumnMetaDataAccess()
                        .useInParameterNames("ORGANISM")
                        .useInParameterNames("swname")
                        .useInParameterNames("swversion")
                        .useInParameterNames("genepropertyname")
                        .useInParameterNames("transcripttypename")
                        .declareParameters(
                                new SqlParameter("ORGANISM", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("swname", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("swversion", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("genepropertyname", Types.VARCHAR))
                        .declareParameters(
                                new SqlParameter("transcripttypename", Types.VARCHAR));
        MapSqlParameterSource params = new MapSqlParameterSource();
        params.addValue("ORGANISM", bundle.getOrganism())
                .addValue("swname", bundle.getSource())
                .addValue("swversion", bundle.getVersion())
                .addValue("genepropertyname", bundle.getGeneField())
                .addValue("transcripttypename", bundle.getBioentityField());
        procedure.execute(params);
        log.info("DONE");
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

            int subBatchSize = 90000;
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


    private static class BEPropertyMapper implements RowMapper {

        public Object mapRow(ResultSet resultSet, int i) throws SQLException {
            BioEntity be = new BioEntity();
            be.setIdentifier(resultSet.getString(1));
            Clob clob = resultSet.getClob(2);
            be.setPropertyString(clob.toString());


            return be;
        }
    }

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
            BioEntity be = new BioEntity();

            be.setId(resultSet.getLong(1));
            be.setIdentifier(resultSet.getString(2));

            return be;
        }
    }

}
