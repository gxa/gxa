package html;

import java.util.HashMap;
import java.io.PrintWriter;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class TableWriter {
    public PrintWriter pw;

    public TableWriter(PrintWriter pw) {
        this.pw = pw;
    }

    public void writeHeader() {
        pw.println("<table class=\"sofT\" border=\"1\" cellpadding=\"2\">");
    }

    public void writeRow(Object elt) {
        pw.write("<tr><td>" + elt.toString() + "</td></tr>\n");
    }

    public void writeFooter() {
        pw.println("</table>");
    }
}
