/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.gxa.annotator.dao.AnnotationSourceDAO;
import uk.ac.ebi.gxa.annotator.loader.biomart.BioMartAnnotator;
import uk.ac.ebi.gxa.annotator.loader.listner.AnnotationLoaderListener;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;
import uk.ac.ebi.gxa.annotator.model.AnnotationSourceClass;
import uk.ac.ebi.gxa.annotator.model.biomart.BioMartAnnotationSource;
import uk.ac.ebi.gxa.annotator.model.genesigdb.GeneSigAnnotationSource;
import uk.ac.ebi.gxa.annotator.process.Annotator;
import uk.ac.ebi.gxa.annotator.process.FileBasedAnnotator;
import uk.ac.ebi.gxa.dao.bioentity.BioEntityPropertyDAO;
import uk.ac.ebi.gxa.exceptions.LogUtil;

/**
 * User: nsklyar
 * Date: 28/07/2011
 */
public class AnnotationProcessor {

    private AtlasBioEntityDataWriter beDataWriter;
    private AnnotationSourceDAO annSrcDAO;
    private BioEntityPropertyDAO propertyDAO;

    public AnnotationProcessor(AtlasBioEntityDataWriter beDataWriter, AnnotationSourceDAO annotationSourceDAO, BioEntityPropertyDAO propertyDAO) {
        this.beDataWriter = beDataWriter;
        this.annSrcDAO = annotationSourceDAO;
        this.propertyDAO = propertyDAO;
    }

    public void updateAnnotations(String annSrcId, AnnotationLoaderListener listener) {
        final Annotator annotator = getAnnotator(annSrcId);
        annotator.setListener(listener);
        annotator.updateAnnotations();
    }

    public void updateMappings(String annSrcId, AnnotationLoaderListener listener) {
        final Annotator annotator = getAnnotator(annSrcId);
        annotator.setListener(listener);
        annotator.updateMappings();
    }

    private AnnotationSource fetchAnnSrcById(String id) {
        AnnotationSource annSrc = null;
        if (!StringUtils.isEmpty(id)) {
            try {
                final long idL = Long.parseLong(id.trim());
                annSrc = annSrcDAO.getById(idL);
            } catch (NumberFormatException e) {
                throw LogUtil.createUnexpected("Cannot fetch Annotation Source. Wrong ID ", e);
            }
        }
        return annSrc;
    }

    private Annotator getAnnotator(String id) {
        final AnnotationSource annotationSource = fetchAnnSrcById(id);
        final AnnotationSourceClass annSrcClass = AnnotationSourceClass.getByClass(annotationSource.getClass());
        if (annSrcClass.equals(AnnotationSourceClass.BIOMART)) {
            return new BioMartAnnotator((BioMartAnnotationSource) annotationSource, annSrcDAO, propertyDAO, beDataWriter);
        } else if (AnnotationSourceClass.FILE.equals(annSrcClass)) {
            return new FileBasedAnnotator((GeneSigAnnotationSource) annotationSource, beDataWriter);
        } else
            throw new IllegalArgumentException("There is no annotator for class " + annSrcClass);
    }
}
