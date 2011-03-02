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
import uk.ac.ebi.gxa.requesthandlers.base.AbstractRestRequestHandler;
import uk.ac.ebi.gxa.requesthandlers.base.restutil.RequestWrapper;
import uk.ac.ebi.gxa.web.AtlasPlotter;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Pavel Kurnosov
 */
public class PlotterRequestHandler extends AbstractRestRequestHandler {
    final Logger log = LoggerFactory.getLogger(getClass());

    AtlasPlotter plotter;

    public void setPlotter(AtlasPlotter plotter) {
        this.plotter = plotter;
    }

    public Object process(HttpServletRequest request) {
        RequestWrapper req = new RequestWrapper(request);

        String ef = req.getStr("ef");
        if (Strings.isNullOrEmpty(ef))
            ef = "default";
        return plotter.getGeneInExpPlotData(req.getStr("gid"), req.getStr("eacc"),
                ef, req.getStr("efv"), req.getStr("plot"));
    }
}
