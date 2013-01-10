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

package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.data.AtlasDataException;
import uk.ac.ebi.gxa.data.ExperimentWithData;
import uk.ac.ebi.gxa.download.ExperimentDownloadData;
import uk.ac.ebi.gxa.download.dsv.DsvDocumentCreateException;
import uk.ac.ebi.gxa.download.dsv.DsvDocumentCreator;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.export.dsv.ExperimentExpressionDataTableDsv;
import uk.ac.ebi.gxa.service.DownloadDataService;
import uk.ac.ebi.gxa.service.experiment.ExperimentDataService;
import uk.ac.ebi.gxa.utils.dsv.DsvDocumentWriter;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;
import uk.ac.ebi.gxa.web.filter.ResourceWatchdogFilter;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.servlet.http.HttpServletResponse;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.google.common.base.Joiner.on;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.data.ExperimentPart.find;
import static uk.ac.ebi.gxa.utils.dsv.DsvFormat.tsv;

/**
 * Enabling programmatic access to experiment data downloads. Downloads are streamed to the client.
 */
@Controller
public class DirectDownloadDataViewController extends AtlasViewController {

    /**
     * Local copy from ExperimentDownloadData
     */
    private class ExperimentExpressionsDocCreator implements DsvDocumentCreator {

        private final String expAcc;
        private final String adAcc;

        private ExperimentExpressionsDocCreator(String expAcc, String adAcc) {
            this.adAcc = adAcc;
            this.expAcc = expAcc;
        }

        @Override
        public String getName() {
            return "ExpRawData-" + expAcc + "-" + adAcc;
        }

        @Override
        public DsvRowIterator create() throws DsvDocumentCreateException {
            try {
                // ExperimentWithData needs to be closed eventually
                // otherwise the Netcdf FileCache will leak
                final ExperimentWithData experimentWithData = expDataService.getExperimentWithData(expAcc);
                ResourceWatchdogFilter.register(new Closeable() {
                    public void close() {
                        closeQuietly(experimentWithData);
                    }
                });
                return ExperimentExpressionDataTableDsv.createDsvDocument(find(experimentWithData, adAcc).getExpressionData());
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

    protected final static Logger log = LoggerFactory.getLogger(DirectDownloadDataViewController.class);

    private ExperimentDownloadData experimentDownloadData;

    private ExperimentDataService expDataService;

    @Autowired
    public DirectDownloadDataViewController(ExperimentDownloadData experimentDownloadData, ExperimentDataService expDataService) {
        this.experimentDownloadData = experimentDownloadData;
        this.expDataService = expDataService;
    }

    private String newToken(String... parts) {
        //TODO proper token generation needed
        return on("").join(parts);
    }

    /**
     * Writes a ZipOutputStream to a HttpServletResponse using DsvDocumentCreators
     */
    private void createZipOutput(
            Collection<? extends DsvDocumentCreator> creators,
            HttpServletResponse response, String token)
            throws IOException, DsvDocumentCreateException {

        // set content type and returned filename here
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + token + ".zip\"");
        response.setHeader("Pragma", "public"); // so it works in IE6&7

        ZipOutputStream zout = null;
        boolean emptyZip = true;
        try {
            zout = new ZipOutputStream(response.getOutputStream());

            for (DsvDocumentCreator docCreator : creators) {
                DsvRowIterator doc = docCreator.create();
                zout.putNextEntry(new ZipEntry(docCreator.getName() + ".tab"));

                (new DsvDocumentWriter(tsv().newWriter(new OutputStreamWriter(zout)), null)).write(doc);
            }
            zout.closeEntry();
            emptyZip = false;
        } finally {
            if (!emptyZip)
                closeQuietly(zout);
        }
    }

    @RequestMapping(value = "/directDownload/" + DownloadDataService.EXPERIMENT_ANALYTICS, method = RequestMethod.GET)
    public void directDownloadExperimentAnalytics(
            @RequestParam("eacc") String expAcc, HttpServletResponse response)
            throws IOException, DsvDocumentCreateException, RecordNotFoundException {

        Collection<? extends DsvDocumentCreator> creators = experimentDownloadData.newDsvCreatorForAnalytics(expAcc);

        String token = newToken(DownloadDataService.EXPERIMENT_ANALYTICS, "_", expAcc, "_", UUID.randomUUID().toString());
        log.info("directDownloadExperimentAnalytics(token={})", token);

        createZipOutput(creators, response, token);
    }

    @RequestMapping(value = "/directDownload/" + DownloadDataService.EXPERIMENT_EXPRESSIONS, method = RequestMethod.GET)
    public void directDownloadExperimentExpressions(
            @RequestParam("eacc") String expAcc, HttpServletResponse response)
            throws IOException, DsvDocumentCreateException, RecordNotFoundException {

        // copied from ExperimentDownloadData to allow for using custom ExperimentExpressionDocCreator
        Experiment exp = expDataService.getExperiment(expAcc);
        List<DsvDocumentCreator> creators = new ArrayList<DsvDocumentCreator>();
        for (final ArrayDesign ad : exp.getArrayDesigns()) {
            log.debug("new ExperimentExpressionsDocCreator(eacc=" + expAcc + ", ad=" + ad.getAccession() + ")");
            creators.add(new ExperimentExpressionsDocCreator(expAcc, ad.getAccession()));
        }

        String token = newToken(DownloadDataService.EXPERIMENT_EXPRESSIONS, "_", expAcc, "_", UUID.randomUUID().toString());
        log.info("directDownloadExperimentExpressions(token={})", token);

        createZipOutput(creators, response, token);
    }

    @RequestMapping(value = "/directDownload/" + DownloadDataService.EXPERIMENT_DESIGN, method = RequestMethod.GET)
    public void directDownloadExperimentDesign(
            @RequestParam("eacc") String expAcc, HttpServletResponse response)
            throws IOException, DsvDocumentCreateException, RecordNotFoundException {

        Collection<? extends DsvDocumentCreator> creators = experimentDownloadData.newDsvCreatorForDesign(expAcc);

        String token = newToken(DownloadDataService.EXPERIMENT_DESIGN, "_", expAcc, "_", UUID.randomUUID().toString());
        log.info("directDownloadExperimentDesign(token={})", token);

        createZipOutput(creators, response, token);
    }

}