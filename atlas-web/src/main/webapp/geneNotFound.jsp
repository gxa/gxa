<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<jsp:include page="start_head.jsp"></jsp:include>
ArrayExpress Atlas
<jsp:include page="end_head.jsp"></jsp:include>
<link rel="stylesheet" href="atlas.css" type="text/css" />
<link rel="stylesheet" href="geneView.css" type="text/css" />
<jsp:include page='start_body_no_menus.jsp' />
<jsp:include page='end_menu.jsp' />

<div id="ae_pagecontainer">

<table width="100%" style="position:relative;top:-10px;border-bottom:thin solid lightgray">
    <tr>
        <td align="left">
            <a href="index.jsp"><img border="0" src="images/atlasbeta.jpg" width="50" height="25" /></a></a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/index.html">about the project</a> |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/faq.html">faq</a> |
            <a id="feedback_href" href="javascript:showFeedbackForm()">feedback</a> <span id="feedback_thanks" style="font-weight:bold;display:none">thanks!</span> |
            <a target="_blank" href="http://arrayexpress-atlas.blogspot.com">blog</a> |
            <a target="_blank" href="http://www.ebi.ac.uk/microarray/doc/atlas/api.html">web services api</a> (<b>new!</b>) |
            <a href="http://www.ebi.ac.uk/microarray/doc/atlas/help.html">help</a>
        </td>
        <td align="right">
            <a href="http://www.ebi.ac.uk/microarray"><img border="0" height="20" title="EBI ArrayExpress" src="images/aelogo.png"/></a>
        </td>
    </tr>
</table>
<div align="left" style="color:red;font-weight:bold;margin-top:50px">
    We're sorry we could not find the gene requested. Please try a new <a href="index.jsp">search</a>
</div>
<jsp:include page="end_body.jsp"></jsp:include>
</div>