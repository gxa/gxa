/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.requesthandlers.base.restutil;

import java.io.IOException;

/**
 * REST result formatter interface
 * @author pashky
 */
public interface RestResultRenderer {
    /**
     * Render object into output using specified profile
     * @param object object to render
     * @param where where to append result text
     * @param profile profile to use
     * @throws RestResultRenderException if anything goes wrong with formatting
     * @throws IOException if i/o write error occurs
     */
    void render(Object object, Appendable where, final Class profile) throws RestResultRenderException, IOException;

    /**
     * Error wrapper interface
     */
    public interface ErrorWrapper {
        Object wrapError(Throwable e);
    }

    /**
     * Sets error wrapper interface implementation
     * @param wrapper wrapper implementation
     */
    void setErrorWrapper(ErrorWrapper wrapper);
}
