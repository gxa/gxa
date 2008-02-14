package output;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Map;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class TableWriter {
    protected PrintWriter pw;

    public TableWriter(PrintWriter pw) throws IOException {
        this.pw = pw;
    }

    public void writeHeader() {
        pw.println("<table class=\"sofT\" border=\"1\" cellpadding=\"2\">");
    }

    public void writeRow(Map elt) throws IOException {
        pw.write("<tr><td>" + elt.toString() + "</td></tr>\n");
    }

    public void writeFooter() {
        pw.println("</table>");
    }

    protected PrintWriter getWriter() {
        return pw;
    }
}
