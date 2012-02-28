package uk.ac.ebi.gxa.loader.service;

import java.io.*;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Utility class for converting .gtf files into annotation format usable by WiggleRequestHandler
 *
 * @author rpetry
 */
public class GeneAnnotationFormatConverterService {

    static class Triple {
        String chromosomeId;
        long geneStart;
        long geneEnd;
    }

    private final static TreeMap<String, Triple> map = new TreeMap<String, Triple>();

    /**
     * Convert data in gtfFile into format usable by WiggleRequestHandler and write it into annotationFile
     *
     * @param gtfFile
     * @param annotationFile
     * @throws IOException
     */
    public static void transferAnnotation(File gtfFile, File annotationFile) throws IOException {
        final StringBuilder sb = new StringBuilder();
        final BufferedReader reader = new BufferedReader(new FileReader(gtfFile));
        final BufferedWriter out = new BufferedWriter(new FileWriter(annotationFile));
        final Pattern p = Pattern.compile("^.+gene_id \"([^\"]+)\".+$");
        while (true) {
            final String line = reader.readLine();
            if (line == null) {
                break;
            }
            String[] fields = line.split("\t");
            final String chromosomeId = fields[0];
            final long exonStart = Long.parseLong(fields[3]);
            final long exonEnd = Long.parseLong(fields[4]);
            final String geneId = p.matcher(fields[8]).replaceFirst("$1");
            addExon(geneId, chromosomeId, exonStart, exonEnd);
        }

        for (Map.Entry<String, Triple> entry : map.entrySet()) {
            final Triple t = entry.getValue();
            sb.append(entry.getKey()).append("\t").append(t.chromosomeId).append("\t").append(t.geneStart).append("\t").append(t.geneEnd);
            out.write(sb.toString());
            out.newLine();
        }
    }

    private static void addExon(String geneId, String chromosomeId, long geneStart, long geneEnd) {
        Triple t = map.get(geneId);
        if (t == null) {
            t = new Triple();
            t.chromosomeId = chromosomeId;
            t.geneStart = geneStart;
            t.geneEnd = geneEnd;
            map.put(geneId, t);
        } else {
            t.geneStart = Math.min(t.geneStart, geneStart);
            t.geneEnd = Math.max(t.geneEnd, geneEnd);
        }
    }
}
