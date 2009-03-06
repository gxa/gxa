var atlas = {};

(function($){
    atlas.homeUrl = '';

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
            max: 15,
            extraParams: { type: 'efv', factor: factor },
            extraContent: atlas.makeHideAutocompleteCode,
            formatItem: function(row) { return row[0]; },
            formatResult: function(row) { return row[0].indexOf(' ') >= 0 ? '"' + row[0] + '"' : row[0]; }
        };
        return acoptions;
    };

    atlas.initSimpleForm = function() {
        var gpropfield = $('input[name=gprop_0]');
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
                .autocomplete(atlas.homeUrl +"fval", atlas.makeFvalAcOptions('', true));

        form.bind('submit', function () {
            $('input.ac_input', form).hideResults();
            atlas.startSearching(form);
        });
    };

    atlas.startSearching = function(form) {
        var v = $(form).find('input[type=submit]');
        v.val('Searching...');
    };
})(jQuery);

