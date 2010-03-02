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

package uk.ac.ebi.gxa.web;

/**
 * Session variables used in the Atlas web interface.
 *
 * @author Tony Burdett
 * @date 09-Nov-2009
 */
public enum Atlas {
    DOWNLOAD_SERVICE("atlas.download.service"),
    GENES_CACHE("atlas.genelist.cache.service"),
    ATLAS_SOLR_DAO("atlas.solr.dao"),
    ATLAS_DAO("atlas.dao"),
    ADMIN_PAGE_NUMBER("atlas.admin.page.number"),
    ADMIN_EXPERIMENTS_PER_PAGE("atlas.admin.expts.per.page");

    private String key;

    Atlas(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }
}
