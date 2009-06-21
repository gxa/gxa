<%@ page import="org.json.JSONObject"%>
<%@ page import="ae3.service.Download"%>
<%@ page import="java.util.Map" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page language="java" contentType="application/json; charset=UTF-8"%>
<% 
    Map<Integer, Download> downloads = ArrayExpressSearchService.instance().getDownloadService().getDownloads(session.getId());
    JSONObject result = new JSONObject();

    if(downloads != null){
	    request.setAttribute("downloads",downloads);
    	JSONObject progress = new JSONObject();
    	JSONObject dn;
    	for(Map.Entry<Integer,Download> download : downloads.entrySet()){
    		dn = new JSONObject();
    		dn.put("progress", download.getValue().getProgress());
    		progress.put(download.getKey().toString(), dn);
    	}
    	result.put("results",progress);
    }

    out.print(result);
%>