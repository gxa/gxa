var counter = 0;

function escapeHtml(s) {
    return s.replace(/\"/g,"&quot;")
            .replace(/</g,"&lt;")
            .replace(/>/g,"&gt;")
            .replace(/&/g,"&amp;");
}

(function($){
     window.initQuery = function() {

         $("#gene0,#gene")
             .defaultvalue("(all genes)","(all genes)")
             .autocomplete("autocomplete.jsp", {
                               minChars:2,
                               matchCase: true,
                               matchSubset: false,
                               multiple: false,
                               selectFirst: false,
                               extraParams: {type:"gene"},
                               formatItem:function(row) {return row[0] + " (" + row[1] + ")";}
                           });

         var fval0old  = $("#fval0")
             .defaultvalue("(all conditions)")
             .autocomplete("fval", {
                            minChars:2,
                            matchCase: true,
                            matchSubset: false,
                            multiple: true,
                            selectFirst: false,
                            multipleSeparator: " ",
                            scroll: false,
                            scrollHeight: 180,
                            max: 10,
                            extraParams: { 'factor' : '' },
                            formatItem: function(row) { return row[0]; },
                            formatResult: function(row) { return row[0].indexOf(' ') >= 0 ? '"' + row[0] + '"' : row[0]; }
                           })
             .keyup(function (e) { console.log(this.value); if(this.value != fval0old) $("#simpleform .expansion").remove(); }).val();

         console.log(fval0old);

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

             var selects = createSelect('specie_' + (++counter), options['species'], true, value);
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
                                                          data: { mode: 'all', factor: factor.options[factor.selectedIndex].value },
                                                          success: function(data) {
                                                              var rows = data.split("\n");
                                                              var sel = createSelect(what.attr('name'), rows);
                                                              what.replaceWith(sel);
                                                              callback(sel);
                                                          }
                                                      });
                                           };

             factor.className = 'factor';
             factor.onchange = function() {
                 if (this.selectedIndex < 0) return;

                 var newv = this.options[this.selectedIndex].value;
                 fval.find('input.value').focus().attr('value', '').blur()
                     .setOptions({ extraParams: { factor : newv } }).flushCache();
                 loadValues(fval.find('select'), function() { });
                 tr.find("td.expansion").empty();
                 if(this.selectedIndex == 0)
                    fval.find('input.choose').attr('disabled', 'disabled');
                 else
                     fval.find('input.choose').removeAttr('disabled');
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

                 var makeinput = function () {
                     return $('<input type="text" class="value"/>')
                        .attr('name', "fval_" + andid + '_' + orid)
                        .autocomplete("fval", {
                            minChars:1,
                            matchCase: true,
                            matchSubset: false,
                            multiple: false,
                            selectFirst: false,
                            multipleSeparator: " ",
                            scroll: false,
                            scrollHeight: 180,
                            max: 10,
                            extraParams: { 'factor' : factor.options[factor.selectedIndex].value },
                            formatItem: function(row) { return row[0]; }
                        })
                        .flushCache();
                 };

                 var input = makeinput();

                 var makechoose = function () {
                     var c = $('<input type="button" value="..." title="choose from list..." class="choose" />')
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
                        });
                     if(factor.selectedIndex == 0)
                        c.attr('disabled', 'disabled');

                     return c;
                 };

                 var choose = makechoose();

                 if(value != null) {
                     input.keyup(function () { if(value != this.value) tr.find("td.expansion").text('');});
                     input.val(value);
                 }

                 var newval = $('<div class="value" />')
                     .append($('<div class="input" />')
                             .append(input)
                             .append(choose))
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
                                                                    var havesel = newval.find('div.input select');
                                                                    if(havesel.length != 0)
                                                                    {
                                                                        havesel.replaceWith(input = makeinput());
                                                                        input.after(choose = makechoose());
                                                                    } else {
                                                                        input.focus().attr('value', '').blur()
                                                                                .setOptions({ extraParams: { factor : '' } }).flushCache();
                                                                    }
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


     window.hmc = function (igene, iefv, tdd) {
         $("div.expopup .closebox").click();

         var td = $(tdd);

         var gene = resultGenes[igene];
         var efv = resultEfvs[iefv];

         var waiter = $('<div/>').addClass('waiter').append($('<img/>').attr('src','expandwait.gif'));

         td.prepend(waiter);
         td.addClass('counter').removeClass('acounter');
         td.unbind('click');

         $.ajax({
             mode: "queue",
             port: "sexpt",
             url: 'experiments.jsp',
             dataType: "html",
             data: { gene:gene.geneAtlasId, ef: efv.ef, efv: efv.efv },
             complete: function(resp) {
                 waiter.remove();

                 var popup = $("<div/>").addClass('expopup')
                         .html(resp.responseText)
                         .prepend($("<div/>").addClass('closebox')
                         .click(
                         function(e) {
                             td.click(function() { hmc(igene, iefv, tdd); });
                             td.addClass('acounter').removeClass('counter');
                             popup.hide('normal',function() { popup.remove(); });
                             e.stopPropagation();
                             return false;
                         }).text('X'))
                         .click(function(e){e.stopPropagation();})
                         .hide();

                 td.prepend(popup);
                 popup.show('normal');
             }
         });
     };

     window.drawEfvNames = function () {
         $('.hitrunc').truncate({max_length: 60, more: '...»', less: '«'});

         var cs = 0.707106781186548;
         var attr = {"font": '12px Tahoma', 'text-anchor': 'start'};

         var testR = Raphael(0,0,10,10);
         var maxH = 0;
         var lastW = 0;
         for(var k = 0; k < resultEfvs.length; ++k)
         {
             var txt = testR.text(0, 0, resultEfvs[k].efv).attr(attr);
             var bw = txt.getBBox().width * cs;
             if(maxH < bw)
                 maxH = bw;
             if(k == resultEfvs.length - 1)
                 lastW = bw;
         }
         testR.remove();

         var ff = document.getElementById("fortyfive");
         var sq = document.getElementById("squery");

         var R = Raphael("fortyfive", sq.offsetWidth + Math.round(lastW) + 20, Math.round(maxH) + 20);

         var colors = ['#000000','#999999'];

         k = 0;
         var cp = -1;
         var curef = null;
         $("#squery tbody tr:first td:gt(0)").each(function () {
                                                       if(curef == null || curef != resultEfvs[k].ef)
                                                       {
                                                           if(++cp == colors.length)
                                                               cp = 0;
                                                           curef = resultEfvs[k].ef;
                                                       }
                                                       var x = this.offsetLeft;
                                                       var txt = R.text(x + 5, R.height - 5, resultEfvs[k].efv).attr(attr).attr({fill: colors[cp]});
                                                       var bb = txt.getBBox();
                                                       txt.matrix(cs, cs, -cs, cs, bb.x - cs * bb.x - cs * bb.y, bb.y + cs * bb.x - cs * bb.y);
                                                       R.path({stroke: "#cdcdcd", 'stroke-width': 2}).moveTo(x - 1, R.height).lineTo(x - 1, R.height - 20);
                                                       ++k;
                                                   });

     };

     window.structuredMode = function() {
         $("#simpleform").hide('fast');
         $("#structform").show('fast');
     };

     window.simpleMode = function() {
         $("#structform").hide('fast');
         $("#simpleform").show('fast');
     };

 })(jQuery);
