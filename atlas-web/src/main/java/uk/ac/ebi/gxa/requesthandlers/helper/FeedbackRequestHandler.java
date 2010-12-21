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

package uk.ac.ebi.gxa.requesthandlers.helper;

import com.google.common.base.Strings;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Properties;


/**
 * @author pashky
 */
public class FeedbackRequestHandler implements HttpRequestHandler {
    private AtlasProperties atlasProperties;

    public void setAtlasProperties(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            boolean debug = false;

             //Set the host smtp address
            Properties props = new Properties();
            props.put("mail.smtp.host", atlasProperties.getFeedbackSmtpHost());

            // create some properties and get the default Session
            Session smtpSession = Session.getDefaultInstance(props, null);
            smtpSession.setDebug(debug);

            // create a message
            Message msg = new MimeMessage(smtpSession);

            // set the from and to address
            InternetAddress addressFrom = new InternetAddress(atlasProperties.getFeedbackFromAddress());
            msg.setFrom(addressFrom);
            msg.setRecipients(Message.RecipientType.TO, new InternetAddress[] { new InternetAddress(atlasProperties.getFeedbackToAddress()) });

            String email = request.getParameter("e");
            if (!Strings.isNullOrEmpty(email))
                msg.setReplyTo(new InternetAddress[] {new InternetAddress(request.getParameter("e"))});

            // Setting the Subject and Content Type
            msg.setSubject(atlasProperties.getFeedbackSubject());
            msg.setContent(request.getParameter("f"), "text/plain");

            Transport.send(msg);
            response.getWriter().write("SEND OK");
        } catch (Exception e) {
            response.getWriter().write("SEND FAIL");
        }
    }
}
