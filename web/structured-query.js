var counter = 0;
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
             if (name) e.name = name;
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

             var tr = $('<tr><td class="prefix">' + (numrow > 0 ? 'or' : 'specie') + '</td></tr>')
                 .append($('<td />').append(createSelect('specie_' + (++counter), options['species'], true, value)))
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
                                                            $('td.prefix', tr.next()).html('specie');
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

             factor.className = 'factor';
             factor.onchange = function() {
                 if (this.selectedIndex < 1) return; // TODO: why this?

                 var newv = this.options[this.selectedIndex].value;
                 fval.find('input.value').focus().attr('value', '').blur()
                     .setOptions({ extraParams: { factor : newv } }).flushCache();
             };


             tr.append($('<td class="common"/>').append(createSelect("gexp_" + andid, options['expressions'], false, condition && condition.expression))
                       .append(' in ').append(factor))
                 .append(fval)
                 .append($('<td class="andbuttons" />')
                         .append($('<div/>').append($('<input type="button" value=" and ">')
                                                    .bind('click', function() { addConditionAnd($(this)); } ))));

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
                                   }).flushCache();
                 if(value != null)
                     input.val(value);

                 var newval = $('<div class="value" />')
                     .append($('<div class="input" />')
                             .append(input)
                             .append($('<input type="button" value="V"/>')
                                     .bind('click', function () {
                                               if(factor.options[factor.selectedIndex].value == "")
                                                   return;
                                               var vbutt = $(this);
                                               $.ajax({
                                                          // try to leverage ajaxQueue plugin to abort previous requests
                                                          mode: "abort",
                                                          // limit abortion to this input
                                                          port: "fvalues" + andid + orid,
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
                                                              input.replaceWith(createSelect(input.attr('name'), list));
                                                              vbutt.remove();
                                                          }
                                                      });
                                           })))
                     .append($('<div class="buttons" />')
                             .append(createRemoveButton(function (where) {
                                                            if(fval.find('div.value').length == 1)
                                                            {
                                                                if(tbody.find('tr').length != 1)
                                                                    tr.remove();
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


     window.loadExperiments = function(where,url,gene) {
         var w = $(where);
         w.find('img').attr('src','expandwait.gif');
         $.ajax({
             // try to leverage ajaxQueue plugin to abort previous requests
             mode: "queue",
             port: "sexpt",
             url: url,
             dataType: "json",
             success: function(o) {
                 if(o.empty)
                    return;

                 var i = 0;
                 w.parents('tr:first').find('td.counter').each(function () {

                     var makeExps = function (exp, clas) {
                         var c = $('<ul/>').addClass(clas);
                         if(exp.length == 0)
                            c.append('<li>&nbsp;</li>')
                         for(var i = 0; i < exp.length; ++i)
                            c.append('<li><a href="http://www.ebi.ac.uk/microarray-as/aew/DW?queryFor=gene&gene_query='
                                     + encodeURI(gene)
                                     + '&species=&displayInsitu=on&exp_query=' + encodeURI(exp[i].experimentAccessment)
                                     + '" title="' + escape(exp[i].experimentName) + '">'
                                     + exp[i].experimentAccessment + '</a></li>');
                         return c;
                     }


                     if(o[i].ups.length > 0 || o[i].downs.length > 0)
                         $(this).append($('<div class="exps"/>')
                                 .append(makeExps(o[i].ups, "upexp")).append(makeExps(o[i].downs, "dnexp")));
                     ++i;
                 });
                 w.find('img').attr('src','expandclose.gif');
                 w.get(0).onclick = function () {
                     $(this).find('img').attr('src','expandopen.gif').parents('tr:first').find('td.counter div.exps').remove();
                     this.onclick = function() { loadExperiments(where,url,gene); }
                 };
             }
         });
     }

 })(jQuery);
