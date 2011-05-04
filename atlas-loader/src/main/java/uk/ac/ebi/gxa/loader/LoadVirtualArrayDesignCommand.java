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

package uk.ac.ebi.gxa.loader;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;

/**
 * Load virtual array design command has URL and gene identifier priority as parameters
 * @deprecated
 *
 */
public class LoadVirtualArrayDesignCommand extends AbstractURLCommand {
    private Collection<String> geneIdentifierPriority = Arrays.asList("ensgene");

    /**
     * Creates command for URL
     *
     * @param url url
     */
    public LoadVirtualArrayDesignCommand(URL url) {
        super(url);
    }

    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }

    public Collection<String> getGeneIdentifierPriority() {
        return geneIdentifierPriority;
    }

    public String getAdType() {
        return "virtual design";
    }

    @Override
    public String toString() {
        return "Load virtual array design from " + getUrl();
    }
}
