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

package uk.ac.ebi.gxa.annotator.loader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.biomart.*;
import uk.ac.ebi.gxa.annotator.loader.data.*;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;

import java.io.IOException;
import java.net.URISyntaxException;

import static com.google.common.io.Closeables.closeQuietly;
import static java.lang.System.currentTimeMillis;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class BioMartAnnotator extends Annotator {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private final AnnotationSourceDAO annSrcDAO;

    private final BioEntityPropertyDAO propertyDAO;

    private final BioMartAnnotationSource annSrc;

    public BioMartAnnotator(BioMartAnnotationSource annSrc, AnnotationSourceDAO annSrcDAO, BioEntityPropertyDAO propertyDAO, AtlasBioEntityDataWriter beDataWriter) {
        super(beDataWriter);
        this.annSrcDAO = annSrcDAO;
        this.propertyDAO = propertyDAO;
        this.annSrc = annSrc;
    }

    @Override
    public void updateAnnotations() {
        BioMartAnnotationLoader annotLoader = null;

        try {
            String organismName = annSrc.getOrganism().getName();
            reportProgress("Loading Ensembl annotations for organism " + organismName);
            annotLoader = new BioMartAnnotationLoader(annSrc);

            reportProgress("Loading bio-entities for " + organismName);
            annotLoader.loadBioEntities();

            reportProgress("Loading synonyms for " + organismName);
            annotLoader.loadSynonyms(propertyDAO.findOrCreate("synonym"));

            for (ExternalBioEntityProperty externalProperty : annSrc.getExternalBioEntityProperties()) {
                reportProgress("Loading property " + externalProperty.getBioEntityProperty().getName() + " ("
                        + externalProperty.getName() + ") for " + organismName);
                log.debug("Parsing property {} ", externalProperty.getBioEntityProperty().getName());

                long startTime = currentTimeMillis();
                annotLoader.loadPropertyValues(externalProperty);
                log.debug("Done. {} ms).\n", (currentTimeMillis() - startTime));
            }

            writeBioEntities(annotLoader.getBioEntityData());
            writePropertyValues(annotLoader.getPropertyValuesData(), annSrc, false);

            reportSuccess("Update annotations for Organism " + organismName + " completed");

        } catch (IOException e) {
            reportError(e);
        } catch (BioMartException e) {
            reportError(e);
        } catch (URISyntaxException e) {
            reportError(e);
        } catch (InvalidAnnotationDataException e) {
            reportError(e);
        } catch (InvalidCSVColumnException e) {
            reportError(e);
        } finally {
            closeQuietly(annotLoader);
        }
    }

    @Override
    public void updateMappings() {
        BioMartAnnotationLoader annotLoader = null;
        try {
            String organismName = annSrc.getOrganism().getName();
            reportProgress("Loading Ensembl design element mappings for organism " + organismName);
            annotLoader = new BioMartAnnotationLoader(annSrc);

            if (!annSrc.isApplied()) {
                reportProgress("Loading bioentities for " + organismName);
                annotLoader.loadBioEntities();
                writeBioEntities(annotLoader.getBioEntityData());
            }

            for (ExternalArrayDesign externalArrayDesign : annSrc.getExternalArrayDesigns()) {
                reportProgress("Loading design elements for " + externalArrayDesign.getArrayDesign().getAccession() +
                        " (" + externalArrayDesign.getName() + ") for " + organismName);

                long startTime = currentTimeMillis();
                annotLoader.loadDesignElementMappings(externalArrayDesign);
                log.debug("Done. {} ms).\n", (currentTimeMillis() - startTime));

                writeDesignElements(annotLoader.getDeMappingsData(),
                        externalArrayDesign.getArrayDesign(),
                        annSrc.getSoftware(),
                        annSrcDAO.isAnnSrcAppliedForArrayDesignMapping(annSrc, externalArrayDesign.getArrayDesign()));
            }

            reportSuccess("Update mappings for Organism " + organismName + " completed");
        } catch (IOException e) {
            reportError(e);
        } catch (BioMartException e) {
            reportError(e);
        } catch (URISyntaxException e) {
            reportError(e);
        } catch (InvalidAnnotationDataException e) {
            reportError(e);
        } catch (InvalidCSVColumnException e) {
            reportError(e);
        } finally {
            closeQuietly(annotLoader);
        }
    }
}
