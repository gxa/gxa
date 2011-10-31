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

package uk.ac.ebi.gxa.dao.bioentity;

import com.google.common.collect.ArrayListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.gxa.dao.SoftwareDAO;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Organism;
import uk.ac.ebi.microarray.atlas.model.bioentity.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import static com.google.common.collect.Iterables.partition;

/**
 * @author Nataliya Sklyar
 */
public class BioEntityDAO {

    public static final int MAX_QUERY_PARAMS = 15;
    public static final int SUB_BATCH_SIZE = 70;

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

        String query = "SELECT distinct " + GeneDesignElementMapper.FIELDS + "\n" +
                "  FROM a2_designelement de\n" +
                "  join a2_arraydesign ad on ad.arraydesignid = de.arraydesignid\n" +
                "  join a2_designeltbioentity debe on debe.designelementid = de.designelementid \n" +
                "  JOIN a2_bioentity be ON be.bioentityid = debe.bioentityid\n" +
                "  join a2_software sw on sw.softwareid = debe.softwareid\n" +
                "  JOIN a2_bioentitytype bet ON bet.bioentitytypeid = be.bioentitytypeid \n" +
                "  WHERE bet.ID_FOR_INDEX = 1\n" +
                "  and sw.isactive = 'T'";

        template.query(query,
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


    /////////////////////////////////////////////////////////////////////////////
    //   Write methods
    /////////////////////////////////////////////////////////////////////////////
    public void writeBioEntities(final Collection<BioEntity> bioEntities) {
        String query = "merge into a2_bioentity p\n" +
                "  using (select  1 from dual)\n" +
                "  on (p.identifier = ? and p.bioentitytypeid = ?)\n" +
                "  when not matched then \n" +
                "  insert (identifier, name, organismid, bioentitytypeid)   \n" +
                "  values (?, ?, ?, ?) ";

        ListStatementSetter<BioEntity> statementSetter = new ListStatementSetter<BioEntity>() {

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).getIdentifier());
                ps.setLong(2, list.get(i).getType().getId());
                ps.setString(3, list.get(i).getIdentifier());
                ps.setString(4, list.get(i).getName());
                ps.setLong(5, list.get(i).getOrganism().getId());
                ps.setLong(6, list.get(i).getType().getId());
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
                ps.setLong(2, list.get(i).getProperty().getBioEntitypropertyId());
                ps.setString(3, list.get(i).getValue());
                ps.setLong(4, list.get(i).getProperty().getBioEntitypropertyId());
            }
        };

        int loadedRecords = writeBatchInChunks(query, propertyValues, statementSetter);
        log.info("PropertieValues merged : " + loadedRecords);

    }

    /**
     * @param beProperties - a Collection of Pair, which contains values:
     *                     [0] - BioEntity identifier
     *                     [1] - BEPropertyValue
     * @param beType
     * @param software
     */
    public void writeBioEntityToPropertyValues(final Collection<Pair<String, BEPropertyValue>> beProperties, final BioEntityType beType,
                                               final Software software) {

        String query = "insert into a2_bioentitybepv (bioentityid, bepropertyvalueid, softwareid) \n" +
                "  values (\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ? and be.bioentitytypeid = ?),\n" +
                "  (select pv.bepropertyvalueid from a2_bioentitypropertyvalue pv " +
                "where pv.VALUE = ? " +
                "  and pv.bioentitypropertyid = ?),\n" +
                "  ?)";


        ListStatementSetter<Pair<String, BEPropertyValue>> statementSetter = new ListStatementSetter<Pair<String, BEPropertyValue>>() {
            long softwareId = software.getSoftwareid();

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).getFirst());
                ps.setLong(2, beType.getId());
                ps.setString(3, list.get(i).getSecond().getValue());
                ps.setLong(4, list.get(i).getSecond().getProperty().getBioEntitypropertyId());
                ps.setLong(5, softwareId);
            }

        };

        writeBatchInChunks(query, beProperties, statementSetter);
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

    public void writeDesignElementBioEntityMappings(final Collection<Pair<String, String>> deToBeMappings, final BioEntityType beType,
                                                    final Software software,
                                                    final ArrayDesign arrayDesign) {

        String query = "INSERT INTO a2_designeltbioentity \n" +
                " (designelementid, bioentityid, softwareid)\n" +
                " VALUES\n" +
                " ((select de.designelementid from A2_DESIGNELEMENT de where de.accession = ? and de.arraydesignid = ?),\n" +
                "  (select be.bioentityid from a2_bioentity be where be.identifier = ? and be.bioentitytypeid = ?),\n" +
                "  ?)";

        ListStatementSetter<Pair<String, String>> setter = new ListStatementSetter<Pair<String, String>>() {
            long swId = software.getSoftwareid();
            long adId = arrayDesign.getArrayDesignID();
            long typeId = beType.getId();

            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setString(1, list.get(i).getFirst());
                ps.setLong(2, adId);
                ps.setString(3, list.get(i).getSecond());
                ps.setLong(4, typeId);
                ps.setLong(5, swId);
            }
        };

        writeBatchInChunks(query, deToBeMappings, setter);
    }

    public int deleteDesignElementBioEntityMappings(final Software software, final ArrayDesign arrayDesign) {
        String query = "DELETE FROM A2_DESIGNELTBIOENTITY DEBE\n" +
                "WHERE DEBE.SOFTWAREID= ? \n" +
                "and debe.DESIGNELEMENTID IN (SELECT DE.DESIGNELEMENTID FROM A2_DESIGNELEMENT DE WHERE DE.ARRAYDESIGNID=?)";

       return template.update(query, software.getSoftwareid(), arrayDesign.getArrayDesignID());
    }

    public int deleteBioEntityToPropertyValues(final Organism organism, final Software software) {
        String query = "DELETE FROM A2_BIOENTITYBEPV BEPV\n" +
                " WHERE BEPV.SOFTWAREID = ?\n" +
                " and bepv.bioentityid in (select bioentityid from A2_BIOENTITY where organismid=?)";

        return template.update(query, software.getSoftwareid(), organism.getId());
    }

    private <T> int writeBatchInChunks(String query,
                                       final Collection<T> entityList,
                                       ListStatementSetter<T> statementSetter) throws DataAccessException {
        int loadedRecordsNumber = 0;

        for (List<T> subList : partition(entityList, SUB_BATCH_SIZE)) {
            statementSetter.setList(subList);
            int[] rowsAffectedArray = template.batchUpdate(query, statementSetter);
            loadedRecordsNumber += rowsAffectedArray.length;
            if (loadedRecordsNumber % (SUB_BATCH_SIZE * 100) == 0) { // report every 100 batches
                log.info("Number of rows loaded to the DB = " + loadedRecordsNumber);
            }
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

            namedTemplate.query("select " + GenePropertyMapper.FIELDS + "\n" +
                    "  from a2_bioentitybepv bebepv \n" +
                    "  join a2_bioentitypropertyvalue bepv on bepv.bepropertyvalueid = bebepv.bepropertyvalueid\n" +
                    "  join a2_bioentityproperty bep on bep.bioentitypropertyid = bepv.bioentitypropertyid\n" +
                    "  where bebepv.softwareid in (:swid)  " +
                    "  and bebepv.bioentityid in (:geneids)", propertyParams, genePropertyMapper);
        }
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
            Organism organism = new Organism(resultSet.getLong(5), intern(resultSet.getString(4)));
            BioEntity gene = new BioEntity(resultSet.getString(2), resultSet.getString(3), type, organism);

            gene.setId(resultSet.getLong(1));

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
