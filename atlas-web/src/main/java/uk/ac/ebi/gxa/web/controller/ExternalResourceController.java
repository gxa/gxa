package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import uk.ac.ebi.gxa.properties.AtlasProperties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Closeables.closeQuietly;

/**
 * @author Olga Melnichuk
 *         Date: 11/01/2011
 */
@Controller
public class ExternalResourceController extends AtlasViewController {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private static enum ResourcePattern {
        CSS("text/plain", "css"),
        PNG("image/png", "png");

        private String contentType;
        private Pattern pattern;

        private ResourcePattern(String contentType, String extension) {
            this.contentType = contentType;
            this.pattern = Pattern.compile("(:?[^\\.]+)\\." + extension);
        }

        public boolean handle(File dir, String resourceName, HttpServletResponse response) throws ResourceNotFoundException, IOException {
            Matcher m = pattern.matcher(resourceName);
            if (!m.matches()) {
                return false;
            }

            File f = new File(dir, resourceName);
            if (!f.exists()) {
                throw new ResourceNotFoundException("Resource doesn't exist: " + f.getAbsolutePath());
            }

            BufferedInputStream in = null;
            try {
                response.setContentType(contentType);
                copy(in, response.getOutputStream());
                response.getOutputStream().flush();
            } finally {
                closeQuietly(in);
            }

            return true;
        }
    }

    private AtlasProperties atlasProperties;

    @Autowired
    public ExternalResourceController(AtlasProperties atlasProperties) {
        this.atlasProperties = atlasProperties;
    }

    @RequestMapping(value = "/look/*", method = RequestMethod.GET)
    public void getResource(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ResourceNotFoundException, IOException {

        String[] uri = request.getServletPath().split("/");
        String resourceName = uri[uri.length - 1];

        File dir = new File(atlasProperties.getLafResourcesDir());
        if (!dir.exists() || !dir.isDirectory()) {
            throw new ResourceNotFoundException("Look and feel directory for external resources is not set: \"" + dir.getPath() + "\"");
        }

        for (ResourcePattern rp : ResourcePattern.values()) {
            if (rp.handle(dir, resourceName, response)) {
                return;
            }
        }
        throw new ResourceNotFoundException("Undefined type of external resource: " + resourceName);
    }
}
