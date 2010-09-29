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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Load experiment by URL loader command
 * @author pashky
 */
public class LoadExperimentCommand extends AbstractURLCommand {
    private Collection<String> possibleQTypes = Collections.emptyList();
    private final Map<String,String[]> userData;

    /**
     * Returns possible quantitation types
     * @return list of strings
     */
    public Collection<String> getPossibleQTypes() {
        return possibleQTypes;
    }

    /**
     * Creates load command by URL
     * @param url url of experiment idf file
     */
    public LoadExperimentCommand(URL url, Map<String,String[]> userData) {
        super(url);
        this.userData = userData;
    }

    /**
     * Creates load command by string URL
     * @param url string with url of experiment idf file
     * @throws MalformedURLException if url is invalid
     */
    public LoadExperimentCommand(String url, Map<String,String[]> userData) throws MalformedURLException {
        super(url);
        this.userData = userData;
    }

    /**
     * Creates load command by URL and possible quantitation types
     * @param possibleQTypes collection of possible quantitation types names
     * @param url url of experiment idf file
     */
    public LoadExperimentCommand(URL url, Collection<String> possibleQTypes, Map<String,String[]> userData) {
        super(url);
        this.possibleQTypes = possibleQTypes;
        this.userData = userData;
    }

    /**
     * Creates load command by string URL and possible quantitation types
     * @param url string with url of experiment idf file
     * @param possibleQTypes collection of possible quantitation types names
     * @throws MalformedURLException if url is invalid
     */
    public LoadExperimentCommand(String url, Collection<String> possibleQTypes, Map<String,String[]> userData) throws MalformedURLException {
        super(url);
        this.possibleQTypes = possibleQTypes;
        this.userData = userData;
    }

    public Map<String,String[]> getUserData() {
        return Collections.unmodifiableMap(userData);
    }

    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }

    @Override
    public String toString() {
        return "Load experiment from " + getUrl();
    }
}
