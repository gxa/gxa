package html;

import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.io.PrintWriter;
import java.io.IOException;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class TableWriter {
    public HttpServletResponse response;
    protected PrintWriter pw;

    public TableWriter(HttpServletResponse response) throws IOException {
        this.response = response;
        this.pw = response.getWriter();
    }

    public void writeHeader() {
        pw.println("<table class=\"sofT\" border=\"1\" cellpadding=\"2\">");
    }

    public void writeRow(Object elt) throws IOException {
        pw.write("<tr><td>" + elt.toString() + "</td></tr>\n");
    }

    public void writeFooter() {
        pw.println("</table>");
    }

    protected PrintWriter getWriter() {
        return pw;
    }
}
