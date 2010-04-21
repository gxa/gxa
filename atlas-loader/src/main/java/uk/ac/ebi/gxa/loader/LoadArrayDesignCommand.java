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
import java.net.MalformedURLException;
import java.util.Collection;
import java.util.Arrays;

/**
 * Load array design command has URL and gene identifier priority as parameters
 * @author pashky
 */
public class LoadArrayDesignCommand extends AbstractURLCommand {
    private Collection<String> geneIdentifierPriority = Arrays.asList("ensembl","uniprot");

    /**
     * Creates command for URL
     * @param url url
     */
    public LoadArrayDesignCommand(URL url) {
        super(url);
    }

    /**
     * Creates command for string URL
     * @param url string
     * @throws MalformedURLException if url is invalid
     */
    public LoadArrayDesignCommand(String url) throws MalformedURLException {
        super(url);
    }

    /**
     * Creates command for URL and gene identifier priority
     * @param url string
     * @param geneIdentifierPriority collection of gene identifier names in order
     */
    public LoadArrayDesignCommand(URL url, Collection<String> geneIdentifierPriority) {
        super(url);
        this.geneIdentifierPriority = geneIdentifierPriority;
    }

    /**
     * Creates command for string URL and gene identifier priority
     * @param url string
     * @param geneIdentifierPriority collection of gene identifier names in order
     * @throws MalformedURLException if url is invalid
     */
    public LoadArrayDesignCommand(String url, Collection<String> geneIdentifierPriority) throws MalformedURLException {
        super(url);
        this.geneIdentifierPriority = geneIdentifierPriority;
    }

    /**
     * Returns gene identifier priority list
     * @return collection of id names
     */
    public Collection<String> getGeneIdentifierPriority() {
        return geneIdentifierPriority;
    }

    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }

    @Override
    public String toString() {
        return "Load array design from " + getUrl();
    }
}
