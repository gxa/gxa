<%@ page import="org.json.JSONObject"%>
<%@ page import="org.json.JSONException" %>
<%@ page import="ae3.service.AtlasPlotter" %>
<%@ page import="org.slf4j.Logger" %>
<%@ page import="org.slf4j.LoggerFactory" %>
<%@ page language="java" contentType="application/json; charset=UTF-8"%>
<%
    final Logger log = LoggerFactory.getLogger("plot.jsp");

    String gid = request.getParameter("gid");
    String eid = request.getParameter("eid");
    String ef="default";

    if(request.getParameter("ef") != null)
        ef=request.getParameter("ef");

    try {
        JSONObject jsonString = AtlasPlotter
                .instance()
                .getGeneInExpPlotData(
                        request.getParameter("gid"),
                        request.getParameter("eid"),
                        ef);

        if (jsonString != null) {
            JSONObject options = new JSONObject(
                           "{  xaxis: {    ticks: 0 }, " +
                            " legend: {     show: true, " +
                            "           position: 'sw', " +
                            "          container: '#" + eid + "_" + gid + "_legend', " +
                            "       extContainer: '#" + eid + "_" + gid + "_legend_ext', " +
                            "          noColumns: 1 }," +
                            "   grid: {  " +
                            "    backgroundColor: '#fafafa',	" +
                            "      autoHighlight: false, " +
                            "          hoverable: true, " +
                            "          clickable: true, " +
                            "        borderWidth: 1}," +
                            " selection: {  mode: 'x' } }");

            jsonString.put("options", options);
            response.setContentType("text/javascript");
            response.getWriter().print(jsonString);
        }
    } catch (JSONException e) {
        log.error("Error constructing JSON when plotting gene " + gid + ", experiment " + eid + ", ef " + ef, e);
    }
%>