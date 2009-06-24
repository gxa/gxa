var atlas = {};

(function($){
    atlas.homeUrl = '';

    $.extend($.fn, {
        efoTree: function(o, handler) {
            if(!o.slideSpeed) o.slideSpeed = 500;
            if(!o.root) o.root = '';

            $(this).each( function() {
                var treeel = $(this);
                var hl;
                function showTree(c, t, downTo) {
                    c.addClass('wait');
                    $.getJSON('efo',
                        {
                            id: t,
                            downTo: o.root == t ? downTo : '' ,
                            hl: o.root == t ? o.highlight : ''
                        }, function(result) {
                            c.removeClass('wait');
                            var ul = $('<ul/>').hide();
                            c.append(ul);

                            var currentDepth = 0;
                            var n;
                            var maxDepth = 0;
                            var scrollToElem = c;

                            if(!hl && result.hl) {
                                hl = [];
                                for(var i = 0; i < result.hl.length; ++i)
                                    hl[result.hl[i].id] = 1;
                            }
                            var data = result.tree;

                            for(var i = 0; i < data.length; ++i) {
                                var dc = data[i];
                                if(dc.depth > currentDepth) {
                                    if(dc.depth > maxDepth) {
                                        maxDepth = dc.depth;
                                        scrollToElem = n;
                                    }
                                    ul = $('<ul/>');
                                    n.addClass('expanded').removeClass('collapsed').append(ul);
                                } else while(dc.depth < currentDepth) {
                                    ul = ul.parent().parent();
                                    currentDepth--;
                                }
                                currentDepth = dc.depth;
                                var a = $('<a />').text(dc.term).append(' <em>(' + dc.count + ') ' + dc.id + '</em>');
                                if(hl && hl[dc.id])
                                    a.addClass("hl");
                                n = $('<li />')
                                    .append($('<i>&nbsp;</i>'))
                                    .append(a);
                                $.data(n.get(0), "efotree", dc);
                                if(dc.expandable) {
                                    n.addClass('expandable').addClass('collapsed');
                                }
                                ul.append(n);
                            }

                            while(currentDepth > 0) {
                                ul = ul.parent().parent();
                                --currentDepth;
                            }

                            bindTree(c);

                            var scrollIt = function() {
                                var e = scrollToElem.get(0);
                                var t = treeel.get(0);
                                var y = e.offsetTop + e.clientHeight;
                                if(e.clientHeight < t.clientHeight && y > t.scrollTop + t.clientHeight)
                                    treeel.get(0).scrollTop = y - t.clientHeight;
                                else if(e.clientHeight > t.clientHeight && e.offsetTop > t.scrollTop)
                                    treeel.get(0).scrollTop = e.offsetTop;
                            };
                    
                            if(t == o.root) {
                                ul.show();
                                scrollIt();
                            } else {
                                ul.show().parent().removeClass('collapsed').addClass('expanded');
                                scrollIt();
                            }

                        });
                }

                function generateId(nodeid) {
                    return treeel.get(0).id + '_' + nodeid;
                }

                function bindTree(t) {
                    t.find('LI.expandable > i').click(function(event) {
                        var diz = $(this).parent();
                        if( diz.hasClass('collapsed') ) {
                            // Expand
                            if( !o.multiFolder ) {
                                diz.siblings('li.expanded').children('UL').parent().removeClass('expanded').addClass('collapsed').end().remove();
                                var dc = $.data(diz.get(0), "efotree");
                                showTree( diz, dc.id );
                            }
                        } else {
                            // Collapse
                            diz.find('UL').parent().removeClass('expanded').addClass('collapsed').end().remove();
                        }
                        event.stopPropagation();
                        return false;
                    });
                    t.find('a').click(function (event) {
                        if(handler) {
                            $(this).parent().andSelf().addClass('selected');
                            var dc = $.data($(this).parent().get(0), "efotree");
                            handler.call(treeel, dc);
                        } else {
                            if(event.ctrlKey || event.metaKey) {
                                $(this).parent().andSelf().toggleClass('selected');
                            } else {
                                treeel.find('LI.selected,a.selected').removeClass('selected');
                                $(this).parent().andSelf().addClass('selected');
                            }
                        }
                        event.stopPropagation();
                        return false;
                    });
                    t.find('LI').filter(':not(LI.expandable)').click(function (event) {
                        event.stopPropagation();
                        return false;
                    });
                }

                function killevent(e) { 
                    e.stopPropagation();
                    return false;
                }
                treeel.bind('startselect', killevent).bind('dblclick', killevent);
                treeel.bind('mousedown', function (e) {
                    if(e.shiftKey) e.stopPropagation(); return false;
                });

                showTree( treeel, o.root, o.downTo );
            });
            return $(this);
        },
        getSelection: function() {
            if ( this.length ) {
                var elem = $(this[0]);
                return jQuery.map(elem.find('LI.selected'), function (e) { return e.id.substr(elem.get(0).id.length + 1); });
            }
            return undefined;
        }
    });

    atlas.makeHideAutocompleteCode = function (input) {
        var closebox = $('<div class="ac_hide">hide suggestions</div>');
        closebox.click(function () { input.hideResults(); });
        return closebox;
    };

    function optionalQuote(s) {
        return s.indexOf(' ') >= 0 ? '"' + s + '"' : s;
    }

    function parseAcJson(formatResult) {
        return function (json, query) {
            var rows = json.completions[query];
            for (var i=0; i < rows.length; i++) {
                var row = rows[i];
                rows[i] = {
                    data: row,
                    value: row.value,
                    result: formatResult(row)
                };
            }
            return rows;
        }
    }

    atlas.makeGeneAcOptions = function (property, dumbmode) {
        var acoptions = {
            minChars: property == "" ? 1 : 0,
            width: property == "" ? '500px' : '300px',
            matchCase: false,
            matchSubset: false,
            selectFirst: false,
            multiple: false,
            multipleSeparator: " ",
            multipleQuotes: true,
            scroll: false,
            scrollHeight: 180,
            max: 15,
            dataType: "json",
            extraContent: atlas.makeHideAutocompleteCode,
            extraParams: { type: 'gene', 'factor' : property },
            parse: parseAcJson(function(r) { return optionalQuote(r.value); }),
            highlight: function (value,term) { return value; }
        };

        if(property == '') {
            acoptions.formatItem = function(row, num, max, val, term) {
                var text = $.Autocompleter.defaults.highlight(row.value.length > 30 ? row.value + '...' : row.value, term);
                if(row.property == 'name') {
                    var ext = '(' + row.otherNames.join(',') + ') ' + row.species;
                    return '<nobr><em>gene:</em>&nbsp;' + text + '&nbsp;<em>' + ext + '</em></nobr>';
                } else {
                    return '<nobr><em>' + row.property + ':</em>&nbsp;' + text + '&nbsp;<em>(' + row.count + ')</em></nobr>';
                }
            };
        } else {
            acoptions.formatItem = function(row, num, max, val, term) {
                var text = $.Autocompleter.defaults.highlight(row.value.length > 50 ? row.value + '...' : row.value, term);
                return '<nobr>' + text + ' (' + row.count + ')</nobr>';
            };
        }

        return acoptions;
    };

    atlas.makeFvalAcOptions = function (factor, dumbmode) {
        var actype = 'efv';
        if(factor == '') {
            actype = 'efoefv';
        } else if(factor == 'efo') {
            actype = 'efo';
        }
        return {
            minChars: 1,
            matchCase: false,
            matchSubset: false,
            multiple: !dumbmode,
            selectFirst: false,
            multipleQuotes: true,
            multipleSeparator: " ",
            scroll: false,
            scrollHeight: 180,
            width: '300px',
            max: 100,
            queryMax: 15,
            dataType: "json",
            extraParams: { type: actype, factor: factor },
            extraContent: atlas.makeHideAutocompleteCode,
            parse: parseAcJson(function(row) {
                if(row.property == 'efo')
                    return row.id;
                return optionalQuote(row.value);
            }),
            formatItem: function(row, num, max, val, term) {
                if(row.property == 'efo') {
                    var indent = '';
                    for(var i = 0; i < row.depth; ++i)
                        indent += '&nbsp;&nbsp;&nbsp;';                    
                    return '<nobr>' + indent + row.value + ' <em>(' + row.count + ' genes) ' + row.id + '</em></nobr>';
                }
                return '<nobr>' + row.value + ' <em>(' + row.count + ' genes)</em></nobr>';
            }
        };
    };

    atlas.tokenizeConditionInput = function (fvalfield, factor, defaultvalue) {
        var actype = 'efv';
        if(factor == '') {
            actype = 'efoefv';
        } else if(factor == 'efo') {
            actype = 'efo';
        }

        fvalfield.tokenInput(atlas.homeUrl + "fval",
        {
            extraParams: {
                property: factor,
                type: actype,
                limit: 15
            },

            defaultValue: defaultvalue,

            formatListItem: function(row, q, i) {
                var text = $.highlightTerm(row.value.length > 50 ? row.value.substr(0, 50) + '...' : row.value, q, 'b');
                if(row.property == 'efo') {
                    var indent = '';
                    for(var i = 0; i < row.depth; ++i)
                        indent += '&nbsp;&nbsp;&nbsp;';
                    return '<nobr>' + indent + text + ' <em>(' + row.count + ' genes) ' + row.id + '</em></nobr>';
                }
                return '<nobr>' + text + ' <em>(' + row.count + ' genes)</em></nobr>';
            },

            formatToken: function(row) {
                return row.value.length > 20 ? row.value.substr(0, 20) + '...' : row.value;
            },

            formatId: function(res) {
                if(res.property == 'efo')
                    return res.id;
                else
                    return res.value;
            },

            getItemList:  function (json, query) {
                return json.completions[query];
            },

            browser: factor == "" || factor == "efo" ? function (onResult, lastId, values) {
                return $('<div/>').addClass('tree').efoTree({ slideSpeed: 200, downTo: lastId, highlight: values }, function(dc) {
                    onResult({ id: dc.id,  count: dc.count, property: 'efo', value: dc.term, depth: dc.depth });
                });
            } : null,

            hideText: "hide suggestions",
            noResultsText: "no results found",

            classes: {
                tokenList: "tokeninput",
                token: "tokeninput",
                selectedToken: "tokeninputsel",
                highlightedToken: "token-input-highlighted-token-facebook",
                dropdown: "tokeninputdrop",
                dropdownItem: "tokendropitem",
                dropdownItem2: "tokendropitem2",
                selectedDropdownItem: "tokendropitemsel",
                inputToken: "tokeninputinput",
                hideText: "tokendrophide",
                searching: "tokeninputsearching",
                browseIcon: "efoexpand"
            }
        });
    };

    atlas.initSimpleForm = function() {
        var gpropfield = $('input[name=gprop_0]');
        var factfield = $('input[name=fact_0]');
        var gvalfield = $('input[name=gval_0]');
        var fvalfield = $('input[name=fval_0]');
        var speciessel = $('select[name=specie_0]');
        var form = gvalfield.parents('form:first');
        var oldval = gvalfield.val();
        gvalfield
                .defaultvalue("(all genes)","(all genes)")
                .autocomplete(atlas.homeUrl + "fval", atlas.makeGeneAcOptions('', true))
                .result(function (unused, res) {
            var newprop = res.property;
            if(res.property == 'name') {
                location.href = atlas.homeUrl + 'gene?gid=' + res.id;
                atlas.startSearching(form);
                return;
            }
            gpropfield.val(newprop);
            var oldval = $(this).val();
            this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); };
            //  $(this).setOptions({extraParams: { type: 'gene', factor: newprop }}).flushCache();
        }).each(function () { this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); }; });

        atlas.tokenizeConditionInput(fvalfield, '', '(all conditions)');

        form.bind('submit', function () {
            $('input.ac_input', form).hideResults();
            atlas.startSearching(form);
        });

        speciessel.bind('change', function () {
            gvalfield.hideResults().setOptions({extraParams:{ type: 'gene', 'factor' : '', f: 'species', species: $(this).val() }}).flushCache();
        });
    };

    atlas.startSearching = function(form) {
        var v = $(form).find('input[type=submit]');
        v.val('Searching...');
    };
    
    initExpPageAutoComplete = function(){
    	var genefield = $("#geneInExp_qry");
    	var form = genefield.parents('form:first');
        var oldval = genefield.val();
        var gpropfield;
        genefield
                .autocomplete(atlas.homeUrl + "fval", atlas.makeGeneAcOptions('', true))
                .result(function (unused, res) {
            var newprop = res.property;
                gpropfield.val(newprop);
            var oldval = $(this).val();
            this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); };
            //  $(this).setOptions({extraParams: { type: 'gene', factor: newprop }}).flushCache();
        }).each(function () { this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); }; });
    };
})(jQuery);

