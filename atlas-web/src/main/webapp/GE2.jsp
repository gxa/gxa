<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN"
       "http://www.w3.org/TR/html4/loose.dtd">
<%@ page import="java.util.*" %>

<link href="/css-common.css" media="screen" rel="stylesheet" type="text/css">
<link href="/home.css" media="screen" rel="stylesheet" type="text/css">
<link href="/style.css" media="screen" rel="stylesheet" type="text/css">
<!--link rel="stylesheet" href="http://static.jquery.com/ui/themeroller/app_css/app_screen.css" type="text/css" media="all" /-->
<link type="text/css" href="/hge_includes/jquery-ui-1.8.2.custom/css/overcast/jquery-ui-1.8.2.custom.css" rel="Stylesheet" />
<link rel="stylesheet" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/jquery.jqplot.css" />
<link rel="stylesheet" type="text/css" href="/hge_includes/jquery.jqplot.0.9.7/examples/examples.css" />

<%--><link rel="stylesheet" href="/themeroller/css/parseTheme.css.php?ctl=themeroller" type="text/css" media="all" /> <--%>


<head>

<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-ui-1.8.2.custom.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.accordion.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.dialog.js"></script>


<script type="text/javascript" src="/hge_includes/scripts/functions.js"></script>
<!--script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/jquery-1.3.2.min.js"></script-->
<script type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.boxplotRenderer.js"></script>

<!-- BEGIN: load jqplot -->
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/jquery.jqplot.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.cursor.min.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.dateAxisRenderer.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.categoryAxisRenderer.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.ohlcRenderer.js"></script>
  <script language="javascript" type="text/javascript" src="/hge_includes/jquery.jqplot.0.9.7/plugins/jqplot.highlighter.js"></script>
<!-- END: load jqplot -->



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

          /*var ohlc = [['07/06/09', 138.7, 139.68, 135.18, 135.4],
          ['06/29/09', 143.46, 144.66, 139.79, 140.02],
          ['06/22/09', 140.67, 143.56, 132.88, 142.44],
          ['06/15/09', 136.01, 139.5, 134.53, 139.48],
          ['06/08/09', 143.82, 144.56, 136.04, 136.97],
          ['06/01/09', 136.47, 146.4, 136, 144.67],
          ['05/26/09', 124.76, 135.9, 124.55, 135.81],
          ['05/18/09', 123.73, 129.31, 121.57, 122.5],
          ['05/11/09', 127.37, 130.96, 119.38, 122.42],
          ['05/04/09', 128.24, 133.5, 126.26, 129.19],
          ['04/27/09', 122.9, 127.95, 122.66, 127.24],
          ['04/20/09', 121.73, 127.2, 118.6, 123.9],
          ['04/13/09', 120.01, 124.25, 115.76, 123.42],
          ['04/06/09', 114.94, 120, 113.28, 119.57],
          ['03/30/09', 104.51, 116.13, 102.61, 115.99]
          ];  */
          var data = [['aaa', 138.7, 138.8, 139.18, 139.4, 140.0],
                     ['bbb', 129.3, 129.6, 130.18, 130.4, 130.6],
                     ['ccc', 138.1, 138.6, 138.8, 139.4, 139.8],
                     ['ddd', 129.3, 129.6, 130.18, 130.4, 130.6]] ;
      plot2 = $.jqplot('boxplot',[data],{
      title: '"&gene"',
      axesDefaults:{},
      axes: {
          xaxis: {
              renderer:$.jqplot.BoxplotRenderer,
              tickOptions:{formatString:'string'}
          },
          yaxis: {
              tickOptions:{formatString:'string'}
          }
      },
      series: [{renderer:$.jqplot.BoxplotRenderer}],
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

<script type="text/javascript">
    /**
* Copyright by Fabian Dill, 2010
* Licensed under the MIT License (http://www.opensource.org/licenses/mit-license.php).
*
* This script was written by Fabian Dill and published
* at http://informationandvisualization.de
*
* If you use it, it would be nice if you link to our page
* and/or drop us a line where you use it (for our interest only).
* to fabian.dill(at)googlemail.com
*/
var lowerWhisker;
var q1;
var median;
var q3;
var upperWhisker;
var mildOutliers;
var extremeOutliers;
var min;
var max;

function sortNumber(a, b) {
	return a - b;
}

// map the values onto a scale of fixed height
function mapValue(v, height) {
	return Math.round(height - (((v - min) / (max - min)) * height));
}

function calculateValues(data) {
	data.sort(sortNumber);
	var n = data.length;
	// lower quartile
	var q1Pos = (n * 0.25);
	if (q1Pos % 1 != 0) {
	    q1Pos = Math.floor(q1Pos);
	    q1 = data[q1Pos];
	} else {
	    q1Pos = Math.floor(q1Pos);
	    q1 = (data[q1Pos] + data[q1Pos-1]) / 2;
	}
	// median
	var medianPos = (n * 0.5);
	if (medianPos % 1 != 0) {
	    medianPos = Math.floor(medianPos);
	    median = data[medianPos];
	} else {
	    medianPos = Math.floor(medianPos);
	    median = (data[medianPos] + data[medianPos-1]) / 2;
	}
	// upper quartile
	var q3Pos = (n * 0.75);
	if (q3Pos % 1 != 0) {
	    q3Pos = Math.floor(q3Pos);
	    q3 = data[q3Pos];
	} else {
	    q3Pos = Math.floor(q3Pos);
	    q3 = (data[q3Pos] + data[q3Pos-1]) / 2;
	}
	min = data[0];
	max = data[n - 1];

	var iqr = q3 - q1;
	mildOutliers = new Array();
	extremeOutliers = new Array();
	lowerWhisker = min;
	upperWhisker = max;
	if (min < (q1 - 1.5 * iqr)) {
		for (var i = 0; i < q1Pos; i++) {
			// we have to detect outliers
			if (data[i] < (q1 - 3 * iqr)) {
				extremeOutliers.push(data[i]);
			} else if (data[i] < (q1 - 1.5 * iqr)) {
				mildOutliers.push(data[i]);
			} else if (data[i] >= (q1 - 1.5 * iqr)) {
				lowerWhisker = data [i];
				break;
			}
		}
	}
	if (max > (q3 + (1.5 * iqr))) {
		for (i = q3Pos; i < data.length; i++) {
			// we have to detect outliers
			if (data[i] > (q3 + 3 * iqr)) {
				extremeOutliers.push(data[i]);
			} else if (data[i] > (q3 + 1.5 * iqr)) {
				mildOutliers.push(data[i]);
			} else if (data[i] <= (q3 + 1.5 * iqr)) {
				upperWhisker = data[i];
			}
		}
	}
}

function roundVal(val){
	var dec = 2;
	var result = Math.round(val*Math.pow(10,dec))/Math.pow(10,dec);
	return result;
}

function createBoxPlot(dataArray, height, divID) {
	calculateValues(dataArray);
	var overallID = divID + "overall";

	var mlowerWhisker = mapValue(lowerWhisker, height);
	var mq1 = mapValue(q1, height);
	var mmedian = mapValue(median, height);
	var mq3 = mapValue(q3, height);
	var mupperWhisker = mapValue(upperWhisker, height);
	var mmildOutliers = new Array(mildOutliers.length);
	for (i = 0; i < mildOutliers.length; i++) {
		mmildOutliers[i] = mapValue(mildOutliers[i], height);
	}
	var mextremeOutliers = extremeOutliers;
	for (i = 0; i < extremeOutliers.length; i++) {
		mextremeOutliers[i] = mapValue(extremeOutliers[i], height);
	}

  var overallDiv = document.createElement("div");
	overallDiv.style.height = height + "px";
	overallDiv.style.width = "56px";
	overallDiv.style.border = "none";
	overallDiv.style.borderRight = "1px dotted";
	overallDiv.id = overallID;
	document.getElementById(divID).appendChild(overallDiv);

	var upperDiv = document.createElement("div");
	upperDiv.id = "upperBox" + divID;
	upperDiv.className = "boxplot-element";
	upperDiv.style.top = mq3 + "px";
	upperDiv.style.height = (mmedian - mq3) + "px";
	document.getElementById(overallID).appendChild(upperDiv);

	var lowerDiv = document.createElement("div");
	lowerDiv.id = "lowerBox" + divID;
	lowerDiv.className = "boxplot-element";
	lowerDiv.style.top = mmedian + "px";
	lowerDiv.style.height = mq1 - mmedian + "px";
	document.getElementById(overallID).appendChild(lowerDiv);

	var lowerWhiskerDiv = document.createElement("div");
	lowerWhiskerDiv.id = "lowerWhisker" + divID;
	lowerWhiskerDiv.className = "boxplot-element";
	lowerWhiskerDiv.style.top = mlowerWhisker + "px";
	document.getElementById(overallID).appendChild(lowerWhiskerDiv);

	var upperWhiskerDiv = document.createElement("div");
	upperWhiskerDiv.id = "upperWhisker" + divID;
	upperWhiskerDiv.className = "boxplot-element";
	upperWhiskerDiv.style.top = mupperWhisker + "px";
	document.getElementById(overallID).appendChild(upperWhiskerDiv);

	for(i = 0; i < mildOutliers.length; i++) {
		var newDiv = document.createElement("div");
		newDiv.className = "boxplot-element";
		newDiv.style.width="4px";
		newDiv.style.height="4px";
		newDiv.style.top = mmildOutliers[i] + "px";
		newDiv.style.left= "50px";
		document.getElementById(overallID).appendChild(newDiv);
	}
	for(i = 0; i < extremeOutliers.length; i++) {
		var newDiv = document.createElement("div");
		newDiv.className = "boxplot-element";
		newDiv.style.background = "#666";
		newDiv.style.width="4px";
		newDiv.style.height="4px";
		newDiv.style.top = mextremeOutliers[i] + "px";
		newDiv.style.left= "50px";
		document.getElementById(overallID).appendChild(newDiv);
	}
	// labels
	var lowerLabel = document.createElement("div");
	lowerLabel.className = "boxplot-label";
	lowerLabel.innerHTML = "" + roundVal(lowerWhisker);
	lowerLabel.style.top = mlowerWhisker + "px";
	lowerLabel.style.left = "0px";
	document.getElementById(overallID).appendChild(lowerLabel);

	var q1Label = document.createElement("div");
	q1Label.className = "boxplot-label";
	q1Label.innerHTML = "" + roundVal(q1);
	q1Label.style.top = (mq1 - 9) + "px";
	q1Label.style.left = "80px";
	document.getElementById(overallID).appendChild(q1Label);

	var medianLabel = document.createElement("div");
	medianLabel.className = "boxplot-label";
	medianLabel.innerHTML = "" + roundVal(median);
	medianLabel.style.top = (mmedian - 9) + "px";
	medianLabel.style.left = "0px";
	document.getElementById(overallID).appendChild(medianLabel);

	var q3Label = document.createElement("div");
	q3Label.className = "boxplot-label";
	q3Label.innerHTML = "" + roundVal(q3);
	q3Label.style.top = (mq3 - 9) + "px";
	q3Label.style.left = "80px";
	document.getElementById(overallID).appendChild(q3Label);

	var upperLabel = document.createElement("div");
	upperLabel.className = "boxplot-label";
	upperLabel.innerHTML = "" + roundVal(upperWhisker);
	upperLabel.style.top = (mupperWhisker - 9) + "px";
	upperLabel.style.left = "0px";
	document.getElementById(overallID).appendChild(upperLabel);

	for (i = 0; i < mmildOutliers.length; i++) {
		var label = document.createElement("div");
		label.className = "boxplot-label";
		label.innerHTML = "" + roundVal(mildOutliers[i]);
		label.style.top = (mmildOutliers[i] - 9) + "px";
		if (i%2 == 0) {
			label.style.left = "10px";
		} else {
			label.style.left = "70px";
		}
		document.getElementById(overallID).appendChild(label);
	}
}
</script>

<!-- creation of the example data -->
<script type="text/javascript">
var dataArray = new Array(8.37, 2.09, 0.41, 5.08, 2.4, 3.16, 4.59, 0.17, 0.41, 7.72, 15.2, 0.01);

createBoxPlot(dataArray, 200, "content");

var dataArray2 = new Array(18.37, 22.09, 30.41, 25.08, 22.4, 23.16, 34.59, 20.17, 30.41, 37.72, 15.2, 20.01);
createBoxPlot(dataArray2, 200, "content2");
</script>
<!-- Tracking code -->
<script type="text/javascript">
<!--//--><![CDATA[//><!--
var gaJsHost = (("https:" == document.location.protocol) ? "https://ssl." : "http://www.");document.write(unescape("%3Cscript src='" + gaJsHost + "google-analytics.com/ga.js' type='text/javascript'%3E%3C/script%3E"));
//--><!]]>
</script>
<script type="text/javascript">
<!--//--><![CDATA[//><!--
try{var pageTracker = _gat._getTracker("UA-64695-4");pageTracker._trackPageview();} catch(err) {}
//--><!]]>
</script> 
<style type="text/css">
   div.boxplot-element {
	background: #fff;
	position: absolute;
	left: 40px;
	border: thin solid #000;
	width:30px;
	height: 0px;
}

div.boxplot-label {
	position: absolute;
	border: none;
}
</style>
<style type="text/css">
body {
 font: 100%/1.45 Georgia,serif;
}
/* Header style */
h1 {
	border-bottom:1px solid #222222;
	border-top:1px solid #222222;
	color:#222222;
	font-size:65px;
	font-weight:normal;
	margin:0;
	text-decoration:none;
	text-shadow:3px 3px 3px #C0C0C0;
}
a {
	color:#222222;
	text-decoration:none;
}
#header {
	text-align: center;
	width: 823px;
}

#header h1 {
	line-height:1.2;
}
#header h2 {
	text-align: left;
	margin-top: 5px;
}

#back_link {
	display: block;
	margin-top: 265px;
}

#back_link:hover {
	text-decoration: underline;
}
/***************************************
* Positioning of the box plot containers
****************************************/
div.boxplot-container {
	border: none;
	padding-bottom: 30px;
	position: absolute;
	left: 100px;
	top: 150px;
}

div#content2 {
	left: 300px;
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
    <a id="link" href="http://localhost:9000/home.jsp" title="Human Gene Expression Map Homepage">
        <!--img border="0" width="100" src="/NewLogo.png" alt="Human Gene Expression Map"/-->
        <img border="0" width="200" src="/logo-3.png" alt="Human Gene Expression Map"/>
        <!--a id="link" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html? keywords=E-MTAB-62"style="color:#3383bb">[E-MTAB-62]</a-->
    </a>

    <h1 style="float:right;color:#3383bb; border:none; background:none; font-family:Verdana, Helvetica, sans-serif;">
        ArrayExpress Ref:
        <a id="link" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=E-MTAB-62">[E-MTAB-62]</a>
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

    <!--><span class="BodyTextBlueSmall"><a href="javascript:window.print()"><img src="/silk_print.gif"></a>  </span>
    <span class="BodyTextBlueSmall"><a href="" title="Print results"><img src="/silk_print.gif" alt="Print results"/></a></span>
    <span class="BodyTextBlueSmall"><a href="" title="Save results in a Tab-delimited format"><img src="/silk_save_txt.gif" alt="Save results in a Tab-delimited format"/></a></span>
    <span class="BodyTextBlueSmall"><a href="" title="Open results table in Excel"><img src="/silk_save_xls.gif" alt="Open results table in Excel"/></a></span>
    <span class="BodyTextBlueSmall"><a href="" title="Get RSS feed with first page results matching selected criteria"><img src="/silk_save_feed.gif" alt="Open results as RSS feed"/></a></span><-->

    <%--><span class="ui-state-default ui-corner-all" title=".ui-icon-print"><span class="ui-icon ui-icon-print"></span></span><--%>

    <span class='Breadcrumb' style="margin-left:270px;">You are here:
        <SPAN class='Breadcrumbon'>
            <a href="http://localhost:9000/home.jsp">Home</a> >
            <a href="http://localhost:9000/GE.jsp">GE</a>   <!--  NO...LOCAL PAGE NN HA IL LINK!!!!! -->
        </SPAN>
    </span>


</table>


<!-- Drop down menu-->

<div class="nav_query">
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

        <!--form>
            <input name="radioset" type="radio" checked="checked" onclick="disable(), new_border('green')" value="Disable list">Differential expression
            <a href="#" id="dialog_link" class="ui-state-default ui-corner-all">More</a><br>
            <input name="radioset" type="radio" onclick="enable(), new_border('#3d53f2')" value="Enable list">Gene expression across condition
            <a href="#" id="dialog_link_2" class="ui-state-default ui-corner-all">More</a> <br>
            <input name="radioset" type="radio" onclick="enable(), new_border('#cc0000')" value="Enable list">Differentially expressed probeset<br>for a gene
            <a href="#" id="dialog_link_3" class="ui-state-default ui-corner-all">More</a> <br>
        </form-->
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
<!--div id="content" style="background:orange"-->
<div class="ui-state-default ui-corner-all" style="padding:10px ; margin-left:265px; margin-right:10px;background:white;">
    <!--div style="-moz-border-radius: 5px; -webkit-border-radius: 5px; border: 1px solid #3d53f2; padding: 5px;" -->
    <b>Gene Expression across condition</b>
    <br> <br>

    <!-- div id="Boxplot" -->

    <!--img border="0" width="250" src="/The.4.meta.groups_AAK1_bOXPLOT.png" alt="Boxplot"/-->
        <ul id="icons" class="ui-widget ui-helper-clearfix">
            <li class="ui-state-default ui-corner-all" title=".ui-icon-print"><span class="ui-icon ui-icon-print"></span></li>
        </ul>

        <!--img src="/The.4.meta.groups_AAK1_bOXPLOT.png"-->



    <?php include "nav.inc"; ?>
    <div id="boxplot" style="margin:20px;height:240px; width:640px;"></div>
    <br> <br>
    <div id="content" class="boxplot-container">
</div>
<div id="content2" class="boxplot-container">
</div>
    <br><br>
    
    <b>Ensembl Gene Annotation</b> <br>
    <p style="font:bold">Probes:</p><br>
    <p style="font:bold">Ensembl ID:</p> <br>
    <p style="font:bold">HGNC symbol:</p> <br>
    <p style="font:bold">Description:</p> <br>
</div>

<!--/div-->
<!--/div-->
<!-- Drop down menu-->


</body>

<jsp:include page= '/WEB-INF/jsp/includes/end_body.jsp' />