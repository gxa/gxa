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

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Element;
import org.hibernate.SessionFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.orm.hibernate3.SessionFactoryUtils;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
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
public class AtlasDAO {
    private static final Logger log = LoggerFactory.getLogger(AtlasDAO.class);
    public static final String AD_CACHE = AtlasDAO.class.getSimpleName() + ".ad";
    private final ArrayDesignDAO arrayDesignDAO;
    private final BioEntityDAO bioEntityDAO;
    private final JdbcTemplate template;
    private final ExperimentDAO experimentDAO;
    private final AssayDAO assayDAO;
    private final SessionFactory sessionFactory;
    private final OntologyDAO ontologyDAO;
    private PropertyValueDAO propertyValueDAO;
    private OntologyTermDAO ontologyTermDAO;

    public AtlasDAO(ArrayDesignDAO arrayDesignDAO, BioEntityDAO bioEntityDAO, JdbcTemplate template,
                    ExperimentDAO experimentDAO, AssayDAO assayDAO, OntologyDAO ontologyDAO,
                    OntologyTermDAO ontologyTermDAO, PropertyValueDAO propertyValueDAO,
                    SessionFactory sessionFactory) {
        this.arrayDesignDAO = arrayDesignDAO;
        this.bioEntityDAO = bioEntityDAO;
        this.template = template;
        this.experimentDAO = experimentDAO;
        this.assayDAO = assayDAO;
        this.ontologyDAO = ontologyDAO;
        this.ontologyTermDAO = ontologyTermDAO;
        this.propertyValueDAO = propertyValueDAO;
        this.sessionFactory = sessionFactory;

        CacheManager cacheManager = CacheManager.getInstance();
        if (!cacheManager.cacheExists(AD_CACHE))
            cacheManager.addCache(AD_CACHE);
    }

    public List<Experiment> getAllExperiments() {
        return experimentDAO.getAll();
    }

    /**
     * Gets a single experiment from the Atlas Database, queried by the accession of the experiment.
     *
     * @param accession the experiment's accession number (usually in the format E-ABCD-1234)
     * @return an object modelling this experiment
     */
    public Experiment getExperimentByAccession(String accession) throws RecordNotFoundException {
        return experimentDAO.getByName(accession);
    }

    public List<Experiment> getExperimentsByArrayDesignAccession(String accession) {
        return experimentDAO.getExperimentsByArrayDesignAccession(accession);
    }

    public ArrayDesign getArrayDesignByAccession(String accession) {
        // TODO: 4alf: all this can be done with a single @Cache directive.
        Cache cache = CacheManager.getInstance().getCache(AD_CACHE);
        Element element = cache.get(accession);

        ArrayDesign result;
        if (element == null || element.isExpired() || element.getObjectValue() == null) {
            result = arrayDesignDAO.getArrayDesignByAccession(accession);
            cache.put(new Element(accession, result));
        } else {
            result = ArrayDesign.class.cast(element.getObjectValue());
        }
        return result;
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
     * @deprecated Use {@link org.springframework.transaction.annotation.Transactional} instead
     */
    @Deprecated
    public void startSession() {
        log.debug("startSession()");
        SessionFactoryUtils.initDeferredClose(sessionFactory);
    }

    /**
     * @deprecated Use {@link org.springframework.transaction.annotation.Transactional} instead
     */
    @Deprecated
    public void finishSession() {
        log.debug("finishSession()");
        SessionFactoryUtils.processDeferredClose(sessionFactory);
    }

    public PropertyValue getOrCreatePropertyValue(final String name, final String value) {
        return propertyValueDAO.getOrCreatePropertyValue(name, value);
    }

    public Ontology getOrCreateOntology(final String ontologyName,
                                        final String ontologyDescription,
                                        final String ontologySourceUri,
                                        final String ontologyVersion) {
        return ontologyDAO.getOrCreateOntology(ontologyName, ontologySourceUri, ontologyDescription, ontologyVersion);
    }

    public OntologyTerm getOrCreateOntologyTerm(final String accession,
                                                final String term,
                                                final String description,
                                                final Ontology ontology) {
        return ontologyTermDAO.getOrCreateOntologyTerm(accession, term, description, ontology);
    }

    public Ontology getOntologyByName(final String ontologyName) throws RecordNotFoundException {
        return ontologyDAO.getByName(ontologyName);
    }


    public OntologyTerm getOntologyTermByAccession(final String accession) throws RecordNotFoundException {
        return ontologyTermDAO.getByName(accession);
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
