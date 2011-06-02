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

package ae3.model;

import com.google.common.base.Function;
import org.apache.solr.common.SolrDocument;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RestOut;
import uk.ac.ebi.gxa.utils.LazyMap;
import uk.ac.ebi.microarray.atlas.model.AssayProperty;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.annotation.Nonnull;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.google.common.collect.Collections2.transform;
import static com.google.common.collect.Sets.newTreeSet;

/**
 * View class, wrapping Atlas experiment data stored in SOLR document
 */
@RestOut(xmlItemName = "experiment")
public class AtlasExperiment {
    private final Experiment experiment;
    private final SolrDocument exptSolrDocument;

    public static AtlasExperiment createExperiment(ExperimentDAO edao, SolrDocument exptdoc) {
        final Experiment experiment = edao.getById((Long) exptdoc.getFieldValue("id"));
        return new AtlasExperiment(experiment, exptdoc);
    }

    /**
     * Constructor
     *
     * @param experiment DB experiment to use
     * @param exptdoc    SOLR document to wrap
     */
    @SuppressWarnings("unchecked")
    private AtlasExperiment(Experiment experiment, SolrDocument exptdoc) {
        this.experiment = experiment;
        exptSolrDocument = exptdoc;
    }

    public String getTypeString() {
        return getType().toString();
    }

    public Type getType() {
        return Type.getTypeByPlatformName(getPlatform());
    }

    /**
     * Returns map of factor values
     *
     * @return map of factor values
     */
    public Map<String, Collection<String>> getFactorValuesForEF() {
        return new LazyMap<String, Collection<String>>() {
            @Override
            protected Collection<String> map(String s) {
                TreeSet<String> result = newTreeSet();
                for (uk.ac.ebi.microarray.atlas.model.Assay assay : experiment.getAssays()) {
                    result.addAll(transform(assay.getProperties(s), new Function<AssayProperty, String>() {
                        @Override
                        public String apply(@Nonnull AssayProperty input) {
                            return input.getValue();
                        }
                    }));
                }
                return result;
            }

            @Override
            protected Iterator<String> keys() {
                throw LogUtil.createUnexpected("I'm a JSP function, not a map!");
            }
        };
    }

    /**
     * Returns experiment accession
     *
     * @return experiment accession
     */
    @RestOut(name = "accession")
    public String getAccession() {
        return experiment.getAccession();
    }

    /**
     * Returns experiment description
     *
     * @return experiment description
     */
    @RestOut(name = "description")
    public String getDescription() {
        return experiment.getDescription();
    }

    /**
     * Returns PubMed ID
     *
     * @return PubMedID
     */
    @RestOut(name = "pubmedId")
    public String getPubmedId() {
        return experiment.getPubmedId();
    }

    /**
     * Returns set of experiment factors
     *
     * @return all factors from the experiment
     */
    public Set<String> getExperimentFactors() {
        return experiment.getExperimentFactors();
    }

    private String getPlatform() {
        return (String) exptSolrDocument.getFieldValue("platform");
    }

    @RestOut(name = "abstract")
    public String getAbstract() {
        return experiment.getAbstract();
    }

    public Collection<String> getArrayDesigns() {
        return new TreeSet<String>(Arrays.asList(getPlatform().split(",")));
    }

    public Integer getNumSamples() {
        return (Integer) exptSolrDocument.getFieldValue("numSamples");
    }

    @RestOut(name = "archiveUrl")
    public String getArchiveUrl() {
        return "/data/" + this.getAccession() + ".zip";
    }

    private static String dateToString(Date date) {
        return date == null ? null : (new SimpleDateFormat("dd-MM-yyyy").format(date));
    }

    @RestOut(name = "loaddate")
    public String getLoadDateString() {
        return dateToString(experiment.getLoadDate());
    }

    @RestOut(name = "releasedate")
    public String getReleaseDateString() {
        return dateToString(experiment.getReleaseDate());
    }

    /**
     * Not yet implemented, always new
     *
     * @return "new"
     */
    @RestOut(name = "status")
    public String getStatus() {
        return "new";
    }

    public Experiment getExperiment() {
        return experiment;
    }
}

