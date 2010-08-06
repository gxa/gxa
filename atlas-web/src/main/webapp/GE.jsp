<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="java.util.*" %>

<link href="/css-common.css" media="screen" rel="stylesheet" type="text/css">
<link href="/print.css" media="print" rel="stylesheet" type="text/css">
<link href="/home.css" media="screen" rel="stylesheet" type="text/css">
<link href="/style.css" media="screen" rel="stylesheet" type="text/css">
<!--link rel="stylesheet" href="http://static.jquery.com/ui/themeroller/app_css/app_screen.css" type="text/css" media="all" /-->
<link type="text/css" media="screen, print" href="/hge_includes/jquery-ui-1.8.1.custom/css/overcast/jquery-ui-1.8.1.custom.css" rel="Stylesheet" />
<link rel="stylesheet" media="screen, print" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/jquery.jqplot.css" />
<link rel="stylesheet" media="screen, print" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/examples/examples.css" />

<%--><link rel="stylesheet" href="/themeroller/css/parseTheme.css.php?ctl=themeroller" type="text/css" media="all" /> <--%>


<head>

<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/js/jquery-ui-1.8.1.custom.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.accordion.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.1.custom/development-bundle/ui/jquery.ui.dialog.js"></script>


<script type="text/javascript" src="/hge_includes/scripts/functions.js"></script>

<!--script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/jquery-1.3.2.min.js"></script-->

<!-- BEGIN: load jqplot -->                                    
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/jquery.jqplot.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.cursor.min.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.dateAxisRenderer.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.categoryAxisRenderer.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.ohlcRenderer.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.highlighter.js"></script>
   <!--script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.categoryAxisRenderer.min.js"></script-->
<!-- END: load jqplot -->


<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.canvasTextRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.canvasAxisTickRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.categoryAxisRenderer.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.barRenderer.min.js"></script>

<script src="/hge_includes/scripts/jquery.jqprint.0.3.js" type="text/javascript"></script> 

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


<script language="javascript" type="text/javascript">
      $(document).ready(function(){

         var ohlc = [['1', 138.7, 139.68, 135.18, 135.4],
          ['2', 143.46, 144.66, 139.79, 140.02],
          ['3', 128.24, 133.5, 126.26, 129.19],
          ['4', 122.9, 127.95, 122.66, 127.24],
          ['5', 121.73, 127.2, 118.6, 123.9],
          ['6', 136.47, 146.4, 136, 144.67],
          ['7', 124.76, 135.9, 124.55, 135.81],
          ['8', 123.73, 129.31, 121.57, 122.5],
          ['9', 127.37, 130.96, 119.38, 122.42],
          ['10', 140.67, 143.56, 132.88, 142.44],
          ['11', 136.01, 139.5, 134.53, 139.48],
          ['12', 143.82, 144.56, 136.04, 136.97],
          ['13', 120.01, 124.25, 115.76, 123.42],
          ['14', 114.94, 120, 113.28, 119.57],
          ['15', 104.51, 116.13, 102.61, 115.99]
          ];
          /*var ohlc = [[138.7, 139.68, 135.18, 135.4],
          [143.46, 144.66, 139.79, 140.02],
          [140.67, 143.56, 132.88, 142.44],
          [136.01, 139.5, 134.53, 139.48]
          ];   */
      ticks=['Blood Neoplasm Cell Line', 'Blood Non Neoplastic Desease', 'Breast Cancer',
          'Germ Cell Neoplasm', 'Leukemia', 'Nervous System Neoplasm', 'Non Breast Carcinoma',
          'Non Leukemic Blood Neoplasm','Non Neoplastic Cell Line','Normal Blood',
          'Normal Solid Tissue','Other Neoplasm','Sarcoma','Solid Tissue Neoplasm Cell Line',
          'Solid Tissue Non Neoplastic Disease'];
    //  ticks=['1', '2', '3', '4'];
      plot2 = $.jqplot('boxplot',[ohlc],{
      title: 'Gene: AAK1',
      axesDefaults:{tickRenderer: $.jqplot.CanvasAxisTickRenderer },
      axes: {
          xaxis: {
              //renderer:$.jqplot.DateAxisRenderer,
              renderer:$.jqplot.CategoryAxisRenderer,
              ticks:ticks,
              tickOptions:{
                  formatString:'string',
                  angle:-30,
                  fontSize: '8pt'}
          }
         /* yaxis: {
              tickOptions:{formatString:'string'}
          }       */
      },
      /*axes:{xaxis:{ticks:ticks, renderer:$.jqplot.CategoryAxisRenderer}},    */
      series: [{renderer:$.jqplot.OHLCRenderer, rendererOptions:{candleStick:true}}],
      cursor:{
          zoom:true,
          tooltipOffset: 10,
          tooltipLocation: 'nw'
      },
      highlighter: {
          showMarker:false,
          tooltipAxes: 'xy',
          yvalues: 5,
          formatString:'<table class="jqplot-highlighter"><tr><td>95th percentile:</td><td>%s</td></tr><tr><td>75th percentile:</td><td>%s</td></tr><tr><td>Median:</td><td>%s</td></tr><tr><td>25th percentile:</td><td>%s</td></tr><tr><td>5th percentile:</td><td>%s</td></tr></table>'
      }
    });
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
            <a href="http://localhost:9000/GE.jsp">GE</a>   <!--  NO...LOCAL PAGE NN HA IL LINK!!!!! -->
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
        <br>
        <label> Query for:  </label>
        <br>

         <form method="post" action="DE.jsp">
             <label>
                 <input name="radioset" type="radio" checked="checked" onclick="disableMenu()" value="DE">
             </label>Differential expression
             <a href="#" id="dialog_link" class="ui-state-default ui-corner-all">More</a><br>
             <label>
                 <input name="radioset" type="radio" onclick="enableMenu()" value="GE">
             </label>Gene expression across condition
            <a href="#" id="dialog_link_2" class="ui-state-default ui-corner-all">More</a> <br>
             <label>
                 <input name="radioset" type="radio" onclick="enableMenu()" value="PR">
             </label>Differentially expressed probeset<br>for a gene
            <a href="#" id="dialog_link_3" class="ui-state-default ui-corner-all">More</a> <br>



        <div class="clearfix">
            Choose a class:<br>
            <label>
                <SELECT id="ddlClass"></SELECT>
            </label>
            <br>
            and choose a gene: <a href="http://localhost:9000/PCA.jsp"> ? </a> <br>
            <label>
                <SELECT id="ddlGene" disabled="disabled"></SELECT>
            </label>
            <br>
            <input type="submit" name="query" value="Query expression" onclick="query_hge()">
            <!-- <input type="button" value="Show experiment" onclick="queryExperiment('E-AFMX-5')">  -->
        </div>

       </form>

        <br>

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
        <b>Published:</b><br>
	    <p>Nature Biotechnology 2010 Apr</p><br>
		<a href="http://wwwdev.ebi.ac.uk/microarray/hge/atlas/index.html" title="Paper">A global map of human gene expression</a>
		<br><br>Margus Lukk&sup1;, Misha Kapushesky&sup1;, Janne Nikkila&sup2;,
		Helen Parkinson&sup1;, Angela Goncalves&sup1;, Wolfgang Huber&sup1;, Esko Ukkonen&sup2;,
		Alvis Brazma&sup1;<br><br>
		&sup1; EMBL-EBI<br>&sup2; University of Helsinki<br><br>
		Using server:<br>http://wwwdev.ebi.ac.uk/tc-test/microarray-as/biocep/cmd
    </div>

 </div>


<div id="content">
<div class="ui-state-default ui-corner-all" style="media:screen; padding:10px ; margin-left:265px; margin-right:10px;background:white;">
    <!--div style="-moz-border-radius: 5px; -webkit-border-radius: 5px; border: 1px solid #3d53f2; padding: 5px;" -->

    <b>Gene Expression across condition</b>
    <br> <br>
    <label>
        <input type="button" style="float:right;" value="Print this page" onClick="window.print()">
    </label>
    <!-- div id="Boxplot" -->

    <!--img border="0" width="250" src="/The.4.meta.groups_AAK1_bOXPLOT.png" alt="Boxplot"/-->
        <!--ul id="icons" class="ui-widget ui-helper-clearfix">
            <li class="ui-state-default ui-corner-all" title=".ui-icon-print"><span class="ui-icon ui-icon-print"></span></li> 
        </ul-->

        <!--img src="/The.4.meta.groups_AAK1_bOXPLOT.png"-->



    <?php include "nav.inc"; ?>
    <div id="boxplot" style="margin:20px;margin-left:45px;height:260px; width:640px;"></div>
    <br>
    <b>Ensembl Gene Annotation</b> <br>
    <p style="font:bold">Probes: 205434_s_at, 205435_s_at, 211186_s_at</p><br>
    <p style="font:bold">Ensembl ID: ENSG00000115977 </p><br>
    <p style="font:bold">HGNC symbol: AAK1</p> <br>
    <p style="font:bold">Description: AP2-associated protein kinase 1 (EC 2.7.11.1)(Adaptor-associated kinase 1) [Source:UniProtKB/Swiss-Prot;Acc:Q2M2I8]</p> <br>
</div>
</div>
<!--/div-->
<!--/div-->
<!-- Drop down menu-->


</body>

<jsp:include page= '/WEB-INF/jsp/includes/end_body.jsp' />