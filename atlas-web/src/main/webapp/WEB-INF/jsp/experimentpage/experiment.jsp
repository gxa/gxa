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

<u:htmlTemplate file="look/experimentPage.head.html" />

<jsp:include page="../includes/query-includes.jsp"/>

<!--[if IE]><script type="text/javascript" src="${pageContext.request.contextPath}/scripts/excanvas.min.js"></script><![endif]-->
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot-0.6.atlas.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.boxplot.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.selection.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.selectboxes.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-ui-1.7.2.atlas.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tmpl.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/experiment.js"></script>
<%--<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jqModal.js"></script>--%>

<%--<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/lightbox2.04/js/prototype.js"></script>--%>
<%--<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/lightbox2.04/js/scriptaculous.js?load=effects,builder"></script>--%>
<%--<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/lightbox2.04/js/lightbox.js"></script>--%>


<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.easing.1.3.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.slideviewer.1.2.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery-lightbox-0.5/js/jquery.lightbox-0.5.js"></script>


<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/listview.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/jquery-ui-1.7.2.atlas.css" type="text/css"/>
<link rel="stylesheet" href="${pageContext.request.contextPath}/slideViewer.css" text="text/css"/>
<%--<link rel="stylesheet" href="${pageContext.request.contextPath}/jqModal.css" text="text/css"/>--%>
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

    #searchForm td {
        vertical-align: middle;
    }

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

</style>

<script type="text/javascript">
$(function() {
	$("a.lightbox").lightBox(); // Select all links with lightbox class
});
</script>

<script id="source" language="javascript" type="text/javascript">
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
        initPlotTabs();

        function initPlotTabs() {
            var sel = $(".btabs .sel")[0];

            if (!sel) {
                sel = $(".btabs #tab_" + plotType)[0];
                $(sel).addClass("sel");
            }

            $(".btabs li").each(function() {
                $(this).bind("click", function() {
                    if (sel == this) {
                        return;
                    }

                    if (sel) {
                        $(sel).removeClass("sel");
                    }

                    $(this).addClass("sel");
                    sel = this;
                    changePlotType(this.id.split("_")[1]);
                });
            });
        }

        arrayDesign = '${arrayDesign}';

        //atlas.initGeneBox($('#geneFilter'));

        bindTableFromJson(experiment.accession, '', '', '', '');

        $('#expressionListFilterForm').bind('submit', function(){
            //$('#geneFilter').val() - does not work with autocomplete
            bindTableFromJson(experiment.accession, $('#geneFilter').val(), '', $('#efvFilter').val(), $('#updownFilter').val());
            return false;
        });

    });
</script>


${atlasProperties.htmlBodyStart}


<div class="contents" id="contents">
    <div id="ae_pagecontainer">

        <jsp:include page="experiment-header.jsp"/>

        <div id="result_cont" style="margin-top:20px; margin-bottom:10px;">

            <table id="twocol" style="margin-top:5px">
                <tr>
                    <td style="padding:0px">
                        <div class="header"
                             style="padding-bottom: 10px; padding-left:45px;margin-bottom:5px;padding-top:4px">
                            <div id="EFpagination" class="pagination_ie">
                            </div>
                        </div>

                        <div style="position:relative;width:100%">
                            <table cellpadding="0" cellspacing="0" style="padding:0px;">
                                <tr>
                                    <td style="padding:0px;width:500px">
                                        <div class="bigplot" id="plot"
                                             style="width:500px;height:150px;padding:0px;background:url('${pageContext.request.contextPath}/images/indicator.gif'); background-repeat:no-repeat; background-position:center; "></div>
                                        <div id="plot_thm"
                                             style="border:thin; height: 120px;padding:0px"></div>
                                    </td>
                                    <td align="left" style="padding:0px;width:150px;" valign="top">
                                        <div id="legend"
                                             style="position:relative;top:-10px;text-align:left"></div>
                                        <div id="zoomControls"
                                             style="position:absolute;top:150px;left:525px"></div>
                                    </td>
                                    <td style="padding-left:10px">
                                    </td>
                                </tr>
                            </table>
                            <div class="btabs" style="width:650px">
                                <ul>
                                    <li id="tab_box">box plot</li>
                                    <li id="tab_large">line plot</li>
                                </ul>
                            </div>
                        </div>
                    </td>

                </tr>
            </table>
        </div>

    <script id="expressionValueTableRowTemplate1" type="text/x-jquery-tmpl">
    <tr>
        <td class="padded"><a onclick="addGeneToPlot(\${geneId},'\${geneIdentifier}','\${geneName}','\${rawef}','\${de}');return false;">
            <img border="0" src="images/iconf.png"/></a></td>
        <td class="padded genename">
            <a href="${pageContext.request.contextPath}/gene/\${geneIdentifier}" alt="${gene}">\${gene}</a>
            <div class="gtooltip">
                <div class="genename"><b>${row.gene.hilitGeneName}</b> (<c:forEach items="${atlasProperties.geneAutocompleteNameFields}" var="prop"><c:if test="${!empty row.gene.geneProperties[prop]}">${row.gene.hilitGeneProperties[prop]}, </c:if></c:forEach>${row.gene.geneIdentifier})</div>
                <c:forEach items="${atlasProperties.geneTooltipFields}" var="prop">
                    <c:if test="${!empty row.gene.geneProperties[prop]}"><div><b>${f:escapeXml(atlasProperties.curatedGeneProperties[prop])}:</b> ${row.gene.hilitGeneProperties[prop]}</div></c:if>
                </c:forEach>
            </div>
        </td>
        <td class="padded">\${de}</td>
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
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">&nbsp;</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Gene</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Design Element</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Experimental Factor</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">Factor Value</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">UP/DOWN</th>

                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">T-Statistic</th>
                <th align="left" class="padded" style="border-bottom:1px solid #CDCDCD">P-Value</th>
        </tr>

        <tr>
            <td class="padded">&nbsp;</td>
            <td class="padded"><input type="text" class="value" id="geneFilter" style="width:100%;" value="${gid}" /></td>
            <td class="padded">&nbsp;</td>
            <td class="padded" colspan="2">
                <select id="efvFilter" style="width:100%;">
                    <option value="">Choose factor value</option>
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
                <select id="updownFilter" style="width:100%;"><option value="UP_DOWN">up/down</option><option value="UP">up</option><option value="DOWN">down</option><option value="NON_D_E">non d.e.</option></select>
            </td>
            <td class="padded" colspan="2">
                <input type="submit" value="SEARCH"/>
                <!--
                <input type="button" value="show all"/>
                -->
            </td>
        </tr>

        <tbody id="expressionTableBody">
        </tbody>
    </table>
    </form>
    </div>


    </div>
    <!-- /id="ae_pagecontainer" -->
</div>
<!-- /id="contents" -->

<u:htmlTemplate file="look/footer.html" />
</body></html>
