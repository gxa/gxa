package uk.ac.ebi.gxa.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class logging common error cases
 */
public final class LogUtil {
    private static Logger log = LoggerFactory.getLogger(LogUtil.class);

    private LogUtil() {
        // utility class
    }

    public static UnexpectedException logUnexpected(String message, Throwable e) {
        log.error(message, e);
        return new UnexpectedException(message, e);
    }

    public static UnexpectedException logUnexpected(String message) {
        log.error(message);
        return new UnexpectedException(message);
    }

    public static RuntimeException todoImplementMe() {
        return new RuntimeException("TODO: implement me");
    }
}
