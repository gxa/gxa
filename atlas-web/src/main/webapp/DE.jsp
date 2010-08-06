<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="java.util.*" %>

<link href="/css-common.css" media="screen" rel="stylesheet" type="text/css">
<link href="/home.css" media="screen" rel="stylesheet" type="text/css">
<link href="/style.css" media="screen" rel="stylesheet" type="text/css">
<!--link rel="stylesheet" href="http://static.jquery.com/ui/themeroller/app_css/app_screen.css" type="text/css" media="all" /-->
<link type="text/css" href="/hge_includes/jquery-ui-1.8.1.custom/css/overcast/jquery-ui-1.8.1.custom.css" rel="Stylesheet" />
<link type="text/css" href="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/themes/overcast/jquery.ui.theme.css" rel="Stylesheet" />



<%--><link rel="stylesheet" href="/themeroller/css/parseTheme.css.php?ctl=themeroller" type="text/css" media="all" /> <--%>

<html>
<head>

<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/js/jquery-ui-1.8.1.custom.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.accordion.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.dialog.js"></script>

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

    
    <!-- Load the AJAX API-->
    <script type="text/javascript" src="http://www.google.com/jsapi"></script>
    <script type="text/javascript">
      //Load the visualization API and the linechart package
      google.load("visualization", "1", {packages:["table","linechart"]});
      //google.load("visualization", "1", {packages:["table"]});
      google.load("prototype", "1.6");
    </script>

    <script type="text/javascript" src="http://systemsbiology-visualizations.googlecode.com/svn/trunk/src/main/js/load.js"></script>
    <script type="text/javascript">
        systemsbiology.load("visualization", "1.0", {packages:["bioheatmap"]});
    </script>


    <script type='text/javascript' src='http://www.google.com/jsapi'></script>

    <script type="text/javascript">
      google.load('visualization', '1', {packages: ['table']});
    </script>
    <script type="text/javascript">
    var visualization;

    var sortData = new google.visualization.DataTable();
    sortData.addColumn('string', 'Caracteristic');
    sortData.addColumn('string', 'Up-regulated genes');
    sortData.addColumn('string', 'Down-regulated genes');
    sortData.addRows(4);
    sortData.setCell(0, 0, 'Cell Line');
    sortData.setCell(1, 0, 'Disease');
    sortData.setCell(2, 0, 'Neoplasm');
    sortData.setCell(3, 0, 'Normal');
    sortData.setCell(0, 1, 'A2M AAK1    ABAT    ABCA1');
    sortData.setCell(1, 1, 'ACP5	ACSL1	ACSL5	ACTA');
    sortData.setCell(2, 1, 'ADD1	ADD3	ADH1B	AEBP1');
    sortData.setCell(3, 1, 'AIM1	AKAP13	AKAP7	ALAS2');
    sortData.setCell(0, 2, 'ALDH1A1	ALDH2	ALDH5A1	ALG13');
    sortData.setCell(1, 2, 'AP1S2	APOBEC3A	APOBEC3G	APOD');
    sortData.setCell(2, 2, 'BIN2	BIRC3	BLNK	BNIP3L');
    sortData.setCell(3, 2, 'CA2	CALCOCO1	CALM1	CAMK2B');

    function drawVisualization() {
      var table = new google.visualization.Table(document.getElementById('table'));
      table.draw(sortData, null);

      //var chart = new google.visualization.BarChart(document.getElementById('chart'));
      //chart.draw(sortData, null);

      google.visualization.events.addListener(table, 'sort',
          function(event) {
            sortData.sort([{column: event.column, desc: !event.ascending}]);
            //chart.draw(sortData, null);
          });
    }


    google.setOnLoadCallback(drawVisualization);
    </script>
    <style type="text/css">
		body {font-size: 62.5%; margin: 20px; font-family: Verdana, sans-serif; color: #444;}
		h1 {font-size: 1.5em; margin: 1em 0;}
		h2 {font-size: 1.3em; margin: 2em 0 .5em;}
		h2 a {font-size: .8em;}
		p {font-size: 1.2em; }
		ul {margin: 0; padding: 0;}
		li {margin: 2px; position: relative; padding: 4px 0; cursor: pointer; float: left;  list-style: none;}
		span.ui-icon {float: left; margin: 0 4px;}
		span.text {float: left; width: 180px;}
	</style>
    <script type="text/javascript">
		$(function(){
			$('.ui-state-default').hover(
				function(){ $(this).addClass('ui-state-hover'); },
				function(){ $(this).removeClass('ui-state-hover'); }
			);
			$('.ui-state-default').click(function(){ $(this).toggleClass('ui-state-active'); });
			$('.icons').append(' <a href="#">Toggle text</a>').find('a').click(function(){ $('.icon-collection li span.text').toggle(); return false; }).trigger('click');
		});
	</script> 

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
    <a class="link" href="http://localhost:9000/home_hge_atlas.jsp" title="Human Gene Expression Map Homepage">
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

    <!--Print icon --not working>
    <ul id="icons" class="ui-widget ui-helper-clearfix" style="float:right;">
        <li class="ui-state-default ui-corner-all" title=".ui-icon-print" >
            <a href="javascript:window.print()">
                <span class="ui-icon ui-icon-print"></span>
            </a>
        </li>
    </ul>
    <-->

    <hr>



    <span class='Breadcrumb' style="margin-left:270px;">You are here:
        <SPAN class='Breadcrumbon'>
            <a href="http://localhost:9000/home_hge_atlas
            +.jsp">Home</a> >
            <a href="http://localhost:9000/DE.jsp">DE</a>   <!--  NO...LOCAL PAGE NN HA IL LINK!!!!! -->
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
             </label>Differentially expressed probeset<br>for a gene
            <a href="#" id="dialog_link_3" class="ui-state-default ui-corner-all">More</a> <br/>
        </form>


  

        <div class="clearfix">
            Choose a class: <a href="http://localhost:9000/PCA.jsp" > ? </a> <br/>

            <label>
                <SELECT id="ddlClass"></SELECT>
            </label>
            <br/>
            and choose a gene:  <br/>
            <label>
                <SELECT id="ddlGene" disabled="disabled"></SELECT>
            </label>
            <br/>
            <!--input type="submit" name="go_de1" value="Query expression"-->
            <!--input type="button" value="Show experiment" onclick="queryExperiment(document.getElementById('ddlClass').value, document.getElementById('ddlGene').value);return false"-->
            <a href="" onclick='showPlot(); return false;'>show plot</a> <br/>
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

   <br>
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
    <!--div style="-moz-border-radius: 5px; -webkit-border-radius: 5px; border: 1px solid #3d53f2; padding: 5px;" -->
    <b>Differential Expression</b>
    <br/> <br/>
    <p class="normal_text">
        In this table there are two columns, "Up" and "Down", which contain over or under-expressed genes respectively,
        for the possible values of the chosen class (indicated by rows). <br/>
        Gene names are followed by a number in brackets which indicates
        the number of probesets annotated as that gene that have the corresponding behaviour. <br/><br/>
        Mouseover a gene -> display of general information on the related gene. <br/>
        Click on a gene -> redirect to a gene expression page with boxplot of that gene referred to the class chosen.
    </p>
    
    <br/><br/>
    <div class="ui-state-default ui-corner-all" style="padding:10px;background:white" >
        <br/>
        <b style="font-size:15px">Table</b>
    <!-->   <a href="#" class="ui-state-default ui-corner-all" title=".ui-icon-print"><span class="ui-icon ui-icon-print"></span> Print</a>
        <a href="javascript:window.print()">Print this page</a>       <-->
        <br/><br/>
        <div id="table" style="overflow:scroll;"></div>
         <input type="submit" name="query" value="Query expression" onclick="createDynTable('row','col')">
    </div>
    <!--div class="ui-state-default ui-corner-all" style="padding:10px;background:white">
    <a href="http://www.ebi.ac.uk:80/gxa/api?experiment=E-AFMX-5&updownIn=cellType&format=json&indent">json</a>  <br>
    <a href="http://www.ebi.ac.uk:80/gxa/api?experiment=E-AFMX-5&updownIn=cellType&format=xml&indent">xml</a>   <br>
    <a href="http://www.ebi.ac.uk:80/gxa/api?experiment=E-AFMX-5&geneIs=ENSG00000159216&updownIn=cellType&format=json&indent">json</a>    
    </div-->

</div>



</body>

</html>

<jsp:include page= '/WEB-INF/jsp/includes/end_body.jsp' />