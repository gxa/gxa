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

package uk.ac.ebi.gxa.loader.steps;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCache;
import uk.ac.ebi.gxa.loader.cache.AtlasLoadCacheRegistry;
import uk.ac.ebi.gxa.Experiment;
import uk.ac.ebi.gxa.Model;

/**
 * Experiment loading step that creates an experiment (an atlas model object)
 * from MAGETABInvestigation information.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */
public class CreateExperimentStep implements Step {
    private final Model atlasModel;
    private final MAGETABInvestigation investigation;
    private final Multimap<String, String> userData;

    public CreateExperimentStep(Model atlasModel, MAGETABInvestigation investigation) {
        this(atlasModel, investigation, HashMultimap.<String, String>create());
    }

    public CreateExperimentStep(Model atlasModel, MAGETABInvestigation investigation, Multimap<String, String> userData) {
        this.atlasModel = atlasModel;
        this.investigation = investigation;
        this.userData = userData;
    }

    public String displayName() {
        return "Setting up an experiment data";
    }

    public void run() throws AtlasLoaderException {
        if (investigation.accession == null) {
            throw new AtlasLoaderException(
                    "There is no accession number defined - " +
                            "cannot load to the Atlas without an accession, " +
                            "use Comment[ArrayExpressAccession]"
            );
        }

        Experiment experiment = atlasModel.createExperiment(investigation.accession);

        if (userData.containsKey("private"))
            experiment.setPrivate(Boolean.parseBoolean(userData.get("private").iterator().next()));
        if (userData.containsKey("curated"))
            experiment.setCurated(Boolean.parseBoolean(userData.get("curated").iterator().next()));

        experiment.setDescription(investigation.IDF.investigationTitle);

        experiment.setLab(investigation.IDF.personAffiliation.size() > 0 ? investigation.IDF.personAffiliation.get(0) : "");

        String performer = "";
        if (investigation.IDF.personFirstName.size() > 0) {
            performer += investigation.IDF.personFirstName.get(0);
        }
        if (investigation.IDF.personMidInitials.size() > 0) {
            if (performer.length() > 0) {
                performer += ' ';
            }
            performer += investigation.IDF.personMidInitials.get(0);
        }
        if (investigation.IDF.personLastName.size() > 0) {
            if (performer.length() > 0) {
                performer += ' ';
            }
            performer += investigation.IDF.personLastName.get(0);
        }
        experiment.setPerformer(performer);

        if (investigation.IDF.pubMedId != null && investigation.IDF.pubMedId.size() > 0) {
            experiment.setPubmedIdString(investigation.IDF.pubMedId.get(0));
        }

        // add the experiment to the cache
        AtlasLoadCache cache = AtlasLoadCacheRegistry.getRegistry().retrieveAtlasLoadCache(investigation);
        cache.setExperiment(experiment);
    }
}
