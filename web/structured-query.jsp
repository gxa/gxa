<%String svnBuildString = "$Rev: 4866 $ $Date: 2008-06-20 11:57:28 +0100 (Fri, 20 Jun 2008) $";%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page import="ae3.service.AtlasStructuredQuery" %>
<%@ page import="ae3.util.StructuredQueryHelper" %>
<%@ page import="ae3.service.AtlasStructuredQueryResult" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<%
    request.setAttribute("service", ArrayExpressSearchService.instance());

    AtlasStructuredQuery atlasQuery = null;
    if(request.getParameterNames().hasMoreElements())
         atlasQuery = StructuredQueryHelper.parseRequest(request);
    request.setAttribute("query", atlasQuery);
%>

<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas Preview
<jsp:include page="end_head.jsp"></jsp:include>

    <link rel="stylesheet" href="blue/style.css" type="text/css" media="print, projection, screen" />
    <link rel="stylesheet" href="jquery.autocomplete.css" type="text/css"/>

    <script type="text/javascript" src="jquery.min.js"></script>
    <script type="text/javascript" src="jquery.cookie.js"></script>
    <script type="text/javascript" src="jquery.tablesorter.min.js"></script>
    <script type="text/javascript" src="jquery-impromptu.1.5.js"></script>
    <script type="text/javascript" src="jquery.autocomplete.js"></script>
    <script type="text/javascript" src="jquerydefaultvalue.js"></script>
    <script type="text/javascript" src="structured-query.js"></script>

    <script type="text/javascript">
        $("#atlasTable").tablesorter({
            sortAppend: [[6,0]],
            headers: {
                6: { sorter : 'digit' },
                7: { sorter : false },
                8: { sorter : false }
            }
        });

        $(document).ready(function()
            {
                $("#q_gene").defaultvalue("(all genes)");
                $("#q_gene").autocomplete("autocomplete.jsp", {
                        minChars:1,
                        matchCase: true,
                        matchSubset: false,
                        multiple: true,
                        multipleSeparator: " ",                    
                        extraParams: {type:"gene"},
                        formatItem:function(row) {return row[0] + " (" + row[1] + ")";}
                });
                initQuery();
            }
        );

        var options = {
            expressions : [
                    <c:forEach var="i" items="${service.geneExpressionOptions}">
                    [ '<c:out value="${u:escapeJS(i[0])}"/>', '<c:out value="${u:escapeJS(i[1])}"/>' ],
                    </c:forEach>
            ],
            factors : [
                <c:forEach var="i" items="${service.experimentalFactorOptions}">
                '<c:out value="${u:escapeJS(i)}"/>',
                </c:forEach>
            ],
            species : [
                <c:forEach var="i" items="${service.allAvailableAtlasSpecies}">
                '<c:out value="${u:escapeJS(i)}"/>',
                </c:forEach>
            ]
        };

        var lastquery;
        <c:if test="${!empty query}">
        lastquery = {
            gene : '<c:out value="${u:escapeJS(query.gene)}"/>',
            species : [<c:forEach var="i" items="${query.species}">'<c:out value="${u:escapeJS(i)}"/>',</c:forEach>],
            conditions : [
                <c:forEach var="c" items="${query.conditions}">
                { factor: '<c:out value="${u:escapeJS(c.factor)}"/>',
                  expression: '<c:out value="${u:escapeJS(c.expression)}"/>',
                  values: [<c:forEach var="v" items="${c.factorValues}">'<c:out value="${u:escapeJS(v)}"/>',</c:forEach>]
                },</c:forEach>
            ]
        };
        </c:if>
    </script>

    <style type="text/css">
        .label {
            font-size: 10px;
        }

        .atlasHelp {
            display: none;
        }

        div.value select, div.value input.value { width :200px; }
        div.value div.buttons { float: right; }
        div.value div.input { float: left; }
        div.value { width: 300px; margin-bottom:2px; }
        #conditions td { vertical-align: top; padding: 2px; }
        #conditions td.factorvalue { padding-bottom: 10px }
        #conditions td.andbuttons { vertical-align: bottom; padding-bottom:10px; }
        div.countup, div.countdn { width:50%;float:left;text-align:center; }
        a.countexp { float:right;display:block; }
        div.expref { clear:both;text-align:left;width:100%;background-color:#f0f0f0; }

    </style>

<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<jsp:include page="end_menu.jsp"></jsp:include>


<form name="atlasform" action="qrs" onsubmit="renumberAll();">
    Search for genes 
    <input type="text" name="gene" id="gene" style="width:150px" value=""/>

    in
    
    <table id="species">
        <tbody></tbody> 
    </table>
    
    which are 

    <table id="conditions">
        <tbody></tbody>
    </table>


        <input type="submit">        
</form>


<c:if test="${!empty query}">
    <div style="margin:0px auto;width:150px;text-align:center;clear:both" id="loading_display">Searching... <img src="indicator.gif" alt="Loading..."/></div>
    <%
        response.flushBuffer();
        AtlasStructuredQueryResult atlasResult = ArrayExpressSearchService.instance().doStructuredAtlasQuery(atlasQuery);
        request.setAttribute("result", atlasResult);
    %>
    <script type="text/javascript">$("#loading_display").hide()</script>

    <c:if test="${result.size > 0}">
    <table>
        <tr>
            <th colspan="2">Gene</th>
            <c:forEach var="c" items="${result.conditions}">
                <th style="padding-left:20px;">
                    <c:out value="${c.expression.description}" escapeXml="true"/> in <c:out value="${c.factor}" escapeXml="true"/>
                    <c:forEach var="v" varStatus="s" items="${c.factorValues}"><c:out value="${v}" escapeXml="true"/><c:if test="${!s.last}"> or </c:if></c:forEach>
                </th>
            </c:forEach>
        </tr>
        <c:forEach var="row" items="${result.results}">
            <tr>
                <td style="vertical-align:top">
                    <c:url var="urlGeneAnnotation" value="http://www.ebi.ac.uk/ebisearch/search.ebi">
                        <c:param name="db" value="genomes"/>
                        <c:param name="t" value="${row.gene.geneIdentifier}"/>
                    </c:url>
                    <a title="Show gene annotation" target="_blank" href="${urlGeneAnnotation}"><c:out value="${row.gene.geneIdentifier}" escapeXml="true"/></a>
                </td>
                <td style="vertical-align:top"><c:out value="${f:join(f:split(row.gene.geneName,';'), ' ')}" escapeXml="true"/></td>
                <c:forEach var="ud" items="${row.counters}">
                    <c:set var="efv">[<c:forEach var="v" items="${ud.condition.factorValues}" varStatus="s">'<c:out value="${u:escapeJS(v)}"/>'<c:if test="${!s.last}">,</c:if></c:forEach>]</c:set>
                    <td style="padding-left:20px;vertical-align:top">
                        <div class="countup" style="background-color:${ud.ups != 0 ? u:heatColor(ud.mpvUp, '100') : '#ffffff'}">
                            <c:if test="${ud.ups != 0}">
                                <a class="countexp" onclick="loadExperiments(this,'${u:escapeJS(row.gene.geneId)}','${u:escapeJS(ud.condition.factor)}',${efv},'1');">▶</a>
                                <c:out value="${ud.ups}"/>
                            </c:if>
                        </div>
                        <div class="countdn" style="background-color:${ud.downs != 0 ? u:heatColor(ud.mpvDn, '001') : '#ffffff'}">
                            <c:if test="${ud.ups != 0}">
                                <a class="countexp" onclick="loadExperiments(this,'${u:escapeJS(row.gene.geneId)}','${u:escapeJS(ud.condition.factor)}',${efv},'-1');">▶</a>
                                <c:out value="${ud.downs}"/>
                            </c:if>
                        </div>
                    </td>
                </c:forEach>
            </tr>
        </c:forEach>
    </table>
    </c:if>

    <jsp:include page="end_body.jsp"></jsp:include>
</c:if>

