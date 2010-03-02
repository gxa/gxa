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

