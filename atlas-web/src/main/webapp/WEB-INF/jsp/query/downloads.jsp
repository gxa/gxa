<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%--
  ~ Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  ~
  ~
  ~ For further details of the Gene Expression Atlas project, including source code,
  ~ downloads and documentation, please see:
  ~
  ~ http://gxa.github.com/gxa
  --%>

<html>
    <head>

        <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js"></script>
        <script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.progressbar.js"></script>
        <link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css" />
        <link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css" />

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
                $.ajax({
                    url:"downloads?progress",
                    cache:false,
                    dataType:"json",
                    success: function(data) {
                        if(data.error) {
                            alert(data.error);
                            return;
                        }

                        for(var i in data) {
                            var p = data[i].progress;
                            $("#query"+i).progressBar(p);
                            if(p >= 100){
                                $("#nodl" + i).hide();
                                $("#dl" + i).show();
                            }
                        }
                        setTimeout("updateProgress()",5000);
                    }
                });
            }

        </script>
        <title>Atlas Downloads</title>
    </head>
    <body>
        <div id="ae_pagecontainer" style="font-size: 9pt;">
            <table style="position:absolute; top:5px;border-bottom:1px solid #DEDEDE;width:100%;height:30px">
                <tr>
                    <td align="left" valign="bottom" width="55" style="padding-right:10px;">
                        <img border="0" width="55" src="${pageContext.request.contextPath}/images/atlas-logo.png" alt="Gene Expression Atlas"/>
                    </td>
                </tr>
            </table>
            <c:choose>
                <c:when test="${!empty downloads}">
                    <div style="position: relative; padding-left: 3px; top: 35px;" class="header"> List of your current downloads </div>
                    <table id="squery" style="position: relative; top: 40px;">
                        <th class="padded header">ID</th>
                        <th class="padded header">Query</th>
                        <th class="padded header">Download Progress</th>
                        <th class="padded header">File</th>
                        <c:forEach items="${downloads}" var="download" varStatus="i">
                            <tr>
                                <td class="padded">${i.index+1}</td>
                                <td class="padded"><c:out value="${download.value.query}"></c:out> </td>
                                <td class="padded"><span class="progressBar" id="query${download.key}"><c:out value="${download.value.progress}"></c:out></span></td>
                                <td class="padded">
                                    <c:if test="${download.value.progress < 100}">
                                    <span id="nodl${download.key}"><img src="${pageContext.request.contextPath}/images/indicator.gif" alt="please wait for data export to complete..."/></span>
                                    </c:if>
                                    <a id="dl${download.key}" style="display:${download.value.progress >= 100 ? 'inline' : 'none'}" href="listviewdownload/${download.value.outputFile.name}">Get file</a></td>
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
