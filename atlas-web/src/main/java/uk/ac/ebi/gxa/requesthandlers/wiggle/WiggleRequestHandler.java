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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.dao.AtlasDAO;
import uk.ac.ebi.gxa.netcdf.reader.AtlasNetCDFDAO;
import uk.ac.ebi.gxa.requesthandlers.wiggle.bam.BAMBlock;
import uk.ac.ebi.gxa.requesthandlers.wiggle.bam.BAMReader;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Property;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;

import static com.google.common.io.CharStreams.readLines;
import static com.google.common.io.Closeables.closeQuietly;
import static uk.ac.ebi.gxa.utils.FileUtil.extension;

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
        response.setContentType("text/plain; charset=UTF-8");

        final PrintWriter out = response.getWriter();

        String uri = request.getRequestURI();
        uri = uri.substring(uri.lastIndexOf('/') + 1);
        uri = uri.substring(0, uri.length() - 4);

        final String[] allParams = uri.split("_");
        if (allParams.length != 4) {
            log.error("Parameter number is invalid (" + allParams.length + ") for URL " + uri);
            return;
        }
        final String geneId = allParams[0];
        final String accession = allParams[1];
        final String factorName = URLDecoder.decode(URLDecoder.decode(allParams[2]));
        final String factorValue = URLDecoder.decode(URLDecoder.decode(allParams[3]));


        final File dataDir = atlasNetCDFDAO.getDataDirectory(accession);
        final GeneAnnotation anno =
                new GeneAnnotation(new File(dataDir, "annotations"), geneId, accession);
        final String chromosomeId = anno.chromosomeId();
        long geneStart = anno.geneStart();
        long geneEnd = anno.geneEnd();

        if (chromosomeId == null || geneStart == -1 || geneEnd == -1) {
            log.error("A region for gene " + geneId + " not found");
            return;
        }

        final ArrayList<Assay> assaysToGet = new ArrayList<Assay>();
        for (Assay assay : atlasDAO.getAssaysByExperimentAccession(accession)) {
            for (Property p : assay.getProperties(factorName)) {
                if (p.isFactorValue() && factorValue.equals(p.getValue())) {
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
        final String wiggleName =
                "EBI Expression Atlas (GXA) Experiment " +
                        accession + " - " + factorName + " - " + factorValue;

        out.println("track" +
                " type=wiggle_0" +
                " name=\"" + wiggleName + "\"" +
                " description=\"" + wiggleName + "\"" +
                " visibility=full" +
                " autoScale=on" +
                " color=68,68,68" +
                " yLineMark=11.76" +
                " yLineOnOff=on" +
                " priority=10"
        );

        final File assaysDir = new File(dataDir, "assays");
        final ArrayList<Read> allReads = new ArrayList<Read>();
        for (Assay assay : assaysToGet) {
            final File aDir = new File(assaysDir, assay.getAccession());
            final BAMReader reader = new BAMReader(new File(aDir, "accepted_hits.sorted.bam"));
            try {
                for (BAMBlock b : reader.readBAMBlocks(chromosomeId, geneStart, geneEnd)) {
                    for (int i = b.from; i < b.to;) {
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

class GeneAnnotation {
    @SuppressWarnings({"FieldCanBeLocal"})
    private final Logger log = LoggerFactory.getLogger(getClass());

    private String chromosomeId = null;
    private long geneStart = -1;
    private long geneEnd = -1;

    GeneAnnotation(File annotationDir, String geneId, String accession) {
        BufferedReader reader = null;
        try {
            final File[] annotationFiles = annotationDir.listFiles(extension("anno", false));
            if (annotationFiles == null || annotationFiles.length == 0) {
                log.error("No annotation file for experiment " + accession);
                return;
            }
            if (annotationFiles.length > 1) {
                log.error("Several annotation files for experiment " + accession);
                log.error(annotationFiles[0].getName() + " will be used");
            }
            reader = new BufferedReader(new FileReader(annotationFiles[0]));

            for (String line : readLines(reader)) {
                final String[] fields = line.split("\t");
                if (geneId.equals(fields[0])) {
                    chromosomeId = fields[1];
                    try {
                        geneStart = Long.parseLong(fields[2]);
                        geneEnd = Long.parseLong(fields[3]);
                    } catch (NumberFormatException e) {
                        log.error("Invalid line: {}", line);
                    }
                    break;
                }
            }
        } catch (IOException e) {
            log.error("Cannot read gene mapping file");
        } finally {
            closeQuietly(reader);
        }
    }

    long geneStart() {
        return this.geneStart;
    }

    long geneEnd() {
        return this.geneEnd;
    }

    String chromosomeId() {
        return this.chromosomeId;
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
