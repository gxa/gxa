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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Base class for loader commands having URL as a parameter
 * @author pashky
 */
abstract class AbstractURLCommand implements AtlasLoaderCommand {
    private URL url;

    AbstractURLCommand(URL url) {
        this.url = url;
    }

    AbstractURLCommand(String url) throws MalformedURLException {
        if(!url.matches("^\\w+:.*") && new File(url).exists()) // artifact intelligence
            this.url = new URL("file:" + url.replaceAll("^/+", "/"));
        else if(url.matches("^file:/+.*")) // java bug workaround
            this.url = new URL(url.replaceAll("^file:/+", "file:/"));
        else
            this.url = new URL(url);
    }

    /**
     * Returns parameter URL
     * @return url
     */
    public URL getUrl() {
        return url;
    }
}
