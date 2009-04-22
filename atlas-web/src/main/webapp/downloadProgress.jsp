<%@page import = "java.util.Date" session="true"%>
<%@ page import="org.json.JSONArray"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="org.json.JSONException" %>
<%@page import="java.util.ArrayList"%>
<%@page import="ae3.service.DownloadService"%>
<%@page import="ae3.service.Download"%>
<%@ page language="java" contentType="application/json; charset=UTF-8"%>
<% 
ArrayList<Download> downloads = DownloadService.getDownloads(session.getId());
JSONObject result = new JSONObject();		
if(downloads != null){
	request.setAttribute("downloads",downloads);
	JSONArray progress = new JSONArray();
	JSONObject dn = new JSONObject();
	for(Download download:downloads){
		dn = new JSONObject();
		dn.put("progress",download.getProgress());
		progress.put(dn);
	}	
	result.put("results",progress);
}
response.setContentType("text/javascript");
response.getWriter().print(result);
%>