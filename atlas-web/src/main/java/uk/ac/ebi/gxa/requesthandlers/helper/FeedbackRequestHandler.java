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
