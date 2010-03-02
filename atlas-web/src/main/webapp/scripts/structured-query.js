/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

var atlas;
var resultEfvs;
var resultGenes;

if(!atlas)
    atlas = {};

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
             .val(values != null ? values : "");

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

         atlas.tokenizeConditionInput(input, factor, '(all ' + (factor != '' ? factorLabel : 'condition') + 's)');
         
         var t = $('tr.efvcond:last,tr.speccond:last', $('#conditions'));
         if(t.length)
             t.eq(0).after(tr);
         else
             $('#conditions').append(tr);

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
             .val(values != null ? values : "");

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
                                            tr.remove();
                                            hasConditions(false);
                                        }));

         $('#conditions').append(tr);

         atlas.tokenizeGeneInput(input, property, '(all ' + (property != "" ? label.toLowerCase() : 'gene') +'s)');

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

         atlas.experimentsTemplate = $('#experimentsTemplate').compile({
             '.@id+': function () { return Math.random(10000); },
             'div.head a@href': 'gene?gid=#{gene.identifier}',
             '.gname': 'gene.name',
             '.numup': 'numUp',
             '.numdn': 'numDn',
             '.ef': 'eftext',
             '.efv': 'efv',
             '.experRows': {
                 'experiment <- experiments': {
                     '.expaccession': 'experiment.accession',
                     '.expname': 'experiment.name',
                     'table.oneplot': {
                         'ef <- experiment.efs': {
                             '.efname': 'ef.eftext',
                             '.@id': function(a) { return 'oneplot_' + a.context.counter++; },
                             'a.proflink@href': 'experiment?gid=#{gene.identifier}&eid=#{experiment.accession}'
                         }
                     },
                     '.@class+': function(a) { return (a.pos != a.items.length - 1) ? ' notlast' : ''; },
                     'a.proflink2@href': 'experiment?gid=#{gene.identifier}&eid=#{experiment.accession}',
                     'a.detailink@href': '/microarray-as/ae/browse.html?keywords=#{experiment.accession}&detailedview=on'
                 }
             }
         });
         $('#experimentsTemplate').remove();
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

    function drawPlot(jsonObj, root, efvs){
        if(jsonObj.series) {
            var efvsh = {};
            for(var iefv in efvs)
                efvsh[efvs[iefv].efv.toLowerCase()] = efvs[iefv];

            if(!jsonObj.options)
                jsonObj.options = {};
            if(!jsonObj.options.legend)
                jsonObj.options.legend = {};
            jsonObj.options.legend.container = root.find('.legend');
            jsonObj.options.legend.extContainer = null;
            jsonObj.options.selection = null;

            var height = 1;
            var nlegs = 0;
            var markings = [];
            for (var i = 0; i < jsonObj.series.length; ++i){
                if(jsonObj.series[i].label) {
                    var series = jsonObj.series[i];
                    var efv = efvsh[jsonObj.series[i].label.toLowerCase()];
                    if(efv) {
                        var data = series.data;
                        var xMin= data[0][0] - 0.5;
                        var xMax= data[data.length-1][0] + 0.5;

                        markings.push({ xaxis: { from: xMin, to: xMax }, color: '#FFFFCC' });

                        if(series.label.length > 30)
                            series.label = series.label.substring(0, 30) + '...';
                        var pvalue = efv.pvalue < 1e-16 ? '&lt;1e-16' : efv.pvalue.toExponential(2);
                        series.label = '<span class="' + (efv.isup ? 'expup' : 'expdn') + '">'
                                + series.label + '<br />'
                                + (efv.isup ? '&#8593;' : '&#8595;') + '&nbsp;' + pvalue + '</span>';
                        series.legend.show = true;
                        series.legend.isefv = true;
                        height += 2;
                        nlegs += 1;
                    } else if(series.legend.show) {
                        if(nlegs < 6) {
                            if(series.label.length > 30)
                                series.label = series.label.substring(0, 30) + '...';
                            height += 1;
                            nlegs += 1;
                        } else
                            series.legend.show = false;
                    }
                }
            }

            for (i = 0; nlegs > 6 && i < jsonObj.series.length; ++i) {
                series = jsonObj.series[i];
                if(!series.legend.isefv && series.legend.show) {
                    series.legend.show = false;
                    --nlegs;
                    --height;
                }
            }

            var plotel = root.find('.plot');
            if(height > 5) {
                if(height > 10)
                    height = 10;
                plotel.css({ height: (height * 16 + 20) + 'px' });
            }
            
            root.find('.waiter').remove();
            root.find('.efname,.plot').show();

            jsonObj.options.grid.markings = markings;

            $.plot(plotel, jsonObj.series, jsonObj.options);
        }
    }

    atlas.hmc = function (igene, iefv, event) {
         $("#expopup").remove();

         var gene = resultGenes[igene];

         var efv; var efo;
         if(isNaN(iefv))
             efo = iefv;
         else
             efv = resultEfvs[iefv];

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
                    type: "GET",
                    url: 'experiments',
                    dataType: "json",
                    data: {
                        gene:gene,
                        ef: efo ? 'efo' : efv.ef,
                        efv: efo ? efo : efv.efv
                    },
                    success: function(resp) {
                        $('#waiter').remove();
                        if(resp.error) {
                            alert(resp.error);
                            return;
                        }
                        resp.counter = 0;
                        var tpl = $('<div/>');
                        var popup = $('<div id="expopup" />')
                            .append($("<div/>").addClass('closebox')
                                     .click(
                                         function(e) {
                                             popup.remove();
                                             e.stopPropagation();
                                             return false;
                                         }).text('close'))
                            .append(tpl)
                            .click(function(e){e.stopPropagation();})
                            .attr('title','')
                            .css({ left: left + 'px', top: top + 'px' });

                        $('body').append(popup);
                        tpl.render(resp, atlas.experimentsTemplate);

                        // adjust for viewport
                        adjustPosition(popup);

                        var plots = popup.find('.oneplot');
                        var c = 0;
                        var iexp, ief;
                        for(iexp = 0; iexp < resp.experiments.length; ++iexp)
                            for(ief = 0; ief < resp.experiments[iexp].efs.length; ++ief) {
                                $.ajax({
                                    type: "GET",
                                    url: atlas.homeUrl + "plot",
                                    data: {
                                        gid: gene,
                                        eid: resp.experiments[iexp].id,
                                        ef: resp.experiments[iexp].efs[ief].ef,
                                        plot: 'bar' 
                                    },
                                    dataType: "json",
                                    success: (function(x,cc) { return function(o) {
                                        if(o.error)
                                            alert(o.error);
                                        else
                                            drawPlot(o, plots.filter(cc), x);
                                    }; })(resp.experiments[iexp].efs[ief].efvs, '#oneplot_' + (c++))
                                });
                            }
                    }
                });
     };

     atlas.structMode = function() {
         if($('#structform:visible').length)
            return;
         $('#simpleform').hide();
         $('#structform').show();
     };

     atlas.simpleMode = function() {
         if($('#simpleform:visible').length)
            return;
         $('#simpleform').show();
         $('#structform').hide();
     };

    atlas.popup = function  (url) {
        var width  = 600;
        var height = 200;
        var left   = (screen.width  - width)/2;
        var top    = (screen.height - height)/2;
        var params = 'width='+width+', height='+height;
        params += ', top='+top+', left='+left;
        params += ', directories=no';
        params += ', location=no';
        params += ', menubar=no';
        params += ', resizable=no';
        params += ', scrollbars=no';
        params += ', status=no';
        params += ', toolbar=no';
        newwin=window.open(url,'windowname5', params);
        if (window.focus) {newwin.focus()}
        return false;
    };

    atlas.expandEfo = function (xoffset, yoffset, id) {
        var offset = $('#efoheader').offset();
        offset.top += yoffset;
        offset.left += xoffset;

        var waiter = $('<div />').append($('<img/>').attr('src','images/indicator.gif'))
                .css({ left: offset.left + 'px', top: offset.top + 'px' }).appendTo(document.body);

        $.ajax({
            type: "GET",
            url: 'efo',
            dataType: "json",
            data: { parentsOf: id },
            success: function(resp) {
                waiter.remove();
                if(resp.error) {
                    alert(resp.error);
                    return;
                }

                var entered = false;
                var timeout;
                var popup = $('<div/>').addClass('tokeninputdrop')
                        .css({ width: 'auto', top: offset.top + 'px', left: offset.left + 'px' })
                        .appendTo(document.body)
                        .mouseleave(function (e) {
                            timeout = setTimeout(function () { popup.remove(); }, 300);
                        })
                        .mouseenter(function () {
                            if(timeout) {
                                clearTimeout(timeout);
                                timeout = null;
                            }
                            entered = true;
                        });

                var ul = $('<ul/>')
                    .mouseover(function (e) {
                        var t = $(e.target);
                        var li = t.is('li') ? t : t.parents('li:first');
                        if(li.length) {
                            var d = $.data(li.get(0), "efoup");
                            ul.find('li').removeClass('tokendropitemsel');
                            li.addClass('tokendropitemsel');
                        }
                    })
                    .click(function (e) {
                        var t = $(e.target);
                        var li = t.is('li') ? t : t.parents('li:first');
                        if(li.length) {
                            var d = $.data(li.get(0), "efoup");
                            popup.remove();

                            if(lastquery) {
                                var url = 'qrs?';
                                var i;
                                for(i = 0; i < lastquery.genes.length; ++i) {
                                    url += 'gnot_' + i + '=' + escape(lastquery.genes[i].not) + '&';
                                    url += 'gprop_' + i + '=' + escape(lastquery.genes[i].property) + '&';
                                    url += 'gval_' + i + '=' + escape(lastquery.genes[i].query) + '&';
                                }
                                for(i = 0; i < lastquery.species.length; ++i)
                                    url += 'specie_' + i + '=' + escape(lastquery.species[i]) + '&';

                                var shouldadd = true;
                                for(i = 0; i < lastquery.conditions.length; ++i) {
                                    var fval = lastquery.conditions[i].values;
                                    if(fval.indexOf(d.id) == -1)
                                        for(var j = 0; j < lastquery.conditions[i].efos.length; ++j) {
                                            if(lastquery.conditions[i].efos[j] == id) {
                                                fval += ' ' + d.id;
                                                shouldadd = false;
                                                break;
                                            }
                                        }
                                    url += 'fexp_' + i + '=' + escape(lastquery.conditions[i].expression) + '&';
                                    url += 'fval_' + i + '=' + escape(fval) + '&';
                                    url += 'fact_' + i + '=' + escape(lastquery.conditions[i].factor) + '&';
                                }

                                if(shouldadd) {
                                    i = lastquery.conditions.length;
                                    url += 'fexp_' + i + '=UP_DOWN&fact_' + i + '=&';
                                    url += 'fval_' + i + '' + '=' + escape(d.id) + '&';
                                }

                                url += 'view=' + escape(lastquery.view);
                                
                                atlas.startSearching($('#simpleform:visible,#structform:visible'));
                                window.location.href = url;
                            }
                        }
                    });

                var k = 0;
                for(var i in resp.tree) {
                    var indent = '';
                    for(var j = 0; j < resp.tree[i].depth; ++j)
                        indent += '&nbsp;&nbsp;&nbsp;';

                    var li = $('<li />')
                        .html(indent).append($('<span/>').text(resp.tree[i].term)).append(' <em>(' + resp.tree[i].count + ') ' + resp.tree[i].id + '</em>')
                        .addClass(++k % 2 ? 'tokendropitem' : 'tokendropitem2')
                        .appendTo(ul);
                    $.data(li.get(0), "efoup", resp.tree[i]);
                }

                ul.find('li:first').addClass('tokendropitemsel');

                popup.append(ul);

            }});
    };

    atlas.showListThumbs = function (row) {
        var efv = $("#"+row.id+" .lvrowefv").text();

        var data = row.id.split("_");
        var gid = data[0];
        var ef = data[1];
        var i = data[2];

        $(".thumb" + i).not(".done").each(function(){
            var plot_id = this.id;
            var tokens = plot_id.split('_');
            var eid = tokens[0];
            var divEle = $(this);
            $.ajax({
                type: "GET",
                url: atlas.homeUrl + "plot",
                data: { gid: gid, eid: eid, ef: ef, efv: efv, plot: 'thumb' },
                dataType:"json",

                success: function(jsonObj){
                    if(jsonObj.error)
                        alert(jsonObj.error);
                    else if(jsonObj.series){
                        $.plot(divEle, jsonObj.series, jsonObj.options);
                    }
                },
                error: atlas.onAjaxError
            });

            $(this).addClass("done");
        });
    };


 })(jQuery);
