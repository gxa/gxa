<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f"%>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
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

<c:set var="timeStart" value="${u:currentTime()}" />
<jsp:include page="../includes/start_head.jsp" />
Gene Expression Atlas Summary for ${atlasGene.geneName} (${atlasGene.geneSpecies}) - Gene Expression Atlas
<jsp:include page="../includes/end_head.jsp" />

<meta name="Description" content="${atlasGene.geneName} (${atlasGene.geneSpecies}) - Gene Expression Atlas Summary"/>
<meta name="Keywords" content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays" />

<jsp:include page="../includes/query-includes.jsp"/>
<!--[if IE]><script language="javascript" type="text/javascript" src="${pageContext.request.contextPath}/scripts/excanvas.min.js"></script><![endif]-->

<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquerydefaultvalue.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/feedback.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="${pageContext.request.contextPath}/scripts/common-query.js"></script>
<script language="javascript" type="text/javascript" src="${pageContext.request.contextPath}/scripts/jquery.flot.atlas.js"></script>

<link rel="stylesheet" href="${pageContext.request.contextPath}/atlas.css" type="text/css" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/geneView.css" type="text/css" />


<script type="text/javascript">

    function showThumbTooltip(x, y, contents) {
        if(contents != 'Mean'){
            $('<div id="tooltip" />').text(contents).css( {
                position: 'absolute',
                display: 'none',
                top: (y+5)+'px',
                left: (x+5)+'px',
                border: '1px solid #005555',
                margin: '0px',
                backgroundColor: '#EEF5F5'
            }).appendTo("body").fadeIn('fast');
        }
    }

    function drawPlot(jsonObj, plot_id){
        if(jsonObj.series){
            var legend_id = plot_id.replace("_plot", "_legend");
            jsonObj.options.legend.container = '#' + legend_id;
            var plot = $.plot($('#'+plot_id), jsonObj.series, jsonObj.options);

            var previousPoint = null;
            $('#'+plot_id).bind("plothover", function (event, pos, item) {
                if (item) {
                    if (previousPoint != item.datapoint) {
                        previousPoint = item.datapoint;
                        $("#tooltip").remove();
                        showThumbTooltip(item.pageX, item.pageY, item.series.label);
                    }
                }else {
                    $("#tooltip").remove();
                    previousPoint = null;
                }
            });
            return plot;
        }
        return null;
    }

    function drawPlots(){
        $(".plot").each(function(){
            var plot_id = this.id;
            var tokens = plot_id.split('_');
            var eid = tokens[0];
            var gid = tokens[1];
            $.ajax({
                type: "GET",
                url: atlas.homeUrl + "plot",
                data: { gid: gid, eid: eid, plot: 'bar' },
                dataType:"json",
                success: function(o){
                    if(o.error)
                        alert(o.error);
                    else
                        drawPlot(o,plot_id);
                },
                error: atlas.onAjaxError
            });
        });
    }

    function redrawPlotForFactor(eid,gid,ef,mark,efv){
        var plot_id = eid+"_"+gid+"_plot";
        $.ajax({
            type: "GET",
            url: atlas.homeUrl + "plot",
            data: { gid: gid, eid: eid, ef: ef, plot: 'bar' },
            dataType:"json",
            success: function(o){
                var plot = drawPlot(o,plot_id);
                if(mark){
                    markClicked(eid, gid, ef, efv, plot, o);
                }
            },
            error: atlas.onAjaxError
        });
        drawEFpagination(eid,gid,ef);
    }

    function drawEFpagination(eid,gid,currentEF,plotType) {
        var panelContent = [];

        $("#"+eid+"_EFpagination *").each(function(){
            var ef = $(this).attr("id");
            var ef_txt = $(this).html();
            if(ef == currentEF){
                panelContent.push("<span id='"+ef+"' class='current'>"+ef_txt+"</span>")
            }
            else{
                panelContent.push('<a id="'+ef+'" onclick="redrawPlotForFactor( \''+eid+'\',\''+gid+'\',\''+ef+'\',\''+plotType+'\',false)">'+ef_txt+'</a>');
            }
        });

        $("#"+eid+"_EFpagination").empty();
        $("#"+eid+"_EFpagination").html(panelContent.join(""));
    }


    function markClicked(eid,gid,ef,efv,plot,jsonObj) {

        var plot_id = eid+'_'+gid+'_plot';
        var allSeries = plot.getData();
        var series;
        var markColor;

        for (var i = 0; i < allSeries.length; ++i){
            if(allSeries[i].label){
                if(allSeries[i].label.toLowerCase()==efv.toLowerCase()){
                    series = allSeries[i];
                    markColor = series.color;
                    break;
                }
            }
        }

        if(series==null){
            return ;
        }

        var data = series.data;
        var xMin= data[0][0]
        var xMax= data[data.length-1][0]

        var overviewDiv = $('#'+plot_id+'_thm');
        if(allSeries.length>10 && data.length<5){
            if(overviewDiv.height()!=0){
                var overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series, jsonObj.options);

                overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
            }
            plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#FFFFCC' }]},
                xaxis: { min: xMin-10, max: xMax+10  }, yaxis: {labelWidth:40}
            }));
        }
        else{

            plot = $.plot($('#'+plot_id), jsonObj.series, $.extend(true, {}, jsonObj.options, {
                grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#FFFFCC' }]},
                yaxis: {labelWidth:40}
            }));
            if(overviewDiv.height() != 0) {
                var overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']}));

                overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
            }
        }
    }


    function reloadExps(){

        $('#ExperimentResult').load("${pageContext.request.contextPath}/geneExpList",{gid:${atlasGene.geneId},from:"1", to:"5"},drawPlots);
        $('#pagingSummary').empty();
        $(".heatmap_over").removeClass("heatmap_over");
        paginateExperiments();
        countExperiments();
	}

    function countExperiments(){
       $("#pagingSummary").text("${noAtlasExps} experiment${noAtlasExps>1?'s':''} showing differential expression");  
    }

    function paginateExperiments(){
         // Create pagination element
         <c:if test="${noAtlasExps > 5}">
        	$("#Pagination").pagination(${noAtlasExps}, {
				num_edge_entries: 2,
				num_display_entries: 5,
				items_per_page:5,
                callback: pageselectCallback
         	});
         </c:if>
    }

	function pageselectCallback(page_id){
		var fromPage = (page_id*5) +1;
		var toPage = (page_id*5) + 5;
		$('#ExperimentResult').load("${pageContext.request.contextPath}/geneExpList",{gid:${atlasGene.geneId},from:fromPage, to: toPage},drawPlots);
	}


	function FilterExps(el,fv,ef){

        $('#ExperimentResult').empty();

		$('#ExperimentResult').load("${pageContext.request.contextPath}/geneExpList",{gid:${atlasGene.geneId}, efv: fv, factor:ef},
							function(){
								for (var i = 0; i < exps.length; ++i){
									var eid = jQuery.trim(exps[i].id);
									redrawPlotForFactor(eid,'${atlasGene.geneId}',ef,true,fv);
								}
								$('#pagingSummary').text(exps.length+" experiment"+(exps.length>1?"s":'')+" showing differential expression in \""+ fv + "\"");
								var lnk = $("<a>Show all studies</a>").bind("click", reloadExps);
								$("#Pagination").empty().append(lnk);
							});


        old = $(".heatmap_over");
		old.removeClass("heatmap_over");
        old.addClass ("heatmap_row");
		el.className = "heatmap_over";
		window.scrollTo(0,0);
	}

    jQuery(document).ready(function()
    {
        countExperiments();
        paginateExperiments();
        $("#heatmap_tbl").tablesorter({
            headers: {2: {sorter: false}}});
        drawPlots();
    });
</script>


<link rel="stylesheet" href="${pageContext.request.contextPath}/blue/style.css" type="text/css" media="print, projection, screen" />
<link rel="stylesheet" href="${pageContext.request.contextPath}/structured-query.css" type="text/css" />
<jsp:include page='../includes/start_body_no_menus.jsp' />

<div class="contents" id="contents">
<div id="ae_pagecontainer">

<table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
    <tr>
        <td align="left" valign="bottom" width="55" style="padding-right:10px;">
            <a href="${pageContext.request.contextPath}/" title="Gene Expression Atlas Homepage"><img border="0" width="55" src="${pageContext.request.contextPath}/images/atlas-logo.png" alt="Gene Expression Atlas"/></a>
        </td>
        <td align="right" valign="bottom">
            <a href="${pageContext.request.contextPath}/">home</a> |
            <a href="${pageContext.request.contextPath}/help/AboutAtlas">about the project</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasFaq">faq</a> |
	    <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
	    <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
	    <a href="${pageContext.request.contextPath}/help/AtlasDasSource">das</a> |
            <a href="${pageContext.request.contextPath}/help/AtlasApis">api</a> <b>new</b> |
            <a href="${pageContext.request.contextPath}/help">help</a>
        </td>
    </tr>
</table>


<table width="100%" style="background-color: white">

    <tr>
        <td align="left" class="geneName">${atlasGene.geneName}</td>
        <td style="vertical-align: text-bottom">${atlasGene.geneSpecies}</td>
    </tr>

    <tr>
        <td colspan="2" align="left" style="padding-bottom:1em;padding-top:1em">
           ${f:escapeXml(atlasGene.geneDescription)}
        </td>
    </tr>


    <tr>
        <td class="geneAnnotHeader">Synonyms</td>
        <td align="left">${atlasGene.synonym}</td>
    </tr>

    <c:if test="${!empty atlasGene.orthologs}">
        <tr>
        </tr>
        <tr>
            <td class="geneAnnotHeader">Orthologs</td>

            <td align="left">
                <c:forEach var="ortholog" items="${atlasGene.orthoGenes}">
                    <a href="${u:GeneUrl(pageContext.request,ortholog.geneEnsembl)}" target="_self" title="Gene Atlas Data For ${ortholog.geneName} (${ortholog.geneSpecies})">${ortholog.geneName} (${ortholog.geneSpecies})</a>&nbsp;
                </c:forEach>
                (<a href="${pageContext.request.contextPath}/qrs?gprop_0=&gval_0=${atlasGene.orthologsIds}+${atlasGene.geneIdentifier}&fexp_0=UP_DOWN&fact_0=&specie_0=&fval_0=(all+conditions)&view=hm"
                    target="_self">Compare orthologs</a>)
            </td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.interProTerm}">
        <tr>
            <td></td>
            <td>

            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">InterPro Term</td>
            <td align="left">${atlasGene.shortInterProTerms}</td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.disease}">
        <tr>
            <td></td>
            <td>
            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">Diseases</td>
            <td align="left">${atlasGene.shortDiseases}</td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.goTerm}">
        <tr>
            <td></td>
            <td>

            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">GO Terms</td>
            <td align="left">${atlasGene.shortGOTerms}</td>
        </tr>
    </c:if>

    <c:if test="${!empty atlasGene.uniprotId}">
        <tr>
            <td></td>
            <td>

            </td>
        </tr>
        <tr>
            <td class="geneAnnotHeader">Uniprot</td>
            <td align="left">
                <c:forEach var="uniprot" items="${atlasGene.uniprotIds}">
                    <a href="http://www.uniprot.org/uniprot/${uniprot}" target="_blank">${uniprot}</a>&nbsp;
                </c:forEach>
            </td>
        </tr>
    </c:if>

    <tr>
        <td></td>
        <td></td>
    </tr>

    <tr>
        <td class="geneAnnotHeader">Search EB-eye</td>
        <td align="left">
            <a title="Show gene annotation" target="_blank"	href="http://www.ebi.ac.uk/ebisearch/search.ebi?db=genomes&t=${atlasGene.geneIdentifier}">
                ${atlasGene.geneIdentifier}
            </a>
        </td>

        <!-- td width="8%">&nbsp;</td-->

    </tr>

    <tr>
        <td colspan="2">

        </td>
    </tr>
</table>
<table cellspacing="0" cellpadding="0" style="margin-top:40px">


    <tr>
        <td>
            <table cellspacing="0" cellpadding="0" border="0">
                <tr>

                    <td valign="top" style="padding-right: 30px">
                        <table>
                            <tr>
                                <td class="sectionHeader">
                                    <div style="float:right;font:normal"><c:import url="../includes/apilinks.jsp"><c:param name="apiUrl" value="geneIs=${atlasGene.geneIdentifier}"/></c:import></div>
                                    Expression Summary
                                </td>
                            </tr>
                            <tr>
                                <td align="left" class="header">
                                    ${f:length(heatMapRows)} factor values, click each to filter
                                </td>

                            </tr>

                            <tr>
                                <td style="padding-top: 3px">

                                    <table class="heatmap" cellpadding="0" cellspacing="0" border="0" id ="heatmap_tbl">
                                        <thead>
                                            <tr style="height:26px;border-top:1px solid #CDCDCD">
                                                <th style="border: 1px solid #CDCDCD;padding: 1px 5px 1px 4px;">Factor Value</th>
                                                <th style="border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding-left:4px;padding-top:1px;padding-bottom:1px">Factor</th>
                                                <th style="border-top:1px solid #CDCDCD;border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD;padding: 1px 2px 1px 4px;">Up/Down</th>
                                            </tr>
                                            <tr>
                                                <td valign="top" height="30" align="center" colspan="3" style="border-bottom:1px solid #CDCDCD;background-color:white;border-left:1px solid #CDCDCD;border-right:1px solid #CDCDCD">
                                                    Legend: <img style="position:relative;top:6px" src="${pageContext.request.contextPath}/images/legend-sq.png" height="20"/> - number of studies the gene is <span style="color:red;font-weight:bold">up</span>/<span style="color:blue;font-weight:bold">down</span> in
                                                </td>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            <c:forEach var="row" items="${heatMapRows}" varStatus="i">
                                                <tr class="heatmap_row"

                                                    onclick="FilterExps(this,'${u:escapeJS(row.fv)}','${u:escapeJS(row.ef)}')"
                                                    title="${atlasGene.geneName}${row.text}">
                                                    <td nowrap="true" style="padding: 1px 5px 1px 4px;border-bottom:1px solid #CDCDCD; min-width: 100px;border-left:1px solid #CDCDCD;">
                                                        <span style="font-weight: bold">
                                                                ${row.shortFv}
                                                        </span>
                                                    </td>

                                                    <td nowrap="true" style="padding: 1px 5px 1px 4px;border-bottom:1px solid #CDCDCD;min-width: 80px;">
                                                        <fmt:message key="head.ef.${row.ef}"/>
                                                    </td>

                                                    <td class="acounter" align="right" style="border-bottom:1px solid #CDCDCD;border-right:1px solid #CDCDCD">
                                                        <c:choose>
                                                            <c:when test="${row.mixedCell}">
                                                                <div style="width:26px">
                                                                    <div class="sq"><div class="tri" style="border-right-color:${row.cellColor['dn']};border-top-color:${row.cellColor['up']}"></div>
                                                                        <div style="color:${row.cellText['dn']}" class="dnval">${row.count_dn}</div>
                                                                        <div style="color:${row.cellText['up']}" class="upval">${row.count_up}</div></div>
                                                                </div>
                                                            </c:when>
                                                            <c:otherwise>
                                                                <div style="width:26px;background-color:${row.cellColor[row.expr]};color:${row.cellText[row.expr]}">
                                                                    <div class="heatmap_cell"> <c:if test="${row.count_dn!=0}"> <c:out value="${row.count_dn}"/> </c:if>
                                                                        <c:if test="${row.count_up!=0}"> <c:out value="${row.count_up}"/> </c:if> </div>
                                                                </div>
                                                            </c:otherwise>
                                                        </c:choose>
                                                    </td>
                                                </tr>
                                            </c:forEach>
                                        </tbody>
                                    </table>

                                </td>
                            </tr>
                        </table>
                    </td>
                    <td valign="top" align="left">
                        <table align="left">
                            <tr>
                                <td id="expHeader_td" class="sectionHeader" style="vertical-align: top">Expression Profiles</td>
                                <td align="right">
                                    <div id="Pagination" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; "></div>
                                    <div id="Pagination1" class="pagination_ie" style="padding-bottom: 3px; padding-top: 3px; ">
                                    </div>
                                </td>
                            </tr>

                            <tr>
                                <td align="left"  valign="top" style="border-bottom:1px solid #CDCDCD;padding-bottom:5px">
                                    <div id="pagingSummary" class="header"></div>
                                </td>
                                <td align="right" style="border-bottom:1px solid #CDCDCD;padding-bottom:5px">
                                    <div id="expSelect"></div>
                                </td>

                            </tr>
                            <tr>
                                <td colspan="2">
                                    <div id="ExperimentResult">
                                        <c:import url="/geneExpList">
                                            <c:param name="gid" value="${atlasGene.geneId}" />
                                            <c:param name="from" value="1" />
                                            <c:param name="to" value="5" />
                                        </c:import>
                                    </div>
                                </td>
                            </tr>
                        </table>
                    </td>
                </tr>
            </table>
        </td>
    </tr>

</table>

<c:set var="timeFinish" value="${u:currentTime()}" />
<div align="center">Processing time: <c:out value="${(timeFinish - timeStart) / 1000.0}" /> secs.</div>

</div><!-- /id="ae_pagecontainer" -->
</div><!-- /id="contents" -->

<!-- end page contents here -->
<jsp:include page='../includes/end_body.jsp' />
