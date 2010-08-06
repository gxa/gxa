<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="java.util.*" %>

<link href="/css-common.css" media="screen" rel="stylesheet" type="text/css">
<link href="/home.css" media="screen" rel="stylesheet" type="text/css">
<link href="/style.css" media="screen" rel="stylesheet" type="text/css">
<!--link rel="stylesheet" href="http://static.jquery.com/ui/themeroller/app_css/app_screen.css" type="text/css" media="all" /-->
<link type="text/css" href="/hge_includes/jquery-ui-1.8.1.custom/css/overcast/jquery-ui-1.8.1.custom.css" rel="Stylesheet" />


<%--><link rel="stylesheet" href="/themeroller/css/parseTheme.css.php?ctl=themeroller" type="text/css" media="all" /> <--%>


<head>

<script type="text/javascript" src="http://www.ebi.ac.uk/microarray-as/ae/assets/scripts/ae_browse-1.4.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/js/jquery-1.4.2.min.js"></script>    
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/js/jquery-ui-1.8.1.custom.min.js"></script>
<!--script type="text/javascript" src="/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.core.js"></script-->
<!--script type="text/javascript" src="/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.widget.js"></script-->
<!--script type="text/javascript" src="/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.accordion.js"></script-->
<!--script type="text/javascript" src="/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.dialog.js"></script-->

<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.cursor.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.dateAxisRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.ohlcRenderer.min.js"></script>


<script type="text/javascript" src="/hge_includes/scripts/functions.js"></script>




<style type="text/css">
    /*Default CSS for pan containers*/
    .pancontainer{
        position:relative; /*keep this intact*/
        overflow:hidden; /*keep this intact*/
        width:300px;
        height:300px;
        border:1px solid black;
    }
</style>



</head>

<%@ page contentType="text/html;charset=UTF-8" language="java" %>

<jsp:include page= '/WEB-INF/jsp/includes/start_head.jsp' />
HGE
<jsp:include page= '/WEB-INF/jsp/includes/end_head.jsp' />

<!-- add header scripts here -->

<body>
<jsp:include page= '/WEB-INF/jsp/includes/start_body_no_menus_max.jsp' />
<jsp:include page= '/WEB-INF/jsp/includes/end_menu.jsp' />

<!-- start page contents  here -->
<table style="margin:0 0 10px 0;width:100%;height:30px;">
    <a class="link" href="http://localhost:9000/home.jsp" title="Human Gene Expression Map Homepage">
        <!--img border="0" width="100" src="/NewLogo.png" alt="Human Gene Expression Map"/-->
        <img border="0" width="200" src="/logo-3.png" alt="Human Gene Expression Map"/>
        <!--a id="link" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html? keywords=E-MTAB-62"style="color:#3383bb">[E-MTAB-62]</a-->
    </a>
     <h1 style="float:right;color:#3383bb; border:none; background:none; font-family:Verdana, Helvetica, sans-serif;">
        ArrayExpress Ref:
        <a class="link" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=E-MTAB-62">[E-MTAB-62]</a>
     </h1>
    <!--h1 style="color:#3383bb; border:none; background:none; font-family:Verdana, Helvetica, sans-serif;">
        Human Gene Expression Map
        <a id="link" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html? keywords=E-MTAB-62">[E-MTAB-62]</a>
    </h1-->

    <!--ul id="icons" class="ui-widget ui-helper-clearfix" style="float:right;">
        <li class="ui-state-default ui-corner-all" title=".ui-icon-print" >
            <a href="javascript:window.print()">
                <span class="ui-icon ui-icon-print"></span>
            </a>
        </li>
    </ul-->


    <hr>


    <span class='Breadcrumb' style="margin-left:270px;">You are here:
        <SPAN class='Breadcrumbon'>
            <a href="http://localhost:9000/home_hge_atlas.jsp">Home</a> >
            <a href="http://localhost:9000/PCA.jsp">PCA</a>   <!--  NO...LOCAL PAGE NN HA IL LINK!!!!! -->
        </SPAN>
    </span>


</table>


<!-- Drop down menu-->

<div id="nav_query">
    <div class="ui-state-default ui-corner-all" style="padding:10px">
        <!--div id="nav_query_border" style=" background-color: #fbf9f9; -moz-border-radius: 5px; -webkit-border-radius: 5px; border: 1px solid #999999; padding: 5px;" -->
        <!--RADIO BUTTONS-->
        <button id="button" onclick='bindFactors();bindGenes(); return false;'>Load data</button>
        <br/>
        <!-->
        <a href="" onclick='showPlot(); return false;'>show plot</a>
        <br/>
        <img id="imgBoxPlot" src=""/>
        <-->
        <br/>
        <label> Query for:  </label>
        <br/>

         <form>
             <label>
                 <input name="radioset" type="radio" checked="checked" onclick="disableMenu()" value="Disable list">
             </label>Differential expression
             <a href="#" id="dialog_link" class="ui-state-default ui-corner-all">More</a><br/>
             <label>
                 <input name="radioset" type="radio" onclick="enableMenu()" value="Enable list">
             </label>Gene expression across condition
            <a href="#" id="dialog_link_2" class="ui-state-default ui-corner-all">More</a> <br/>
             <label>
                 <input name="radioset" type="radio" onclick="enableMenu()" value="Enable list">
             </label>Differentially expressed probeset<br/>for a gene
            <a href="#" id="dialog_link_3" class="ui-state-default ui-corner-all">More</a> <br/>
        </form>



        <div class="clearfix">
            Choose a class:<br/>
            <label>
                <SELECT id="ddlClass"></SELECT>
            </label>
            <br/>
            and choose a gene:  <br/>
            <label>
                <SELECT id="ddlGene" disabled="disabled"></SELECT>
            </label>
            <br/>
            <input type="submit" name="go_de1" value="Query expression">
            <!-- <input type="button" value="Show experiment" onclick="queryExperiment('E-AFMX-5')">  -->
        </div>

        <br/>

    </div>
 <div>
 <!-- ui-dialog -->
        <div id="dialog" title="Differential expression">
            <p>Interrogate the HGE dataset by querying for a biological condition of interest
                to find all genes up od down-regulated.</p>
        </div>

        <!-- ui-dialog -->
        <div id="dialog_2" title="Gene expression across condition">
            <p>Interrogate the HGE dataset by querying for a gene of interest
                to find biological conditions where it is differentially expressed.</p>
        </div>

        <!-- ui-dialog -->
        <div id="dialog_3" title="Differentially expressed probeset for a gene">
            <p>Interrogate the HGE dataset by querying for a gene in order
                to find whether it has differentially expressed probeset.</p>
        </div>

 </div>

<br/>
    <div class="ui-state-default ui-corner-all" style="background:#fdfee9;padding:10px">
        <b>Published:</b><br/>
	    <p>Nature Biotechnology 2010 Apr</p><br/>
		<a href="http://wwwdev.ebi.ac.uk/microarray/hge/atlas/index.html" title="Paper">A global map of human gene expression</a>
		<br/><br/>Margus Lukk&sup1;, Misha Kapushesky&sup1;, Janne Nikkila&sup2;,
		Helen Parkinson&sup1;, Angela Goncalves&sup1;, Wolfgang Huber&sup1;, Esko Ukkonen&sup2;,
		Alvis Brazma&sup1;<br/><br/>
		&sup1; EMBL-EBI<br/>&sup2; University of Helsinki<br/><br/>
		Using server:<br/>http://wwwdev.ebi.ac.uk/tc-test/microarray-as/biocep/cmd
    </div>

</div>
<!--div id="content" style="background:orange"-->
<div class="ui-state-default ui-corner-all" style="padding:10px ; margin-left:265px; margin-right:10px;background:white;">

    <div>
     <b style="padding:5px">Classifications</b>
     <a href="" title="Print results"><img align="right" src="/silk_print.gif" alt="Print results"/></a>
     <a href="/PCA_Fig/pca_2.pdf" title="Download pdf">
         <img align="right" src="/PCA_Fig/put-pdf-document-website-200X200.jpg" width="20" alt="Download Pdf"/></a>
    </div>

    <p class="normal_text" style="padding:5px;">
        <b>Principal component analysis.</b><br/><br/>
        Each dot represents one of the 5,372 samples in a
        multidimensional gene expression space projected on the principal plane formed by the first
        (hematopoietic) and second (malignancy) principal axes. The dots are colored semitransparently
        according to the biological group the sample belongs to.
    </p>
    <br/>
    <!-- PCA and legend-->
    <div style="padding:5px;height:500px; overflow-y:scroll;">
            <p class="normal_text" align="right">
            <a href="/PCA_Fig/SupplementaryFigure_3a.png" target="_blank">
                <img class="ui-state-default ui-corner-all"align="left" border="0" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3a.png" alt="PCA-Blood/non Blood"/></a>
            <br/><br/>The first principal component separates hematopoietic system–derived samples from the rest of the samples, with connective tissues and
            incompletely differentiated cell–based samples forming a relatively compact group on the right.
            The cyan dots among the blood samples on the right side represent samples from bronchoalveolar lavage cells
            (a possible sample contamination with blood) and kidney. The dark green dots at the center include embryonic stem cells.</p>
        <br/><br/>

        <p class="normal_text">
        <a href="/PCA_Fig/SupplementaryFigure_3b.png" target="_blank">
            <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3b.png" alt="PCA-Blood/non Blood"/></a>
        <br/><br/>The second principal axis predominantly arranges cell line samples at the bottom,
            neoplasm samples in the middle and a mixture of nonneoplastic disease and normal samples at the top.
        </p>
        <br/><br/>

        <p class="normal_text">
        <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3e.png" alt="PCA-Blood/non Blood"/>
            <br/><br/>Samples colored by 6 clusters (see legend) identified on self-vs-self heat-map of 96 biological groups.
            The samples are visualized on the 1st and 2nd principal component plane.
        </p>
        <br/><br/>

        <p class="normal_text">
            <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3c.png" alt="PCA-Blood/non Blood"/>
            <br/><br/>Samples colored by 15 meta-groups (see legend) on the 1st and 2nd principal component plane.
        </p>
        <br/><br/>
       
        <p class="normal_text">
            <img class="ui-state-default ui-corner-all" align="left" style="background:white;margin-right:20px;margin-bottom:10px;" width="350"src="/PCA_Fig/SupplementaryFigure_3d.png" alt="PCA-Blood/non Blood"/>
            <br/><br/>Samples colored by 15 groups of tissues of origin (see legend) on the 1st and 2nd principal component plane.
        </p>
        <br/><br/>

    <br/>
    
        
</div>
    <br/>
</div>

</body>

<jsp:include page= '/WEB-INF/jsp/includes/end_body.jsp' />