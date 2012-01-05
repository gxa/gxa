
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
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationData;
import uk.ac.ebi.gxa.annotator.loader.data.BioEntityAnnotationDataBuilder;
import uk.ac.ebi.gxa.annotator.model.FileBasedAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.GeneSigAnnotationSource;
import uk.ac.ebi.gxa.utils.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

/**
 * User: nsklyar
 * Date: 05/12/2011
 */
public class FileBasedAnnotator<T extends FileBasedAnnotationSource>  extends Annotator<T>{

    static final private Logger log = LoggerFactory.getLogger(FileBasedAnnotator.class);

    public FileBasedAnnotator(T annSrc, AtlasBioEntityDataWriter beDataWriter) {
        super(annSrc, beDataWriter);
    }

    @Override
    public void updateAnnotations() {
        try {
            //Create a list with biomart attribute names for bioentity types of  annotation source
            BETypeExternalAttributesHandler attributesHandler = new BETypeExternalAttributesHandler(annSrc);
            BioEntityAnnotationDataBuilder builder = new BioEntityAnnotationDataBuilder();
            AnnotationParser<BioEntityAnnotationData> parser = AnnotationParser.initParser(attributesHandler.getTypes(), builder);
            parser.setSeparator(annSrc.getSeparator());

            reportProgress("Reading properties from Annotation Source " + annSrc.getName());
            File contentAsFile = FileUtil.tempFile("genesig.tmp");
            URLContentLoader.getContentAsFile(annSrc.getUrl(), contentAsFile);

            reportProgress("Parsing properties from Annotation Source " + annSrc.getName());
            parser.parsePropertyValues(attributesHandler.getBioEntityProperties(),
                    new FileInputStream(contentAsFile), true);

            final BioEntityAnnotationData data = parser.getData();

            beDataWriter.writePropertyValues(data.getPropertyValues(), listener);
            beDataWriter.writeBioEntityToPropertyValues(data, annSrc, true, listener);

            reportSuccess("Update annotations from Annotation Source " + annSrc.getName() + " completed");
            if (!contentAsFile.delete()){
                log.error("Couldn't delete temp file " + contentAsFile.getAbsolutePath());
            }
        } catch (AnnotationException e) {
            reportError(e);

        } catch (FileNotFoundException e) {
            reportError(new AnnotationException("Cannot read annotations from URL " + annSrc.getUrl() +
                    " for AnnSrc " + annSrc.getName(), e));
        }
    }

    @Override
    public void updateMappings() {
        throw new UnsupportedOperationException(this.getClass().getSimpleName() + " doesn't support method updateMappings ");
    }

}
