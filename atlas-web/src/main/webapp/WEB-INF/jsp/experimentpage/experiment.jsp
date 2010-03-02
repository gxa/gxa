<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>

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

<jsp:include page="../includes/start_head.jsp"/>
Gene Expression Profile in Experiment ${exp.accession} - Gene Expression Atlas
<jsp:include page="../includes/end_head.jsp"/>

<script src="${pageContext.request.contextPath}/scripts/jquery-1.3.2.min.js" type="text/javascript"></script>

<jsp:include page="../includes/query-includes.jsp"/>
<!--[if IE]><script language="javascript" type="text/javascript" src="${pageContext.request.contextPath}/scripts/excanvas.min.js"></script><![endif]-->
<script language="javascript"
        type="text/javascript"
        src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.selectboxes.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-ui-1.7.2.atlas.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/experiment.js"></script>
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/listview.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/jquery-ui-1.7.2.atlas.css" type="text/css"/>

<style type="text/css">
    .ui-tabs .ui-tabs-hide {
        display: none;
    }

    .sample_attr_values {
        display: none;
    }

    #searchForm td {
        vertical-align: middle;
    }
</style>


<script id="source" language="javascript" type="text/javascript">
    genesToPlot = [
        <c:forEach items="${genes}" var="gene" varStatus="s">
        { id: ${gene.geneId}, identifier: '${gene.geneIdentifier}', name: '${u:escapeJS(gene.geneName)}' }<c:if test="${!s.last}">,</c:if>
        </c:forEach>
    ];

    currentEF = '${u:escapeJS(ef)}';

    experiment = { id: '${exp.id}', accession: '${u:escapeJS(exp.accession)}' };

    <c:forEach var="char" varStatus="s" items="${exp.sampleCharacteristics}">
    curatedSCs['${char}'] = '${u:getCurated(char)}';
    </c:forEach>
    <c:forEach var="ef" varStatus="s" items="${exp.experimentFactors}">
    curatedEFs['${ef}'] = '${u:getCurated(ef)}';
    </c:forEach>

    $(document).ready(function()
    {
        addGeneToolTips();
        plotBigPlot();
        drawZoomControls();
        bindPlotEvents();
        bindGeneMenus();
        bindSampleAttrsSelector();
    });
</script>


<jsp:include page='../includes/start_body_no_menus.jsp'/>

<div class="contents" id="contents">
    <div id="ae_pagecontainer">

        <table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
            <tr>
                <td align="left" valign="bottom" width="55"
                    style="padding-right: 10px;"><a href="${pageContext.request.contextPath}/"
                                                    title="Gene Expression Atlas Homepage"><img border="0" width="55"
                                                                                                src="${pageContext.request.contextPath}/images/atlas-logo.png"
                                                                                                alt="Gene Expression Atlas"/></a>
                </td>
                <td align="right" valign="bottom">
                    <a href="${pageContext.request.contextPath}/">home</a> |
                    <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
                    <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a>
                    <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a><span id="feedback_thanks"
                                                                                                 style="font-weight: bold; display: none">thanks!</span>
                    |
                    <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
                    <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
                    <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> <b>new</b> |
                    <a href="${pageContext.request.contextPath}/help">help</a></td>
            </tr>
        </table>


        <a href="http://www.ebi.ac.uk/arrayexpress/experiments/${exp.accession}" target="_blank"
           title="Experiment information and full data in ArrayExpress Archive" class="geneName"
           style="vertical-align: baseline">${exp.accession}</a>
        <span class="sectionHeader" style="vertical-align: baseline">${exp.description}</span>

        <div id="result_cont" style="margin-top:20px">

            <table id="twocol" style="margin-top:5px">
                <tr>
                    <td style="padding:0px">
                        <div class="header"
                             style="padding-bottom: 10px; padding-left:45px;margin-bottom:5px;padding-top:4px">
                            <div id="EFpagination" class="pagination_ie">
                                <c:forEach var="EF" items="${exp.experimentFactors}">
                                    <a id="efpage${EF}"
                                       onclick="redrawForEF('${EF}')"><fmt:message
                                            key="head.ef.${EF}"/></a>
                                </c:forEach>
                            </div>
                        </div>

                        <div style="position:relative;width:100%">
                            <table cellpadding="0" cellspacing="0" style="padding:0px;width:650px">
                                <tr>
                                    <td style="padding:0px;width:500px">
                                        <div class="bigplot" id="plot"
                                             style="width:500px;height:150px;padding:0px;background:url('${pageContext.request.contextPath}/images/indicator.gif'); background-repeat:no-repeat; background-position:center; "></div>
                                        <div id="plot_thm"
                                             style="border:thin; height: 120px;padding:0px"></div>
                                    </td>
                                    <td align="left" style="padding:0px;width:150px" valign="top">
                                        <div id="zoomControls"
                                             style="position:absolute;top:153px;right:120px"></div>
                                        <div id="legend"
                                             style="position:relative;top:-10px;text-align:left"></div>
                                    </td>
                                </tr>
                            </table>
                        </div>
                    </td>

                    <td rowspan="2">
                        <div>
                            <c:import url="../includes/apilinks.jsp">
                                <c:param name="apiUrl" value="experiment=${exp.accession}"/>
                                <c:param name="callback" value="calcApiLink"/>
                            </c:import>
                        </div>

                        <div id="gene_menu" style="width:500px">
                            <div><a href="#" style="font-size:12px">Display genes matching by name or
                                attribute</a></div>
                            <div>
                                <form id="searchForm" action="javascript:void()">
                                    <table>
                                        <tr>
                                            <td>
                                                <label for="geneInExp_qry" style="font-size:12px">Find genes</label>
                                            </td>
                                            <td>
                                                <input type="text" class="value" name="gval_0" id="geneInExp_qry"
                                                       style="width:200px">
                                            </td>
                                            <td>
                                                <input type="submit" value="Search">
                                            </td>
                                        </tr>
                                    </table>
                                </form>
                                <div id="qryHeader" style="padding-top: 10px;"></div>
                                <div id="qryResult" style="padding-top: 10px;"></div>
                            </div>

                            <div><a href="#" style="font-size:12px">Display genes with similar expression
                                profiles</a></div>
                            <div>
                                <form class="visinsimple" id="simForm" action="javascript:void()">
                                    <label for="simSelect" style="font-size:12px">Show ten genes similar
                                        (Pearson correlation) to</label>
                                    <select id="simSelect" style="font-size:12px"></select>
                                    <button type="submit">Search</button>
                                </form>

                                <div id="simHeader" style="padding-top: 10px;font-size:12px"></div>
                                <div id="simResult" style="padding-top: 10px;"></div>
                            </div>


                            <div><a href="#" style="font-size:12px">Choose from top ten differentially
                                expressed genes</a></div>
                            <div>
                                <div id="topGenes">
                                    <c:import url="gene-list.jsp"  />
                                </div>
                            </div>

                        </div>
                        <!--/gene_menu-->

                    </td>
                </tr>
                <tr valign="top">
                    <td valign="top">
                        <table width="600"
                               style="border:1px solid #5E9E9E;margin-top:30px;height:150px"
                               cellpadding="0"
                               cellspacing="0">
                            <tr>
                                <th style="background-color:#EDF6F5;padding:5px;border-right:0px solid #CDCDCD"
                                    class="header">Sample Attributes
                                </th>
                                <th style="background-color:#EDF6F5;padding:5px" class="header">Attribute Values</th>
                            </tr>
                            <tr>
                                <td width="200" style="border-bottom:0px solid #CDCDCD">
                                    <ul style="margin: 0px; padding: 5px">
                                        <c:forEach var="char" items="${exp.sampleCharacteristics}">
                                            <li style="list-style-type: none; padding-left: 0px"
                                                id="${char}_title"><a class="sample_attr_title" href="#">
                                                <fmt:message key="head.ef.${char}"/>
                                                <c:if test="${!empty exp.factorValuesForEF[char]}">&nbsp;(EF)</c:if></a>
                                            </li>
                                            <div id="${char}_values" class="sample_attr_values">
                                                <c:forEach var="value"
                                                           items="${exp.sampleCharacterisitcValues[char]}"
                                                           varStatus="r">
                                                    <a class="sample_attr_value" id="${char}_${r.count}"
                                                       onclick="highlightPoints('${char}','${f:escapeXml(u:escapeJS(value))}', false, this);return false;"
                                                       href="#">${f:escapeXml(value)}</a>
                                                    <br/>
                                                </c:forEach>
                                            </div>
                                        </c:forEach>
                                        <c:forEach var="EF" items="${exp.experimentFactors}">
                                            <c:if test="${empty exp.sampleCharacterisitcValues[EF]}">
                                                <li style="list-style-type: none; padding-left: 0px"
                                                    id="${EF}_title"><a class="sample_attr_title" href="#">
                                                    <fmt:message key="head.ef.${EF}"/>&nbsp;(EF)</a>
                                                </li>
                                                <div id="${EF}_values" class="sample_attr_values">
                                                    <c:forEach var="value"
                                                               items="${exp.factorValuesForEF[EF]}"
                                                               varStatus="r">
                                                        <a class="sample_attr_value" id="${EF}_${r.count}" href="#"
                                                           onclick="highlightPoints('${EF}','${f:escapeXml(u:escapeJS(value))}', true, this);return false;">${f:escapeXml(value)}</a>
                                                        <br/>
                                                    </c:forEach>
                                                </div>
                                            </c:if>
                                        </c:forEach>
                                    </ul>
                                </td>
                                <td style="padding:5px">
                                    <div id="display_attr_values" style="height:200px;overflow:auto">Select a sample
                                        attribute...
                                    </div>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </div>
    </div>
    <!-- /id="ae_pagecontainer" -->
</div>
<!-- /id="contents" -->

<jsp:include page='../includes/end_body.jsp'/>
