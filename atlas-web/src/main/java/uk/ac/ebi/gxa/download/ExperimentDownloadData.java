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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExpressionDataCursor;
import uk.ac.ebi.gxa.data.StatisticsNotFoundException;
import uk.ac.ebi.gxa.download.dsv.DsvDocumentCreateException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.export.dsv.ExperimentDesignTableDsv;
import uk.ac.ebi.gxa.export.dsv.ExperimentExpressionDataTableDsv;
import uk.ac.ebi.gxa.service.experiment.ExperimentAnalytics;
import uk.ac.ebi.gxa.service.experiment.ExperimentDataService;
import uk.ac.ebi.gxa.download.dsv.DsvDocumentCreator;
import uk.ac.ebi.gxa.export.dsv.ExperimentTableDsv;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;
import uk.ac.ebi.gxa.web.controller.ExperimentDesignUI;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * @author Olga Melnichuk
 */
public class ExperimentDownloadData {

    protected final static Logger log = LoggerFactory.getLogger(ExperimentDownloadData.class);

    private final ExperimentDataService expDataService;

    @Autowired
    public ExperimentDownloadData(ExperimentDataService expDataService) {
        this.expDataService = expDataService;
    }

    public Collection<? extends DsvDocumentCreator> newDsvCreatorForAnalytics(final String expAcc) throws RecordNotFoundException {
        Experiment exp  = expDataService.getExperiment(expAcc);
        List<DsvDocumentCreator> creators = new ArrayList<DsvDocumentCreator>();
        for(final ArrayDesign ad: exp.getArrayDesigns()) {
           log.debug("new ExperimentAnalyticsDocCreator(eacc=" + expAcc + ", ad=" + ad.getAccession() + ")");
           creators.add(new ExperimentAnalyticsDocCreator(expAcc, ad.getAccession()));
        }
        return creators;
    }

    public Collection<? extends DsvDocumentCreator> newDsvCreatorForExpressions(String expAcc) throws RecordNotFoundException {
        Experiment exp  = expDataService.getExperiment(expAcc);
        List<DsvDocumentCreator> creators = new ArrayList<DsvDocumentCreator>();
        for(final ArrayDesign ad: exp.getArrayDesigns()) {
            log.debug("new ExperimentExpressionsDocCreator(eacc=" + expAcc + ", ad=" + ad.getAccession() + ")");
            creators.add(new ExperimentExpressionsDocCreator(expAcc, ad.getAccession()));
        }
        return creators;
    }

    public Collection<? extends DsvDocumentCreator> newDsvCreatorForDesign(final String expAcc) {
        log.debug("new ExperimentDesignDocCreator(eacc=" + expAcc + ")");
        return asList(new ExperimentDesignDocCreator(expAcc));
    }

    private ExperimentAnalytics getExperimentAnalytics(String expAcc, String adAcc)
            throws AtlasDataException, RecordNotFoundException, StatisticsNotFoundException {
        return expDataService.getExperimentAnalytics(expAcc, adAcc);
    }

    private ExperimentDesignUI getExperimentDesignUI(String expAcc) throws RecordNotFoundException {
        return expDataService.getExperimentDesignUI(expAcc);
    }


    private ExpressionDataCursor getExperimentExpressionData(String expAcc, String adAcc)
            throws AtlasDataException, ResourceNotFoundException, RecordNotFoundException {
        return expDataService.getExperimentExpressionData(expAcc, adAcc);
    }

    private class ExperimentDesignDocCreator implements DsvDocumentCreator {
        private final String expAcc;

        private ExperimentDesignDocCreator(String expAcc) {
            this.expAcc = expAcc;
        }

        @Override
        public String getName() {
            return "ExpDesign-" + expAcc;
        }

        @Override
        public DsvRowIterator create() throws DsvDocumentCreateException {
            try {
                return ExperimentDesignTableDsv.createDsvDocument(getExperimentDesignUI(expAcc));
            } catch (RecordNotFoundException e) {
                throw documentCreateException(e);
            }
        }

        private DsvDocumentCreateException documentCreateException(Throwable e) {
            return new DsvDocumentCreateException("Can't create experiment design dsv doc: acc = " + expAcc, e);
        }
    }

    private class ExperimentAnalyticsDocCreator implements DsvDocumentCreator {
        private final String expAcc;
        private final String adAcc;

        private ExperimentAnalyticsDocCreator(String expAcc, String adAcc) {
            this.adAcc = adAcc;
            this.expAcc = expAcc;
        }

        @Override
        public String getName() {
            return "ExpAnalysedData-" + expAcc + "-" + adAcc;
        }

        @Override
        public DsvRowIterator create() throws DsvDocumentCreateException {
            try {
                return ExperimentTableDsv.createDsvDocument(getExperimentAnalytics(expAcc, adAcc));
            } catch (AtlasDataException e) {
                throw documentCreateException(e);
            } catch (RecordNotFoundException e) {
                throw documentCreateException(e);
            } catch (StatisticsNotFoundException e) {
                throw documentCreateException(e);
            }
        }

        private DsvDocumentCreateException documentCreateException(Throwable e) {
            return new DsvDocumentCreateException("Can't create experiment analytics dsv doc: acc = " + expAcc + ", ad = " + adAcc, e);
        }
    }

    private class ExperimentExpressionsDocCreator implements DsvDocumentCreator {
        private final String expAcc;
        private final String adAcc;

        private ExperimentExpressionsDocCreator(String expAcc, String adAcc) {
            this.adAcc = adAcc;
            this.expAcc = expAcc;
        }

        @Override
        public String getName() {
            return "ExpRawData-"+ expAcc + "-" + adAcc;
        }

        @Override
        public DsvRowIterator create() throws DsvDocumentCreateException {
            try {
                return ExperimentExpressionDataTableDsv.createDsvDocument(getExperimentExpressionData(expAcc, adAcc));
            } catch (AtlasDataException e) {
                throw documentCreateException(e);
            } catch (ResourceNotFoundException e) {
                throw documentCreateException(e);
            } catch (RecordNotFoundException e) {
                throw documentCreateException(e);
            }
        }

        private DsvDocumentCreateException documentCreateException(Throwable e) {
            return new DsvDocumentCreateException("Can't create experiment expression data dsv doc: acc = " + expAcc + ", ad = " + adAcc, e);
        }
    }

}
