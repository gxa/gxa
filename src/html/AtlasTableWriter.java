package html;

import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.HashMap;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class AtlasTableWriter extends TableWriter {
    private int count = 0;
    private String last_expt = "";
    private String last_ef = "";

    public AtlasTableWriter(HttpServletResponse response) throws IOException {
        super(response);
    }

    public void writeRow(Object elt) throws IOException {
        HashMap expt = (HashMap) elt;

        String this_expt = (String) expt.get("expt_acc");
        String this_ef   = (String) expt.get("ef");

        pw.println("<tr style=\"height:2.4em;" + (!last_expt.equals(this_expt) ? "background-color:lightgray" : "") + "\">" +
            "<td valign=\"top\">" + (!last_expt.equals(this_expt) ? this_expt : "")             + "</td>" +
            "<td valign=\"top\">" + (!last_expt.equals(this_expt) ? expt.get("expt_desc") : "") + "</td>" +
            "<td valign=\"top\">" + ((!last_ef.equals(this_ef) || !last_expt.equals(this_expt)) ? this_ef : "" )    + "</td>" +
            "<td valign=\"top\">" + expt.get("efv")       + "</td>" +
            "<td valign=\"top\">" + expt.get("updn")      + "</td>" +
            "<td valign=\"top\">" + expt.get("gene")      + "</td> " +
            "<td valign=\"top\">" + String.format("%.3g", (Double) expt.get("rank")) + "</td>" +
        "</tr>");

        response.flushBuffer();

        last_expt = this_expt;
        last_ef   = this_ef;
    }
}
