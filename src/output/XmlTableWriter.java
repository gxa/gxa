package output;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * User: ostolop
 * Date: 13-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class XmlTableWriter extends TableWriter {

    public XmlTableWriter(PrintWriter pw) throws IOException {
        super(pw);
    }

    public void writeHeader() {
        pw.println("<?xml version='1.0'?>\n<atlas>\n");
    }

    public void writeRow(Object elt) throws IOException {
        HashMap expt = (HashMap) elt;

        pw.println("<atlas_result>" +
            "<expt_acc>"  + expt.get("expt_acc")      + "</expt_acc>" +
            "<expt_desc>" + expt.get("expt_desc")     + "</expt_desc>" +
            "<ef>"        + expt.get("ef")            + "</ef>" +
            "<efv>"       + expt.get("efv")           + "</efv>" +
            "<updn>"      + expt.get("updn")          + "</updn>" +
            "<gene>"      + expt.get("gene")          + "</gene>" +
            "<rank>"      + expt.get("rank")          + "</rank>" +
        "</atlas_result>");
    }

    public void writeFooter() {
        pw.println("</atlas>");
    }
}
