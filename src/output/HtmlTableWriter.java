package output;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.math.BigDecimal;

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

    public void writeHeader(String[] headers) {
        super.writeHeader(new String[] {"accession", "description", "factor", "factor value", "up/down", "p-value", "gene name", "gene id", "..."});
    }

    public void writeRow(Map expt) throws IOException {
        String this_expt = (String)  expt.get("experiment_accession");
        String this_ef   = (String)  expt.get("ef");
        String expt_desc      = (String)  expt.get("experiment_description");
        String efv       = (String)  expt.get("efv");
        Integer updn        = (Integer) expt.get("updn");
        Double updn_pvaladj = (Double) expt.get("updn_pvaladj");
        String gene_name = (String) expt.get("gene_name");
        String gene_identifier = (String) expt.get("gene_identifier");

//        pw.println("<tr style=\"height:2.4em;" + (!last_expt.equals(this_expt) ? "background-color:lightgray" : "") + "\">" +
//            "<td valign=\"top\">" + (!last_expt.equals(this_expt) ? this_expt : "")             + "</td>" +
//            "<td valign=\"top\">" + (!last_expt.equals(this_expt) ? expt_desc : "") + "</td>" +
//            "<td valign=\"top\">" + ((!last_ef.equals(this_ef) || !last_expt.equals(this_expt)) ? this_ef : "" )    + "</td>" +

        pw.println("<tr style=\"height:2.4em\">" +
                "<td valign=\"top\">" + this_expt + "</td>" +
                "<td valign=\"top\">" + expt_desc + "</td>" +
                "<td valign=\"top\">" + this_ef   + "</td>" +

            "<td valign=\"top\">" + efv       + "</td>" +
            "<td valign=\"top\">" + (updn == 1 ? " up " : " down") + "</td>" +
            "<td valign=\"top\">" + "<i>p</i> (adj) = " + String.format("%.3g", updn_pvaladj) + "" + "</td>" +
            "<td valign=\"top\">" + gene_name +  "</td>" +
            "<td valign=\"top\"><a target='_blank' href='http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query=" + gene_identifier + "&species=&displayInsitu=on&exp_query=" + this_expt + "'>"
                + gene_identifier + "</a></td> " +
            //"<td valign='top'>" +  expt.get("gene_hls") + "</td>" +
        "</tr>");

        response.flushBuffer();

        last_expt = this_expt;
        last_ef   = this_ef;
    }
}
