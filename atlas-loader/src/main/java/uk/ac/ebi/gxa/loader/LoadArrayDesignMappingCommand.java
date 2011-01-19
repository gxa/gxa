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

/**
 * Load bioentities command has URL and boolean to indicate either corresponding virtual arraydesign needs to be loaded/updated
 */
public class LoadArrayDesignMappingCommand extends AbstractURLCommand {

    //ToDo: pass in a command already processed properties
    private String adAccMappingFile;
    private String accession;

    public LoadArrayDesignMappingCommand(URL url, String adAccMappingFile, String accession) {
        super(url);
        this.adAccMappingFile = adAccMappingFile;
        this.accession = accession;
    }

    public LoadArrayDesignMappingCommand(String url, String adAccMappingFile, String accession) throws MalformedURLException {
        super(url);
        this.adAccMappingFile = adAccMappingFile;
        this.accession = accession;
    }

    public LoadArrayDesignMappingCommand(URL url) {
        super(url);
    }

    public LoadArrayDesignMappingCommand(String url) throws MalformedURLException {
        super(url);
    }

    public void visit(AtlasLoaderCommandVisitor visitor) throws AtlasLoaderException {
        visitor.process(this);
    }

    public LoadArrayDesignMappingCommand(URL url, String accession) {
        super(url);
        this.accession = accession;
    }

    public LoadArrayDesignMappingCommand(String url, String accession) throws MalformedURLException {
        super(url);
        this.accession = accession;
    }

    public String getAdAccMappingFile() {
        return adAccMappingFile;
    }

    public String getAccession() {
        return accession;
    }

    public void setAdAccMappingFile(String adAccMappingFile) {
        this.adAccMappingFile = adAccMappingFile;
    }

    @Override
    public String toString() {
        return "Load array design mappings from " + getUrl();
    }
}
