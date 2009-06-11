<%@page import="org.json.JSONStringer"%>
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
    String plotType = "full";
    String ef="default";
    String efv="";
    String updn="up";
    String gplotIds="";

    if(request.getParameter("plot") != null)
    	plotType=request.getParameter("plot");
    
    if(request.getParameter("ef") != null && !request.getParameter("ef").equals(""))
        ef=request.getParameter("ef");
    
    if(request.getParameter("efv") != null)
        efv=request.getParameter("efv");
    
    if(request.getParameter("updn") != null)
        updn=request.getParameter("updn");
    
    if(request.getParameter("gplotIds") != null)
    	gplotIds=request.getParameter("gplotIds");
    

    try {
        JSONObject jsonString = AtlasPlotter.instance().getGeneInExpPlotData(request.getParameter("gid"),request.getParameter("eid"),ef,efv,plotType,gplotIds);

        if (jsonString != null) {
            
        	if(plotType.equals("bar")){
        	
        	JSONObject options = new JSONObject(
                           "{  xaxis: {  ticks: 0, autoscaleMargin: 0.05 }, " +
                            " legend: {     show: true, " +
                            "           position: 'sw', " +
                            "          container: '#" + eid + "_" + gid + "_legend', " +
                           // "       extContainer: '#" + eid + "_" + gid + "_legend_ext', " +
                            "          noColumns: 1 }," +
                            "   grid: {  " +
                            "    backgroundColor: '#fafafa',	" +
                            "      autoHighlight: false, " +
                            "          hoverable: true, " +
                            "          clickable: true, " +
                            "        borderWidth: 1}," +
                            " selection: {  mode: 'x' } }");
        		jsonString.put("options", options);
        	}
        	else if(plotType.equals("thumb")){
        		String color = updn.equals("UP") ? "#FE2E2E" : "#2E2EFE";
        		JSONObject options = new JSONObject(
                        "{  xaxis: {    ticks: 0, autoscaleMargin: 0.05 }, " +
                         "  yaxis: {    ticks: 0 }, "+	
                         "  colors: ['#edc240'],    "+
                         " legend: {     show: false }," +
                         "   grid: {  " +
                         "    backgroundColor: '#F0FFFF',	" +
                         "      autoHighlight: false, " +
                         "          hoverable: true, " +
                         "          clickable: true, " +
                         "			markings: [{ xaxis: { from: "+jsonString.get("startMarkIndex")+", to: "+jsonString.get("endMarkIndex")+" },"+
                        	 							" color: '"+"#F5F5DC"+"' }],"+
                         "        borderWidth: 1}," +
                         " selection: {  mode: 'x' } }");
        		jsonString.put("options", options);
 
        	}else if(plotType.equals("large")){
        		JSONObject options = new JSONObject(
                        "{  xaxis: { ticks:0, autoscaleMargin: 0.05    }, " +
                         "  yaxis: { ticks:3    }, " +
                         "  points: { show:true, fill:false    }, " +
                         "  lines: { show:true, steps:true    }, " +
                         " legend: {     show: true," +
                         "          	 container: '#legend', " +
                         "             labelFormatter:TEST}, " +
                         "   grid: {  " +
                         "    backgroundColor: '#fafafa',	" +
                         "      autoHighlight: true, " +
                         "          hoverable: true, " +
                         "          clickable: true, " +
                         "			markings: ["+jsonString.getString("markings")+"],"+
                         "        borderWidth: 0}," +
                         " selection: {  mode: 'x' } }");
     		jsonString.put("options", options);
     		

        	}
            
            response.setContentType("text/javascript");
            response.getWriter().print(jsonString);
        }
    } catch (JSONException e) {
        log.error("Error constructing JSON when plotting gene " + gid + ", experiment " + eid + ", ef " + ef, e);
    }
%>