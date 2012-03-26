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

package uk.ac.ebi.gxa.spring.view.csv;


import au.com.bytecode.opencsv.CSVWriter;
import org.springframework.web.servlet.view.AbstractView;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public abstract class AbstractCsvView extends AbstractView {

    private boolean disableCaching = true;

    public AbstractCsvView() {
        setContentType("text/csv");
    }

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
        }
    }

    @Override
    protected void renderMergedOutputModel(Map<String, Object> model, HttpServletRequest request,
                                           HttpServletResponse response) throws Exception {
        CsvDocument doc = buildDocument(model);
        response.setContentType(getContentType());
        ServletOutputStream out = response.getOutputStream();
        write(out, doc);
    }

    abstract protected CsvDocument buildDocument(Map<String, Object> model);

    private void write(OutputStream out, CsvDocument doc) throws IOException {
        Writer writer = new OutputStreamWriter(out);

        String[] comments = doc.getComments();
        for (String c : comments) {
            writer.write(c);
        }
        
        CSVWriter csvWriter = new CSVWriter(writer, '\t');
        csvWriter.writeNext(doc.getHeader());
        Iterator<String[]> rowIterator = doc.getRowIterator();
        while(rowIterator.hasNext()) {
            csvWriter.writeNext(rowIterator.next());
        }
        csvWriter.flush();
    }

    protected static interface CsvDocument {

        public String[] getComments();

        public String[] getHeader();

        public Iterator<String[]> getRowIterator();
    }
}
