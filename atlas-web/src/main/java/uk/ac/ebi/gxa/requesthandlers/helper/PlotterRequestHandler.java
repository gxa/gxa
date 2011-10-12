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

package uk.ac.ebi.gxa.requesthandlers.helper;

import com.google.common.base.Strings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.gxa.dao.ExperimentDAO;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RequestWrapper;
import uk.ac.ebi.gxa.web.AtlasPlotter;
import uk.ac.ebi.microarray.atlas.model.Experiment;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 * @author Pavel Kurnosov
 */
public class PlotterRequestHandler extends AbstractRestRequestHandler {
    final Logger log = LoggerFactory.getLogger(getClass());

    private final AtlasPlotter plotter;
    private final ExperimentDAO experimentDAO;

    public PlotterRequestHandler(AtlasPlotter plotter, ExperimentDAO experimentDAO) {
        this.plotter = plotter;
        this.experimentDAO = experimentDAO;
    }

    public Object process(HttpServletRequest request) throws ServletException {
        RequestWrapper req = new RequestWrapper(request);

        try {
            final Experiment eacc = experimentDAO.getByName(req.getStr("eacc"));
            return plotter.getGeneInExpPlotData(req.getStr("gid"), eacc, req.getStr("ef"), req.getStr("efv"), req.getStr("plot"));
        } catch (RecordNotFoundException e) {
            throw new ServletException(e.getMessage());
        }
    }
}
