<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="java.util.*" %>


<link href="/print.css" media="print" rel="stylesheet" type="text/css">
<link href="/css-common.css" media="screen" rel="stylesheet" type="text/css">
<link href="/home.css" media="screen" rel="stylesheet" type="text/css">

<link rel="stylesheet" href="http://static.jquery.com/ui/themeroller/app_css/app_screen.css" type="text/css" media="all" />
<link type="text/css" href="/hge_includes/jquery-ui-1.8.2.custom/css/overcast/jquery-ui-1.8.2.custom.css" rel="Stylesheet" />
<link rel="stylesheet" media="screen, print" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/jquery.jqplot.css" />
<link rel="stylesheet" media="screen, print" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/examples/examples.css" />



<head>

<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-ui-1.8.2.custom.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.accordion.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.dialog.js"></script>

<!-- BEGIN: load jqplot -->
<script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/jquery.jqplot.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.cursor.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.dateAxisRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.ohlcRenderer.min.js"></script>
<script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.categoryAxisRenderer.js"></script>
<script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.highlighter.js"></script>
<script type="text/javascript" src="/hge_includes/scripts/functions.js"></script>
 <!-- END: load jqplot -->

<!-- Load drop-down menu data -->
<script type="text/javascript">
      window.onload=bindFactors();
      window.onload=bindGenes();
</script>

<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.canvasTextRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.canvasAxisTickRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.categoryAxisRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.barRenderer.min.js"></script>

<script src="/hge_includes/scripts/jquery.jqprint.0.3.js" type="text/javascript"></script> 


<style type="text/css">
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



<body>
<jsp:include page= '/WEB-INF/jsp/includes/start_body_no_menus_max.jsp' />
<jsp:include page= '/WEB-INF/jsp/includes/end_menu.jsp' />

<!--  page contents  -->
<table style="margin:0 0 10px 0;width:100%;height:30px;"id="ignore">
    <a class="link" href="http://localhost:9000/home_hge_atlas.jsp" title="Human Gene Expression Map Homepage">
        <img border="0" width="200" src="/logo-3.png" alt="Human Gene Expression Map"/>
    </a>

    <h2 style="float:right;color:#3383bb; border:none; background:none; font-family:Verdana, Helvetica, sans-serif;">
        ArrayExpress Ref:
        <a class="link" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=E-MTAB-62">[E-MTAB-62]</a>
    </h2>
<hr/>

  
</table>


<!-- Drop down menu-->

<div id="nav_query">
    <br/>
    <div class="ui-state-default ui-corner-all" style="padding:10px">
        <!--RADIO BUTTONS-->

        <label> Query for:  </label>
        <br/>

        <form>
            <label>
                <input name="radioset" type="radio" checked="checked" onclick="disableMenu()" value="Disable list">
            </label>
            Differential expression
            <a href="#" id="dialog_link" class="ui-state-default ui-corner-all">More</a><br/>
            <label>
                <input name="radioset" type="radio" onclick="enableMenu()" value="Enable list">
            </label>
            Gene expression across condition
            <a href="#" id="dialog_link_2" class="ui-state-default ui-corner-all">More</a> <br/>
            <label>
                <input name="radioset" type="radio" onclick="enableMenu()" value="Enable list">
            </label>
            Differentially expressed probeset<br/>for a gene
            <a href="#" id="dialog_link_3" class="ui-state-default ui-corner-all">More</a> <br/>
        </form>

        <div class="clearfix">
            Choose a classification:<a href="http://localhost:9000/PCA_template.jsp" > ?</a><br/>
            <label>
                <SELECT id="ddlClass"></SELECT>
            </label>
            <br/>
            and choose a gene:  <br/>
            <label>
                <SELECT id="ddlGene" disabled="disabled"></SELECT>
            </label>
            <br/>
            <input type="submit" value="Show results" onclick="queryExperiment(document.getElementById('ddlClass').value,document.getElementById('ddlGene').value);return false;">
           
        </div>
          
        <br/>
    </div>

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

    <br/>

    <div class="ui-state-default ui-corner-all" style="background:#fdfee9;padding:10px">
        <b>Published:</b><br/>
	    <p>Nature Biotechnology 2010 Apr</p><br/>
		<a href="http://www.nature.com/nbt/journal/v28/n4/full/nbt0410-322.html" title="Paper">A global map of human gene expression</a>
		<br/><br/>Margus Lukk&sup1;, Misha Kapushesky&sup1;, Janne Nikkila&sup2;,
		Helen Parkinson&sup1;, Angela Goncalves&sup1;, Wolfgang Huber&sup1;, Esko Ukkonen&sup2;,
		Alvis Brazma&sup1;<br/><br/>
		&sup1; EMBL-EBI<br/>&sup2; University of Helsinki<br/><br/>
		Using server:<br/>http://wwwdev.ebi.ac.uk/tc-test/microarray-as/biocep/cmd
    </div>

</div>

</body>

<jsp:include page= '/WEB-INF/jsp/includes/end_body.jsp' />