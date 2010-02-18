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
            atlas.homeUrl + "feedback",
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

