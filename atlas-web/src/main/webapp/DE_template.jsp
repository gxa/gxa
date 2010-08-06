<jsp:include page= 'template.jsp' />

<head>    
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


      google.visualization.events.addListener(table, 'sort',
          function(event) {
            sortData.sort([{column: event.column, desc: !event.ascending}]);
          });
    }


    google.setOnLoadCallback(drawVisualization);
    </script>

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

    <!-- Mouse over function-->
    <script type="text/javascript">
        /***********************************************
        * Highlight Table Cells Script- © Dynamic Drive DHTML code library (www.dynamicdrive.com)
        * Visit http://www.dynamicDrive.com for hundreds of DHTML scripts
        * This notice must stay intact for legal use
        ***********************************************/

        //Specify highlight behavior. "TD" to highlight table cells, "TR" to highlight the entire row:
        var highlightbehavior="TD"

        var ns6=document.getElementById&&!document.all
        var ie=document.all

        function changeto(e,highlightcolor){
            source=ie? event.srcElement : e.target
            if (source.tagName=="TABLE")
                return
            while(source.tagName!=highlightbehavior && source.tagName!="HTML")
                source=ns6? source.parentNode : source.parentElement
            if (source.style.backgroundColor!=highlightcolor&&source.id!="ignore")
                source.style.backgroundColor=highlightcolor
        }

        function contains_ns6(master, slave) { //check if slave is contained by master
            while (slave.parentNode)
                if ((slave = slave.parentNode) == master)
                    return true;
            return false;
        }

        function changeback(e,originalcolor){
           if (ie&&(event.fromElement.contains(event.toElement)||source.contains(event.toElement)||source.id=="ignore")||source.tagName=="TABLE")

                return
            else if (ns6&&(contains_ns6(source, e.relatedTarget)||source.id=="ignore"))
                return
            if (ie&&event.toElement!=source||ns6&&e.relatedTarget!=source)
                source.style.backgroundColor=originalcolor
        }

    </script>

    
</head>

 <span class='Breadcrumb' style="margin-left:270px;">You are here:
        <SPAN class='Breadcrumbon'>
            <a href="http://localhost:9000/home_hge_atlas.jsp">Home</a> >
            <a href="http://localhost:9000/PCA.jsp">DE</a>   <!--  NO...LOCAL PAGE NN HA IL LINK!!!!! -->
        </SPAN>
    </span>

<div id="content" style="padding-top:3px;" >
<div class="ui-state-default ui-corner-all" style="padding:10px; background:white;">

    <b>Differential Expression</b>
    <a href="" title="Print results"><img align="right" src="/silk_print.gif" alt="Print results"hspace="10px;"onClick="window.print()"/></a>
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

        <div class="h" align="right">Search Gene:
        <label>
            <input type="text" id="search_gene" style="margin-left:20px;" onkeydown="doSearch(arguments[0]||event)"/>
        </label>
        <button onclick="gridReload()" id="submitButton" style="margin-left:20px;">Search</button>
</div>


<br/>
        <div id="table"onMouseover="changeto(event, '#cdcf73')" onMouseout="changeback(event, 'white')"style="overflow:scroll;"></div>
        <!--div id="table" style="overflow:scroll;"></div--> 
       
    </div>


</div>

</div>