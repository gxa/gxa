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

package uk.ac.ebi.gxa.spring.view.dsv;

import uk.ac.ebi.gxa.utils.dsv.DsvFormat;
import uk.ac.ebi.gxa.utils.dsv.TsvFormat;

/**
 * @author Olga Melnichuk
 */
public abstract class AbstractTsvView<T> extends AbstractDsvView<T> {

    private final TsvFormat format;

    public AbstractTsvView() {
        format = new TsvFormat();
        setContentType(format.getContentType());
    }

    /**
     * Enforces error throwing when value contains illegal TSV characters (tab and end-of-line)
     *
     * @param strict if <code>true</code> and value contains illegal for TSV characters
     *               an {@link IllegalArgumentException} is thrown
     */
    public void setStrictTsv(boolean strict) {
        format.setStrict(strict);
    }

    @Override
    DsvFormat getDsvFormat() {
        return format;
    }
}
