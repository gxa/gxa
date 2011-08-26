package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;

/**
 * @author Olga Melnichuk
 *         Date: 11/01/2011
 */
@Controller
public class ExternalResourceController extends AtlasViewController {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private AtlasProperties atlasProperties;

    @Autowired
    public ExternalResourceController(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    @RequestMapping(value = "/look/*", method = RequestMethod.GET)
    public void getResource(HttpServletRequest request, HttpServletResponse response) throws ResourceNotFoundException, IOException {
        String[] uri = request.getServletPath().split("/");
        String resourceName = uri[uri.length - 1];

        File dir = new File(atlasProperties.getLafResourcesDir());
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ResourceNotFoundException("Look and feel directory for external resources is not set: \"" + dir.getPath() + "\"");
        }

        ResourceType type = ResourceType.getByFileName(resourceName);
        if (type == null) {
            throw new ResourceNotFoundException("Undefined type of external resource: " + resourceName);
        }

        send(response, new File(dir, resourceName), type);
    }
}
