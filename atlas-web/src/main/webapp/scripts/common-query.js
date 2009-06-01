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

                            var origul = ul;
                        
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
                                if(hl[dc.id])
                                    a.addClass("hl");
                                n = $('<li />')
                                    .append($('<i>&nbsp;</i>'))
                                    .append(a).attr('id', generateId(dc.id));
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
                            //                            ul.slideDown(o.slideSpeed, function () {
                            //                                $(this).parent().removeClass('collapsed').addClass('expanded');
                            //                                scrollIt();
                            //                            });
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
                                showTree( diz, diz.get(0).id.substr(treeel.get(0).id.length + 1) );
//                                    tocoll.slideUp(o.slideSpeed, function () {
//                                        $(this).parent().removeClass('expanded').addClass('collapsed').end().remove();
//                                        load();
//                                    });
                            }
                        } else {
                            // Collapse
                            diz.find('UL').parent().removeClass('expanded').addClass('collapsed').end().remove();
//                            .slideUp(o.slideSpeed, function () {
//                                $(this).parent().removeClass('expanded').addClass('collapsed').end().remove();
//                            });
                        }
                        event.stopPropagation();
                        return false;
                    });
                    t.find('a').click(function (event) {
                        if(handler) {
                            $(this).parent().andSelf().addClass('selected');
                            handler.call(treeel, $(this).parent().get(0).id.substr(treeel.get(0).id.length + 1));
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
                    atlas.preventBlur = true;
                    if(e.shiftKey) e.stopPropagation(); return false;
                }).bind('mouseup', function () { atlas.preventBlur = false; });

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

    atlas.makeGeneAcOptions = function (property, dumbmode) {
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
            max: 15,
            extraContent: atlas.makeHideAutocompleteCode,
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
    };

    atlas.makeFvalAcOptions = function (factor, dumbmode) {
        var actype = 'efv';
        if(factor == '') {
            actype = 'efoefv';
        } else if(factor == 'efo') {
            actype = 'efo';
        }
        var acoptions = {
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
            extraParams: { type: actype, factor: factor },
            extraContent: atlas.makeHideAutocompleteCode,
            formatItem: function(row, num, max, val, term) {
                if(row[0] == 'efo') {
                    var ext = row[3].split('$');
                    var indent = '';
                    for(var i = 0; i < ext[1]; ++i)
                        indent += '&nbsp;&nbsp;&nbsp;';                    
                    return '<nobr>' + indent + row[1] + ' <em>(' + row[2] + ') ' + ext[0] + '</em></nobr>';
                }
                return '<nobr>' + row[1] + ' <em>(' + row[2] + ')</em></nobr>';
            },
            formatResult: function(row) {
                if(row[0] == 'efo') {
                    var ext = row[3].split('$');
                    return ext[0];
                }
                return row[1].indexOf(' ') >= 0 ? '"' + row[1] + '"' : row[1];
            }
        };
        return acoptions;
    };

    atlas.bindEfoTree = function(fvalfield, appender) {

        var fvalexpicon = $('<div/>');
        fvalfield = fvalfield.wrap('<div class="efoinput" />').before(fvalexpicon);


        function fvalexpfunc () {
            fvalfield.hideResults();
            var downTo = fvalfield.lastWord();
            var offset = fvalfield.offset();
            var hider;
            var efotree = $('<div/>')
                    .addClass('ac_results')
                    .css({
                        position: 'absolute',
                        minWidth: '300px',
                        maxWidth: '600px',
                        top: offset.top + fvalfield.get(0).offsetHeight,
                        left: offset.left
                    })
                    .append($('<div/>').addClass('tree').efoTree({ slideSpeed: 200, downTo: downTo, highlight: fvalfield.val() }, function(newv) {
                                 $(this).next().click();
                                 if(appender) {
                                    fvalfield.focus();
                                    var v = fvalfield.val();
                                    if(v.length > 0 && v.substr(v.length - 1) != ' ')
                                       v += ' ';
                                    newv = v + newv + ' ';
                                 }
                                 fvalfield.val(newv);
                            }))
                    .append(hider = $('<div class="ac_hide">hide tree</div>')
                            .click(function () {
                                 fvalfield.before($('<div/>').click(fvalexpfunc)).unbind('.efo');
                                 $(this).parent().remove();
                            }))
                    .appendTo(document.body);
                    
            function close () { hider.click(); }
            fvalfield.focus().bind('keydown.efo', close).bind('blur.efo', function() { if(atlas.preventBlur) fvalfield.focus(); else close();  }).bind('click.efo', close);
            $(this).remove();
        }
        
        fvalexpicon.click(fvalexpfunc);
    };

    atlas.initSimpleForm = function() {
        var gpropfield = $('input[name=gprop_0]');
        var factfield = $('input[name=fact_0]');
        var genefield = $('input[name=gval_0]');
        var fvalfield = $('input[name=fval_0]');
        var form = genefield.parents('form:first');
        var oldval = genefield.val();
        genefield
                .defaultvalue("(all genes)","(all genes)")
                .autocomplete(atlas.homeUrl + "fval", atlas.makeGeneAcOptions('', true))
                .result(function (unused, res) {
            var newprop = res[0];
            if(res[0] == 'name') {
                location.href = atlas.homeUrl + 'gene?gid=' + res[3].split('$')[2];
                atlas.startSearching(form);
                return;
            }
            gpropfield.val(newprop);
            var oldval = $(this).val();
            this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); };
            //  $(this).setOptions({extraParams: { type: 'gene', factor: newprop }}).flushCache();
        }).each(function () { this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); }; });

        fvalfield
                .defaultvalue("(all conditions)")
                .autocomplete(atlas.homeUrl +"fval", atlas.makeFvalAcOptions('', true))
                .result(function (unused, res) {
                            var newprop = res[0];
                            if(newprop != 'efo')
                                newprop = '';
                            factfield.val(newprop);
                            var oldval = $(this).val();
                            this.onkeyup = function () { if(oldval != this.value) factfield.val(''); };
                        });

        atlas.bindEfoTree(fvalfield);

        form.bind('submit', function () {
            $('input.ac_input', form).hideResults();
            atlas.startSearching(form);
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
            var newprop = res[0];
                gpropfield.val(newprop);
            var oldval = $(this).val();
            this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); };
            //  $(this).setOptions({extraParams: { type: 'gene', factor: newprop }}).flushCache();
        }).each(function () { this.onkeyup = function () { if(oldval != this.value) gpropfield.val(''); }; });
    };
})(jQuery);

