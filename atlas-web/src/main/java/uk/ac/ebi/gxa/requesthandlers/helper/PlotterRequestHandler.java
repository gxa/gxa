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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.requesthandlers.helper;

import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.web.AtlasPlotter;

import javax.servlet.http.HttpServletRequest;

/**
 * @author pashky
 */
public class PlotterRequestHandler extends AbstractRestRequestHandler {
    final Logger log = LoggerFactory.getLogger(getClass());

    AtlasPlotter plotter;

    public void setPlotter(AtlasPlotter plotter) {
        this.plotter = plotter;
    }

    public Object process(HttpServletRequest request) {

        String gid = request.getParameter("gid");
        String eid = request.getParameter("eid");
        String plotType = "bar";
        String ef = "default";
        String efv = "";

        if(request.getParameter("plot") != null)
            plotType = request.getParameter("plot");

        if(request.getParameter("ef") != null && !request.getParameter("ef").equals(""))
            ef = request.getParameter("ef");

        if(request.getParameter("efv") != null)
            efv = request.getParameter("efv");

        return plotter.getGeneInExpPlotData(gid, eid, ef, efv, plotType);
    }
}
