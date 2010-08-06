
    jQuery(function($){
        // Accordion
	    $("#accordion").accordion({ header: "h3" });

		// Tabs
		$('#tabs').tabs();

        // Dialog
		$('#dialog').dialog({
				autoOpen: false,
				width: 600,
				buttons: {
					"Ok": function() {
						$(this).dialog("close");
					}
				}
		});
        $('#dialog_2').dialog({
			    autoOpen: false,
				width: 600,
				buttons: {
					"Ok": function() {
						$(this).dialog("close");
					}
				}
		});
        $('#dialog_3').dialog({
				autoOpen: false,
				width: 600,
				buttons: {
					"Ok": function() {
						$(this).dialog("close");
					}
				}
		});

		// Dialog Link
		$('#dialog_link').click(function(){
				$('#dialog').dialog('open');
				return false;
		});
        $('#dialog_link_2').click(function(){
				$('#dialog_2').dialog('open');
				return false;
	    });
        $('#dialog_link_3').click(function(){
				$('#dialog_3').dialog('open');
				return false;
	    });

		// Datepicker
		$('#datepicker').datepicker({
				inline: true
		});

		// Slider
		$('#slider').slider({
				range: true,
				values: [17, 67]
		});

		// Progressbar
		$("#progressbar").progressbar({
				value: 20
		});


				//hover states on the static widgets
		//		$('#dialog_link, ul#icons li').hover(
		//			function() { $(this).addClass('ui-state-hover'); },
		//			function() { $(this).removeClass('ui-state-hover'); }
		//		);
    });

    function new_border(color)
    {
        document.getElementById("nav_query_border").style.borderColor = color;
    }

    var _data;


    function getData(url, callback){

        var head = document.getElementsByTagName('head');
        var script = document.createElement('script');
        script.type = "text/javascript";

        var src = url + '&format=json&callback=' + callback + '&' + Math.random();

        script.src = src;
        head[0].appendChild(script);

        return _data;

        /* more code to run on page load */
    } 



    function processData(data){
        _data = data;
        alert(_data);
    }
    function bindFactorsCallback(data){
        if(data.error) {
            alert(data.error);
            return;
        }
        var experiment = data.results[0];
        var numOfExperimentalFactors = experiment.experimentDesign.experimentalFactors.length;
        for(var j = 0; j < numOfExperimentalFactors; ++j) {
            var experimentalFactor = experiment.experimentDesign.experimentalFactors[j];
            var ddl = document.getElementById("ddlClass");
            ddl.options[ddl.options.length] = new Option(experimentalFactor,experimentalFactor,false,false);
        }
    }
    function bindFactors(){
        getData("http://www.ebi.ac.uk/gxa/api?experiment=E-AFMX-5","bindFactorsCallback");
    }
    function bindGenesCallback(data){
        if(data.error) {
            alert(data.error);
            return;
        }
        var numOfGenes = data.results.length;
        for(var j = 0; j < numOfGenes; ++j) {
            var gene = data.results[j].gene.id;
            var ddl = document.getElementById("ddlGene");
            ddl.options[ddl.options.length] = new Option(gene,gene,false,false);
        }
    }


    function bindGenes(){
        var genes = getData("http://www.ebi.ac.uk/gxa/api?upInExperiment=E-AFMX-5","bindGenesCallback");
    }       


          function showPlot() {
              var selectedGene = document.getElementById("ddlGene").value;
              var selectedFactor = document.getElementById("ddlClass").value;
              document.getElementById("imgBoxPlot").src = "BoxPlot.jsp?Gene=" + selectedGene + "&Factor=" + selectedFactor;
          }


    function queryExperiment(experimentId, geneId) {
        var head = document.getElementsByTagName('head');
        var script = document.createElement('script');
        script.type = "text/javascript";
        var src = atlasHomeUrl + "/api?experiment="+escape(experimentId)+"&format=json&callback=processData";
        var geneIds = geneId.split(" ");
        for(var g in geneIds)
            src += "&ddlGene="+escape(geneIds[g]);
        src += '&' + Math.random();
        script.src = src;
        head[0].appendChild(script);
    }


    if(location.href.indexOf('?') >= 0) {
        var experimentId = '';
        var geneIds = '';
        var params = location.href.substr(location.href.indexOf('?') + 1).split('&');
        for(var p in params) {
            var kv = params[p].split('=');
            if(kv[0] == 'experiment')
                experimentId = unescape(kv[1]);
            else if(kv[0] == 'gene')
                geneIds += unescape(kv[1]);
        }
        if(experimentId != '')
            queryExperiment(experimentId, geneIds);
    } else {
        queryExperiment('E-MEXP-748', 'ENSMUSG00000025867 ENSMUSG00000070385');
    }



    function disableMenu(){
        document.getElementById("ddlGene").disabled=true;
    }


    function enableMenu(){
        document.getElementById("ddlGene").disabled=false;
    }
  function query_hge(){
     if(request.getParameter("radioset") != null) {
                if(request.getParameter("radioset").equals("DE")) {
                    window.open("http://localhost:9000/DE.jsp");
                }
                else if(request.getParameter("radioset").equals("GE")) {
                    window.open("http://localhost:9000/GE.jsp");
                }
                else if(request.getParameter("radioset").equals("Pr")) {
                    window.open("http://localhost:9000/GE.jsp");
                }
            }
      }


      $(function() {
                $("#btnPaypal").hover(
                    function() { $("#thirsty").toggle(); },
                    function() { $("#thirsty").toggle(); }
                );
            });
            $(function() {
                dp.SyntaxHighlighter.ClipboardSwf = '/js/clipboard.swf';
                dp.SyntaxHighlighter.HighlightAll('code');

                $("#btnTest1").click( function() {
                    $('#divTest1').jqprint();
                    return false;
                });

                $("#btnTest2").click( function() {
                    $('.divToPrint').jqprint({ debug: true });
                    return false;
                });

                $("#btnTest3").click( function() {
                    $('.divToPrint').jqprint({ printContainer: false });
                    return false;
                });

                $("#btnTest4").click( function() {
                    $('#divOpera').jqprint({ operaSupport: true });
                    return false;
                });
            });


     function createDynTable(row, col)
     {
         oContextMenu.innerHTML="";
        //oContextObject=oTarget;

         iMenuItems=0;

         oContextMenu.setCapture();
         // get the reference for the body

         var mybody = document.getElementsByTagName("body")[0];var mydiv = document.createElement("div");
         // creates a <table> element and a <tbody> element

         mytable = document.createElement("table");
         mytablebody = document.createElement("tbody");
         // creating all cells

         for(var j = 0; j < row; j++) {
             // creates a <tr> element
             mycurrent_row = document.createElement("tr");

             for(var i = 0; i < col; i++) {
                 // creates a <td> element
                 mycurrent_cell = document.createElement("td");
                 // add edit cell;
                 mycurrent_cell.ondblclick = function (evt) { editCell(this);};
                 // creates a text node
                 currenttext = document.createTextNode("cell is row "+j+", column "+i);
                 // appends the text node we created into the cell <td>
                 mycurrent_cell.appendChild(currenttext);
                 // appends the cell <td> into the row <tr>
                 mycurrent_row.appendChild(mycurrent_cell);
             }

             // appends the row <tr> into <tbody>
             mytablebody.appendChild(mycurrent_row);
         }

         // appends <tbody> into <table>
         mytable.appendChild(mytablebody);
         mydiv.appendChild(mytable);
         // appends <table> into <body>
         mybody.appendChild(mytable);
         // sets the border attribute of mytable to 2;
         mytable.setAttribute("border", "2");

         //return mytable;
         oContextMenu.innerHTML=mytable;
         iMenuItems++;

     }

 