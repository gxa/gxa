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

var feedback_formtxt = 'What were you trying to do:<br/>' +
          '<textarea style="width:100%" rows="5" id="feedback_context" name="feedback_context"/><br/><br/>' +
          'What went wrong:<br/>' +
          '<textarea style="width:100%" rows="5" id="feedback_error" name="feedback_error"/><br/><br/>' +
          'What could be done better:<br/>' +
          '<textarea style="width:100%" rows="5" id="feedback_dobetter" name="feedback_dobetter"/><br/><br/>' +
          'Email (optional): ' +
          '<input size="20" id="feedback_email" name="feedback_email"/>';

function resetFeedback() {
   $("#feedback_thanks").hide();
}

function sendFeedback(v,m){
      if(v) {
        var email = m.children('#feedback_email').val();
        if (email && !validateEmail(email)) {
            alert('Invalid email address = ' + email);
            m.children('#feedback_email').val(''); // reset the email field
            return false;
        }

        $.post(
          atlas.homeUrl + "feedback",
          { context:    m.children('#feedback_context').val(),
            error:      m.children('#feedback_error').val(),
            dobetter:   m.children('#feedback_dobetter').val(),
            email:      email,
            url:        window.location.href
          },
          function(res) {
                if(-1 != res.indexOf("SEND OK")) {
                    $("#feedback_thanks").show();
                    setTimeout(resetFeedback, 3000);
                } else {
                    alert("Failed to send feedback! Sorry!");
                    return false;
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


/**
 * From http://jquerybyexample.blogspot.com/2011/04/validate-email-address-using-jquery.html
 * @param txtEmail
 */
function validateEmail(email) {
    var filter = /^[a-zA-Z0-9]+[a-zA-Z0-9_.-]+[a-zA-Z0-9_-]+@[a-zA-Z0-9]+[a-zA-Z0-9.-]+[a-zA-Z0-9]+.[a-z]{2,4}$/;
    return filter.test(email);
}
