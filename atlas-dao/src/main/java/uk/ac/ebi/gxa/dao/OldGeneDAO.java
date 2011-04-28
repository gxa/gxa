package uk.ac.ebi.gxa.dao;

import com.google.common.collect.ArrayListMultimap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.BioEntity;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.collect.Iterables.partition;

public class OldGeneDAO implements BioEntityDAOInterface {

    public static final String GENES_SELECT =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_organism s " +
                    "WHERE g.organismid=s.organismid";


    public static final String GENES_BY_EXPERIMENT_ACCESSION =
            "SELECT DISTINCT g.geneid, g.identifier, g.name, s.name AS species " +
                    "FROM a2_gene g, a2_organism s, a2_designelement d, a2_assay a, " +
                    "a2_experiment e " +
                    "WHERE g.geneid=d.geneid " +
                    "AND g.organismid = s.organismid " +
                    "AND d.arraydesignid=a.arraydesignid " +
                    "AND a.experimentid=e.experimentid " +
                    "AND e.accession=?";

    public static final String DESIGN_ELEMENTS_BY_GENEID =
            "SELECT de.designelementid, de.arraydesignid, de.accession, de.name " +
                    "FROM a2_designelement de " +
                    "WHERE de.geneid=?";

    public static final String PROPERTIES_BY_RELATED_GENES =
            "SELECT ggpv.geneid, gp.name AS property, gpv.value AS propertyvalue FROM a2_genepropertyvalue gpv\n" +
                    " JOIN a2_geneproperty gp ON gpv.genepropertyid=gp.genepropertyid\n" +
                    " JOIN a2_genegpv ggpv ON ggpv.genepropertyvalueid = gpv.genepropertyvalueid\n" +
                    "WHERE ggpv.geneid in (:geneids)";

    public static final String GENES_COUNT =
            "SELECT COUNT(*) FROM a2_gene";

    private Logger log = LoggerFactory.getLogger(getClass());
    protected JdbcTemplate template;

    public List<BioEntity> getAllGenesFast() {
        // do the query to fetch genes without design elements
        return template.query(GENES_SELECT, new GeneMapper());
    }


    public void getPropertiesForGenes(List<BioEntity> bioEntities) {
        // populate the other info for these genes
        if (bioEntities.size() > 0) {
            fillOutGeneProperties(bioEntities);
        }
    }

    public int getGeneCount() {
        return template.queryForInt(GENES_COUNT);
    }

    public List<String> getSpeciesForExperiment(long experimentId) {
        return template.query("select distinct o.name from a2_organism o\n" +
                "  join a2_sample s on s.ORGANISMID = o.ORGANISMID\n" +
                "  join A2_ASSAYSAMPLE ass on ass.SAMPLEID = s.SAMPLEID\n" +
                "  join A2_ASSAY a on a.ASSAYID = ass.ASSAYID\n" +
                "where a.EXPERIMENTID = ?",
                new Object[]{experimentId},
                new SingleColumnRowMapper<String>());
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

        // if we have more than 'MAX_QUERY_PARAMS' genes, split into smaller queries
        List<Long> geneIDs = new ArrayList<Long>(genesByID.keySet());
        int maxQueryParams = 10;
        for (List<Long> geneIDsChunk : partition(geneIDs, maxQueryParams)) {
            // now query for properties that map to one of these genes
            MapSqlParameterSource propertyParams = new MapSqlParameterSource();
            propertyParams.addValue("geneids", geneIDsChunk);
            namedTemplate.query(PROPERTIES_BY_RELATED_GENES, propertyParams, genePropertyMapper);
        }
    }

    public void setJdbcTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public ArrayListMultimap<Long, DesignElement> getAllDesignElementsForGene() {
        throw new UnsupportedOperationException("this method is not implemented!");
    }

    private static class GeneMapper implements RowMapper<BioEntity> {
        public BioEntity mapRow(ResultSet resultSet, int i) throws SQLException {
            BioEntity gene = new BioEntity(resultSet.getString(2));

            gene.setId(resultSet.getLong(1));
            gene.setName(resultSet.getString(3));
            gene.setSpecies(resultSet.getString(4));

            return gene;
        }
    }

    private static class GenePropertyMapper implements RowMapper<Property> {
        private Map<Long, BioEntity> genesByID;

        public GenePropertyMapper(Map<Long, BioEntity> genesByID) {
            this.genesByID = genesByID;
        }

        public Property mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            long geneID = resultSet.getLong(1);

            property.setName(resultSet.getString(2).toLowerCase());
            property.setValue(resultSet.getString(3));

            genesByID.get(geneID).addProperty(property);

            return property;
        }
    }

}
