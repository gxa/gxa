<%@taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@page import="ae3.dao.AtlasDao" %>
<%@page import="ae3.model.AtlasExperiment" %>
<%@page import="ae3.service.ArrayExpressSearchService" %>
<%@page import="org.apache.commons.lang.StringUtils" %>
<%
    String expAcc = request.getParameter("eid");
    String geneId = request.getParameter("gid");
    String ef = request.getParameter("ef");
    AtlasDao dao = ArrayExpressSearchService.instance().getAtlasDao();
    if (expAcc != null && !"".equals(expAcc)) {
        AtlasExperiment exp = dao.getExperimentByAccession(expAcc);
        if(exp != null) {
            request.setAttribute("exp", exp);
            request.setAttribute("eid", exp.getDwExpId());

            if ((ef == null || "".equals(ef)) && (geneId != null)) {
                AtlasDao.AtlasGeneResult result = dao.getGeneById(StringUtils.split(geneId, ",")[0]);
                if(result.isFound()) {
                    ef = result.getGene().getHighestRankEF(exp.getDwExpId()).getFirst();
                    request.setAttribute("topRankEF", ef);
                }
            }
        }
    }
    
    request.setAttribute("gid", geneId);
    request.setAttribute("ef", ef);
    request.setAttribute("eAcc", expAcc);
%>

<jsp:include page="AtlasHomeUrl.jsp" />

<jsp:include page="start_head.jsp"/>
Gene Expression Profile in Experiment ${exp.dwExpAccession} - Gene Expression Atlas
<jsp:include page="end_head.jsp"/>

<script src="<%=request.getContextPath()%>/scripts/jquery-1.3.2.min.js" type="text/javascript"></script>



<!--[if IE]><script language="javascript" type="text/javascript" src="scripts/excanvas.min.js"></script><![endif]-->

<script language="javascript" type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.flot.atlas.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.pagination.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery-ui.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/plots.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/feedback.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.tablesorter.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.selectboxes.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery-ui-1.7.2.atlas.min.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/jquery.token.autocomplete.js"></script>
<script type="text/javascript" src="<%=request.getContextPath()%>/scripts/common-query.js"></script>
<link rel="stylesheet" href="<%=request.getContextPath()%>/structured-query.css" type="text/css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/atlas.css" type="text/css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/listview.css" type="text/css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/geneView.css" type="text/css"/>
<link rel="stylesheet" href="<%=request.getContextPath()%>/jquery-ui-1.7.2.atlas.css" type="text/css" />



<link rel="stylesheet" href="<%=request.getContextPath()%>/structured-query.css" type="text/css"/>
<style type="text/css">
    .ui-tabs .ui-tabs-hide {
        display: none;
    }

    .sample_attr_values {
        display: none;
    }

    #searchForm td { vertical-align: middle; }
</style>


<script id="source" language="javascript" type="text/javascript">
    var genesToPlot = new Array("${gid}");

    var sampleAttrs;
    var assayIds;
    var assay2samples;
    var characteristics;
    var charValues;
    var EFs;
    var plot;
    var prevSelections = new Array();
    var geneCounter = 0;
    var geneIndeces = new Array();
    geneIndeces.push(geneCounter);

    //AZ:2009-06-09:carry highlighted EF for removeGene
    var currentEF = '${ef}';


    $(document).ready(function()
    {

        $("#topGenes").load("<%=request.getContextPath()%>/expGenes", {eid:'${eid}',eAcc:'${exp.dwExpAccession}',gid:'${gid}',query:'top'}, function() {
            initPaging();
        });

        $("#accordion").accordion({
            collapsible: true,
            active:false,
            autoHeight: false

        });

        atlas.tokenizeGeneInput($("#geneInExp_qry"), '', '(all genes)');
        
        $("#gene_menu").accordion({
            collapsible: true,
            active: 2,
            autoHeight: false
        });

        //AZ:2009-06-26:only plot if gid is not supplyed
        if(window.location.href.indexOf('gid=')>0){
            plotBigPlot(genesToPlot.toString(), '${eid}', '${ef}', true, geneIndeces.toString());
        }

        $("button").click(function() {

            $("#drill").slideToggle("fast");

        });

        $("#zoom").click(function() {

            if ($("#zoom").text() == "(hide)")
                $("#zoom").text("(show)");
            else
                $("#zoom").text("(hide)");
            $("#plot_thm").toggle("fast");
        });

        $("#simForm").submit(function() {
            $("#simResult").empty();
            var name = $('select option:selected').text();
            $("#simHeader").html("<img src='<%=request.getContextPath()%>/images/indicator.gif' />&nbsp;Searching for profiles similar to " + name + "...");
            $("#simHeader").show();
            var DEid_ADid = $("select option:selected").val();
            var tokens = DEid_ADid.split('_');
            var DEid = tokens[0];
            var ADid = tokens[1];
            $("#simResult").load("<%=request.getContextPath()%>/expGenes", {eid:'${eid}', deid:DEid, adid:ADid, eAcc:'${exp.dwExpAccession}',query:'sim'}, function() {
                $("#simHeader").hide()
            });
            return false;
        });

        $("#searchForm").submit(function() {
            var qry = $("#geneInExp_qry").trigger('preSubmit').val();
            atlas.tokenizeGeneInput($("#geneInExp_qry").trigger('restore'), '', '(all genes)');
            $("#qryHeader").html("<img src='<%= request.getContextPath()%>/images/indicator.gif' />&nbsp;Loading...");
            $("#qryHeader").show();
            $("#qryResult").load("<%=request.getContextPath()%>/expGenes", {eid:'${eid}', gene:qry, eAcc:'${exp.dwExpAccession}',query:'search'}, function() {
                $("#qryHeader").hide()
            });
            return false;
        });

        $(".sample_attr_title").click(function() {
            var savals = $(this).next().clone();
            $("#display_attr_values").empty().append(savals);
	    savals.show();

	    $(".sample_attr_title").each(function() { $(this).css('font-weight','normal') });
            $(this).css('font-weight','bold');
        });

    });

    function addGeneToPlot(gid, gname, eid, ef) {

        if (genesToPlot[gname] != null) {
            return false;
            // if ef is different from current one redraw plot for new ef
        }
        geneCounter++;
        genesToPlot.push(gid);
        genesToPlot[gname] = gid;
        geneIndeces.push(geneCounter);
        plotBigPlot(genesToPlot.toString(), eid, ef, false, geneIndeces.toString());
        currentEF = ef;
    }


    //function called on each added gene, and draw plot for first 5 of them
       function addGeneToPlotIfEmpty(gid, gname, eid, ef) {

         if(window.location.href.indexOf("gid=")<=0) {
             if(geneCounter>4)
                return;

            geneCounter++;
            genesToPlot.push(gid);
            genesToPlot[gname] = gid;
            geneIndeces.push(geneCounter);

            if(geneCounter==5){
                plotBigPlot(genesToPlot.toString(), eid, ef, true, geneIndeces.toString());
                currentEF = ef;
            }
         }

    }

    function redrawForEF(eid, ef, efTxt) {

        //redrawPlotForFactor(eid,genesToPlot.toString(),ef,'large',false,"",geneIndeces.toString());
        plotBigPlot(genesToPlot.toString(), eid, ef, false, geneIndeces.toString());
        $('#sortHeader').text("Expression profile sorted by " + efTxt);
        currentEF = ef;
    }


    function radioLabel(label) {
        return label + '&nbsp;<img id="' + label + '"class="rmButton" height="8" src="images/closeButton.gif"/>';
    }

    function removeGene(gname) {
        if (genesToPlot.length == 1)
            return false;

        var gid = genesToPlot[gname];
        delete genesToPlot[gname];
        for (var i = 0; i < genesToPlot.length; i++)
        {
            if (genesToPlot[i] == gid) {
                genesToPlot.splice(i, 1);
                geneIndeces.splice(i, 1);
            }
        }
        //$("#"+gname+":parent").hide();
        //AZ:2009-06-26:do not jump to default EF (use curentEF)
        plotBigPlot(genesToPlot.toString(), '${eid}', currentEF, false, geneIndeces.toString());
    }


    var curatedChars = new Array();
    var curatedEFs = new Array();
    <c:forEach var="char" varStatus="s" items="${exp.sampleCharacteristics}">
    curatedChars['${char}'] = '${u:getCurated(char)}';
    </c:forEach>
    <c:forEach var="ef" varStatus="s" items="${exp.experimentFactors}">
    curatedEFs['${ef}'] = '${u:getCurated(ef)}';
    </c:forEach>


</script>


<jsp:include page='start_body_no_menus.jsp'/>

<div class="contents" id="contents">
<div id="ae_pagecontainer">

    <table style="border-bottom:1px solid #DEDEDE;margin:0 0 10px 0;width:100%;height:30px;">
        <tr>
            <td align="left" valign="bottom" width="55"
                style="padding-right: 10px;"><a href="<%= request.getContextPath()%>/"
                                                title="Gene Expression Atlas Homepage"><img border="0" width="55"
                                                                                            src="<%= request.getContextPath()%>/images/atlas-logo.png"
                                                                                            alt="Gene Expression Atlas"/></a>
            </td>
            <td align="right" valign="bottom"><a href="./">home</a> | <a
                    href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about
                the project</a> | <a
                    href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> | <a
                    id="feedback_href" href="javascript:showFeedbackForm()">feedback</a>
                <span id="feedback_thanks" style="font-weight: bold; display: none">thanks!</span>
                | <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a>
                | <a target="_blank"
                     href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web
                    services api</a> | <a
                        href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a></td>
        </tr>
    </table>



    <a href="http://www.ebi.ac.uk/arrayexpress/experiments/${exp.dwExpAccession}" target="_blank"
       title="Experiment information and full data in ArrayExpress Archive" class="geneName"
       style="vertical-align: baseline">${exp.dwExpAccession}</a>
    <span class="sectionHeader" style="vertical-align: baseline">${exp.dwExpDescription}</span>

    <div id="result_cont" style="margin-top:20px">

        <table id="twocol" style="margin-top:5px">
            <tr>
                <td style="padding:0px">
                    <div class="header"
                         style="padding-bottom: 10px; padding-left:45px;margin-bottom:5px;padding-top:4px">
                        <div id="${exp.dwExpId}_EFpagination" class="pagination_ie">
                            <c:forEach var="EF" items="${exp.experimentFactors}">
                                <c:choose>
                                    <c:when test="${EF == topRankEF}">
                                        <span class="current" id="${EF}"><fmt:message
                                                key="head.ef.${EF}"/></span>
                                    </c:when>
                                    <c:otherwise>
                                        <a id="${EF}"
                                           onclick="redrawForEF('${exp.dwExpId}','${EF}','<fmt:message key="head.ef.${EF}"/>')"><fmt:message
                                                key="head.ef.${EF}"/></a>
                                    </c:otherwise>
                                </c:choose>
                            </c:forEach>
                        </div>
                    </div>

                    <div style="position:relative;width:100%">
                        <table cellpadding="0" cellspacing="0" style="padding:0px;width:650px">
                            <tr>
                                <td style="padding:0px;width:500px">
                                    <div class="bigplot" id="plot"
                                         style="width:500px;height:150px;padding:0px;background:url('<%= request.getContextPath()%>/images/indicator.gif'); background-repeat:no-repeat; background-position:center; "></div>
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
                    <div id="gene_menu" style="width:500px">
                        <div><a href="#" style="font-size:12px">Display genes matching by name or
                            attribute</a></div>
                        <div>
                            <form id="searchForm" action="javascript:void()">
                                <table><tr><td>
                                    <label for="geneInExp_qry" style="font-size:12px">Find genes</label>
                                </td><td>
                                    <input type="text" class="value" name="gval_0" id="geneInExp_qry"
                                           style="width:200px">
                                </td>
                                    <td>
                                        <input type="submit" value="Search">
                                    </td>
                                </tr></table>
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
                            <div id="topGenes"><img src='<%= request.getContextPath()%>/images/indicator.gif'/>&nbsp;Loading gene
                                list...
                            </div>
                        </div>

                    </div>
                    <!--/gene_menu-->

                </td>
            </tr>
            <tr valign="top">
                <td valign="top">
                    <table width="600" style="border:1px solid #5E9E9E;margin-top:30px;height:150px" cellpadding="0" cellspacing="0">
                        <tr>
                            <th style="background-color:#EDF6F5;padding:5px;border-right:0px solid #CDCDCD" class="header">Sample Attributes</th>
                            <th style="background-color:#EDF6F5;padding:5px" class="header">Attribute Values</th>
                        </tr>
                        <tr>
                            <td width="200" style="border-bottom:0px solid #CDCDCD">
                                <ul style="margin: 0px; padding: 5px">
                                    <c:forEach var="char" items="${exp.sampleCharacteristics}">
                                        <li style="list-style-type: none; padding-left: 0px" id="${char}_title" class="sample_attr_title"><a href="#">
                                            <fmt:message key="head.ef.${char}"/>
                                            <c:if test="${!empty exp.factorValuesForEF[char]}">&nbsp;(EF)</c:if></a>
                                        </li>
                                        <div id="${char}_values" class="sample_attr_values">
                                            <c:forEach var="value"
                                                       items="${exp.sampleCharacterisitcValues[char]}" varStatus="r">
                                                <a style="text-transform: capitalize;" class="sample_attr_value" id="${char}_${r.count}"
                                                   onclick="highlightSamples('${char}','${u:escapeJS(value)}','<fmt:message key="head.ef.${char}"/>', false, this);return false;" href="#">${value}</a>
                                                <br/>
                                            </c:forEach>
                                        </div>
                                    </c:forEach>
                                    <c:forEach var="EF" items="${exp.experimentFactors}">
                                        <c:if test="${empty exp.sampleCharacterisitcValues[EF]}">
                                            <li style="list-style-type: none; padding-left: 0px" id="${EF}_title" class="sample_attr_title"><a href="#">
                                                <fmt:message key="head.ef.${EF}"/>&nbsp;(EF)</a>
                                            </li>
                                            <div id="${EF}_values" class="sample_attr_values">
                                                <c:forEach var="value" items="${exp.factorValuesForEF[EF]}" varStatus="r">
                                                    <a style="text-transform: capitalize;" class="sample_attr_value" id="${EF}_${r.count}" href="#"
                                                       onclick="highlightSamples('${EF}','${u:escapeJS(value)}','<fmt:message key="head.ef.${EF}"/>',true, this)">${value}</a>
                                                    <br/>
                                                </c:forEach>
                                            </div>
                                        </c:if>
                                    </c:forEach>
                                </ul>
                            </td>
                            <td style="padding:5px">
                                <div id="display_attr_values" style="height:200px;overflow:auto">Select a sample attribute...</div>
                            </td>
                        </tr>
                    </table>
                </td>
            </tr>
        </table>
    </div>
</div><!-- /id="ae_pagecontainer" -->
</div><!-- /id="contents" -->

<jsp:include page='end_body.jsp'/>
