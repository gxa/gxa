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

    function drawPlot(jsonObj,root, efvs, eid, eacc, gid){
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

            jsonObj.options.arrayDesignContainer = '#' + eid + '_' + gid +'_arraydesign';
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
            
            root.find('.plotwaiter').remove();
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

         var waiter = $('<div class="waiter" />').append($('<img/>').attr('src','images/indicator.gif'))
                 .css({ left: left + 'px', top: top + 'px' });

         $('body').append(waiter);
         adjustPosition(waiter);

         atlas.ajaxCall('experiments', {
             gene:gene,
             ef: efo ? 'efo' : efv.ef,
             efv: efo ? efo : efv.efv
         }, function(resp) {
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
                     atlas.ajaxCall("plot", {
                         gid: gene,
                         eid: resp.experiments[iexp].id,
                         eacc: resp.experiments[iexp].accession,
                         ef: resp.experiments[iexp].efs[ief].ef,
                         plot: 'bar'
                     }, (function(eid, eacc, gid, x, cc) {
                         return function(o) {
                             drawPlot(o, plots.filter(cc), x, eid, eacc, gid);
                         };
                     })(resp.experiments[iexp].id, resp.experiments[iexp].accession, gene, resp.experiments[iexp].efs[ief].efvs, '#oneplot_' + (c++))
                             );
                 }
         });
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

    atlas.expandEfo = function (xoffset, yoffset, id, parentsOrChildrenOf) {
        var offset = $('#efoheader').offset();
        offset.top += yoffset;
        offset.left += xoffset;

        $('<div class="waiter"/>').append($('<img/>').attr('src','images/indicator.gif'))
                .css({ left: offset.left + 'px', top: offset.top + 'px' }).appendTo($('body'));

        var f = {};
        f[parentsOrChildrenOf] = id;

        atlas.ajaxCall('efo', f , function(resp) {
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
                                        if (li.text() != "all children") {
                                            fval += ' ' + d.id;
                                        } else {
                                            fval += ' ' + d; // for 'all children' d is a String of ids
                                        }
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
                            if (li.text() != "all children") {
                                url += 'fval_' + i + '' + '=' + escape(d.id) + '&';
                            } else {
                                url += 'fval_' + i + '' + '=' + escape(d) + '&'; // for 'all children' d is a String of ids
                            }
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

                // Add 'all children' item to the dropdown list hanging off efo id's '+' button in heatmap header
                var allChildrenLi;
                if (i == 0 && parentsOrChildrenOf == 'childrenOf') {
                    allChildrenLi = $('<li />')
                        .html(indent).append($('<span/>').text("all children")).addClass(++k % 2 ? 'tokendropitem' : 'tokendropitem2').appendTo(ul);
                    $.data(allChildrenLi.get(0), "efoup", '@' + id); // '@' preamble indicates that efo id's children should be included
                }
                var li = $('<li />')
                        .html(indent).append($('<span/>').text(resp.tree[i].term)).append(' <em>(' + resp.tree[i].count + ') ' + resp.tree[i].id + '</em>')
                        .addClass(++k % 2 ? 'tokendropitem' : 'tokendropitem2')
                        .appendTo(ul);
                $.data(li.get(0), "efoup", resp.tree[i]);
            }

            ul.find('li:first').addClass('tokendropitemsel');

            popup.append(ul);

        });
    };

    atlas.showListThumbs = function (row) {
        var efv = $("#" + row.id + " .lvrowefv").text();

        var m = /([^_]*)_(.*)_([^_]*)/.exec(row.id);

        if (m) {
            var gid = m[1];
            var ef = m[2];
            var i = m[3];

            $(".thumb" + i).not(".done").each(function() {
                var plot_id = this.id;
                var tokens = plot_id.split('_');
                var eid = tokens[0];
                var eacc = tokens[1];
                var divEle = $(this);
                atlas.ajaxCall("plot", { gid: gid, eid: eid, eacc: eacc, ef: ef, efv: efv, plot: 'thumb' }, function(jsonObj) {
                    if (jsonObj.series) {
                        $.plot(divEle, jsonObj.series, jsonObj.options);
                    }
                });

                $(this).addClass("done");
            });
        } else {
            if (console) {
                console.log("Wrong id format: " + row.id);
            }
        }
    };


 })(jQuery);
