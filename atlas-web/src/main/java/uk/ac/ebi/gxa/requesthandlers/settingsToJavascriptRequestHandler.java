package uk.ac.ebi.gxa.requesthandlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.HttpRequestHandler;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Dec 10, 2010
 * Time: 2:35:25 PM
 * To change this template use File | Settings | File Templates.
 */
public class SettingsToJavascriptRequestHandler implements HttpRequestHandler {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private AtlasProperties atlasProperties;

    public void setAtlasProperties(AtlasProperties atlasProperties){
        this.atlasProperties = atlasProperties; 
    }

    public void handleRequest(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws ServletException, IOException {
        
        httpServletResponse.getOutputStream().print("hello world");
    }
}
