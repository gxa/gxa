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
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityData;
import uk.ac.ebi.gxa.annotator.loader.data.DesignElementMappingData;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.web.admin.AnnotationCommandListener;
import uk.ac.ebi.microarray.atlas.model.ArrayDesign;
import uk.ac.ebi.microarray.atlas.model.bioentity.Software;

/**
 * User: nsklyar
 * Date: 02/12/2011
 */
public abstract class Annotator {
    final private Logger log = LoggerFactory.getLogger(this.getClass());

    private final AtlasBioEntityDataWriter beDataWriter;
    private AnnotationCommandListener listener;

    protected Annotator(AtlasBioEntityDataWriter beDataWriter) {
        this.beDataWriter = beDataWriter;
    }

    public void setListener(AnnotationCommandListener listener) {
        this.listener = listener;
    }

    public abstract void updateAnnotations();

    public abstract void updateMappings();

    protected void reportProgress(String report) {
        log.info(report);
        if (listener != null)
            listener.commandProgress(report);
    }

    protected void reportError(Throwable error) {
        log.error("Annotation failed! ", error);
        if (listener != null)
            listener.commandError(error);
    }

    protected void reportSuccess(String message) {
        log.info(message);
        if (listener != null)
            listener.commandSuccess(message);
    }

    protected void writeBioEntities(BioEntityData data) {
        beDataWriter.writeBioEntities(data, listener);
    }

    protected void writePropertyValues(BioEntityAnnotationData data, AnnotationSource annSrc, boolean checkBioEntities) {
        beDataWriter.writePropertyValues(data.getPropertyValues(), listener);
        beDataWriter.writeBioEntityToPropertyValues(data, annSrc, checkBioEntities, listener);
    }

    protected void writeDesignElements(DesignElementMappingData data, ArrayDesign arrayDesign, Software software, boolean deleteBeforeWrite) {
        beDataWriter.writeDesignElements(data, arrayDesign, software, deleteBeforeWrite, listener);
    }
}
