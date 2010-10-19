/*!
 * slideViewer 1.2
 * Examples and documentation at: 
 * http://www.gcmingati.net/wordpress/wp-content/lab/jquery/imagestrip/imageslide-plugin.html
 * 2007-2010 Gian Carlo Mingati
 * Version: 1.2.3 (9-JULY-2010)
 * Dual licensed under the MIT and GPL licenses:
 * http://www.opensource.org/licenses/mit-license.php
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Requires:
 * jQuery v1.4.1 or later, jquery.easing.1.2
 * 
 */

jQuery(function(){
   jQuery("div.svw").prepend("<img src='spinner.gif' class='ldrgif' alt='loading...'/ >"); 
});
var j = 0;
var quantofamo = 0;
jQuery.fn.slideView = function(settings) {
	settings = jQuery.extend({
		easeFunc: "easeInOutExpo",
		easeTime: 750,
		uiBefore: false,
		toolTip: false,
		ttOpacity: 0.9
	}, settings);
	return this.each(function(){
		var container = jQuery(this);
		container.find("img.ldrgif").remove();
		container.removeClass("svw").addClass("stripViewer");		
		var pictWidth = container.find("img").width();
		var pictHeight = container.find("img").height();
		var pictEls = container.find("li").size();
		var stripViewerWidth = pictWidth*pictEls;
		container.find("ul").css("width" , stripViewerWidth);
		container.css("width" , pictWidth);
		container.css("height" , pictHeight);
		container.each(function(i) {
    (!settings.uiBefore) ? jQuery(this).after("<div class='stripTransmitter' id='stripTransmitter" + (j) + "'><ul><\/ul><\/div>") : jQuery(this).before("<div class='stripTransmitter' id='stripTransmitter" + (j) + "'><ul><\/ul><\/div>");			
		jQuery(this).find("li").each(function(n) {
		jQuery("div#stripTransmitter" + j + " ul").append("<li><a title='" + jQuery(this).find("img").attr("alt") + "' href='#'>"+(n+1)+"<\/a><\/li>");												
		});
		jQuery("div#stripTransmitter" + j + " a").each(function(z) {
		jQuery(this).bind("click", function(){		
		jQuery(this).addClass("current").parent().parent().find("a").not(jQuery(this)).removeClass("current"); // wow!
		var cnt = -(pictWidth*z);
		container.find("ul").animate({ left: cnt}, settings.easeTime, settings.easeFunc);
		return false;
		});
		});
		
		
		container.bind("click", function(e){
			var ui = (!settings.uiBefore) ? jQuery(this).next().find("a.current") : jQuery(this).prev().find("a.current");
			var bTotal = parseFloat(jQuery(this).css('borderLeftWidth').replace("px", "")) +  parseFloat(jQuery(this).css('borderRightWidth').replace("px", ""));
			var dOs = jQuery(this).offset();
			var zeroLeft = (bTotal/2 + pictWidth) - (e.pageX - dOs.left);
			if(zeroLeft >= pictWidth/2) { 
				var uiprev = ui.parent().prev().find("a");	
				(jQuery(uiprev).length != 0)? uiprev.trigger("click") : ui.parent().parent().find("a:last").trigger("click");							
			} 
			else {
				var uinext = ui.parent().next().find("a");
			  (jQuery(uinext).length != 0)? uinext.trigger("click") : ui.parent().parent().find("a:first").trigger("click");
			}
		});
		
		
		jQuery("div#stripTransmitter" + j).css("width" , pictWidth);
		jQuery("div#stripTransmitter" + j + " a:first").addClass("current");
		jQuery('body').append('<div class="tooltip" style="display:none;"><\/div>');
		

		if(settings.toolTip){
		var aref = jQuery("div#stripTransmitter" + j + " a");

		aref.live('mousemove', function(e) {
		var att = jQuery(this).attr('title');
		posX=e.pageX+10;
		posY=e.pageY+10;
		jQuery('.tooltip').html(att).css({'position': 'absolute', 'top': posY+'px', 'left': posX+'px', 'display': 'block', 'opacity': settings.ttOpacity});
		});
		aref.live('mouseout', function() {
		jQuery('.tooltip').hide();
		});				
		}
		});
		j++;
	});	
};