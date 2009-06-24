<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>
<%@ page import="ae3.service.ArrayExpressSearchService" %>
<%@ page buffer="0kb" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%
    request.setAttribute("service", ArrayExpressSearchService.instance());
%>

<jsp:include page="start_head.jsp"></jsp:include>
Gene Expression Atlas - Large Scale Meta-Analysis of Public Microarray Data
<jsp:include page="end_head.jsp"></jsp:include>

    <meta name="Description" content="Gene Expression Atlas is a semantically enriched database of meta-analysis statistics for condition-specific gene expression.">
    <meta name="Keywords" content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays" />


    <jsp:include page="query-includes.jsp" />
    <link rel="stylesheet" href="structured-query.css" type="text/css" />
    <script type="text/javascript" src="scripts/common-query.js"></script>

    <script type="text/javascript">
        function toggleAtlasHelp(e) {
            if($("div.atlasHelp").is(":hidden")) {
                showAtlasHelp();
            } else {
                hideAtlasHelp();
            }
            if(e && typeof(e.stopPropagation) == 'function')
                e.stopPropagation();
            return false;
        }

        function showAtlasHelp() {
            if($("div.atlasHelp").is(":hidden")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("hide help");
            }
            $.cookie('atlas_help_state','shown');
        }

        function hideAtlasHelp() {
            if($("div.atlasHelp").is(":visible")) {
                $("div.atlasHelp").slideToggle();
                $("#atlasHelpToggle").text("show help");
            }
            $.cookie('atlas_help_state','hidden');
        }

        $(document).ready(function()
            {
                atlas.initSimpleForm();

                $("#atlasHelpToggle").click(toggleAtlasHelp);


                if (($.cookie('atlas_help_state') == "shown") && ($("div.atlasHelp").is(":hidden"))) {
                   showAtlasHelp();
                } else if (($.cookie('atlas_help_state') == "hidden") && ($("div.atlasHelp").is(":visible"))) {
                   hideAtlasHelp();
                }
            }
        );
    </script>

    <style type="text/css">
        .label {
            font-size: 10px;
        }

        .atlasHelp {
            display: none;
            text-align:center;
        }

        .atlasHelp .div1 {
            font-size: 0px; line-height: 0%; width: 0px;
            border-bottom: 20px solid #EEF5F5;
            border-left: 10px solid white;border-right: 10px solid white;
        }

        .atlasHelp .div2 {
            background-color: #EEF5F5; text-align:left; height:100%; width: 140px;padding:5px;
        }

        #newexplist {
            font-size: 10px;
            text-align:left;margin-top:70px;margin-right:auto;margin-left:auto;width:740px;
        }

        #newexplist table {
            border: none;
            padding: 0;
            margin: 10px 0 0 0;
        }

        #newexplist td {
            padding: 5px 10px 0 0;
            vertical-align:top;
            font-size: 10px;
        }
.rcs{display:block}
.rcs *{
  display:block;
  height:1px;
  overflow:hidden;
  font-size:.01em;
  background:#EEF5F5}
.rcs1{
  margin-left:3px;
  margin-right:3px;
  padding-left:1px;
  padding-right:1px;
  border-left:1px solid #f7fafa;
  border-right:1px solid #f7fafa;
  background:#f2f7f7}
.rcs2{
  margin-left:1px;
  margin-right:1px;
  padding-right:1px;
  padding-left:1px;
  border-left:1px solid #fdfefe;
  border-right:1px solid #fdfefe;
  background:#f1f6f6}
.rcs3{
  margin-left:1px;
  margin-right:1px;
  border-left:1px solid #f1f6f6;
  border-right:1px solid #f1f6f6;}
.rcs4{
  border-left:1px solid #f7fafa;
  border-right:1px solid #f7fafa}
.rcs5{
  border-left:1px solid #f2f7f7;
  border-right:1px solid #f2f7f7}
.rcsfg{
  background:#EEF5F5}
    </style>

<meta name="verify-v1" content="uHglWFjjPf/5jTDDKDD7GVCqTmAXOK7tqu9wUnQkals=" />
<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<div id="ae_pagecontainer"  style="position:absolute;z-index:2;width:100%;padding:0;">
    <div style="width:740px;margin-left:auto;margin-right:auto;margin-top:120px;" >
    <jsp:include page="simpleform.jsp"></jsp:include>
        
<div style="position:relative">
<div style="margin-top:50px;width:200px;position:absolute;left:0px">
  <b class="rcs">
  <b class="rcs1"><b></b></b>
  <b class="rcs2"><b></b></b>
  <b class="rcs3"></b>
  <b class="rcs4"></b>
  <b class="rcs5"></b></b>

  <div class="rcsfg">
   <div style="padding:10px">
    <div style="font-weight:bold;margin-bottom:5px">Atlas Data Release <c:out value="${service.stats.dataRelease}"/>:</div>
    <table cellpadding="0" cellspacing="0" width="100%">
    <tr><td align="left">new experiments</td><td align="right"><c:out value="${f:length(service.stats.newExperiments)}"/></td></tr>
    <tr><td align="left">total experiments</td><td align="right"><c:out value="${service.stats.numExperiments}"/></td></tr>
    <tr><td align="left">assays</td><td align="right"><c:out value="${service.stats.numAssays}"/></td></tr>
    <tr><td align="left">conditions</td><td align="right"><c:out value="${service.stats.numEfvs}"/></td></tr>
     </table>
   </div>
  </div>

  <b class="rcs">
  <b class="rcs5"></b>
  <b class="rcs4"></b>
  <b class="rcs3"></b>
  <b class="rcs2"><b></b></b>
  <b class="rcs1"><b></b></b></b>
</div>

<div style="margin-top:50px;width:530px;position:absolute;left:210px">
  <b class="rcs">
  <b class="rcs1"><b></b></b>
  <b class="rcs2"><b></b></b>
  <b class="rcs3"></b>
  <b class="rcs4"></b>
  <b class="rcs5"></b></b>

  <div class="rcsfg">
   <div style="padding:10px">
    <div style="font-weight:bold;margin-bottom:5px">Gene Expression Atlas</div>
	
The Gene Expression Atlas is a semantically enriched database of
meta-analysis based summary statistics over a curated subset of
ArrayExpress Archive, servicing queries for condition-specific gene
expression patterns as well as broader exploratory searches for
biologically interesting genes/samples.  

    </div>
  </div>

  <b class="rcs">
  <b class="rcs5"></b>
  <b class="rcs4"></b>
  <b class="rcs3"></b>
  <b class="rcs2"><b></b></b>
  <b class="rcs1"><b></b></b></b>





</div>

            <div style="color:#cdcdcd; margin-left: auto; margin-right: auto; width:100%; text-align:center; position: absolute; top:250px">
                <form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
                            For news and updates, subscribe to the <a href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:&nbsp;&nbsp;
                            <input type="text" name="email" size="10" value="" style="border:1px solid #cdcdcd;"/>
                            <input type="submit" name="email-button" value="Subscribe" />
                </form>
            </div>

</div>
    </div>
</div>


<jsp:include page="end_body.jsp"></jsp:include>
