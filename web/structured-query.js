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

         function createSelect(name, options, optional) {
             var e = document.createElement("select");
             if (name) e.name = name;
             if (optional) e.options[0] = new Option("(any)", "");
             if (options) {
                 for (var i = 0; i < options.length; i++) {
                     var option;
                     if (typeof(options[i]) == "object") {
                         option = new Option(options[i][1], options[i][0]);
                     } else {
                         option = new Option(options[i], options[i]);
                     }
                     e.options[e.options.length] = option;
                 }
             }
             return e;
         }


         var tbody = $("#species > tbody");

         function addSpecieOr() {
             var numrow = tbody.get(0).rows.length;

             var tr = $('<tr><td class="prefix">' + (numrow > 0 ? 'or' : 'specie') + '</td></tr>')
                 .append($('<td />').append(createSelect('specie_' + (++counter), options['species'], true)))
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

         function addConditionAnd(where) {
             var tbody = $('#conditions > tbody');

             var andid = ++counter;

             var tr = $('<tr/>');
             var fval = $('<td class="factorvalue"/>');

             var factor = createSelect("fact_" + andid, options['factors']);

             factor.className = 'factor';
             factor.onchange = function() {
                 if (this.selectedIndex < 1) return; // TODO: why this?

                 var newv = this.options[this.selectedIndex].value;
                 fval.find('input.value').focus().attr('value', '').blur()
                     .setOptions({ extraParams: { factor : newv } }).flushCache();
             };


             tr.append($('<td class="common"/>').append(createSelect("gexp_" + andid, options['expressions'])).append(' in ').append(factor))
                 .append(fval)
                 .append($('<td class="andbuttons" />')
                     .append($('<div/>').append($('<input type="button" value=" and ">')
                                 .bind('click', function() { addConditionAnd($(this)); } ))));

             function addConditionOr(where) {
                 var orid = ++counter;

                 var input = $('<input type="text" class="value"/>').attr('name', "fval_" + andid + '_' + orid)
                         .autocomplete("factorvalues.jsp", {
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

                 var newval = $('<div class="value" />')
                     .append($('<div class="input" />')
                             .append(input)
                             .append($('<input type="button" value="V"/>')
                                     .bind('click', function () {
                                               var vbutt = $(this);
                                               $.ajax({
                                                          // try to leverage ajaxQueue plugin to abort previous requests
                                                          mode: "abort",
                                                          // limit abortion to this input
                                                          port: "fvalues" + andid + orid,
                                                          url: "factorvalues.jsp",
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
                                                              input.replaceWith(createSelect(input.name, list));
                                                              vbutt.remove();
                                                          }
                                                      });
                                           })))
                     .append($('<div class="buttons" />')
                             .append(createRemoveButton(function (where) {
                                                            if(fval.find('div').length == 1)
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

             addConditionOr();

             if(where)
                 where.parents('tr:first').after(tr);
             else
                 tbody.append(tr);
         }

         addSpecieOr();
         addConditionAnd();
     };

    window.renumberAll = function() {
        var i = 0;
        $('#species select').each(function(){ this.name.replace(/_\d+/, '_' + (++i)); });
        i = 0;
        $('#conditions tr').each(function(){
            $('td.common select', this).each(function(){ this.name.replace(/_\d+/, '_' + i); });
            var j = 0;
            $('td.factorvalue div.value', this)
                    .each(function(){
                $('input,select', this)
                        .each(function(){ this.name.replace(/_\d+_\d+/, '_' + i + '_' + j); });
                ++j;
            });
            ++i;
        });
    }
})(jQuery);
