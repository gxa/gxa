<%@ page import="java.util.*" %>
<link href="/print.css" media="print" rel="stylesheet" type="text/css">
<link href="/css-common.css" media="screen" rel="stylesheet" type="text/css">
<link href="/homeJquery.css" media="screen" rel="stylesheet" type="text/css">
<link type="text/css" media="screen, print" href="/hge_includes/jquery-ui-1.8.2.custom/css/overcast/jquery-ui-1.8.2.custom.css" rel="Stylesheet" />



<head>

<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-1.4.2.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/js/jquery-ui-1.8.2.custom.min.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.core.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.widget.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.accordion.js"></script>
<script type="text/javascript" src="/hge_includes/jquery-ui-1.8.2.custom/development-bundle/ui/jquery.ui.dialog.js"></script>

<script type="text/javascript" src="/hge_includes/scripts/functions.js"></script>


<!-- Load drop-down menu data -->
<script type="text/javascript">
      window.onload=bindFactors();
      window.onload=bindGenes();
</script>

</head>



<jsp:include page= '/WEB-INF/jsp/includes/start_head.jsp' />
HGE
<jsp:include page= '/WEB-INF/jsp/includes/end_head.jsp' />


<body>

<jsp:include page= '/WEB-INF/jsp/includes/start_body_no_menus_max.jsp' />
<jsp:include page= '/WEB-INF/jsp/includes/end_menu.jsp' />


<!--  page contents  here -->
<table style="margin:0 0 10px 0;width:100%;height:30px;"id="ignore">

     <a class="link" href="http://localhost:9000/home_hge_atlas.jsp" title="Human Gene Expression Map Homepage">
        <img border="0" width="200" src="/logo-3.png" alt="Human Gene Expression Map" />
    </a>

    <h1 style="float:right;color:#0067AC; border:none; background:none; font-family:Verdana, Helvetica, sans-serif;">
        Array Express Ref:
        <a class="link" href="http://www.ebi.ac.uk/microarray-as/ae/browse.html?keywords=E-MTAB-62">[E-MTAB-62]</a>
    </h1>
    <br>
    <hr>
    <a href="" title="Print the page"><img align="right" src="/silk_print.gif" alt="Print"hspace="10px;"onClick="window.print()"/></a>

    <span class='Breadcrumb'style="margin-left:240px;">You are here:
        <SPAN class='Breadcrumbon'><a class="link" href="http://localhost:9000/home_hge_atlas.jsp">Home</a>
        </SPAN>
    </span>

</table>


<div class="nav_query_home">
    <div id="rightcolumn_print"style="media:print">

    <div style=" background-color: #fdfee9; -moz-border-radius: 5px; -webkit-border-radius: 5px; border: 1px solid #8A8D09; padding: 5px;" >
        <span style="color:#8A8D09; font-weight: bold;">Release</span>  <br><br>
         <div class="box_right"><p>Total samples:    5372  </p>  </div><br>
         <div class="box_right"><p>Biological groups:    369   </p>  </div>  <br>
         <div class="box_right"><p>Total studies:    206  </p>    </div> <br>
         <div class="box_right"><p>N. laboratories:   163</p>   </div>  <br>
    </div>
    <br>
</div>

    <div class="ui-state-default ui-corner-all" style="padding:10px">

    <label> Query for:  </label> <br>
        <form>
            <label>
                <input name="radioset" type="radio" checked="checked" onclick="disableMenu()" value="Disable list">
            </label>Differential expression
            <br>
            <input name="radioset" type="radio" onclick="enableMenu()" value="Enable list">Gene expression across condition
            <br>
            <input name="radioset" type="radio" onclick="enableMenu()" value="Enable list">Differentially expressed<br>probeset for a gene
            <br>
        </form>

        <br>
        <div class="clearfix">
           Choose a classification: <a href="http://localhost:9000/PCA_template.jsp" > ?</a> <br>
            <label>
                <SELECT id="ddlClass"></SELECT>
            </label>
            <br>
            and choose a gene:  <br>
            <label>
                <SELECT id="ddlGene" disabled="disabled"></SELECT>
            </label>
        <br><br>

        <input type="submit" value="Show results" onclick="queryExperiment(document.getElementById('ddlClass').value,document.getElementById('ddlGene').value);return false;">
        <br> <br>
        </div>
    </div>
    <br>



    <div class="ui-state-default ui-corner-all" style="padding:10px">
    <b>Published:</b>
    <p>Nature Biotechnology 2010 Apr</p><br>
    <a href="http://www.nature.com/nbt/journal/v28/n4/full/nbt0410-322.html" title="Paper">A global map of human gene expression</a>
    <br><br>Margus Lukk&sup1;, Misha Kapushesky&sup1;, Janne Nikkila&sup2;,
		Helen Parkinson&sup1;, Angela Goncalves&sup1;, Wolfgang Huber&sup1;, Esko Ukkonen&sup2;,
		Alvis Brazma&sup1;<br><br>
		&sup1; EMBL-EBI<br>&sup2; University of Helsinki<br><br>
       </div>
</div>





<div id="rightcolumn">

    <div style=" background-color: #fdfee9; -moz-border-radius: 5px; -webkit-border-radius: 5px; border: 1px solid #8A8D09; padding: 5px;" >
        <span style="color:#8A8D09; font-weight: bold;">Release</span>  <br><br>
         <div class="box_right"><p>Total samples:    5372  </p>  </div><br>
         <div class="box_right"><p>Biological groups:    369   </p>  </div>  <br>
         <div class="box_right"><p>Total studies:    206  </p>    </div> <br>
         <div class="box_right"><p>N. laboratories:   163</p>   </div>  <br>
    </div>
    <br>
</div>


<div id="content_home">
    <div style=" background-color: #edf6fc; -moz-border-radius: 5px; -webkit-border-radius: 5px; border: 1px solid #0067AC; padding: 5px;" >
	    <br/><b style="color:#0067AC;padding:5px;padding-top:10px;">About the Human Gene Expression (HGE) Atlas</b>
        <p style="padding:5px;text-align:justify;">We have constructed a global gene expression map by integrating microarray data from 5,372 human samples
            representing 369 different cell and tissue types, disease states and cell lines. </p>
        <p style="padding:5px;text-align:justify;">We collected over 9,000 raw data files generated on the human gene expression array
            Affymetrix U133A from the public databases Gene Expression Omnibus (http://www.ncbi.nlm.nih.gov/geo/)
            and ArrayExpress (http://www.ebi.ac.uk/microarray-as/ae/).
            <br/>After we removed duplicate files and applied strict quality controls, data on 5,372 samples from 206 different studies generated in 163 different laboratories remained.
            The raw data were normalized jointly, producing a gene expression matrix of ~22,000 probe sets
            (mapping to ~14,000 genes) times 5,372 samples
            (the complete annotated data set is available from the ArrayExpress repository, accession number E-MTAB-62). </p>
        <p style="padding:5px;text-align:justify;">Using text mining and curation, we binned the samples in 369 biological groups,
            each representing a particular cell or tissue type, disease state or cell line.
            We also introduced 'meta-groups' such as cell lines, neoplasms, non-neoplastic diseases, and normal,
            as well as groups by tissue of origin. </p>
        <p style="padding:5px;text-align:justify;">We applied principal component analysis (PCA) and hierarchical clustering to the expression matrix,
            and produced biologically meaningful classifications that can be queried for differential expression through this interface.</p>
        <p style="padding:5px;text-align:justify;">For additional information we suggest reading the paper:  <a href="http://www.nature.com/nbt/journal/v28/n4/full/nbt0410-322.html" title="Paper">A global map of human gene expression</a> </p>
        <br>
    </div>
    <br>

<!--div id="accordion"-->

<!-- ui-dialog -->
<div id="dialog" title="Differential expression" >
	<p>Interrogate the HGE dataset by querying for a biological condition of interest
            to find all genes up od down-regulated.</p>
</div>

<div id="dialog_2" title="Gene expression across condition">
	<p>Interrogate the HGE dataset by querying for a gene of interest
            to find biological conditions where it is differentially expressed.</p>
</div>
<div id="dialog_3" title="Differentially expressed probeset for a gene">
	<p>Interrogate the HGE dataset by querying for a gene in order
            to find whether it has differentially expressed probeset.</p>
</div>

	<b class="blacktext">Differential Expression
    <a href="#" id="dialog_link" class="ui-state-default ui-corner-all">More</a><br>   </b>
	<div>
		<p>Interrogate the HGE dataset by querying for a biological condition of interest
            (within one of the available classifications) to find all differentially expressed genes
            (both up- or down-regulated).<br/>
            Output:  </p>
        <ul>
            <li class="list1"><span class="blacktext">Table of under/over-expressed genes</span></li>
        </ul>

    </div>
	<b>Gene Expression across condition
     <a href="#" id="dialog_link_2" class="ui-state-default ui-corner-all">More</a> </b>
	<div>
		<p>
		Interrogate the HGE dataset by querying for a gene of interest to find biological conditions
        (within one of the available classifications) in which it is differentially expressed. <br>
        Output: </p>
        <ul>
            <li class="list2"><span class="blacktext">Boxplot</span></li>
        </ul>

    </div>
	<b>Differentially expressed probeset for a gene        </b>
     <a href="#" id="dialog_link_3" class="ui-state-default ui-corner-all">More</a>
	<div>
		<p>
		Interrogate the HGE dataset by querying for a gene in order
        to find whether it has differentially expressed probeset.
        Output:  </p>
        <ul >
             <li class="list3"><span class="blacktext">Table of differentially expressed probeset</span></li>
        </ul>
        <br/>

    </div>


<br/><br/>

</body>

<jsp:include page= '/WEB-INF/jsp/includes/end_body.jsp' />