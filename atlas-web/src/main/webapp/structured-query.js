var counter = 0;

function escapeHtml(s) {
    return s.replace(/"/g,"&quot;")
            .replace(/</g,"&lt;")
            .replace(/>/g,"&gt;")
            .replace(/&/g,"&amp;");
}

(function($){
     window.initQuery = function() {

         function createRemoveButton(callback)
         {
             var removeButton = document.createElement("input");
             removeButton.type = "button";
             removeButton.value = "-";
             removeButton.onclick = function() { callback($(this)); };
             return removeButton;
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
                     if(value == option.value)
                         selected = e.options.length;
                     e.options[e.options.length] = option;
                 }
                 e.selectedIndex = selected;
             }
             return e;
         }


         var tbody = $("#species > tbody");

         function addSpecieOr(value) {
             var numrow = tbody.get(0).rows.length;

             var selects = createSelect('specie_' + (++counter), options['species'], true, value)
             selects.setAttribute("class","speciesSelect");

             var tr = $('<tr><td class="prefix">' + (numrow > 0 ? 'or' : '') + '</td></tr>')
                 .append($('<td/>').append(selects))
                 .append($('<td class="removebutton" />')
                         .append(createRemoveButton(function (where) {
                                                        var tr = where.parents('tr:first');
                                                        var tbody = tr.parents('tbody:first').get(0);
                                                        if(tbody.rows.length == 1)
                                                            return;

                                                        if(tr.get(0).sectionRowIndex == tbody.rows.length - 1) {
                                                            $('td.addbuttons', tr.prev())
                                                                .replaceWith($('<td class="addbuttons" />')
                                                                             .append($('<input type="button" value=" or ">')
                                                                                     .bind('click', addSpecieOr)));
                                                        } else if(tr.get(0).sectionRowIndex == 0) {
                                                            $('td.prefix', tr.next()).html('');
                                                        }
                                                        tbody.deleteRow(tr.get(0).sectionRowIndex);
                                                    })));

             if(numrow > 0) {
                 $('tr:last > td.addbuttons', tbody).empty();
             }

             tr.append($('<td class="addbuttons" />').append($('<input type="button" value=" or ">').bind('click', addSpecieOr)));
             tbody.append(tr);
         };

         function addConditionAnd(where, condition) {
             var tbody = $('#conditions > tbody');

             var andid = ++counter;

             var tr = $('<tr/>');
             var fval = $('<td class="factorvalue"/>');

             var factor = createSelect("fact_" + andid, options['factors'], true, condition && condition.factor);


             var loadValues = function (what,callback) {
                                               if(factor.options[factor.selectedIndex].value == "")
                                                   return;
                                               $.ajax({
                                                          // try to leverage ajaxQueue plugin to abort previous requests
                                                          mode: "abort",
                                                          // limit abortion to this input
                                                          port: "fvalues",
                                                          url: "fval",
                                                          data: { q: '', limit: 1000, factor: factor.options[factor.selectedIndex].value },
                                                          success: function(data) {
                                                              var rows = data.split("\n");
                                                              var list = [];
                                                              for (var i=0; i < rows.length; i++) {
                                                                  var row = $.trim(rows[i]);
                                                                  if (row) {
                                                                      row = row.split("|");
                                                                      list[list.length] = row[0];
                                                                  }
                                                              }
                                                              var sel = createSelect(what.attr('name'), list);
                                                              what.replaceWith(sel);
                                                              callback(sel);
                                                          }
                                                      });
                                           }

             factor.className = 'factor';
             factor.onchange = function() {
                 if (this.selectedIndex < 0) return;

                 var newv = this.options[this.selectedIndex].value;
                 fval.find('input.value').focus().attr('value', '').blur()
                     .setOptions({ extraParams: { factor : newv } }).flushCache();
                 loadValues(fval.find('select'), function() { });
                 tr.find("td.expansion").empty();
             };


             var expr = createSelect("gexp_" + andid, options['expressions'], false, condition && condition.expression);
             tr.append($('<td class="common"/>').append(expr).append(' in ').append(factor))
                 .append(fval)
                 .append($('<td class="andbuttons" />')
                         .append($('<div/>').append($('<input type="button" value=" and ">')
                                                    .bind('click', function() { addConditionAnd($(this)); } ))))
                     .append($('<td class="expansion" />').html(condition != null ? condition.expansion : ''));

             function addConditionOr(where, value) {
                 var orid = ++counter;

                 var input = $('<input type="text" class="value"/>').attr('name', "fval_" + andid + '_' + orid)
                     .autocomplete("fval", {
                                       minChars:1,
                                       matchCase: true,
                                       matchSubset: false,
                                       multiple: true,
                                       multipleSeparator: " ",
                                       scroll: false,
                                       scrollHeight: 180,
                                       max: 10,
                                       extraParams: { 'factor' : factor.options[factor.selectedIndex].value },
                                       formatItem:function(row) {return row[0];}
                                   }).flushCache().keydown(function () {tr.find("td.expansion").text('');});
                 if(value != null)
                     input.val(value);

                 var newval = $('<div class="value" />')
                     .append($('<div class="input" />')
                             .append(input)
                             .append($('<input type="button" value="V"/>')
                                     .bind('click', function() {
                                                                 var vbutt = $(this);
                                                                 var oldval = input.val();
                                                                 loadValues(input, function(sel) {
                                                                      tr.find("td.expansion").text('');
                                                                      //
                                                                      for(var i = 0; i < sel.options.length; ++i)
                                                                          if(sel.options[i].value.indexOf(oldval) >= 0)
                                                                          {
                                                                               sel.selectedIndex = i;
                                                                               break;
                                                                          }
                                                                      vbutt.remove();
                                                                 });
                                                               })))
                     .append($('<div class="buttons" />')
                             .append(createRemoveButton(function (where) {
                                                            tr.find("td.expansion").text('');
                                                            if(fval.find('div.value').length == 1)
                                                            {
                                                                if(tbody.find('tr').length != 1)
                                                                    tr.remove();
                                                                else {
                                                                    factor.selectedIndex = 0;
                                                                    expr.selectedIndex = 0;
                                                                    input.focus().attr('value', '').blur()
                                                                            .setOptions({ extraParams: { factor : '' } }).flushCache();
                                                                }
                                                            } else {
                                                                newval.remove();
                                                            }
                                                        }))
                             .append($('<input type="button" value=" or ">')
                                     .bind('click', function() { addConditionOr($(this)); } )))
                     .append('<div style="clear:both;"/>');

                 if(where)
                     where.parents('div.value:first').after(newval);
                 else
                     fval.append(newval);
             }

             if(condition && condition.values.length) {
                 for(var i = 0; i < condition.values.length; ++i)
                     addConditionOr(null, condition.values[i]);
             } else {
                 addConditionOr();
             }

             if(where)
                 where.parents('tr:first').after(tr);
             else
                 tbody.append(tr);
         }

         if(lastquery && lastquery.species.length) {
             for(var i = 0; i < lastquery.species.length; ++i)
                 addSpecieOr(lastquery.species[i]);
         } else {
             addSpecieOr();
         }

         if(lastquery && lastquery.conditions.length) {
             for(var i = 0; i < lastquery.conditions.length; ++i)
                 addConditionAnd(null, lastquery.conditions[i]);
         } else {
             addConditionAnd();
         }
     };

     window.renumberAll = function() {
         var i = 0;
         $('#species select').each(function(){ this.name = this.name.replace(/_\d+/, '_' + (++i)); });
         i = 0;
         $('#conditions tr').each(function(){
                                      $('td.common select', this).each(function(){ this.name = this.name.replace(/_\d+/, '_' + i); });
                                      var j = 0;
                                      $('td.factorvalue div.value', this)
                                          .each(function(){
                                                    $('input,select', this)
                                                        .each(function(){ this.name = this.name.replace(/_\d+_\d+/, '_' + i + '_' + j); });
                                                    ++j;
                                                });
                                      ++i;
                                  });
     };


     $.fn.extend({
         setonclick: function(callback) {
             return this.each(function() { this.onclick = callback; })
         }
     });

     function adjustExpandAll()
     {
         if($('#squery > tbody > tr.loaded').length == 0)
             $('#squery > thead img.expexp').setonclick(function(){loadExperiments()}).attr('src','expandopen.gif');
         else
             $('#squery > thead img.expexp').setonclick(function(){hideExperiments()}).attr('src','expandclose.gif');
     }

     window.hideExperiments = function(index) {
         var i = index;
         var tr = $(index == null ? '#squery > tbody > tr' : 'tr#squeryrow' + index).filter('.loaded').removeClass('loaded');
         tr.find('img.expexp').setonclick(function(){loadExperiments(i)}).attr('src','expandopen.gif');
         tr.find('td.counter div.exps').remove();
         adjustExpandAll();
     };

     window.loadExperiments = function(index) {
         var tr = $(index == null ? '#squery > tbody > tr' : 'tr#squeryrow' + index).not('.loaded').addClass('loaded');
         tr.find('img.expexp').attr('src','expandwait.gif');
         tr.each(function() {
             var tr = $(this);
             var i = this.id.substr(9);
             var geneDwId = resultGenes[i].geneDwId;
             $.ajax({
                 mode: "queue",
                 port: "sexpt",
                 url: exptUrlBase + '&gene=' + encodeURI(resultGenes[i].geneAtlasId),
                 dataType: "json",
                 success: function(o) {
                     if(o.empty)
                         return;

                     var k = 0;
                     tr.find('td.counter').each(function () {

                         var makeExps = function (exp, clas) {
                             var c = $('<ul/>').addClass(clas);
                             if(exp.length == 0)
                                 c.append('<li>&nbsp;</li>')
                             for(var j = 0; j < exp.length; ++j)
                                 c.append('<li><a href="http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query='
                                         + encodeURI(geneDwId)
                                         + '&species=&displayInsitu=on&exp_query=' + encodeURI(exp[j].experimentAccessment)
                                         + '" title="' + exp[j].experimentAccessment + ': ' + escapeHtml(exp[j].experimentName) + '">'
                                         + exp[j].experimentAccessment + '</a></li>');
                             return c;
                         }


                         if(o[k].ups.length > 0 || o[k].downs.length > 0)
                             $(this).append($('<div class="exps"/>')
                                     .append(makeExps(o[k].ups, "upexp")).append(makeExps(o[k].downs, "dnexp")));
                         ++k;
                     });
                     tr.find('img.expexp').setonclick(function(){hideExperiments(i)}).attr('src','expandclose.gif');

                 }
             });
         });
         adjustExpandAll();
     }

 })(jQuery);
