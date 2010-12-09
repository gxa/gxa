package uk.ac.ebi.gxa.web.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;

/**
 * A base controller class to catch controller specific exceptions and handle them accordingly.
 *
 * @author Olga Melnichuk
 *         Date: Dec 1, 2010
 */
@Controller
public class AtlasViewController {

    private static final String ERROR = "error";

    private final Logger log = LoggerFactory.getLogger(getClass());

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleError(ResourceNotFoundException e) {
        log.info(e.getMessage());
        return errorDetails(e);
    }

    private ModelAndView errorDetails(Throwable e) {
        return new ModelAndView(ERROR)
                .addObject("errorMessage", e.getMessage())
                .addObject("errorClass", e.getClass().getName());
    }
}
