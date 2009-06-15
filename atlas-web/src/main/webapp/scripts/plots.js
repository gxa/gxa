function drawPlot(jsonObj, plot_id){
	   	if(jsonObj.series){
   			var plot = $.plot($('#'+plot_id), jsonObj.series, jsonObj.options);
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
				                 showThumbTooltip(item.pageX, item.pageY,item.series.label);   
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
                        xaxis: { min: xMin-10, max: xMax+10  }, yaxis: {labelWidth:40}
                        }));
        }
        else{

        plot = $.plot($('#'+plot_id), jsonObj.series,$.extend(true, {}, jsonObj.options, {
                        grid:{ backgroundColor: '#fafafa',	autoHighlight: true, hoverable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin-1, to: xMax+1 }, color: '#FFFFCC' }]},
			 yaxis: {labelWidth:40}
                        }));
                        if(overviewDiv.height()!=0){
            overview = $.plot($('#'+plot_id+'_thm'), jsonObj.series,$.extend(true,{},jsonObj.options,{color:['#999999','#D3D3D3']}));

            overview.setSelection({ xaxis: { from: xMin-10, to: xMax+10 }});
            }
                      	}
	}
	
	function showThumbTooltip(x, y, contents) {
		if(contents!='Mean'){
	        $('<div id="tooltip">' + contents + '</div>').css( {
		            position: 'absolute',
		            display: 'none',
		            top: y+5,
		            left:x+5,
		            border: '1px solid #fdd',
		            'background-color': '#fee'
	       	 }).appendTo("body").fadeIn(200);
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
    
    function plotZoomOverview(jsonObj,plot){
    	var divElt = $('#plot_thm');
		divElt.width(500);
		divElt.height(60);
		
		overview = $.plot($('#plot_thm'), jsonObj.series, $.extend(true,{},jsonObj.options,{yaxis: {ticks: 0, labelWidth: 40, min: -plot.getData()[0].yaxis.datamax*0.25},points:{show: false}, grid:{backgroundColor:'#F2F2F2', markings:null,autoHighlight: false},legend:{show:false}, colors:['#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3','#999999','#D3D3D3']}));
		$("#plot_thm #plotHeader").remove();
		bindZooming(overview,jsonObj,plot);
		
    }
    
    
    function bindZooming(overview,jsonObj){
    	$("#zoomin").show();
    	$("#zoomout").show();
    	
    	$('#plot').unbind("plotselected");
    	$('#plot').bind("plotselected", function (event, ranges) {
			// do the zooming
			plot = $.plot($('#plot'), jsonObj.series,	$.extend(true, {}, jsonObj.options, {
			  					xaxis: { min: ranges.xaxis.from, max: ranges.xaxis.to }, yaxis: { labelWidth: 40 }
			       	 	}));

			// don't fire event on the overview to prevent eternal loop
			overview.setSelection(ranges, true);
			return plot;
							
	    });
	    $('#plot_thm').unbind("plotselected");
		$('#plot_thm').bind("plotselected", function (event, ranges) {
			plot.setSelection(ranges);
		});
		
		$("#zoomin").unbind("click");
		$("#zoomin").bind("click",function(){
			var f,t,max,range,oldf,oldt;
			max = plot.getData()[0].data.length;
			if(overview.getSelection() != null ){
				oldf=overview.getSelection().xaxis.from;
				oldt=overview.getSelection().xaxis.to;
				range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
			}else{
				range = max;
				oldt=max;
				oldf=0;
			}
			var windowSize = Math.floor(2/3*range);
			var offset = Math.floor((range-windowSize)/2);
			f=oldf+offset;
			t=Math.floor(oldt-offset);
			$('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});
					   		
		});
		$("#zoomout").unbind("click");
		$("#zoomout").bind("click",function(){
			var f,t,max,range,oldf,oldt;
			max = plot.getData()[0].data.length;
			if(overview.getSelection() != null ){
				range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
				oldf=overview.getSelection().xaxis.from;
				oldt=overview.getSelection().xaxis.to;
			}else{
				return;
			}
			var windowSize = Math.floor(3/2*range);//alert(windowSize);
			var offset = Math.max(Math.floor((windowSize-range)/2),2);
			f= Math.max(oldf-offset,0);
			t= Math.min(Math.floor(oldt+offset),max);
			
			$('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});
			if(f==0 && t == max) overview.clearSelection(true);
		});

                // zoom out completely on double click
		$("#zoomout").unbind("dblclick");
		$("#zoomout").bind("dblclick", function() {
			var max = plot.getData()[0].data.length;
			$('#plot').trigger("plotselected",{ xaxis: { from: 0, to: max }});
                        overview.clearSelection(true);
                });

		$("#panright > img").unbind("click");
		$("#panright > img").bind("click",function(){
			var f,t,max,range,oldf,oldt;
			max = plot.getData()[0].data.length;
			if(overview.getSelection() != null ){
				range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
				oldf=overview.getSelection().xaxis.from;
				oldt=overview.getSelection().xaxis.to;
			}else{
				return;
			}
			t= Math.min(oldt+3,max);
			f= t-range;
			
			$('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});		   		
		});

		$("#panleft > img").unbind("click");
		$("#panleft > img ").bind("click",function(){
			var f,t,max,range,oldf,oldt;
			max = plot.getData()[0].data.length;
			if(overview.getSelection() != null ){
				range = Math.floor(overview.getSelection().xaxis.to - overview.getSelection().xaxis.from);
				oldf=overview.getSelection().xaxis.from;
				oldt=overview.getSelection().xaxis.to;
			}else{
				return;
			}
			f= Math.max(oldf-3,0);
			t= f+range;
			$('#plot').trigger("plotselected",{ xaxis: { from: f, to: t }});		   		
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
    
    function highlightSamples(sc,scv,scText,assay){
    	var sampleAttrJSON = eval('(' + sampleAttrs+ ')' );
    	var assay2samplesJSON = eval('(' + assay2samples+ ')' );
    	
    	clearSelections();
    	
    	if(!assay){
    	
	    	for (i = 0; i < assay2samplesJSON.length; ++i){
	    		var sample_id = assay2samplesJSON[i][0];
	    		var value = eval("sampleAttrJSON."+sample_id+"."+sc);
	    		
	    		if(value==scv){
	    			plot.highlight(0, i);
	    			prevSelections.push(i);
	    		}
    		}
    	}else{
    		var assays = eval('(' + assayIds+ ')' ); 
    		for (i = 0; i < assays.length; ++i){
	    		var assay_id = assays[i];
	    		var value = eval("sampleAttrJSON.a"+assay_id+"."+sc);
	    		if(value==scv){
	    			plot.highlight(0, i);
	    			prevSelections.push(i);
	    		}
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
   					plot = $.plot($('#plot'), jsonObj.series,$.extend(true, {}, jsonObj.options, {yaxis:{labelWidth:40}} ));

   					drawEFpagination(eid,gid,ef,'large');
   					assayIds = jsonObj.assay_ids;
   					if(initial){
	   					sampleAttrs = jsonObj.sAttrs; 
	   					assay2samples = jsonObj.assay2samples;
	   					characteristics = jsonObj.characteristics;
	   					charValues = jsonObj.charValues;
	   					
	   					
	   					//var charJSON = eval('('+characteristics+')');
						
						var names= eval('('+jsonObj.geneNames+')');
						var gids= eval('('+jsonObj.GNids+')');
						var gName = names[0];
						var gID = gids[0];
						
						genesToPlot[gName]=gID;
						$("#plot").bind("mouseleave",function(){
							$("#tooltip").remove();
						});
	   					
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
					    var previousPoint = null;
					    $("#plot").bind("plothover",function(event,pos,item){
					    	if(item){
					    		if (previousPoint != item.datapoint) {
                    				previousPoint = item.datapoint;
                    				$("#tooltip").remove();
                    				showSampleTooltip(item.dataIndex,item.pageX,item.pageY);
					    		}
					    		
					    	} else {
					                $("#tooltip").remove();
					                previousPoint = null;            
					            }
					    	
					    });
					    
					   drawZoomControls();
				        
   					}
   					plotZoomOverview(jsonObj,plot);
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
    
    
    function drawZoomControls(){
    	var contents="";
    	contents= '<div id="zoomin"  style="z-index:1; position:relative; left: 0px; top: 5px;cursor:pointer;display:hidden"><img style="cursor:pointer" src="images/zoomin.gif" title="Zoom in"></div>' +
    		  '<div id="zoomout" style="z-index:1; position: relative; left: 0px; top: 5px;cursor:pointer; display:hidden"><img src="images/zoomout.gif" title="Zoom out"></div>' +
    		  '<div id="panright" style="z-index:2;position: relative; left: 20px; top: -35px;cursor:pointer; display:hidden"><img src="images/panright.gif" title="pan right"></div>' +
    		  '<div id="panleft" style="z-index:2;position: relative; left: -15px; top: -69px;cursor:pointer; display:hidden"><img src="images/panleft.gif" title="pan left"></div>';
    	$("#zoomControls").html(contents);

    	$("#zoomin > img").hover(
				function(){
						$(this).attr("src","images/zoominO.gif");},
				function(){
				        	$(this).attr("src","images/zoomin.gif");
				        }).mousedown(function(){
      						$(this).attr("src","images/zoominC.gif");
    					}).mouseup(function(){
      						$(this).attr("src","images/zoominO.gif");
    					});
		$("#zoomout > img").hover(
				function(){
						$(this).attr("src","images/zoomoutO.gif");},
				function(){
				        	$(this).attr("src","images/zoomout.gif");
				        }).mousedown(function(){
      						$(this).attr("src","images/zoomoutC.gif");
    					}).mouseup(function(){
      						$(this).attr("src","images/zoomoutO.gif");
    					});
    	$("#panright > img").hover(
				function(){
						$(this).attr("src","images/panrightO.gif");},
				function(){
				        	$(this).attr("src","images/panright.gif");
				        }).mousedown(function(){
      						$(this).attr("src","images/panrightC.gif");
    					}).mouseup(function(){
      						$(this).attr("src","images/panrightO.gif");
    					});
		$("#panleft > img").hover(
				function(){
						$(this).attr("src","images/panleftO.gif");},
				function(){
				        	$(this).attr("src","images/panleft.gif");
				        }).mousedown(function(){
      						$(this).attr("src","images/panleftC.gif");
    					}).mouseup(function(){
      						$(this).attr("src","images/panleftO.gif");
    					});
    }
    
    function showSampleTooltip(i,x,y){
    	var sampleAttrJSON = eval('(' + sampleAttrs+ ')' );
		var assay2samplesJSON = eval('(' + assay2samples+ ')' );
		var charJSON = eval('('+characteristics+')');
		var charValuesJSON = eval('('+charValues+')');
			            
		var sample_id = assay2samplesJSON[i][0];            
		var contents = '<ul style="padding-left:0px;margin:0px;list-style-type:none">';
			for (i = 0; i < charJSON.length; ++i) {
			  	 var characteristic = charJSON[i];
			   	 var value = eval("sampleAttrJSON."+sample_id+"."+characteristic);
            	 var txtChar = curatedChars[characteristic];
            	 contents+= '<li style="padding:0px"><span style="font-weight: bold">'+txtChar+':</span> '+value+'</li>';
            }
       contents+='</ul>';
      
		$('<div id="tooltip">' + contents + '</div>').css( {
            position: 'absolute',
            display: 'none',
            top: y + 5,
            'text-align':'left',
            left: x + 5,
            border: '1px solid #005555',
	    margin: '0px',
            'background-color': '#EEF5F5'
        }).appendTo("body").fadeIn("fast");
		
		
    }

	function populateSimMenu(jsonObj){
		var names = eval('(' + jsonObj.geneNames+ ')' );
		var GNs = eval('(' + jsonObj.GNids+ ')' );
		var DEs = eval('(' + jsonObj.DEids+ ')' );
		var AD = jsonObj.ADid;
		$("#simSelect").empty();
//		$("#simSelect").addOption("", "select gene");
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
