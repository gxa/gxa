(function($){
     atlas.counter = 0;
     atlas.helprow = null;

     function createRemoveButton(callback)
     {
         return $('<td class="rm" />').append($('<input type="button" value=" - " />').click(callback));
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

     function hasConditions(state) {
         if(state) {
             $('#structsubmit').removeAttr('disabled');
             $('#structclear').removeAttr('disabled');
             var h = $('#helprow');
             if(h.length)
                 atlas.helprow = h.remove();
         } else {
             if($('#conditions tr').length == 0) {
                 $('#structsubmit').attr('disabled','disabled');
                 $('#structclear').attr('disabled','disabled');
                 if(helprow)
                    $('#conditions').append(atlas.helprow);
             }
         }
     }

     function addSpecie(specie) {
         ++atlas.counter;
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

         var value = $('<td class="specval value">' + specie + '<input class="specval" type="hidden" name="specie_' + atlas.counter + '" value="' + specie + '"></td>');
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
                                                 hasConditions(false);
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
             tr.after($('<tr class="speccond"><td class="left"></td></tr>').append(value).append(remove));
         } else {
             tbody.prepend($('<tr class="speccond"><td class="left">organism</td></tr>')
                           .append(value).append(remove));
         }
         hasConditions(true);
     }

     function addExpFactor(factor,expression,values,expansion) {
         var selopt = $('#factors').get(0).options;
         var factorLabel = factor;
         for(var i = 0; i < selopt.length; ++i)
             if(selopt[i].value == factor) {
                 factorLabel = selopt[i].text.toLowerCase();
             }

         if(factor == "")
             factorLabel = "any condition";

         ++atlas.counter;


         var input = $('<input type="text" class="value"/>')
             .attr('name', "fval_" + atlas.counter)
             .autocomplete(atlas.homeUrl + "fval", atlas.makeFvalAcOptions(factor, false))                 
             .flushCache();

         var tr = $('<tr class="efvcond" />')
             .append($('<td class="left" />')
                 .append(createSelect("fexp_" + atlas.counter, options['expressions'], false, expression))
                 .append('&nbsp;&nbsp;&nbsp;')
                 .append(factorLabel)
                 .append($('<input type="hidden" name="fact_' + atlas.counter + '" value="'+ factor +'">')))
             .append($('<td class="value" />').append(input))
             .append(createRemoveButton(function () {
                                            var tr = $(this).parents('tr:first');
                                            var tbody = tr.parents('tbody:first').get(0);
                                            tr.remove();
                                            hasConditions(false);
                                        }));

         if(values != null) {
             input.val(values);
             input.keyup(function () { if(values != this.value) tr.find("td.expansion").text(''); });
         }

         var t = $('tr.efvcond:last,tr.speccond:last', $('#conditions'));
         if(t.length)
             t.eq(0).after(tr);
         else
             $('#conditions').append(tr);

         input.defaultvalue('(all ' + (factor != '' ? factorLabel.toLowerCase() : 'condition') + 's)');
         
         hasConditions(true);
     }

     function getPropLabel(property)
     {
         var selopt = $('#geneprops').get(0).options;
         var propertyLabel = property;
         for(var i = 0; i < selopt.length; ++i)
             if(selopt[i].value == property) {
                 propertyLabel = selopt[i].text.toLowerCase();
             }

         if(property == "")
             propertyLabel = "any property";

         return propertyLabel;
     }

     function addGeneQuery(property,values,not) {

         ++atlas.counter;


         var label = getPropLabel(property);
         var input = $('<input type="text" class="value"/>')
             .attr('name', "gval_" + atlas.counter)
             .val(values != null ? values : "")
             .autocomplete(atlas.homeUrl + "fval", atlas.makeGeneAcOptions(property))
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
                     })

         var tr = $('<tr class="genecond" />')
             .append($('<td class="left" />')
                 .append($('<select  name="' + ('gnot_' + atlas.counter) + '"><option ' + (not ? '' : 'selected="selected"') + 'value="">has</option><option'
                  + (not ? ' selected="selected"' : '') + ' value="1">hasn&#39;t</option></select>'))
                 .append('&nbsp;&nbsp;&nbsp;')
                 .append($('<span class="gprop" />').text(label))
                 .append($('<input type="hidden" name="gprop_' + atlas.counter + '" value="'+ property +'">')))
             .append($('<td class="value" />').append(input))
             .append(createRemoveButton(function () {
                                            var tr = $(this).parents('tr:first');
                                            var tbody = tr.parents('tbody:first').get(0);
                                            tr.remove();
                                            hasConditions(false);
                                        }));

         $('#conditions').append(tr);
         input.defaultvalue('(all ' + (property != "" ? label.toLowerCase() : 'gene') +'s)');

         hasConditions(true);
     }

     atlas.initStructForm = function(lastquery) {

         atlas.initSimpleForm();

         $(".genename a").tooltip({
                                      bodyHandler: function () {
                                          return $(this).next('.gtooltip').html();
                                      },
                                      showURL: false
                                  });

         $('#geneprops').change(function () {
                                    if(this.selectedIndex >= 1) {
                                        atlas.structMode();
                                        var property = this.options[this.selectedIndex].value;
                                        addGeneQuery(property);
                                    }
                                    this.selectedIndex = 0;
                                });


         $('#species').change(function () {
                                  if(this.selectedIndex >= 1) {
                                      atlas.structMode();
                                      var specie = this.options[this.selectedIndex].value;
                                      addSpecie(specie);
                                  }
                                  this.selectedIndex = 0;
                              });

         $('#factors').change(function () {
                                  if(this.selectedIndex >= 1) {
                                      atlas.structMode();
                                      var factor = this.options[this.selectedIndex].value;
                                      addExpFactor(factor);
                                  }
                                  this.selectedIndex = 0;
                              });

         var form = $('#structform');
         form.bind('submit', function () {
             $('input.ac_input', form).hideResults();
             atlas.startSearching(form);
             atlas.structSubmit();
             return true;
         });

         var i;
         if(lastquery && lastquery.species.length) {
             for(i = 0; i < lastquery.species.length; ++i)
                 addSpecie(lastquery.species[i]);
         }

         if(lastquery && lastquery.conditions.length) {
             for(i = 0; i < lastquery.conditions.length; ++i)
                 addExpFactor(lastquery.conditions[i].factor,
                              lastquery.conditions[i].expression,
                              lastquery.conditions[i].values,
                              lastquery.conditions[i].expansion);
         }

         if(lastquery && lastquery.genes.length) {
             for(i = 0; i < lastquery.genes.length; ++i)
         	 addGeneQuery(lastquery.genes[i].property,
                              lastquery.genes[i].query,
                              lastquery.genes[i].not);
         }
     };

     atlas.structSubmit = function() {
         var i = 0;
         $('input.specval').each(function(){ this.name = this.name.replace(/_\d+/, '_' + (++i)); });

         i = 0;
         $('#conditions tr.efvcond,#conditions tr.genecond').each(function(){
                                                                      $('input,select', this).each(function(){ this.name = this.name.replace(/_\d+/, '_' + i); });
                                                                      ++i;
                                                                  });
         $('#condadders,#species,#geneprops,#factors').remove();
     };

     atlas.clearQuery = function() {
         $('#conditions td.rm input').click();
         $('#conditions td.rm input').click();
         $('#gene0,#fval0,#grop0').val('');
         $('#species0,#expr0').each(function () { this.selectedIndex = 0; });
         atlas.simpleMode();
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

     function drawExperimentPlots(gene_id, ef, efv) {

         function drawPlot(jsonObj, plot_id, efv){
             if(jsonObj.series) {
                 jsonObj.options.legend.container = "#" + plot_id + "_legend";
                 jsonObj.options.legend.extContainer = null;
                 jsonObj.options.selection = null;

                 var series = null;
                 var markColor = null;
                 for (var i = 0; i < jsonObj.series.length; ++i){
                     if(jsonObj.series[i].label){
                         if(jsonObj.series[i].label.toLowerCase() == efv.toLowerCase()){
                             series = jsonObj.series[i];
                             markColor = series.color;
                             break;
                         }
                     }
                 }

                 for (var i = 0; i < jsonObj.series.length; ++i)
                     if(jsonObj.series[i].label && jsonObj.series[i].label.length > 20)
                         jsonObj.series[i].label = jsonObj.series[i].label.substring(0, 20) + '...';

                 if(!series)
                     return;

                 var data = series.data;
                 var xMin= data[0][0] - 0.5;
                 var xMax= data[data.length-1][0] + 0.5;

                 var plotel = $('#'+plot_id);
                 $.plot(plotel, jsonObj.series,
                         $.extend(true, {}, jsonObj.options, {
                             grid:{ backgroundColor: '#fafafa', autoHighlight: true, hoverable: false, clickable: true, borderWidth: 1, markings: [{ xaxis: { from: xMin, to: xMax }, color: '#FFFFCC' }]}
                         }));

                 var link = $('#' + plot_id + '_link');
                 if(link)
                     plotel.bind('click', function (e) { location.href = link.attr('href');e.stopPropagation();return false; })
                             .bind('mousedown', function (e) { e.stopPropagation();return false; });

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

     atlas.hmc = function (igene, iefv, event) {
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

         var waiter = $('<div id="waiter" />').append($('<img/>').attr('src','images/indicator.gif'))
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

     atlas.structMode = function() {
         if($('.visinstruct:visible').length)
            return;
         $('.visinsimple').hide();
         $('.visinstruct').show();
     };

     atlas.simpleMode = function() {
         if($('#simpleform:visible').length)
            return;
         $('.visinsimple').show();
         $('.visinstruct').hide();
     };

     atlas.startSearching = function(form) {
         var v = $(form).find('input[type=submit]');
         v.val('Searching...');
     };


 })(jQuery);
