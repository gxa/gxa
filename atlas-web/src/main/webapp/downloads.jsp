<%@ page language="java" contentType="text/html; charset=ISO-8859-1"    pageEncoding="ISO-8859-1"%>
<%@ page import="java.util.Map" %>
<%@ page import="ae3.service.Download" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
<%
    Map<Integer, Download> downloads = ArrayExpressSearchService.instance().getDownloadService().getDownloads(session.getId());
	request.setAttribute("downloads", downloads);
%>

<script type="text/javascript" src="scripts/jquery-1.3.2.min.js"></script>
<script type="text/javascript" src="scripts/jquery.progressbar.js"></script>
<link rel="stylesheet" href="atlas.css" type="text/css" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<link rel="stylesheet" href="geneView.css" type="text/css" />

<script type="text/javascript">
	$(document).ready(function() {
	    var empty=true;
		$(".progressBar").each(function(){
			$(this).progressBar();
			empty = false;
		});
		
		if(!empty)
			setTimeout("updateProgress()",1000);
		
	});
	
	function updateProgress(){
		$.getJSON("downloadProgress.jsp",
        function(data){
          $.each(data.results, function(i, result) {
          		$("#query"+i).progressBar(result.progress)
          		if(result.progress == 100){
                      $("#nodl" + i).hide();
                      $("#dl" + i).show();
          		}          							
          	});
        });
		
		setTimeout("updateProgress()",1000);
	}
	
</script>
<title>Atlas Downloads</title>
</head>
<body>
    <div id="ae_pagecontainer" style="font-size: 9pt;">
        <table style="position:absolute; top:5px;border-bottom:1px solid #DEDEDE;width:100%;height:30px">
            <tr>
                <td align="left" valign="bottom" width="55" style="padding-right:10px;">
                    <img border="0" width="55" src="images/atlas-logo.png" alt="Gene Expression Atlas"/>
                </td>
            </tr>
        </table>
        <c:choose>
            <c:when test="${!empty downloads}">
                <div style="position: relative; padding-left: 3px; top: 35px;" class="header"> List of your current downloads </div>
                <table id="squery" style="position: relative; top: 40px;">
                    <thead>
                        <th class="padded header">ID</th>
                        <th class="padded header">Query</th>
                        <th class="padded header">Download Progress</th>
                        <th class="padded header">File</th>
                    </thead>
                    <c:forEach items="${downloads}" var="download" varStatus="i">
                        <tr>
                            <td class="padded">${i.index+1}</td>
                            <td class="padded"><c:out value="${download.value.query}"></c:out> </td>
                            <td class="padded"><span class="progressBar" id="query${download.key}"><c:out value="${download.value.progress}"></c:out></span></td>
                            <td class="padded">
                                <span id="nodl${download.key}"><img src="images/indicator.gif" alt="please wait for data export to complete..."/></span>
                                <a id="dl${download.key}" style="display:none" href="listviewdownload/${download.value.outputFile.name}">Get file</a></td>
                        </tr>

                    </c:forEach>
                </table>
            </c:when>

            <c:otherwise>
                <div style="font-weight: bold; text-align: center; position: relative; top: 50px;">No current downloads available. </div>
            </c:otherwise>
        </c:choose>
    </div>
</body>
</html>
