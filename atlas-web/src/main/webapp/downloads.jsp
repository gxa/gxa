<%@page import = "java.util.Date" session="true"%>
<%@page import="java.util.ArrayList"%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@page import="ae3.service.DownloadService;"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<html>
<head>
<% ArrayList downloads = DownloadService.getDownloads(session.getId());
	request.setAttribute("downloads",downloads);
%>
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<script type="text/javascript" src="scripts/jquery.min.js"></script>
<script type="text/javascript" src="scripts/jquery.progressbar.js"></script>
<link rel="stylesheet" href="atlas.css" type="text/css" />
<link rel="stylesheet" href="listView.css" type="text/css" />
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
          $.each(data.results, function(i,result){
          						$("#query"+i).progressBar(result.progress)
          						if(result.progress == 100){
          							$("#btn_"+i).removeAttr("disabled");
          						}
          							
          						});						
        });
		
		setTimeout("updateProgress()",1000);
	}
	
	function getFile(i){
		$('<form action="export.jsp" method="post"><input type="hidden" name="qid" value="'+i+'" /></form>')
							.appendTo('body').submit().remove();
			            return false; 
	}
	
</script>
<title>Atlas Downloads</title>
</head>
<body>
<table style="position:absolute; top:5px;border-bottom:1px solid #DEDEDE;width:100%;height:30px">
        <tr>
            <td align="left" valign="bottom" width="55" style="padding-right:10px;">
                <img border="0" width="55" src="images/atlas-logo.png" alt="Atlas of Gene Expression"/>
            </td>
        </tr>
    </table>
<c:choose>
	<c:when test="${!empty downloads}">
		 <div style="position: relative; padding-left: 3px; top: 35px;" class="header"> List of your current downloads </div> 
	       <table id="grid" cellpadding="2" style="position: relative; top: 40px;">
	       	<thead>
	       		<th style="padding-left: 3px;">ID</th>
	       		<th style="padding-left: 3px;">Query</th>
	       		<th style="padding-left: 3px;">Download Progress</th>
	       		<th style="padding-left: 3px;">File</th>
	       	</thead>
	       		<c:forEach items="${downloads}" var="download" varStatus="i">
	        		<tr>
	        			<td style="padding-left: 3px;">${i.index+1}</td>
	        			<td style="padding-left: 3px;"><c:out value="${download.query}"></c:out> </td>
	        			<td style="padding-left: 3px;"><span class="progressBar" id="query${i.index}"><c:out value="${download.progress}"></c:out></span></td>
	        			<td style="padding-left: 3px;"><input id="btn_${i.index}" disabled="disabled" type="button" onclick="getFile(${i.index})" value="Get file" /></td>
	        		</tr>
	        		
	        	</c:forEach>
	       </table>
	</c:when>

<c:otherwise>
	<div style="font-weight: bold; text-align: center; position: relative; top: 50px;">No current downloads available. </div>
</c:otherwise>
</c:choose>     
        
</body>
</html>