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


import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.gxa.annotator.model.AnnotationSource;

import java.util.Collection;

/**
 * User: nsklyar
 * Date: 19/01/2012
 */
public abstract class AnnotationSourcePropertiesValidator<T extends AnnotationSource> {

    public abstract Collection<String> getInvalidPropertyNames(T annotationSource);

    public String getSummary(T annotationSource) {
        StringBuilder sb = new StringBuilder();

        final Collection<String> invalidPropertyNames = getInvalidPropertyNames(annotationSource);
        if (!CollectionUtils.isEmpty(invalidPropertyNames)) {
            sb.append("Invalid properties: ");
            sb.append(invalidPropertyNames);
        }
        return sb.toString();

    }
}
