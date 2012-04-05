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

package uk.ac.ebi.gxa.annotator.loader.biomart;

import org.apache.http.client.HttpClient;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.loader.data.InvalidAnnotationDataException;
import uk.ac.ebi.gxa.annotator.loader.util.InvalidCSVColumnException;
import uk.ac.ebi.gxa.annotator.model.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.ExternalArrayDesign;
import uk.ac.ebi.gxa.annotator.model.ExternalBioEntityProperty;
import uk.ac.ebi.microarray.atlas.model.bioentity.BioEntityProperty;

import java.io.IOException;

/**
 * @author Olga Melnichuk
 * @version 1/16/12 1:23 PM
 */
public class BioMartAnnotationLoader {

    private final MartServiceClient martClient;

    private final BioMartAnnotationSource annotSource;

    private BioEntityData.Builder entityBuilder;

    private BioEntityAnnotationData.Builder pvBuilder;

    private DesignElementMappingData.Builder deMappingsBuilder;

    public BioMartAnnotationLoader(HttpClient httpClient, BioMartAnnotationSource annotSource) {
        this.martClient = MartServiceClientImpl.create(httpClient, annotSource);
        this.annotSource = annotSource;
    }

    public void loadBioEntities() throws BioMartException, IOException, InvalidCSVColumnException {
        entityBuilder = new BioEntityData.Builder(annotSource.getOrganism());
        (new MartBioEntitiesLoader(annotSource, martClient)).load(entityBuilder);
    }

    public void loadPropertyValues(ExternalBioEntityProperty externalProperty)
            throws BioMartException, IOException, InvalidCSVColumnException {
        pvBuilder = pvBuilder == null ? new BioEntityAnnotationData.Builder() : pvBuilder;
        (new MartPropertyValuesLoader(annotSource, martClient)).load(externalProperty, pvBuilder);
    }

    public void loadSynonyms(BioEntityProperty synonymProperty) throws BioMartException {
        pvBuilder = pvBuilder == null ? new BioEntityAnnotationData.Builder() : pvBuilder;
        (new MartSynonymPropertyValuesLoader(annotSource)).load(synonymProperty, pvBuilder);
    }

    public void loadDesignElementMappings(ExternalArrayDesign externalArrayDesign)
            throws BioMartException, IOException, InvalidCSVColumnException {
        deMappingsBuilder = new DesignElementMappingData.Builder();
        (new MartDesignElementMappingsLoader(annotSource, martClient)).load(externalArrayDesign, deMappingsBuilder);
    }

    public BioEntityData getBioEntityData() throws InvalidAnnotationDataException {
        return entityBuilder == null ? null : entityBuilder.build(annotSource.getTypes());
    }

    public BioEntityAnnotationData getPropertyValuesData() throws InvalidAnnotationDataException {
        return pvBuilder == null ? null : pvBuilder.build(annotSource.getTypes());
    }

    public DesignElementMappingData getDeMappingsData() throws InvalidAnnotationDataException {
        return deMappingsBuilder == null ? null : deMappingsBuilder.build(annotSource.getTypes());
    }
}
