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

package uk.ac.ebi.gxa.web.export;

import net.sf.json.JSONObject;
import net.sourceforge.fluxion.ajax.Ajaxified;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.web.Atlas;
import uk.ac.ebi.microarray.atlas.model.LoadDetails;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.List;

/**
 * An ajaxified bean that exports a list of experiment accessions as a JSON object.  This JSON object represents a list
 * of experiments that will fit on a single page, where the number of experiments per page, and the page number, can be
 * specified in the JSON object take as a parameter.
 * <p/>
 * The general schema for the returned object is:
 * <code><pre>
 *   {
 *     "accessions": [***,...,***]
 *   }
 * </pre></code>
 * <p/>
 * And the schema for the parameter object is:
 * <code><pre>
 *   {
 *     "pageNumber": *
 *     "experimentsPerPage": *
 *   }
 * </pre></code>
 *
 * @author Tony Burdett
 * @date 19-Jan-2010
 */
@Ajaxified
public class ExperimentAccessionsExporter {
    private AtlasDAO atlasDAO;

    private Logger log = LoggerFactory.getLogger(getClass());

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public AtlasDAO getAtlasDAO() {
        return atlasDAO;
    }

    public JSONObject getExperimentAccessions(HttpSession session, JSONObject input) {
        log.debug("Getting experiment accessions for " + input.toString());

        // extract pageNumber, experimentsPerPage params
        int pageNumber = input.getInt("pageNumber");
        int experimentsPerPage = input.getInt("experimentsPerPage");

        // update session variables
        session.setAttribute(Atlas.ADMIN_PAGE_NUMBER.key(), pageNumber);
        session.setAttribute(Atlas.ADMIN_EXPERIMENTS_PER_PAGE.key(), experimentsPerPage);

        // request this page of experiment accessions
        List<LoadDetails> details = getAtlasDAO().getLoadDetailsForExperimentsByPage(pageNumber, experimentsPerPage);
        List<String> accessions = new ArrayList<String>();
        for (LoadDetails ld : details) {
            accessions.add(ld.getAccession());
        }

        // formulate the response object
        JSONObject json = new JSONObject();
        json.put("accessions", accessions);

        log.debug("Response looks like: " + json.toString());

        return json;
    }
}
