<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@page import="java.net.URLEncoder" %>

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

<u:htmlTemplate file="look/experimentPage.head.html" />

<jsp:include page="../includes/query-includes.jsp"/>

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.transform-0.9.0pre.js"></script>
<!--[if IE]><script type="text/javascript" src="${pageContext.request.contextPath}/scripts/excanvas.min.js"></script><![endif]-->
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot-0.6.atlas.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.headers.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.boxplot.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.scroll.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.selection.js"></script>

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.selectboxes.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-ui-1.7.2.atlas.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tmpl.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/experiment.js"></script>

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.easing.1.3.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.slideviewer.1.2.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-lightbox-0.5/js/jquery.lightbox-0.5.js"></script>


<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/listview.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/jquery-ui-1.7.2.atlas.css" type="text/css"/>
<link rel="stylesheet" type="text/css" href="${pageContext.request.contextPath}/scripts/jquery-lightbox-0.5/css/jquery.lightbox-0.5.css" media="screen" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css" />


<style type="text/css">
    .ui-tabs .ui-tabs-hide {
        display: none;
    }

    .sample_attr_values {
        display: none;
    }

    /* bottom tabs for chart type selection */

    .btabs {
        width: 100%;
        height: 20px;
        border-top: 1px solid #006666;
        padding-left: 5px;
        background-color: #edf6f5;
    }

    .btabs ul {
        clear: left;
        float: left;
        list-style: none;
        margin: 0;
        padding: 0;
        position: relative;
        text-align: center;
    }

    .btabs ul li {
        float: left;
        border: 1px solid #006666;
        border-top-width: 0;
        margin: 0 0.2em 0 0;
        color:  #006666;
        padding: 2px 5px;
        cursor: pointer;
    }

    .btabs .sel {
        position: relative;
		top: -1px;
		background: white;
    }


    /* EF pagination */

    .pagination_ie {
        float:left;
        width:100%;
        padding:0;
        margin:0;
    }

    .pagination_ie div {
        float:left;
        padding:1px 2px;
        margin:2px 3px;
        border:1px solid #066;
        font-weight: bold;
        font-size: 80%;
    }
    
    .pagination_ie a, .pagination_ie a:hover, .pagination_ie a:link, .pagination_ie a:visited {
        border: none;
        padding:0;
        margin:0;
    }
</style>

<script type="text/javascript">
$(function() {
	$("a.lightbox").lightBox(); // Select all links with lightbox class
});
</script>

<script id="source" type="text/javascript">
    currentEF = '${u:escapeJS(ef)}';
    experimentEFs = [<c:forEach var="ef" varStatus="s" items="${exp.experimentFactors}">'${u:escapeJS(ef)}'<c:if test="${!s.last}">,</c:if></c:forEach>];
    experiment = { id: '${exp.id}', accession: '${u:escapeJS(exp.accession)}' };

    <c:forEach var="char" varStatus="s" items="${exp.sampleCharacteristics}">curatedSCs['${u:escapeJS(char)}'] = '${u:escapeJS(atlasProperties.curatedEfs[char])}';</c:forEach>
    <c:forEach var="ef" varStatus="s" items="${exp.experimentFactors}">curatedEFs['${u:escapeJS(ef)}'] = '${u:escapeJS(atlasProperties.curatedEfs[ef])}';</c:forEach>

    $(document).ready(function()
    {
        addGeneToolTips();
        bindGeneMenus();
        bindSampleAttrsSelector();

        var plotType= "box";
        initPlotTabs({
            selected: plotType,
            onchange: function(tabId) {
                changePlotType(tabId);
            }
        });

        function initPlotTabs(opts) {
            var tabs = {
                curr: null,

                select: function(tabId) {
                    if (!tabId) {
                        var sel =  $(".btabs .sel")[0];
                        if (sel) {
                            tabId = sel.id.split("_")[1];
                        }
                    }

                    if (this.curr == tabId) {
                        return;
                    }

                    if (this.curr) {
                        this.tabEl(this.curr).removeClass("sel");
                        this.tabContentEl(this.curr).hide();
                    }

                    this.tabEl(tabId).addClass("sel");
                    this.tabContentEl(tabId).show();
  
                    if (this.curr && opts.onchange) {
                        opts.onchange.call(this, tabId);
                    }

                    this.curr = tabId;
                },

                tabEl: function(tabId) {
                    return $(".btabs #tab_" + tabId);
                },

                tabContentEl: function(tabId) {
                    return $("#tab_content_" + tabId);
                }

            };

            tabs.select(opts.selected);

            $(".btabs li").each(function() {
                $(this).bind("click", function() {
                    tabs.select(this.id.split("_")[1]);
                });
            });

        }

        arrayDesign = '${arrayDesign}';
        filteredQuery();

        $('#expressionListFilterForm').bind('submit', function(){
            //$('#geneFilter').val() - does not work with autocomplete
            filteredQuery();
            return false;
        });

        $('#efvFilter').change(function(){
            filteredQuery();
        });

        $('#updownFilter').change(function(){
           filteredQuery();
        });
    });
</script>


${atlasProperties.htmlBodyStart}


<div class="contents" id="contents">
    <div id="ae_pagecontainer">

        <jsp:include page="../includes/atlas-header.jsp"/>

        <div class="column-container exp-page">
            <div class="left-column">

                <span class="sectionHeader" style="vertical-align: baseline">${exp.description}</span>

                <p>
                    <c:import url="../includes/apilinks.jsp">
                        <c:param name="apiUrl" value="experiment=${exp.accession}"/>
                        <c:param name="callback" value="calcApiLink"/>
                    </c:import>
                </p>

                <p>
                    ${exp.abstract}
                    <c:if test="${exp.pubmedId!=null}">(<a href="http://www.ncbi.nlm.nih.gov/pubmed/${exp.pubmedId}"
                        target="_blank">PubMed ${exp.pubmedId}</a>)</c:if>
                </p>

                <h3>Data shown for array design: ${arrayDesign}</h3>

                <div id="result_cont" style="margin-top:20px; margin-bottom:10px;">

                    <div>
                        <div id="EFpagination" class="pagination_ie"></div>
                        <div class="clean"></div>
                    </div>

                    <div style="position:relative;width:100%; margin-top:10px;">

                        <div id="tab_content_large" style="display:none">
                            <table cellpadding="0" cellspacing="0" style="padding:0;">
                                <tr>
                                    <td>

                                        <div id="plot_large" class="bigplot"
                                             style="width:700px;height:150px;padding:0;"></div>
                                        <div id="plot_overview_large"
                                             style="width:700px;height:60px;padding:0;"></div>
                                        <div id="legend_large"></div>
                                    </td>
                                    <td valign="bottom">
                                        <div id="zoomControls_large"></div>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <div id="tab_content_box" style="display:none">
                            <table cellpadding="0" cellspacing="0" style="padding:0;">
                                <tr>
                                    <td>
                                        <div id="plot_box" class="bigplot"
                                             style="width:700px;height:150px;padding:0;"></div>
                                        <div id="plot_overview_box"
                                             style="width:700px;height:60px;padding:0;"></div>
                                        <div id="legend_box"></div>
                                    </td>
                                    <td valign="bottom">
                                        <div id="zoomControls_box"></div>
                                    </td>
                                </tr>
                            </table>
                        </div>

                        <div class="btabs" style="width:100%;margin-top:10px">
                            <ul>
                                <li id="tab_box">box plot</li>
                                <li id="tab_large">line plot</li>
                            </ul>
                        </div>

                    </div>
                </div>

            </div>


            <div class="right-column">
                <jsp:include page="experiment-header.jsp"/>
            </div>

            <div class="clean">&nbsp;</div>

        </div>

<script id="expressionValueTableRowTemplate1" type="text/x-jquery-tmpl">
    <tr style="height:25px;">
        <td class="padded" style="text-align:center;" id="results_\${deId}">
            <a onclick="addDesignElementToPlot(\${deId}, \${geneId},'\${geneIdentifier}','\${geneName}','\${rawef}','\${de}');return false;">
                <img title="Add to plot" border="0" src="images/chart_line_add.png" style="margin:auto;cursor:pointer;"/></a>
        </td>
        <td class="padded genename">
            <a href="${pageContext.request.contextPath}/gene/\${geneIdentifier}" alt="${geneName}">\${geneName}</a>
        </td>
        <td class="padded">\${de}</td>
        <c:if test="${exp.typeString=='RNA_SEQ'}">
          <c:choose>
            <c:when test="${exp.platform=='A-ENST-1'}">
	      <td class="padded wiggle"><a target="_blank" href="http://www.ensembl.org/Homo_sapiens/Location/View?g=\${geneIdentifier};contigviewbottom=url:http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/wiggle/\${geneIdentifier}_${exp.accession}_\${ef_enc}_\${efv_enc}.wig">Genome View</a></td>
            </c:when>
            <c:otherwise>
	      <td class="padded wiggle"><a target="_blank" href="http://www.ensembl.org/Mus_musculus/Location/View?g=\${geneIdentifier};contigviewbottom=url:http://${pageContext.request.serverName}:${pageContext.request.serverPort}${pageContext.request.contextPath}/wiggle/\${geneIdentifier}_${exp.accession}_\${ef_enc}_\${efv_enc}.wig">Genome View</a></td>
            </c:otherwise>
          </c:choose>
        </c:if>
        <td class="padded">\${ef}</td>
        <td class="padded">\${efv}</td>
        <td class="padded">\${expr}</td>
        <td class="padded">{{html tstat}}</td>
        <td class="padded">{{html pvalue}}</td>
    </tr>
</script>

<script id="geneToolTipTemplate" type="text/x-jquery-tmpl">
    <div class="gtooltip">
      <div class="genename">
        <b>\${name}</b> \${identifiers}
       </div> 
        {{tmpl(properties) "#geneToolTipPropertyTemplate"}}
    </div>
</script>

<script id="geneToolTipPropertyTemplate" type="text/x-jquery-tmpl">
    <b>\${name}:</b> \${value}<br/>
</script>

    <div id="qryHeader" style="border:none; position:absolute; background-color:#F0F0F0; opacity:0.5; text-align:center;"></div>

    <div class="hrClear">
        <hr/>
    <form id="expressionListFilterForm" action="alert('error');">    
    <table width="100%" id="squery">
        <tr class="header">
                <th align="left" width="20" class="padded" style="border-bottom:1px solid #CDCDCD">&nbsp;</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Gene</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Design Element</th>
                <c:if test="${exp.typeString=='RNA_SEQ'}">
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Genome View</th>
                </c:if>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Experimental Factor</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Factor Value</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">UP/DOWN</th>

                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">T-Statistic</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">P-Value</th>
        </tr>

        <tr>
            <td class="padded" width="20">&nbsp;</td>
            <c:choose>
            <c:when test="${exp.typeString=='RNA_SEQ'}">
            <td class="padded" colspan="3"><input type="text" class="value" id="geneFilter" style="width:99%;" value="${gid}" /></td>
            </c:when>
            <c:otherwise>
            <td class="padded" colspan="2"><input type="text" class="value" id="geneFilter" style="width:99%;" value="${gid}" /></td>
            </c:otherwise>
            </c:choose>
            <td class="padded" colspan="2">
                <select id="efvFilter" style="width:100%;">
                    <option value="">All factor values</option>
                    <c:forEach var="EF" items="${exp.experimentFactors}">
                        <optgroup label="${f:escapeXml(atlasProperties.curatedEfs[EF])}">
                        <c:forEach var="EFV" items="${exp.factorValuesForEF[EF]}">
                            <option value='${EF}||"${u:escapeURL(EFV)}"'>${f:escapeXml(EFV)}</option>
                        </c:forEach>
                        </optgroup>
                    </c:forEach>
                </select>
            </td>
            <td class="padded">
                <select id="updownFilter" style="width:100%;"><option value="UP_DOWN">All expressions</option><option value="UP">up</option><option value="DOWN">down</option><option value="NON_D_E">non d.e.</option></select>
            </td>
            <td class="padded" colspan="2">
                <input type="submit" value="SEARCH" style="visibility:hidden"/>
                <!--
                <input type="button" value="show all"/>
                -->
            </td>
        </tr>

        <tbody id="expressionTableBody">
        </tbody>
    </table>

    <div class="errorMessage" id="divErrorMessage">No matching results found. See <a href="javascript:defaultQuery();">all</a> genes.</div>

    </form>
    </div>


    </div>
    <!-- /id="ae_pagecontainer" -->
</div>
<!-- /id="contents" -->

<u:htmlTemplate file="look/footer.html" />
</body></html>
