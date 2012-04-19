/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.spring.view.dsv;


import org.springframework.web.servlet.view.AbstractView;
import uk.ac.ebi.gxa.utils.dsv.DsvFormat;
import uk.ac.ebi.gxa.utils.dsv.DsvWriter;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public abstract class AbstractDsvView extends AbstractView {

    private boolean disableCaching;

    public void setDisableCaching(boolean disableCaching) {
        this.disableCaching = disableCaching;
    }

    @Override
    protected void prepareResponse(HttpServletRequest request, HttpServletResponse response) {
        response.setContentType(getContentType());
        response.setCharacterEncoding("UTF-8");
        if (disableCaching) {
            response.addHeader("Pragma", "no-cache");
            response.addHeader("Cache-Control", "no-cache, no-store, max-age=0");
            response.addDateHeader("Expires", 1L);
        } else {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.MONTH, 1);
            response.addDateHeader("Expires", cal.getTimeInMillis());
        }
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        DsvDocument doc = buildDsvDocument(model);
        response.setHeader( "Content-Disposition", "attachment;filename="
                + generateFileName(request));

        ServletOutputStream out = response.getOutputStream();
        write(out, doc);
    }

    private String generateFileName(HttpServletRequest request) {
        String uri = request.getRequestURI();
        uri = uri.substring(uri.lastIndexOf("/") + 1);

        String query = request.getQueryString();
        query = query.replaceAll("(^.*?=)|(&.*?=)", "-");
        return getDsvFormat().fileName(uri + query);
    }

    private void write(OutputStream out, DsvDocument doc) throws IOException {
        DsvWriter dsvWriter = getDsvFormat().newWriter(new OutputStreamWriter(out));
        dsvWriter.write(doc.getHeader());
        if (doc.getColumnsDescription() != null)
            dsvWriter.write(doc.getColumnsDescription());
        Iterator<String[]> rowIterator = doc.getRowIterator();
        while (rowIterator.hasNext()) {
            dsvWriter.write(rowIterator.next());
        }
        dsvWriter.flush();
    }

    abstract DsvFormat getDsvFormat();

    abstract protected DsvDocument buildDsvDocument(Map<String, Object> model);
}
