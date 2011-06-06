package uk.ac.ebi.gxa.web.controller;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Closeables.closeQuietly;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 6/2/11
 * Time: 3:31 PM
 * This class stores an enumeration of valid resource mime types and their corresponding file extensions.
 * Its handle() method returns the requested experiment asset provided that its mime type matches one of the
 * mime types enumerated in this class.
 */
public enum ResourcePattern {
    CSS("text/css", "css"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif");

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
            in = new BufferedInputStream(new FileInputStream(f));
            copy(in, response.getOutputStream());
            response.getOutputStream().flush();
        } finally {
            closeQuietly(in);
        }

        return true;
    }
}
