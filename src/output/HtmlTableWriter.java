package output;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * User: ostolop
 * Date: 12-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class HtmlTableWriter extends TableWriter {
    private int count = 0;
    private String last_expt = "";
    private String last_ef = "";
    private HttpServletResponse response;

    public HtmlTableWriter(PrintWriter pw, HttpServletResponse response) throws IOException {
        super(pw);

        this.response = response;
    }

    public void writeRow(Map expt) throws IOException {
        String this_expt = (String) expt.get("expt_acc");
        String this_ef   = (String) expt.get("ef");

        pw.println("<tr style=\"height:2.4em;" + (!last_expt.equals(this_expt) ? "background-color:lightgray" : "") + "\">" +
            "<td valign=\"top\">" + (!last_expt.equals(this_expt) ? this_expt : "")             + "</td>" +
            "<td valign=\"top\">" + (!last_expt.equals(this_expt) ? expt.get("expt_desc") : "") + "</td>" +
            "<td valign=\"top\">" + ((!last_ef.equals(this_ef) || !last_expt.equals(this_expt)) ? this_ef : "" )    + "</td>" +
            "<td valign=\"top\">" + expt.get("efv")       + "</td>" +
            "<td valign=\"top\">" + ((Integer) expt.get("updn") == 1 ? " up " : " down") + "</td>" +
            "<td valign=\"top\">" + "<i>p</i> (adj) = " + String.format("%.3g", (Float) expt.get("updn_pvaladj")) + "" + "</td>" +
            "<td valign=\"top\">" + expt.get("gene")      + "</td> " +
//            "<td valign=\"top\">" + String.format("%.3g", (Double) expt.get("rank")) + "</td>" +
        "</tr>");

        response.flushBuffer();

        last_expt = this_expt;
        last_ef   = this_ef;
    }
}
