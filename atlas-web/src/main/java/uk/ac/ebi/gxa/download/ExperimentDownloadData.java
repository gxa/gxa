/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.download;

import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.service.experiment.ExperimentAnalytics;
import uk.ac.ebi.gxa.service.experiment.ExperimentDataService;
import uk.ac.ebi.gxa.spring.view.dsv.DsvDocument;
import uk.ac.ebi.gxa.download.dsv.DsvDocumentCreator;
import uk.ac.ebi.gxa.export.dsv.ExperimentTableDsv;

/**
 * @author Olga Melnichuk
 */
public class ExperimentDownloadData {

    private final ExperimentDataService expDataService;

    @Autowired
    public ExperimentDownloadData(ExperimentDataService expDataService) {
        this.expDataService = expDataService;
    }

    public DsvDocumentCreator expAnalyticsAsDsv(final String expAcc, final String adAcc) {
        return new DsvDocumentCreator(){
            @Override
            public DsvDocument create() throws Exception{
                return ExperimentTableDsv.createDsvDocument(getExperimentAnalytics(expAcc, adAcc).asRows());
            }
        };
    }

    public DsvDocumentCreator expExpressionsAsDsv(String expAcc) {
        //TODO
        return null;
    }

    public DsvDocumentCreator expDesignAsDsv(String expAcc) {
        //TODO
        return null;
    }


    private ExperimentAnalytics getExperimentAnalytics(String expAccession, String adAccession)
            throws AtlasDataException, RecordNotFoundException, StatisticsNotFoundException {
        return expDataService.getExperimentAnalytics(expAccession, adAccession);
    }

}
