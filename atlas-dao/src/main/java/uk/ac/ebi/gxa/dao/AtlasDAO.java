/*
* Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.dao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import uk.ac.ebi.gxa.impl.ModelImpl;
import uk.ac.ebi.microarray.atlas.model.*;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * A data access object designed for retrieving common sorts of data from the atlas database.  This DAO should be
 * configured with a spring {@link JdbcTemplate} object which will be used to query the database.
 *
 * @author Tony Burdett
 * @author Alexey Filippov
 * @author Nataliya Sklyar
 * @author Misha Kapushesky
 * @author Pavel Kurnosov
 * @author Andrey Zorin
 * @author Robert Petryszak
 * @author Olga Melnichuk
 */
public class AtlasDAO implements ModelImpl.DbAccessor {
    private ArrayDesignDAO arrayDesignDAO;
    private BioEntityDAO bioEntityDAO;
    private JdbcTemplate template;
    private ExperimentDAO experimentDAO;
    private AssayDAO assayDAO;
    private SampleDAO sampleDAO;

    public AtlasDAO(ArrayDesignDAO arrayDesignDAO, BioEntityDAO bioEntityDAO, JdbcTemplate template,
                    ExperimentDAO experimentDAO, AssayDAO assayDAO, SampleDAO sampleDAO) {
        this.arrayDesignDAO = arrayDesignDAO;
        this.bioEntityDAO = bioEntityDAO;
        this.template = template;
        this.experimentDAO = experimentDAO;
        this.assayDAO = assayDAO;
        this.sampleDAO = sampleDAO;
    }

    public List<Experiment> getAllExperiments() {
        return experimentDAO.getAllExperiments();
    }

    /**
     * Gets a single experiment from the Atlas Database, queried by the accession of the experiment.
     *
     * @param accession the experiment's accession number (usually in the format E-ABCD-1234)
     * @return an object modelling this experiment
     */
    public Experiment getExperimentByAccession(String accession) {
        return experimentDAO.getExperimentByAccession(accession);
    }

    public List<Experiment> getExperimentsByArrayDesignAccession(String accession) {
        return experimentDAO.getExperimentsByArrayDesignAccession(accession);
    }

    /**
     * @param experiment the accession of experiment to retrieve assays for
     * @return list of assays
     * @deprecated Use id instead of accession
     *             TODO: 4alf: it would be good to switch to ID here,
     *             TODO: 4alf: but client code is not ready yet:
     *             TODO: 4alf: first, make sure the Experiment is _always_ a proper persistent (sic!) object
     */
    public List<Assay> getAssaysByExperimentAccession(final Experiment experiment) {
        return assayDAO.getByExperiment(experiment);
    }

    /**
     * @param experimentAccession the accession of experiment to retrieve samples for
     * @param assayAccession      the accession of the assay to retrieve samples for
     * @return list of samples
     * @deprecated Use ids instead of accessions
     */
    @Deprecated
    public List<Sample> getSamplesByAssayAccession(String experimentAccession, String assayAccession) {
        return sampleDAO.getSamplesByAssayAccession(experimentAccession, assayAccession);
    }

    public ArrayDesign getArrayDesignByAccession(String accession) {
        return arrayDesignDAO.getArrayDesignByAccession(accession);
    }

    /**
     * @param accession Array design accession
     * @return Array design (with no design element and gene ids filled in) corresponding to accession
     */
    public ArrayDesign getArrayDesignShallowByAccession(String accession) {
        return arrayDesignDAO.getArrayDesignShallowByAccession(accession);
    }

    // TODO: 4alf: experiment-property value to ontology term mapping. Can as well be a part of Experiment
    public List<OntologyMapping> getOntologyMappingsByOntology(
            String ontologyName) {
        return template.query("SELECT DISTINCT accession, property, propertyvalue, ontologyterm, experimentid " +
                "FROM a2_ontologymapping" + " " +
                "WHERE ontologyname=?",
                new Object[]{ontologyName},
                new ExperimentPropertyMapper() {
                    public OntologyMapping mapRow(ResultSet resultSet, int i) throws SQLException {
                        OntologyMapping mapping = super.mapRow(resultSet, i);
                        mapping.setExperimentId(resultSet.getLong(5));
                        return mapping;
                    }
                });
    }

    // TODO: 4alf: a dump for  Francis Atkinson (chEMBL)
    public List<OntologyMapping> getExperimentsToAllProperties() {
        return template.query("SELECT experiment, property, value, ontologyterm from cur_ontologymapping " +
                "UNION " +
                "SELECT distinct ap.experiment, ap.property, ap.value, null " +
                "FROM cur_assayproperty ap where not exists " +
                "(SELECT 1 from cur_ontologymapping cm " +
                "WHERE cm.property = ap.property " +
                "AND cm.value = ap.value " +
                "AND cm.experiment = ap.experiment)",
                new ExperimentPropertyMapper());
    }

    public AtlasStatistics getAtlasStatistics(final String dataRelease, final String lastReleaseDate) {
        // manually count all experiments/genes/assays
        AtlasStatistics stats = new AtlasStatistics();

        stats.setDataRelease(dataRelease);
        stats.setExperimentCount(experimentDAO.getTotalCount());
        stats.setAssayCount(assayDAO.getTotalCount());
        stats.setGeneCount(bioEntityDAO.getGeneCount());
        stats.setNewExperimentCount(experimentDAO.getCountSince(lastReleaseDate));
        stats.setFactorValueCount(template.queryForInt(
                "SELECT COUNT(DISTINCT propertyvalueid) FROM a2_assayPV"
        ));

        return stats;
    }

    /**
     * Writes the given experiment to the database, using the default transaction strategy configured for the
     * datasource.
     *
     * @param experiment the experiment to write
     */
    public void writeExperimentInternal(Experiment experiment) {
        experimentDAO.save(experiment);
    }

    /**
     * Writes the given assay to the database, using the default transaction strategy configured for the datasource.
     *
     * @param assay the assay to write
     */
    public void writeAssay(final Assay assay) {
        assayDAO.save(assay);
    }

    /**
     * Writes the given sample to the database, using the default transaction strategy configured for the datasource.
     *
     * @param sample              the sample to write
     * @param experimentAccession experiment
     */
    public void writeSample(final Sample sample, final String experimentAccession) {
        sampleDAO.save(sample);
    }

    /**
     * Deletes the experiment with the given accession from the database.  If this experiment is not present, this does
     * nothing.
     *
     * @param experimentAccession the accession of the experiment to remove
     */
    public void deleteExperimentFromDatabase(final String experimentAccession) {
        experimentDAO.delete(experimentAccession);
    }


    private static class ExperimentPropertyMapper implements RowMapper<OntologyMapping> {
        public OntologyMapping mapRow(ResultSet resultSet, int i) throws SQLException {
            OntologyMapping mapping = new OntologyMapping();
            mapping.setExperimentAccession(resultSet.getString(1));
            mapping.setProperty(resultSet.getString(2));
            mapping.setPropertyValue(resultSet.getString(3));
            mapping.setOntologyTerm(resultSet.getString(4));
            return mapping;
        }
    }
}
