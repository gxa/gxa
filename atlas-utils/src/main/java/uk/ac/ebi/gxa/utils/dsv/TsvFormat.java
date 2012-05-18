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

package uk.ac.ebi.gxa.utils.dsv;

/**
 * Support for tab separated values format.
 * http://www.iana.org/assignments/media-types/text/tab-separated-values
 *
 * @author Olga Melnichuk
 */
public class TsvFormat extends DsvFormat {

    private boolean strict;

    public TsvFormat() {
        this(true);
    }

    public TsvFormat(boolean strict) {
        super('\t', "text/tab-separated-values", ".tsv");
        this.strict = strict;
    }

    /**
     * Enforces error throwing when value contains illegal TSV characters (tab and end-of-line)
     *
     * @param strict if <code>true</code> and value contains illegal for TSV characters
     *               an {@link IllegalArgumentException} is thrown
     */
    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    String sanitizeFieldValue(String value) {
        if (value.contains("\t") || value.contains("\n")) {
            if (strict) {
                throw new IllegalArgumentException("Illegal TSV characters [\\t|\\n] in: [" + value + "]");
            }
            value = value.replaceAll("\t|\n", " ");
        }
        return value;
    }
}
