var counter = 0;
var geneCounter = 0;

function escapeHtml(s) {
    return s.replace(/\"/g,"&quot;")
        .replace(/</g,"&lt;")
        .replace(/>/g,"&gt;")
        .replace(/&/g,"&amp;");
}

(function($){

     function createRemoveButton(callback)
     {
         return $('<td />').append($('<input type="button" value="-" />').click(callback));
     }

     function createNotButton(name,checked)
     {
         return $('<td><select name="' + name + '"><option ' + (checked ? '' : 'selected="selected"') + 'value="">has</option><option'
                  + (checked ? ' selected="selected"' : '') + ' value="1">has not</option></select></td>');
     }

     function createSelect(name, options, optional, value) {
         var e = document.createElement("select");
         if (name) {e.name = name;e.id=name;}
         if (optional) e.options[0] = new Option("(any)", "");
         if (options) {
             var selected = 0;
             for (var i = 0; i < options.length; i++) {
                 var option;
                 if (typeof(options[i]) == "object") {
                     option = new Option(options[i][1], options[i][0]);
                 } else {
                     option = new Option(options[i], options[i]);
                 }
                 if(value == option.value){
                     selected = e.options.length;
                 }
                 e.options[e.options.length] = option;
             }
             e.selectedIndex = selected;
         }
         return e;
     }

     function addSpecie(specie) {
         ++counter;
         var sel = $('#species').get(0);
         var found = false;
         for(var i = 0; i < sel.options.length; ++i)
             if(sel.options[i].value.toLowerCase() == specie.toLowerCase()) {
                 specie = sel.options[i].value;
                 sel.options[i] = null;
                 found = true;
                 break;
             }

         if(!found)
             return;

         var value = $('<td class="specval">' + specie + '<input class="specval" type="hidden" name="specie_' + counter + '" value="' + specie + '"></td>');
         var remove = createRemoveButton(function () {
                                             var tr = $(this).parents('tr:first');
                                             var rmvalue = $('input.specval', tr).val();
                                             var tbody = tr.parents('tbody:first');
                                             var specs = $('tr.speccond', tbody);
                                             if(specs.length > 1 && specs.index(tr.get(0)) == 0) {
                                                 var nextr = tr.next('tr');
                                                 $('td.specval', tr).replaceWith($('td.specval', nextr));
                                                 nextr.remove();
                                             } else {
                                                 tr.remove();
                                             }
                                             var i;
                                             var sel = $('#species').get(0);
                                             for(i = 0; i < sel.options.length; ++i)
                                                 if(sel.options[i].value >= rmvalue)
                                                     break;
                                             sel.options.add(new Option(rmvalue,rmvalue), i);
                                         });

         var tbody = $('#conditions');
         var tr = $('tr.speccond:last', tbody);
         if(tr.length > 0) {
             tr.after($('<tr class="speccond"><td colspan="2">or</td></tr>').append(value).append(remove).append($('<td />')));
         } else {
             tbody.prepend($('<tr class="speccond"><td colspan="2">species is</td></tr>')
                           .append(value).append(remove).append($('<td />')));
         }
     }

     function addExpFactor(factor,expression,values,expansion) {
         var selopt = $('#factors').get(0).options;
         var factorLabel = factor;
         for(var i = 0; i < selopt.length; ++i)
             if(selopt[i].value == factor) {
                 factorLabel = selopt[i].text;
             }

         if(factor == "")
             factorLabel = "";

         ++counter;


         var input = $('<input type="text" class="value"/>')
             .attr('name', "fval_" + counter)
             .autocomplete("fval", {
                               minChars:0,
                               matchCase: false,
                               matchSubset: false,
                               selectFirst: false,
                               multiple: false,
                               multipleSeparator: " ",
                               multipleQuotes: true,
                               scroll: false,
                               scrollHeight: 180,
                               max: 20,
                               extraParams: { type: 'efv', 'factor' : factor },
                               formatItem: function(row) { return row[0]; },
                               formatResult: function(row) { return row[0].indexOf(' ') >= 0 ? '"' + row[0] + '"' : row[0]; }
                           })
             .flushCache();

         var tr = $('<tr class="efvcond" />')
             .append($('<td />').append(createSelect("fexp_" + counter, options['expressions'], false, expression)))
             .append($('<td />').text('in ' + factorLabel).append($('<input type="hidden" name="fact_' + counter + '" value="'+ factor +'">')))
             .append($('<td />').append(input))
             .append(createRemoveButton(function () {
                                            var tr = $(this).parents('tr:first');
                                            var tbody = tr.parents('tbody:first').get(0);
                                            tr.remove();
                                        }))
             .append($('<td class="expansion" />').html(expansion != null ? expansion : ""));

         if(values != null) {
             input.val(values);
             input.keyup(function () { if(values != this.value) tr.find("td.expansion").text(''); });
         }

         var t = $('tr.efvcond:last,tr.speccond:last', $('#conditions'));
         if(t.length)
             t.eq(0).after(tr);
         else
             $('#conditions').append(tr);
     }

     function getPropLabel(property)
     {
         var selopt = $('#geneprops').get(0).options;
         var propertyLabel = property;
         for(var i = 0; i < selopt.length; ++i)
             if(selopt[i].value == property) {
                 propertyLabel = selopt[i].text;
             }

         if(property == "")
             propertyLabel = "property";

         return 'gene ' + propertyLabel;
     }

     function makeGeneAcOptions(property) {
         var acoptions = {
             minChars: property == "" ? 1 : 0,
             matchCase: false,
             matchSubset: false,
             selectFirst: false,
             multiple: false,
             multipleSeparator: " ",
             multipleQuotes: true,
             scroll: false,
             scrollHeight: 180,
             max: 20,
             extraParams: { type: 'gene', 'factor' : property },
             formatResult: function(row) { return row[1].indexOf(' ') >= 0 ? '"' + row[1] + '"' : row[1]; }
         };

         if(property == '') {
             acoptions.formatItem = function(row, num, max, val, term) {
                 var text = $.Autocompleter.defaults.highlight(row[1].length > 30 ? row[1] + '...' : row[1], term);
                 if(row[0] == 'name') {
                     var ext = row[3].split('$');
                     ext = ext[0] + ' ' + ext[1];
                     return '<nobr><em>gene:</em>&nbsp;' + text + '&nbsp;<em>' + ext + '</em></nobr>';
                 } else {
                     return '<nobr><em>' + row[0] + ':</em>&nbsp;' + text + '&nbsp;<em>(' + row[2] + ')</em></nobr>';
                 }
             };
             acoptions.highlight = function (value,term) { return value; };
             acoptions.width = '500px';
         } else {
             acoptions.formatItem = function(row, num, max, val, term) {
                 var text = $.Autocompleter.defaults.highlight(row[1].length > 50 ? row[1] + '...' : row[1], term);
                 return text + ' (' + row[2] + ')';
             };
             acoptions.highlight = function (value,term) { return value; };
             acoptions.width = '300px';
         }

         return acoptions;
     }

     function addGeneQuery(property,values,not) {

         ++counter;



         var input = $('<input type="text" class="value"/>')
             .attr('name', "gval_" + counter)
             .val(values != null ? values : "")
             .autocomplete("fval", makeGeneAcOptions(property))
             .flushCache()
             .result(function (unused, res) {
                         var newprop = res[0];
                         var tr = $(this).parents('tr:first');
                         var propi = tr.find('input[type=hidden]');
                         if(propi.val() == '' && newprop == 'name') {
                             newprop = 'identifier';
                             $(this).val(res[3].split('$')[2]);
                         }
                         propi.val(newprop);
                         tr.find('td.gprop').text(getPropLabel(newprop));
                         $(this).setOptions({extraParams: { type: 'gene', factor: newprop }}).flushCache();
                     });

         var tr = $('<tr class="genecond" />')
             .append(createNotButton('gnot_' + counter, not))
             .append($('<td class="gprop" />').text(getPropLabel(property)))
             .append($('<td />').append($('<input type="hidden" name="gprop_' + counter + '" value="'+ property +'">')).append(input))
             .append(createRemoveButton(function () {
                                            var tr = $(this).parents('tr:first');
                                            var tbody = tr.parents('tbody:first').get(0);
                                            tr.remove();
                                        }))
             .append($('<td />'));

         $('#conditions').append(tr);
     }

     window.initQuery = function() {

         var oldval = $('#gene0').val();
         $("#gene0")
             .defaultvalue("(all genes)","(all genes)")
             .autocomplete("fval", makeGeneAcOptions(""))
             .result(function (unused, res) {
                         var newprop = res[0];
                         if(res[0] == 'name') {
                             newprop = 'identifier';
                             $(this).val(res[3].split('$')[2]);
                         }
                         $('#gprop0').val(newprop);
                         var oldval = $(this).val();
                         this.onkeyup = function () { if(oldval != this.value) $('#gprop0').val(''); };
                         //  $(this).setOptions({extraParams: { type: 'gene', factor: newprop }}).flushCache();
                     }).get(0).onkeyup = function () { if(oldval != this.value) $('#gprop0').val(''); };


         var fval0old  = $("#fval0")
             .defaultvalue("(all conditions)")
             .autocomplete("fval", {
                               minChars:1,
                               matchCase: false,
                               matchSubset: false,
                               multiple: false,
                               selectFirst: false,
                               multipleQuotes: true,
                               multipleSeparator: " ",
                               scroll: false,
                               scrollHeight: 180,
                               max: 10,
                               extraParams: { type: 'efv', factor: '' },
                               formatItem: function(row) { return row[0]; },
                               formatResult: function(row) { return row[0].indexOf(' ') >= 0 ? '"' + row[0] + '"' : row[0]; }
                           })
             .keyup(function (e) { if(this.value != fval0old) $("#simpleform .expansion").remove(); }).val();

         $(".genename a").tooltip({
                                      bodyHandler: function () {
                                          return $(this).next('.gtooltip').html();
                                      },
                                      showURL: false
                                  });

         $('#geneprops').change(function () {
                                    if(this.selectedIndex == 0)
                                        return;

                                    var property = this.options[this.selectedIndex].value;
                                    addGeneQuery(property);
                                    this.selectedIndex = 0;
                                });


         $('#species').change(function () {
                                  if(this.selectedIndex == 0)
                                      return;

                                  var specie = this.options[this.selectedIndex].value;
                                  addSpecie(specie);

                                  this.selectedIndex = 0;
                              });

         $('#factors').change(function () {
                                  if(this.selectedIndex == 0)
                                      return;

                                  var factor = this.options[this.selectedIndex].value;
                                  addExpFactor(factor);
                                  this.selectedIndex = 0;
                              });


         if(lastquery && lastquery.species.length) {
             for(var i = 0; i < lastquery.species.length; ++i)
                 addSpecie(lastquery.species[i]);
         }

         if(lastquery && lastquery.conditions.length) {
             for(var i = 0; i < lastquery.conditions.length; ++i)
                 addExpFactor(lastquery.conditions[i].factor,
                              lastquery.conditions[i].expression,
                              lastquery.conditions[i].values,
                              lastquery.conditions[i].expansion);
         }

         if(lastquery && lastquery.genes.length) {
             for(var i = 0; i < lastquery.genes.length; ++i)
         	 addGeneQuery(lastquery.genes[i].property,
                              lastquery.genes[i].query,
                              lastquery.genes[i].not);
         }
     };

     window.renumberAll = function() {
         var i = 0;
         $('input.specval').each(function(){ this.name = this.name.replace(/_\d+/, '_' + (++i)); });

         i = 0;
         $('#conditions tr.efvcond,#conditions tr.genecond').each(function(){
                                                                      $('input,select', this).each(function(){ this.name = this.name.replace(/_\d+/, '_' + i); });
                                                                      ++i;
                                                                  });
         $('#species,#geneprops,#factors').remove();
     };

     function adjustPosition(el) {
         var v = {
             x: $(window).scrollLeft(),
             y: $(window).scrollTop(),
             cx: $(window).width(),
             cy: $(window).height()
         };

         var h = el.get(0);
         // check horizontal position
         if (v.x + v.cx < h.offsetLeft + h.offsetWidth) {
             var left = h.offsetLeft - (h.offsetWidth + 20 + 15);
             el.css({left: left + 'px'});
         }
         // check vertical position
         if (v.y + v.cy < h.offsetTop + h.offsetHeight) {
             var top = h.offsetTop - (h.offsetHeight + 20 + 15);
             el.css({top: top + 'px'});
         }
     }

     window.hmc = function (igene, iefv, event) {
         $("#expopup").remove();

         var gene = resultGenes[igene];
         var efv = resultEfvs[iefv];

         var left;
         var top;
         if ( event.pageX == null && event.clientX != null ) {
             var e = document.documentElement, b = document.body;
             left = event.clientX + (e && e.scrollLeft || b.scrollLeft || 0);
             top = event.clientY + (e && e.scrollTop || b.scrollTop || 0);
         } else {
             left = event.pageX;
             top = event.pageY;
         }         
         left += 15;
         top += 15;

         var waiter = $('<div id="waiter" />').append($('<img/>').attr('src','expandwait.gif'))
                 .css({ left: left + 'px', top: top + 'px' });

         $('body').append(waiter);
         adjustPosition(waiter);

         $.ajax({
                    mode: "queue",
                    port: "sexpt",
                    url: 'experiments.jsp',
                    dataType: "html",
                    data: { gene:gene.geneAtlasId, ef: efv.ef, efv: efv.efv },
                    complete: function(resp) {
                        $('#waiter').remove();

                        var popup = $('<div id="expopup" />')
                            .html(resp.responseText)
                            .prepend($("<div/>").addClass('closebox')
                                     .click(
                                         function(e) {
                                             popup.remove();
                                             e.stopPropagation();
                                             return false;
                                         }).text('close'))
                            .click(function(e){e.stopPropagation();})
                            .attr('title','')
                            .css({ left: left + 'px', top: top + 'px' });

                        $('body').append(popup);

                        // adjust for viewport
                        adjustPosition(popup);

                        drawExperimentPlots(gene.geneAtlasId, efv.ef, efv.efv);
                    }
                });
     };

     window.structMode = function() {
         $("#simpleform").hide('fast');
         $("#structform").show('fast');
     };

     window.simpleMode = function() {
         $("#structform").hide('fast');
         $("#simpleform").show('fast');
     };

     window.drawExperimentPlots = function(gene_id, ef, efv) {

         function drawPlot(jsonObj, plot_id, efv){
             if(jsonObj.series) {
                 jsonObj.options.legend.container = "#" + plot_id + "_legend";
                 jsonObj.options.legend.extContainer = null;

                 var plot = $.plot($('#'+plot_id), jsonObj.series, jsonObj.options);
                 var overview;
                 var allSeries = plot.getData();

                 var series = null;
                 var markColor = null;
                 for (var i = 0; i < allSeries.length; ++i){
      		     if(allSeries[i].label){
       		 	 if(allSeries[i].label.toLowerCase()==efv.toLowerCase()){
       		 	     series = allSeries[i];
       		 	     markColor = series.color;
       		 	     break;
       	 		}
       	 	     }
		 }

                 if(!series)
                     return;

                 var data = series.data;
		 var xMin= data[0][0] - 0.5;
		 var xMax= data[data.length-1][0] + 0.5;

		 plot = $.plot($('#'+plot_id), jsonObj.series,
                               $.extend(true, {}, jsonObj.options, {
                          		    grid:{ backgroundColor: '#fafafa', autoHighlight: true, hoverable: false, borderWidth: 1, markings: [{ xaxis: { from: xMin, to: xMax }, color: '#e8cfac' }]}
                      			}));

             }
         };


	 $(".plot").each(function() {
                             var plot_id = this.id;
                             var eid = plot_id.split('_')[1];
                             $.ajax({
   			                type: "GET",
   			                url: "plot.jsp",
   			                data: { gid: gene_id, eid: eid, ef: 'ba_' + ef },
   			                dataType: "json",
   			                success: function(o){
   			                    drawPlot(o, plot_id, efv);
   			                }
 		                    });

                         });
     };


 })(jQuery);
