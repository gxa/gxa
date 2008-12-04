<%@page import="org.json.JSONArray"%>
<%@page import="org.json.JSONObject"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
	pageEncoding="ISO-8859-1"%>
<%
    	String gid = request.getParameter("gid");
    	String eid = request.getParameter("eid");
    	String ef="default";
    	if(request.getParameter("ef")!=null)
    		ef=request.getParameter("ef");
    	JSONObject jsonString = ae3.service.AtlasPlotter.instance().getGeneInExpPlotData(request.getParameter("gid"), request.getParameter("eid"), ef);
    	int fvs = ((JSONArray)jsonString.get("series")).length();
    	JSONObject options = new JSONObject("{ xaxis:{ticks:0}, " +
										  " legend:{show:true, position:\"sw\", container: \"#"+eid+"_"+gid+"_legend\", extContainer: \"#"+eid+"_"+gid+"_legend_ext\", noColumns:4  }," +
										  "	grid:{ backgroundColor: '#fafafa',	autoHighlight: false, hoverable: true, borderWidth: 1}," +
										 // "	bars:{fill:1}," +
										  " selection: { mode: \"x\" } }");

    	jsonString.put("options", options);
	    if (jsonString != null) {
	        response.setContentType("text/javascript");
	            response.getWriter().print(jsonString);System.out.println("DONE");
	        
	    } else {
	        System.err.println("Null");
	    }
%>

