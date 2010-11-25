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

package uk.ac.ebi.gxa.requesthandlers.wiggle;

import java.io.*;
import java.util.*;

import org.springframework.web.HttpRequestHandler;
import javax.servlet.ServletException;
import javax.servlet.http.*;

import au.com.bytecode.opencsv.CSVReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;

import uk.ac.ebi.microarray.atlas.model.*;
import uk.ac.ebi.gxa.requesthandlers.wiggle.bam.*;

public class WiggleRequestHandler implements HttpRequestHandler {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private AtlasDAO atlasDAO;
    private AtlasNetCDFDAO atlasNetCDFDAO;

    public void setAtlasDAO(AtlasDAO atlasDAO) {
        this.atlasDAO = atlasDAO;
    }

    public void setAtlasNetCDFDAO(AtlasNetCDFDAO atlasNetCDFDAO) {
        this.atlasNetCDFDAO = atlasNetCDFDAO;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        final String geneId = request.getParameter("gene");
        final String accession = request.getParameter("exp");
        final String factorName = request.getParameter("factor");
        final String factorValue = request.getParameter("value");

        final PrintWriter out = response.getWriter();

        final File dataDir = atlasNetCDFDAO.getDataDirectory(accession);
        String chromosomeId = null;
        long geneStart = -1;
        long geneEnd = -1;
        // TODO: ???
        try {
            final File mappingFile = new File(dataDir, "Homo_sapiens.GRCh37.57.txt");
            final CSVReader mappingReader = new CSVReader(new FileReader(mappingFile), '\t');
            final String[] headers = mappingReader.readNext();

            int geneIdIndex = -1;
            int chromosomeIdIndex = -1;
            int geneStartIndex = -1;
            int geneEndIndex = -1;
            for (int i = 0; i < headers.length; ++i) {
                if ("Ensembl Gene ID".equals(headers[i])) {
                    geneIdIndex = i;
                } else if ("Chromosome Name".equals(headers[i])) {
                    chromosomeIdIndex = i;
                } else if ("Gene Start (bp)".equals(headers[i])) {
                    geneStartIndex = i;
                } else if ("Gene End (bp)".equals(headers[i])) {
                    geneEndIndex = i;
                }
            }
            if (geneIdIndex == -1) throw new IOException("No gene id column in the mapping file");
            if (chromosomeIdIndex == -1) throw new IOException("No chromosome id column in the mapping file");
            if (geneStartIndex == -1) throw new IOException("No gene start column in the mapping file");
            if (geneEndIndex == -1) throw new IOException("No gene end column in the mapping file");

            while (true) {
                final String[] line = mappingReader.readNext();
                if (geneId.equals(line[geneIdIndex])) {
                    chromosomeId = line[chromosomeIdIndex];
                    try {
                        geneStart = Long.parseLong(line[geneStartIndex]);
                        geneEnd = Long.parseLong(line[geneEndIndex]);
                    } catch (NumberFormatException e) {
                    }
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Cannot read gene mapping file");
            return;
        }

        if (chromosomeId == null || geneStart == -1 || geneEnd == -1) {
            log.error("A region for gene " + geneId + " not found");
            return;
        }

        final ArrayList<Assay> assaysToGet = new ArrayList<Assay>();
        for (Assay assay : atlasDAO.getAssaysByExperimentAccession(accession)) {
            for (Property p : assay.getProperties()) {
                if (p.isFactorValue() && factorName.equalsIgnoreCase(p.getName()) && factorValue.equals(p.getValue())) {
                    assaysToGet.add(assay);
                    break;
                }
            }
        }

        final long delta = (geneEnd - geneStart) / 5;
        geneStart -= delta;
        if (geneStart < 1) {
            geneStart = 1;
        }
        geneEnd += delta;

        final WigCreator creator = new WigCreator(out, chromosomeId, geneStart, geneEnd);

        final File assaysDir = new File(dataDir, "assays");
        final ArrayList<Read> allReads = new ArrayList<Read>();
        for (Assay assay : assaysToGet) {
            final File aDir = new File(assaysDir, assay.getAccession());
            final BAMReader reader = new BAMReader(new File(aDir, "accepted_hits.sorted.bam"));
            try {
                for (BAMBlock b : reader.readBAMBlocks(chromosomeId, geneStart, geneEnd)) {
                    for (int i = b.from; i < b.to; ) {
                        Read read = new Read(b.buffer, i, b.to);
                        i += read.blockSize;
                        if (!read.isValid) {
                            log.error("Invalid read record has been found for " + assay.getAccession());
                            break;
                        }
                        if ((read.start <= geneEnd) && (read.end >= geneStart)) {
                            allReads.add(read);
                        }
                    }
                }
            } catch (IOException e) {
                log.error(assay.getAccession() + ":" + e.getMessage());
            }
        }
        Collections.sort(allReads);
        for (Read read : allReads) {
            creator.init(read.start);
            creator.fillByZeroes(read.end);
            creator.removeZeroes(read.start);
            creator.addRegion(read.start, read.end);
            creator.printRegions(read.start);
        }
    }
}

class Read implements Comparable<Read> {
    final boolean isValid;
    int blockSize;
    int start;
    int end;

    private static int readInt32(byte[] buffer, int offset) {
        return
            (buffer[offset] & 0xFF) +
            ((buffer[offset + 1] & 0xFF) << 8) +
            ((buffer[offset + 2] & 0xFF) << 16) +
            ((buffer[offset + 3] & 0xFF) << 24);
    }

    Read(byte[] buffer, int from, int to) {
        if (to - from < 24) {
            isValid = false;
            return;
        }
        blockSize = 4 + readInt32(buffer, from);
        if (blockSize < 0 || to - from < blockSize) {
            isValid = false;
            return;
        }
        start = readInt32(buffer, from + 8);
        end = start + readInt32(buffer, from + 20);
        isValid = true;
    }

    public int compareTo(Read read) {
        if (start != read.start) {
            return start - read.start;
        }
        return end - read.end;
    }
}
