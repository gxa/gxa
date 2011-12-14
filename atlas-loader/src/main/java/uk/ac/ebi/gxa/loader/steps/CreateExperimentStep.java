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

import com.google.common.base.Strings;
import com.google.common.collect.Multimap;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;
import uk.ac.ebi.microarray.atlas.model.Experiment;

/**
 * Experiment loading step that creates an experiment (an atlas model object)
 * from MAGETABInvestigation information.
 * Based on the original handlers code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */
public class CreateExperimentStep {
    public static String displayName() {
        return "Setting up an experiment data";
    }

    public Experiment readExperiment(MAGETABInvestigation investigation, Multimap<String, String> userData) throws AtlasLoaderException {
        if (investigation.getAccession() == null) {
            throw new AtlasLoaderException(
                    "There is no accession number defined - " +
                            "cannot load to the Atlas without an accession, " +
                            "use Comment[ArrayExpressAccession]"
            );
        }

        Experiment experiment = new Experiment(investigation.getAccession());

        if (userData.containsKey("private"))
            experiment.setPrivate(Boolean.parseBoolean(userData.get("private").iterator().next()));

        experiment.setDescription(investigation.IDF.investigationTitle);

        experiment.setLab(investigation.IDF.personAffiliation.size() > 0 ? investigation.IDF.personAffiliation.get(0) : "");

        String performer = "";
        if (investigation.IDF.personFirstName.size() > 0 && !Strings.isNullOrEmpty(investigation.IDF.personFirstName.get(0))) {
            performer += investigation.IDF.personFirstName.get(0);
        }
        if (investigation.IDF.personMidInitials.size() > 0 && !Strings.isNullOrEmpty(investigation.IDF.personMidInitials.get(0))) {
            if (performer.length() > 0) {
                performer += ' ';
            }
            performer += investigation.IDF.personMidInitials.get(0);
        }
        if (investigation.IDF.personLastName.size() > 0 && !Strings.isNullOrEmpty(investigation.IDF.personLastName.get(0))) {
            if (performer.length() > 0) {
                performer += ' ';
            }
            performer += investigation.IDF.personLastName.get(0);
        }
        experiment.setPerformer(performer);

        if (investigation.IDF.pubMedId != null && investigation.IDF.pubMedId.size() > 0) {
            experiment.setPubmedId(investigation.IDF.pubMedId.get(0));
        }

        return experiment;
    }
}
