package uk.ac.ebi.ae3.indexbuilder.magetab;


import com.Ostermiller.util.ExcelCSVParser;

import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import org.slf4j.LoggerFactory;
import org.slf4j.Logger;


/**
 * User: ostolop
 * Date: 18-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class MageTabParser {
    private static final Logger log = LoggerFactory.getLogger(MageTabParser.class);    

    public MageTabDocument parseIDF(Reader reader) throws IOException {
        ExcelCSVParser csvp = new ExcelCSVParser(reader);
        csvp.changeDelimiter('\t');
        csvp.setCommentStart("#");

        MageTabDocument mtd = new MageTabDocument();

        String[] line;
        while((line = csvp.getLine()) != null) {
            addLine(mtd, line);
        }

        return mtd;
    }

    public MageTabDocument parseSDRF(Reader reader) throws IOException {
        ExcelCSVParser ecsvp = new ExcelCSVParser(reader);
        ecsvp.setCommentStart("#");
        ecsvp.changeDelimiter('\t');

        MageTabDocument mtd = new MageTabDocument();
        String[] head = ecsvp.getLine();

        String[] line;
        while((line = ecsvp.getLine()) != null) {
            for(int i = 0; i < head.length; i++) {
                if (line.length == 0 ) continue;
                if (line.length <= i) {
                    log.warn("Line " + ecsvp.lastLineNumber() + " has " + line.length + " fields, expecting " + head.length);
                    continue;
                }
                if (line[i] != null && line[i] != "")
                    mtd.addToField(head[i], line[i]);
            }
        }

        return mtd;
    }

    private void addLine(MageTabDocument mtd, String[] fields) {
        if (fields.length <= 1 || fields[0].length() == 0 || fields[1].length() == 0)
            return;

        String[] fieldsX = new String[fields.length - 1];
        System.arraycopy(fields, 1, fieldsX, 0, fields.length - 1);

        mtd.setField(fields[0], Arrays.asList(fieldsX));
        mtd.addToAllFieldValues(fieldsX);
    }
}
