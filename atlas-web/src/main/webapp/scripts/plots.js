function drawPlot(jsonObj, plot_id){
	   	if(jsonObj.series){
   			var plot = $.plot($('#'+plot_id), jsonObj.series,jsonObj.options); 
			var overview;
			var allSeries = plot.getData();
			var tokens = plot_id.split('_');
        	var eid = tokens[0];
        	var gid = tokens[1]; 
			
			if(allSeries.length >10){
				var divElt = $('#'+plot_id+'_thm');
				divElt.width(300);divElt.height(50);
				overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']})); 
				//$('#'+plot_id+'_thm').show();
				//overview.setSelection({ xaxis: { from: 0, to: allSeries.length } });
				
			}else{
				var divElt = $('#'+plot_id+'_thm').empty();
				divElt.width(0);divElt.height(0);
			}
			
			
					
			$('#'+plot_id).bind("plotselected", function (event, ranges) {
        		// do the zooming
        		plot = $.plot($('#'+plot_id), jsonObj.series,	$.extend(true, {}, jsonObj.options, {
           					xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }
                    	 	}));

				if(allSeries.length >10){
					// don't fire event on the overview to prevent eternal loop
					overview.setSelection(ranges, true);
				}
    		});
    
			$('#'+plot_id+'_thm').bind("plotselected", function (event, ranges) {
			        					plot.setSelection(ranges);
									  });
				    
			
				    
			
			//Tooltip
			var previousPoint = null;
			$('#'+plot_id).bind("plothover", function (event, pos, item) {

        		if (item) {
          			if (previousPoint != item.datapoint) {
          			
				          previousPoint = item.datapoint;
				                $("#tooltip").remove();
				                 var x = item.datapoint[0].toFixed(2),
				                     y = item.datapoint[1].toFixed(2);
				                     
				                 showTooltip(item.pageX, item.pageY,item.series.label,plot_id);
				                    
				                }
        		}else {
				          $("#tooltip").remove();
				           previousPoint = null;            
				       }
			});
        				
        			/*	
        			//Legend Colors
        			var allSeries = plot.getData();
        			var tokens = plot_id.split('_');
        			var eid = tokens[0];
        			var gid = tokens[1];
        			for (var i = 0; i < allSeries.length; ++i){
        				var color = allSeries[i].color;
        				var label = allSeries[i].label.toLowerCase().replace(/ /g,'');
        				
        				$("#"+eid+'_'+gid+'_'+label+'_td').css('background-color',color);
        			}*/	
   					 

				return plot;	
				}// if(o.series)
	}
 

	function bindMarkings(jsonObj,plot, plot_id){
	
					//Markings 
					var tokens = plot_id.split('_');
        			var eid = tokens[0];
        			var gid = tokens[1];
        			
        			//unbind from previous events bound to previous plots (different EFs) otherwise they keep adding up.
        			$("#"+eid+'_'+gid+'_tbl').find("input").unbind();
        			
					$("#"+eid+'_'+gid+'_tbl').find("input").click(function(){
						var tokens = this.value.split('_');
						var EFV=tokens[0];
						var EF=tokens[1];
						if(this.checked){
							markClicked(eid,gid,EF,uni2efv(EFV),plot,jsonObj);
						}
						else{
						
						plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                          				grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1}
                      					}));
                      	overview.clearSelection();
                      	
						}
					});
	
	}

    function uni2ent(srcTxt) {
      var entTxt = '';
      var c, hi, lo;
      var len = 0;
      for (var i=0, code; code=srcTxt.charCodeAt(i); i++) {
        var rawChar = srcTxt.charAt(i);
        // needs to be an HTML entity
        if (code > 255) {
          // normally we encounter the High surrogate first
          if (0xD800 <= code && code <= 0xDBFF) {
            hi  = code;
            lo = srcTxt.charCodeAt(i+1);
            // the next line will bend your mind a bit
            code = ((hi - 0xD800) * 0x400) + (lo - 0xDC00) + 0x10000;
            i++; // we already got low surrogate, so don't grab it again
          }
          // what happens if we get the low surrogate first?
          else if (0xDC00 <= code && code <= 0xDFFF) {
            hi  = srcTxt.charCodeAt(i-1);
            lo = code;
            code = ((hi - 0xD800) * 0x400) + (lo - 0xDC00) + 0x10000;
          }

          // wrap it up as Hex entity
          c = "&#" + code.toString(10) + ";";
        }
        else {
          c = rawChar;
        }
        entTxt += c;
        len++;
      }
      return entTxt;
    }

	function markClicked(eid,gid,ef,efv,plot,jsonObj){
								
		var plot_id = eid+'_'+gid+'_plot';
		var allSeries = plot.getData();
		var series;
		var markColor;
        
      	for (var i = 0; i < allSeries.length; ++i){
      		if(allSeries[i].label){
       		 	if(allSeries[i].label.toLowerCase()==efv.toLowerCase()){
       		 		series = allSeries[i];
       		 		markColor = series.color
       		 		break;
       	 		}
       	 	}
		}

        if(series==null){
            return null;
        }

        var data = series.data;
        var xMin= data[0][0]
        var xMax= data[data.length-1][0]

        //var seriesoptions = $.extend(true,{},series.lines,{show:true,lineWidth:1, fill:true})

        //var testSeries=[];

        //testSeries.push(series);
        ////allSeries.push(testSeries);
        //var modSeries = $.extend(true,{},series,{lines:{show:true,lineWidth:1, fill:true}})
        //alert(testSeries);

        //plot = $.plot($('#'+plot_id), allSeries,o.options);
        var overviewDiv = $('#'+plot_id+'_thm');
        if(allSeries.length>10 && data.length<5){



            //showThumbnail(eid+'_'+gid);


            if(overviewDiv.height()!=0){
            overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,jsonObj.options);

            overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
            }
            plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                        grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#FFFFCC' }]},
                        xaxis: { min: xMin-10, max: xMax+10 }
                        }));
        }
        else{

        plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                        grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#FFFFCC' }]}
                        }));
                        if(overviewDiv.height()!=0){
            overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']}));

            overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
            }
                      	}
	}
	
	function showTooltip(x, y, contents, plot_id) {
		if(contents!='Mean'){
	        $('<div id="tooltip">' + contents + '</div>').css( {
		            position: 'absolute',
		            display: 'none',
		            top: 50,
		            left:x-500,
		            border: '1px solid #fdd',
		            padding: '2px',
		            'background-color': '#fee',
		            opacity: 0.80
	       	 }).appendTo("#"+plot_id).fadeIn(200);
    	}
    }
    
    function redrawPlotForFactor(eid,gid,ef,mark,efv){

        var plot_id;
        ef = "ba_"+ef;
        plot_id= eid+"_"+gid+"_plot";
        $.ajax({
   			type: "POST",
   			url:"plot.jsp",
   			data:"gid="+gid+"&eid="+eid+"&ef="+ef+"&plot=bar",
   			dataType:"json",
   			success: function(o){
   					var plot = drawPlot(o,plot_id);
					//bindMarkings(o,plot,plot_id);
					if(mark){
						markClicked(eid,gid,ef,uni2ent(efv),plot,o);
					}
			}
 			});
 			drawEFpagination(eid,gid,ef,'bar');
    }
    
    function plotZoomOverview(jsonObj){
    	var divElt = $('#plot_thm');
		divElt.width(600);divElt.height(55);
		overview = $.plot($('#plot_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{yaxis: {ticks: 1 },legend:{show:false}, colors:['#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3']}));
		$("#plot_thm #plotHeader").remove();
		bindZooming(overview,jsonObj);
    }
    
    function bindZooming(overview,jsonObj){
    	$('#plot').unbind("plotselected");
    	$('#plot').bind("plotselected", function (event, ranges) {
			// do the zooming
			plot = $.plot($('#plot'), jsonObj.series,	$.extend(true, {}, jsonObj.options, {
			  					xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }
			       	 	}));

			// don't fire event on the overview to prevent eternal loop
			overview.setSelection(ranges, true);
							
	    });
	    $('#plot_thm').unbind("plotselected");
		$('#plot_thm').bind("plotselected", function (event, ranges) {
			plot.setSelection(ranges);
		});
    }
    
    function showExps(row){
    	
    	
    	var gid = $("#"+row.id+" #gene").val();
        var ef = "ba_"+$("#"+row.id+" #ef").val();
        var efv = $("#"+row.id+" #efv").val();
    	
    	//$("div[id*=_"+i+"]")
    	
    	
    	var i = row.id.split("_")[2];
    	$("div[id$=_"+i+"]").each(function(){
    	
    	var plot_id = this.id;
        var tokens = plot_id.split('_');
        var eid = tokens[0];
        var updn = tokens[1];
        var divEle = $(this);
        if(!$(this).hasClass("done")){
        $.ajax({
   			type: "POST",
   			url:"plot.jsp",
   			data:"gid="+gid+"&eid="+eid+"&ef="+ef+"&efv="+efv+"&updn="+updn+"&plot=thumb",
   			dataType:"json",
   			
   			success: function(jsonObj){
   				if(jsonObj.series){
   					var plot = $.plot(divEle, jsonObj.series,jsonObj.options); 
   				}
   				//var plot = drawPlot(o,plot_id);//success
   				
   				//bindMarkings(o,plot,plot_id);
   				//$('#'+plot_id).bind("plotclick", function(){
				//	openInAEW(eid);
				//});
   				}
 			});//ajax
        
        $(this).addClass("done")
        }
    	});
    }
    
    
    function clearSelections(){
    	for(i=0; i<prevSelections.length; ++i){
    		plot.unhighlight(0,prevSelections[i]);
    	}
    }
    
    function highlightSamples(sc,scv,scText){
    	var sampleAttrJSON = eval('(' + sampleAttrs+ ')' );
    	var assay2samplesJSON = eval('(' + assay2samples+ ')' );
    	
    	clearSelections();
    	 
    	
    	for (i = 0; i < assay2samplesJSON.length; ++i){
    		var sample_id = assay2samplesJSON[i][0];
    		var value = eval("sampleAttrJSON."+sample_id+"."+sc);
    		
    		if(value==scv){
    			plot.highlight(0, i);
    			prevSelections.push(i);
    		}
    	}
    	$("#bioSampleData").html('<div> <ul> <li><span style="font-weight: bold">'+scText+'</span>:'+scv+'</li></ul></div>');
    }
    
    
    function hideGene(gid){
    	plotBigPlot(gid,eid,ef,initial);
    }
    
    function plotBigPlot(gid,eid,ef,initial,geneIndeces){
    	if(ef=="")
    		ef="default";
    	else
    		ef="ba_"+ef;
    	
    
    	
    	$.ajax({
    		type: "POST",
    		url: "plot.jsp",
    		data: "gid="+gid+"&eid="+eid+"&ef="+ef+"&plot=large"+"&gplotIds="+geneIndeces,
    		dataType:"json",
    		success: function(jsonObj){
    			if(jsonObj.series){
   					plot = $.plot($('#plot'), jsonObj.series,jsonObj.options);
   					plotZoomOverview(jsonObj);
   					drawEFpagination(eid,gid,ef,'large');
   					 
   					if(initial){
   						
	   					sampleAttrs = jsonObj.sAttrs; 
	   					assay2samples = jsonObj.assay2samples;
	   					characteristics = jsonObj.characteristics;
	   					charValues = jsonObj.charValues;
	   					
	   					//var charJSON = eval('('+characteristics+')');
	            		//var charValuesJSON = eval('('+charValues+')');
	   					
	   					/*
	   					var contents = '';
			            for (i = 0; i < charJSON.length; ++i) {
			            	 var characteristic = charJSON[i];
			            	 contents+= '<div><a href="#">'+characteristic+'</a></div> <div> <ul>';
			            	 
			            	 var values = eval("charValuesJSON."+characteristic);
			            	 for(j=0; j<values.length; ++j){
			            	 	contents= contents+'<li><a href="javascript:highlightSamples(\''+characteristic+'\',\''+values[j]+'\')" >'+values[j]+'</a></li>';
			            	 }
			            	  
			            	 contents+= '</ul></div>';
			            }
	            
			            //$("#accordion").html(contents);
			            */
						
						var names= eval('('+jsonObj.geneNames+')');
						var gids= eval('('+jsonObj.GNids+')');
						var gName = names[0];
						var gID = gids[0];
						
						genesToPlot[gName]=gID;
						
	   					
	   					$("#plot").bind("plotclick", function (event, pos, item) {
				        if (item) {
				            clearSelections();
				            
				            plot.highlight(item.series, item.datapoint);
				            prevSelections = new Array();
				            prevSelections.push(item.datapoint);
				            
				            var sampleAttrJSON = eval('(' + sampleAttrs+ ')' );
				            var assay2samplesJSON = eval('(' + assay2samples+ ')' );
				            var charJSON = eval('('+characteristics+')');
				            var charValuesJSON = eval('('+charValues+')');
				            
				            var sample_id = assay2samplesJSON[item.dataIndex][0];
				            
				            var contents = '<div> <ul>';
				            for (i = 0; i < charJSON.length; ++i) {
				            	 var characteristic = charJSON[i];
				            	 var value = eval("sampleAttrJSON."+sample_id+"."+characteristic);

				            	 var txtChar = curatedChars[characteristic];
				            	 contents+= '<li><span style="font-weight: bold">'+txtChar+':</span> '+value+'</li>';
				            }
				            
				            contents+='</ul></div>';
				            $("#bioSampleData").html(contents);
				        }
					    });
					    
					   
				        
   					}
   					
   					populateSimMenu(jsonObj);
   					 $(".rmButton").hover(function(){
				        	$(this).attr("src","images/closeButtonO.gif");
				        },function(){
				        	$(this).attr("src","images/closeButton.gif");
				        });
				        
				        $(".rmButton").each(function(){
				        	$(this).click(function(){
				        		removeGene(this.id);
				        	});
				        });	
	   			}
    		}
    	});
    }
    

	function populateSimMenu(jsonObj){
		var names = eval('(' + jsonObj.geneNames+ ')' );
		var GNs = eval('(' + jsonObj.GNids+ ')' );
		var DEs = eval('(' + jsonObj.DEids+ ')' );
		var AD = jsonObj.ADid;
		$("#simSelect").empty();
		$("#simSelect").addOption("", "select gene");
		for(i=0; i<GNs.length; i++){
			var gnName = names[i];
			var DE = DEs[i];
			var GNid = GNs[i];
			var key = DE+"_"+AD;
			$("#simSelect").addOption(key, gnName);
		}
		$("#simSelect").selectOptions("select gene", true);	
	}

	function drawPlots(){

			$(".plot").each(function(){
        	var plot_id = this.id;
        	var tokens = plot_id.split('_');
        	var eid = tokens[0];
        	var gid = tokens[1]; 
        	$.ajax({
   			type: "POST",
   			url:"plot.jsp",
   			data:"gid="+gid+"&eid="+eid+"&plot=bar",
   			dataType:"json",
   			
   			success: function(o){
   				var plot = drawPlot(o,plot_id);//success
   				//bindMarkings(o,plot,plot_id);
   				}
 			});//ajax
        	
        }); //each



	}//drawPlots redrawPlotForFactor(eid,gid,ef,plotType,mark,efv)
	

	
	function drawEFpagination(eid,gid,currentEF,plotType){
		
    	var panelContent = [];
    	
    	var EFs = $("#"+eid+"_EFpagination *").each(function(){
    		var ef = $(this).attr("id");
    		var ef_txt = $(this).html();
    		ef = jQuery.trim(ef);
    		if("ba_"+ef == currentEF){
    			panelContent.push("<span id='"+ef+"' class='current'>"+ef_txt+"</span>")
    		}
    		else{
    			if(plotType=="large"){
    				panelContent.push('<a id="'+ef+'" onclick="redrawForEF( \''+eid+'\',\''+ef+'\',\''+ef_txt+'\')">'+ef_txt+'</a>');
    			}else
    				panelContent.push('<a id="'+ef+'" onclick="redrawPlotForFactor( \''+eid+'\',\''+gid+'\',\''+ef+'\',\''+plotType+'\',false)">'+ef_txt+'</a>');
    		}
    		});
					
    	$("#"+eid+"_EFpagination").empty();
    	$("#"+eid+"_EFpagination").html(panelContent.join(""));
    }
