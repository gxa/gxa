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

<meta name="Description"
      content="Gene Expression Atlas is a semantically enriched database of meta-analysis statistics for condition-specific gene expression.">
<meta name="Keywords"
      content="ArrayExpress, Atlas, Microarray, Condition, Tissue Specific, Expression, Transcriptomics, Genomics, cDNA Arrays"/>

<jsp:include page="query-includes.jsp" />
<link rel="stylesheet" href="structured-query.css" type="text/css" />
<script type="text/javascript" src="scripts/common-query.js"></script>
<script type="text/javascript" src="scripts/jquery.corner.js"></script>


<style type="text/css">

    .alertNotice {
        padding: 50px 10px 10px 10px;
        text-align: center;
        font-weight: bold;
    }
    
    .alertNotice > p {
        margin: 10px;
    }

    .alertHeader {
        color: red;
    }

    #centeredMain {
        width:740px;
        margin: 0 auto;
        padding:50px 0;
        height: 100%;
    }

    .roundCorner {
        background-color: #EEF5F5;
    }

</style>

<meta name="verify-v1" content="uHglWFjjPf/5jTDDKDD7GVCqTmAXOK7tqu9wUnQkals="/>
<meta name="y_key" content="fcb0c3c66fb1ff11">

<jsp:include page="start_body_no_menus.jsp"></jsp:include>

<div id="contents" class="contents">
<div id="centeredMain">
    <jsp:include page="simpleform.jsp"></jsp:include>

<!--    <div class="alertNotice">
        <p class="alertHeader">Downtime Notice!</p>

        <p>
            Gene Expression Atlas will be unavailable Friday June 26, 12:00-13:00 due to a hardware upgrade.
        </p>
    </div>
-->

    <div style="margin-top:50px">
        <div style="float:left; width:200px;" class="roundCorner">
            <div style="padding:10px">
                <div style="font-weight:bold;margin-bottom:5px">Atlas Data Release <c:out
                        value="${service.stats.dataRelease}"/>:
                </div>
                <table cellpadding="0" cellspacing="0" width="100%">
                    <tr>
                        <td align="left">new experiments</td>
                        <td align="right"><c:out value="${f:length(service.stats.newExperiments)}"/></td>
                    </tr>
                    <tr>
                        <td align="left">total <a href="<%= request.getContextPath() %>/experiment/index.htm" title="Atlas Experiment Index">experiments</a></td>
                        <td align="right"><c:out value="${service.stats.numExperiments}"/></td>
                    </tr>

                    <tr>
                        <td align="left">total <a href="<%= request.getContextPath() %>/gene/index.htm" title="Atlas Gene Index">genes</a></td>
                        <td align="right"><c:out value="${service.stats.numGenes}"/></td>
                    </tr>

                    <tr>
                        <td align="left">assays</td>
                        <td align="right"><c:out value="${service.stats.numAssays}"/></td>
                    </tr>
                    <tr>
                        <td align="left">conditions</td>
                        <td align="right"><c:out value="${service.stats.numEfvs}"/></td>
                    </tr>
                </table>
            </div>
        </div>

        <div style="float:left;width:530px;margin-left:10px;" class="roundCorner">
            <div style="padding:10px">
                <div style="font-weight:bold;margin-bottom:5px">Gene Expression Atlas</div>

                The Gene Expression Atlas is a semantically enriched database of
                meta-analysis based summary statistics over a curated subset of
                ArrayExpress Archive, servicing queries for condition-specific gene
                expression patterns as well as broader exploratory searches for
                biologically interesting genes/samples.
            </div>
        </div>

        <div style="clear:both;"></div>
    </div>
    <div style="font-family: Verdana, helvetica, arial, sans-serif; color:#cdcdcd; margin-left: auto; margin-right: auto; width:100%; text-align:center; margin-top:50px">
        <form method="POST" action="http://listserver.ebi.ac.uk/mailman/subscribe/arrayexpress-atlas">
        For news and updates, subscribe to the <a
            href="http://listserver.ebi.ac.uk/mailman/listinfo/arrayexpress-atlas">atlas mailing list</a>:&nbsp;&nbsp;
        <input type="text" name="email" size="10" value="" style="border:1px solid #cdcdcd;"/>
        <input type="submit" name="email-button" value="Subscribe"/>
        </form>
    </div>
</div>
</div>


<jsp:include page="end_body.jsp"></jsp:include>
