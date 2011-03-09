package uk.ac.ebi.gxa.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SingleColumnRowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import uk.ac.ebi.microarray.atlas.model.DesignElement;
import uk.ac.ebi.microarray.atlas.model.Gene;
import uk.ac.ebi.microarray.atlas.model.Property;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.partition;
import static java.util.Collections.nCopies;

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

    private int maxQueryParams = 10;

    private Logger log = LoggerFactory.getLogger(getClass());
    protected JdbcTemplate template;

    public List<Gene> getAllGenesFast() {
        // do the query to fetch genes without design elements
        return template.query(GENES_SELECT, new GeneMapper());
    }


    public List<Gene> getGenesByExperimentAccession(String exptAccession) {
        // do the first query to fetch genes without design elements
        log.debug("Querying for genes by experiment " + exptAccession);
        List<Gene> results = template.query(GENES_BY_EXPERIMENT_ACCESSION,
                new Object[]{exptAccession},
                new GeneMapper());
        log.debug("Genes for " + exptAccession + " acquired");

        return results;
    }

    public void getPropertiesForGenes(List<Gene> genes) {
        // populate the other info for these genes
        if (genes.size() > 0) {
            fillOutGeneProperties(genes);
        }
    }

    public List<DesignElement> getDesignElementsByGeneID(long geneID) {
        return template.query(DESIGN_ELEMENTS_BY_GENEID,
                new Object[]{geneID},
                new RowMapper<DesignElement>() {
                    public DesignElement mapRow(ResultSet rs, int rowNum)
                            throws SQLException {
                        return new DesignElement(
                                rs.getString(3), rs.getString(4));
                    }
                });
    }

    public int getGeneCount() {
        return template.queryForInt(GENES_COUNT);
    }

    public List<String> getSpeciesForExperiment(long experimentId) {
        List<Long> designIds = template.query("select distinct a.arraydesignid from A2_ASSAY a\n" +
                " where a.EXPERIMENTID = ?\n",
                new Object[]{experimentId},
                new SingleColumnRowMapper<Long>());
        return template.query("select distinct o.name from A2_ORGANISM o\n" +
                " join a2_gene g on g.ORGANISMID = o.ORGANISMID\n" +
                " join a2_designelement de on de.geneid = g.geneid\n" +
                " where de.ARRAYDESIGNID in (" + on(",").join(nCopies(designIds.size(), "?")) + ")",
                designIds.toArray(),
                new SingleColumnRowMapper<String>());
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

        // if we have more than 'MAX_QUERY_PARAMS' genes, split into smaller queries
        List<Long> geneIDs = new ArrayList<Long>(genesByID.keySet());
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

    private static class GeneMapper implements RowMapper<Gene> {
        public Gene mapRow(ResultSet resultSet, int i) throws SQLException {
            Gene gene = new Gene();

            gene.setGeneID(resultSet.getLong(1));
            gene.setIdentifier(resultSet.getString(2));
            gene.setName(resultSet.getString(3));
            gene.setSpecies(resultSet.getString(4));

            return gene;
        }
    }

    private static class GenePropertyMapper implements RowMapper<Property> {
        private Map<Long, Gene> genesByID;

        public GenePropertyMapper(Map<Long, Gene> genesByID) {
            this.genesByID = genesByID;
        }

        public Property mapRow(ResultSet resultSet, int i) throws SQLException {
            Property property = new Property();

            long geneID = resultSet.getLong(1);

            property.setName(resultSet.getString(2).toLowerCase());
            property.setValue(resultSet.getString(3));
            property.setFactorValue(false);

            genesByID.get(geneID).addProperty(property);

            return property;
        }
    }

}
