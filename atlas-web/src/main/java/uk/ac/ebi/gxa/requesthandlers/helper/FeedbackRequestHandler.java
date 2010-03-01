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
 * http://ostolop.github.com/gxa/
 */

package uk.ac.ebi.gxa.requesthandlers.helper;

import org.springframework.web.HttpRequestHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;
import javax.mail.Transport;
import javax.mail.Message;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.Properties;

import ae3.util.AtlasProperties;

/**
 * @author pashky
 */
public class FeedbackRequestHandler implements HttpRequestHandler {
    public void handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            boolean debug = false;

             //Set the host smtp address
            Properties props = new Properties();
            props.put("mail.smtp.host", AtlasProperties.getProperty("atlas.feedback.smtp.host"));

            // create some properties and get the default Session
            Session smtpSession = Session.getDefaultInstance(props, null);
            smtpSession.setDebug(debug);

            // create a message
            Message msg = new MimeMessage(smtpSession);

            // set the from and to address
            InternetAddress addressFrom = new InternetAddress(AtlasProperties.getProperty("atlas.feedback.from.address"));
            msg.setFrom(addressFrom);
            msg.setRecipients(Message.RecipientType.TO, new InternetAddress[] { new InternetAddress(AtlasProperties.getProperty("atlas.feedback.to.address")) });

            String email = request.getParameter("e");
            if (null != email && !email.equals(""))
                msg.setReplyTo(new InternetAddress[] {new InternetAddress(request.getParameter("e"))});

            // Setting the Subject and Content Type
            msg.setSubject(AtlasProperties.getProperty("atlas.feedback.subject"));
            msg.setContent(request.getParameter("f"), "text/plain");

            Transport.send(msg);
            response.getWriter().write("SEND OK");
        } catch (Exception e) {
            response.getWriter().write("SEND FAIL");
        }
    }
}
