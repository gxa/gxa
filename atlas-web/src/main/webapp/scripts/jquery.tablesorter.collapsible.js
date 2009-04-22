/**
 * @author Dan G. Switzer II
 */
(function ($){
	/* declare defaults */
	var defaults = {
		selector: "td.collapsible"        // the default selector to use
		, classChildRow: "expand-child"   // define the "child row" css class
		, classCollapse: "collapsed"      // define the "collapsed" css class
		, classExpand: "expanded"         // define the "expanded" css class
		, showCollapsed: true            // specifies if the default css state should show collapsed (use this if you want to collapse the rows using CSS by default)
		, collapse: true                  // if true will force rows to collapse via JS (use this if you want JS to force the rows collapsed)
		, fx: {hide:"hide",show:"show"}   // the fx to use for showing/hiding elements (fx do not work correctly in IE6)
		, addAnchor: "append"             // how should we add the anchor? append, wrapInner, etc
		, callback:function(){return false;}
	}, bHideParentRow = !!$.browser.msie;

	$.fn.collapsible = function (sel, options){
		var self = this, bIsElOpt = (sel && sel.constructor == Object),
			settings = $.extend({}, defaults, bIsElOpt ? sel : options);
		
		if( !bIsElOpt ) settings.selector = sel;
		// make sure that if we're forcing to collapse, that we show the collapsed css state
		if( settings.collapse ) settings.showCollapsed = true;

		return this.each(function (){
			var $td = $(settings.selector, this),
				// look for existing anchors
				$a = $td.find("a");
			
				// if no anchors, create them
				if( $a.length == 0 ) $a = $td[settings.addAnchor]('<a href="#" class="' + settings[settings.showCollapsed ? "classCollapse" : "classExpand"] + '"></a>').find("a");
				
				$tr = $a.parent().parent()
					
				$tr.bind("click", function(){
					
					var $self = $("#"+this.id + " a:first "), 
						$tr = $(this),
						$trc = $tr.next(), 
						bIsCollapsed = $self.hasClass(settings.classExpand);//alert($self.length)
					// change the css class
					$self[bIsCollapsed ? "removeClass" : "addClass"](settings.classExpand)[!bIsCollapsed ? "removeClass" : "addClass"](settings.classCollapse);
					while( $trc.hasClass(settings.classChildRow) ){
						if( bHideParentRow ){
							// get the tablesorter options
							var ts_config = $.data(self[0], "tablesorter");
							// hide/show the row
							$trc[bIsCollapsed ? settings.fx.hide : settings.fx.show]();
							
							// if we have the ts settings, we need to up zebra stripping if active
							if( !bIsCollapsed && ts_config ){
								if( $tr.hasClass(ts_config.widgetZebra.css[0]) ) $trc.addClass(ts_config.widgetZebra.css[0]).removeClass(ts_config.widgetZebra.css[1]);
								else if( $tr.hasClass(ts_config.widgetZebra.css[1]) ) $trc.addClass(ts_config.widgetZebra.css[1]).removeClass(ts_config.widgetZebra.css[0]);
							}
						}
						// show all the table cells
						$("td", $trc)[bIsCollapsed ? settings.fx.hide : settings.fx.show]();
						// get the next row
						$trc = $trc.next();
					}
					
					if(!bIsCollapsed)
						settings.callback(this);
					
					return false;
					
				});
			
			// if not IE and we're automatically collapsing rows, collapse them now
			if( settings.collapse && !bHideParentRow ){
				$td
					// get the tr element
					.parent()
					.each(function (){
						var $tr = $(this).next();
						while( $tr.hasClass(settings.classChildRow) ){
							// hide each table cell
							$tr = $tr.find("td").hide().end().next();
						}
					});
	  	}

			// if using IE, we need to hide the table rows
			if( settings.showCollapsed && bHideParentRow ){
				$td
					// get the tr element
					.parent()
					.each(function (){
						var $tr = $(this).next();
						while( $tr.hasClass(settings.classChildRow) ){
							$tr = $tr.hide().next();
						}
					});
			}
		});
	}
})(jQuery);
