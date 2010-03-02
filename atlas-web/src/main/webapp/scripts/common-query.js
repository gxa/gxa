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

if(!atlas)
    atlas = {};

(function($){
    if(!atlas.homeUrl)
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
                        o.root == t ? {
                            downTo: downTo,
                            hl: o.highlight
                        } : {
                            childrenOf: t
                        },
                            function(result) {
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
                    if(e.shiftKey) e.stopPropagation();
                    return true;
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

    var commonTokenOptions = {
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
        },

        getItemList:  function (json, query) {
            return json.completions[query];
        },

        formatToken: function(row) {
            return row.value.length > 20 ? row.value.substr(0, 20) + '...' : row.value;
        },

        formatTokenTooltip: function(row) {
            return row.value;
        }
    };

    atlas.tokenizeConditionInput = function (fvalfield, factor, defaultvalue) {
        var actype = 'efv';
        if(factor == '') {
            actype = 'efoefv';
        } else if(factor == 'efo') {
            actype = 'efo';
        }

        fvalfield.tokenInput(atlas.homeUrl + "fval", $.extend(true, {}, commonTokenOptions,
        {
            extraParams: {
                factor: factor,
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

            formatId: function(res) {
                if(res.property == 'efo')
                    return res.id;
                else
                    return res.value;
            },

            browser: factor == "" || factor == "efo" ? function (onResult, lastId, values) {
                return $('<div/>').addClass('tree').efoTree({ slideSpeed: 200, downTo: lastId, highlight: values }, function(dc) {
                    onResult({ id: dc.id,  count: dc.count, property: 'efo', value: dc.term, depth: dc.depth });
                });
            } : null
        }));
    };

    atlas.tokenizeGeneInput = function (fvalfield, property, defaultvalue) {
        fvalfield.tokenInput(atlas.homeUrl + "fval", $.extend(true, {}, commonTokenOptions,
        {
            extraParams: {
                factor: property,
                type: 'gene',
                limit: 15
            },

            defaultValue: defaultvalue,

            formatListItem: function(row, q, i) {
                var text = $.highlightTerm(row.value.length > 50 ? row.value.substr(0, 50) + '...' : row.value, q, 'b');
                if(property == '') {
                    if(row.property == 'gene') {
                        var ext = '(' + row.otherNames.join(',') + ') ' + row.species;
                        return '<em>gene:</em>&nbsp;' + text + '&nbsp;<em>' + ext + '</em>';
                    } else {
                        return '<em>' + row.property.toLowerCase() + ':</em>&nbsp;' + text + '&nbsp;<em>(' + row.count + ')</em>';
                    }
                } else {
                    return '<nobr>' + text + ' (' + row.count + ')</nobr>';
                }
            },

            formatToken: function(row) {
                var text = row.property == 'gene' && row.value == row.id && row.otherNames.length > 0 ? row.otherNames[0] : row.value;
                return text.length > 20 ? text.substr(0, 20) + '...' : text;
            },

            formatTokenTooltip: function(row) {
                return row.property == 'gene' && row.value == row.id && row.otherNames.length > 0 ? row.otherNames[0] : row.value;
            },

            formatId: function(res) {
                return res.property == 'gene' ? res.id : res.value;
            }

        }));
    };

    atlas.initSimpleForm = function() {
        var gvalfield = $('input[name=gval_0]');
        var fvalfield = $('input[name=fval_0]');
        var speciessel = $('select[name=specie_0]');
        var form = gvalfield.parents('form:first');

        atlas.tokenizeConditionInput(fvalfield, '', '(all conditions)');
        atlas.tokenizeGeneInput(gvalfield, '', '(all genes)');

        form.bind('submit', function () {
            $('input.ac_input', form).hideResults();
            atlas.startSearching(form);
        });

        speciessel.bind('change', function () {
            gvalfield
                    .hideResults()
                    .setOptions({extraParams: { property: '', type: 'gene', limit: 15, f: 'species', species: $(this).val() }})
                    .flushCache();
        });
    };

    atlas.startSearching = function(form) {
        var v = $(form).find('input[type=submit]');
        v.val('Searching...');
        $(form).find('input:hidden').trigger('preSubmit');
    };
    
    atlas.showApiLinks = function (url, callback) {
        if(callback) {
            url = callback(url);
            $('.jsonapilink').val(url + '&format=json');
            $('.xmlapilink').val(url + '&format=xml');
        }
        var p = $('#apilinks').show();
        p.find('.closebox').click(function () { p.hide(); });
    };

    atlas.copyText = function(e) {
        e.focus();
        e.select();
        if(e && e.createTextRange) {
            var t = e.createTextRange();
            t.execCommand("Copy");
        }
    };

    atlas.onAjaxError = function() {
        alert('Ajax HTTP error');
    };

})(jQuery);

