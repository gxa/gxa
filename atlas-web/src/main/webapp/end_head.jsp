<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="f" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://ebi.ac.uk/ae3/functions" prefix="u" %>

    </title>

    <link rel="stylesheet"  href="http://www.ebi.ac.uk/inc/css/contents.css"     type="text/css" />
    <link rel="stylesheet"  href="http://www.ebi.ac.uk/inc/css/userstyles.css"   type="text/css" />
    <script  src="http://www.ebi.ac.uk/inc/js/contents.js" type="text/javascript"></script>
    <link rel="SHORTCUT ICON" href="http://www.ebi.ac.uk/bookmark.ico" />

    <!--  start meta tags, css , javascript here   -->
    <link rel="stylesheet"  href="http://www.ebi.ac.uk/inc/css/sidebars.css"   type="text/css" />

    <script type="text/javascript">
        var feedback_formtxt = 'Tell us what you think:<br/>' +
                  '<textarea style="width:100%" rows="5" id="feedback_txt" name="feedback_txt"/><br/><br/>' +
                  'Email (optional): ' +
                  '<input size="20" id="feedback_email" name="feedback_email"/>';

        function resetFeedback() {
           $("#feedback_thanks").hide();
        }

        function sendFeedback(v,m){
              if(v) {
                  $.post(
                    "feedback.jsp",
                    { f: m.children('#feedback_txt').val(),
                      e: m.children('#feedback_email').val()
                    },
                    function(res) {
                        if(-1 != res.indexOf("SEND OK")) {
                          $("#feedback_thanks").show();
                          setTimeout(resetFeedback, 3000);
                        } else {
                            alert ("Failed to send feedback! Sorry!");
                        }
                    }
                  );
              }
              return true;
        }

        function showFeedbackForm() {
            $.prompt(feedback_formtxt,{
              submit: sendFeedback,
              buttons: { Send: true, Cancel: false }
            });
        }
    </script>