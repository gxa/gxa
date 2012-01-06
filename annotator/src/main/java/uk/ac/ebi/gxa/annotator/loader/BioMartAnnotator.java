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
import uk.ac.ebi.gxa.annotator.AnnotationException;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.data.*;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.gxa.annotator.model.connection.AnnotationSourceAccessException;
import uk.ac.ebi.gxa.annotator.model.connection.BioMartConnection;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.utils.Pair;
import uk.ac.ebi.microarray.atlas.model.bioentity.BEPropertyValue;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityType;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static java.lang.System.currentTimeMillis;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * nsklyar
 * Date: 11/04/2011
 */
public class BioMartAnnotator extends Annotator<BioMartAnnotationSource> {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private AnnotationSourceDAO annSrcDAO;

    private BioEntityPropertyDAO propertyDAO;

    public BioMartAnnotator(BioMartAnnotationSource annSrc, AnnotationSourceDAO annSrcDAO, BioEntityPropertyDAO propertyDAO, AtlasBioEntityDataWriter beDataWriter) {
        super(annSrc, beDataWriter);
        this.annSrcDAO = annSrcDAO;
        this.propertyDAO = propertyDAO;
    }

    @Override
    public void updateAnnotations() {

        try {
            reportProgress("Reading Ensembl annotations for organism " + annSrc.getOrganism().getName());

            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeExternalAttributesHandler attributesHandler = new BETypeExternalAttributesHandler(annSrc);
            BioEntityAnnotationDataBuilder builder = new BioEntityAnnotationDataBuilder();
            AnnotationParser<BioEntityAnnotationData> parser = AnnotationParser.initParser(attributesHandler.getTypes(), builder);

            BioMartConnection martConnection = annSrc.createConnection();

            //Read BioEntities
            readBioEntities(martConnection.getAttributesURL(attributesHandler.getExternalBEIdentifiers()), parser);

            //read synonyms
            fetchSynonyms(annSrc, builder);

            //read properties
            for (ExternalBioEntityProperty entityPropertyExternal : annSrc.getExternalBioEntityProperties()) {
                //List of Attributes contains for example: {"ensembl_gene_id", "ensembl_transcript_id", "external_gene_id"}
                List<String> attributes = new ArrayList<String>(attributesHandler.getExternalBEIdentifiers());
                attributes.add(entityPropertyExternal.getName());

                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading property " + entityPropertyExternal.getBioEntityProperty().getName() + " ("
                            + entityPropertyExternal.getName() + ") for " + annSrc.getOrganism().getName());
                    log.debug("Parsing property {} ", entityPropertyExternal.getBioEntityProperty().getName());
                    long startTime = currentTimeMillis();

                    parser.parsePropertyValues(entityPropertyExternal.getBioEntityProperty(), url);

                    log.debug("Done. {} millseconds).\n", (currentTimeMillis() - startTime));
                }
            }

            final BioEntityAnnotationData data = parser.getData();

            beDataWriter.writeBioEntities(data, listener);
            beDataWriter.writePropertyValues(data.getPropertyValues(), listener);
            beDataWriter.writeBioEntityToPropertyValues(data, annSrc, false, listener);

            reportSuccess("Update annotations for Organism " + annSrc.getOrganism().getName() + " completed");

        } catch (AnnotationSourceAccessException e) {
            reportError(new AnnotationException("Cannot update annotations for Organism " + annSrc.getDatasetName(), e));
        } catch (AnnotationException e) {
            reportError(e);
        }
    }

    @Override
    public void updateMappings() {
        try {
            reportProgress("Reading Ensembl design element mappings for organism " + annSrc.getOrganism().getName());

            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeExternalAttributesHandler attributesHandler = new BETypeExternalAttributesHandler(annSrc);
            AnnotationParser<DesignElementMappingData> parser = AnnotationParser.initParser(attributesHandler.getTypes(), new DesignElementDataBuilder());


            BioMartConnection martConnection = annSrc.createConnection();
            if (!annSrc.isApplied()) {
                readBioEntities(martConnection.getAttributesURL(attributesHandler.getExternalBEIdentifiers()), parser);
                beDataWriter.writeBioEntities(parser.getData(), listener);
            }


            for (ExternalArrayDesign externalArrayDesign : annSrc.getExternalArrayDesigns()) {
                parser.createNewBioEntityData();

                List<String> attributes = new ArrayList<String>(attributesHandler.getExternalBEIdentifiers());
                attributes.add(externalArrayDesign.getName());


                URL url = martConnection.getAttributesURL(attributes);
                if (url != null) {
                    reportProgress("Reading design elements for " + externalArrayDesign.getArrayDesign().getAccession() +
                            " (" + externalArrayDesign.getName() + ") for " + annSrc.getOrganism().getName());

                    long startTime = currentTimeMillis();

                    parser.parseDesignElementMappings(url);
                    log.debug("Done. {} millseconds).\n", (currentTimeMillis() - startTime));
                }

                beDataWriter.writeDesignElements(parser.getData(),
                        externalArrayDesign.getArrayDesign(),
                        annSrc.getSoftware(),
                        annSrcDAO.isAnnSrcAppliedForArrayDesignMapping(annSrc, externalArrayDesign.getArrayDesign()), listener);
            }

            reportSuccess("Update mappings for Organism " + annSrc.getOrganism().getName() + " completed");
        } catch (AnnotationSourceAccessException e) {
            reportError(new AnnotationException("Cannot update mappings for Organism.Problem when connecting to biomart. " + annSrc.getDatasetName(), e));
        } catch (AnnotationException e) {
            e.printStackTrace();
            reportError(e);
        }
    }

    private <T extends BioEntityData> void readBioEntities(URL beURL, AnnotationParser<T> parser) throws AnnotationException {
        if (beURL != null) {
            reportProgress("Reading bioentities for " + annSrc.getOrganism().getName());
            parser.parseBioEntities(beURL, annSrc.getOrganism());
        }
    }

    private void fetchSynonyms(BioMartAnnotationSource annSrc, BioEntityAnnotationDataBuilder builder) throws AnnotationSourceAccessException {
        reportProgress("Reading synonyms for " + annSrc.getOrganism().getName());
        BioMartDbDAO bioMartDbDAO = new BioMartDbDAO(annSrc.getMySqlDbUrl());

        BioEntityType ensgene = annSrc.getBioEntityType(BioEntityType.ENSGENE);
        if (ensgene == null) {
            throw createUnexpected("Annotation source for " + annSrc.getOrganism().getName() + " is not for genes. Cannot fetch synonyms.");
        }

        Collection<Pair<String, String>> geneToSynonyms = bioMartDbDAO.getSynonyms(annSrc.getMySqlDbName(), annSrc.getSoftware().getVersion());
        BioEntityProperty propSynonym = propertyDAO.findOrCreate("synonym");
        for (Pair<String, String> geneToSynonym : geneToSynonyms) {
            BEPropertyValue pv = new BEPropertyValue(null, propSynonym, geneToSynonym.getSecond());
            builder.addPropertyValue(geneToSynonym.getFirst(), ensgene, pv);
        }
    }

}
