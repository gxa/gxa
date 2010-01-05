<%@ page import="ae3.service.AtlasDownloadService" %>
<%@ page import="ae3.service.Download" %>
<%@ page import="org.json.JSONObject" %>
<%@ page import="uk.ac.ebi.gxa.web.Atlas" %>
<%@ page import="java.util.Map" %>
<%@ page language="java" contentType="application/json; charset=UTF-8" %>
<%
    AtlasDownloadService downloadsService = (AtlasDownloadService) application.getAttribute(Atlas.DOWNLOAD_SERVICE.key());
    Map<Integer, Download> downloads = downloadsService.getDownloads(session.getId());
    JSONObject result = new JSONObject();

    if (downloads != null) {
        request.setAttribute("downloads", downloads);
        JSONObject progress = new JSONObject();
        JSONObject dn;
        for (Map.Entry<Integer, Download> download : downloads.entrySet()) {
            dn = new JSONObject();
            dn.put("progress", download.getValue().getProgress());
            progress.put(download.getKey().toString(), dn);
        }
        result.put("results", progress);
    }

    out.print(result);
%>