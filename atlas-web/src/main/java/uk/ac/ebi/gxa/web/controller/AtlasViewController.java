package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import uk.ac.ebi.gxa.dao.exceptions.RecordNotFoundException;
import uk.ac.ebi.gxa.exceptions.ResourceNotFoundException;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Closeables.closeQuietly;

/**
 * A base controller class to catch controller specific exceptions and handle them accordingly.
 *
 * @author Olga Melnichuk
 *         Date: Dec 1, 2010
 */
public class AtlasViewController {
    private static final Logger log = LoggerFactory.getLogger(AtlasViewController.class);

    public static final String JSON_ONLY_VIEW = "json-only-view";

    private static final String ERROR = "error";

    @ExceptionHandler({
            ResourceNotFoundException.class,
            RecordNotFoundException.class
    })
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleError(ResourceNotFoundException e) {
        log.error(e.getMessage());
        return errorDetails(e);
    }

    private ModelAndView errorDetails(Throwable e) {
        return new ModelAndView(ERROR)
                .addObject("errorMessage", e.getMessage())
                .addObject("errorClass", e.getClass().getName());
    }

    public static void send(HttpServletResponse response, File file) throws ResourceNotFoundException, IOException {
        ResourceType type = ResourceType.getByFileName(file.getName());
        if (type == null) {
            throw new ResourceNotFoundException("Can't find appropriate content type for file: " + file.getName());
        }
        send(response, file, type);
    }

    public static void send(HttpServletResponse response, File file, ResourceType contentType) throws ResourceNotFoundException, IOException {
        if (!file.exists()) {
            log.warn("send() - unknown file requested: {} as {}", file, contentType);
            throw new ResourceNotFoundException("Resource doesn't exist: " + file.getName());
        }

        BufferedInputStream in = null;
        try {
            response.setContentType(contentType.getContentType());
            in = new BufferedInputStream(new FileInputStream(file));
            copy(in, response.getOutputStream());
            response.getOutputStream().flush();
        } finally {
            closeQuietly(in);
        }
    }
}
