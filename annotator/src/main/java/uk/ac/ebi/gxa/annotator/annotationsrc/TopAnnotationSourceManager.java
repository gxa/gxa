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

package uk.ac.ebi.gxa.annotator.annotationsrc;


import uk.ac.ebi.gxa.annotator.AnnotationSourceType;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;

/**
 * User: nsklyar
 * Date: 23/01/2012
 */
public class TopAnnotationSourceManager {

    private List<AnnotationSourceManager<? extends AnnotationSource>> managers;

    public TopAnnotationSourceManager(List<AnnotationSourceManager<? extends AnnotationSource>> managers) {
        this.managers = managers;
    }

    public Collection<UpdatedAnnotationSource> getAllAnnotationSources() {
        Collection<UpdatedAnnotationSource> result = new HashSet<UpdatedAnnotationSource>();
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            result.addAll(manager.getCurrentAnnotationSources());
        }

        return result;
    }

    public Collection<String> validateProperties(AnnotationSource annSrc) {
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(annSrc.getClass())) {
                return manager.validateProperties(annSrc);
            }
        }
        throw new IllegalArgumentException("Cannot validate annotation source of class " + annSrc.getClass().getName());

    }

    public String getAnnSrcString(String id, String typeName) {
        final AnnotationSourceType type = AnnotationSourceType.getByName(typeName);
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(type.getClazz())) {
                return manager.getAnnSrcString(id);
            }
        }

        throw new IllegalArgumentException("Annotation source manager is not available for type " + type);
    }

    public ValidationReportBuilder saveAnnSrc(String id, String text, String typeName) {
        final AnnotationSourceType type = AnnotationSourceType.getByName(typeName);
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(type.getClazz())) {
                return manager.saveAnnSrc(id, text);
            }
        }
        throw new IllegalArgumentException("Annotation source manager is not available for type " + type);
    }

    public boolean areMappingsApplied(AnnotationSource annSrc) {
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(annSrc.getClass())) {
                return manager.areMappingsApplied(annSrc);
            }
        }
        throw new IllegalArgumentException("Annotation source manager is not available for type " + annSrc.getClass());
    }

    public Collection<String> validateProperties(String annSrcId, String typeName) {
        final AnnotationSourceType type = AnnotationSourceType.getByName(typeName);
        for (AnnotationSourceManager<? extends AnnotationSource> manager : managers) {
            if (manager.isForClass(type.getClazz())) {
                return manager.validateProperties(annSrcId);
            }
        }
        throw new IllegalArgumentException("Cannot validate annotation source of class " + type);

    }
}
