package uk.ac.ebi.gxa.exceptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.lang.String.format;

/**
 * Utility class logging common error cases
 */
public final class LogUtil {
    private static Logger log = LoggerFactory.getLogger(LogUtil.class);

    private LogUtil() {
        // utility class
    }

    public static UnexpectedException createUnexpected(String message, Throwable e) {
        log.error(message, e);
        return new UnexpectedException(message, e);
    }

    public static UnexpectedException createUnexpected(String message) {
        final UnexpectedException exception = new UnexpectedException(message);
        final StackTraceElement caller = exception.getStackTrace()[1];
        log.error(format("%s\n%s: %s\n\tat %s.%s (%s:%d)", message,
                exception.getClass().getName(), message,
                caller.getClassName(), caller.getMethodName(),
                caller.getFileName(), caller.getLineNumber()), exception);
        return exception;
    }
}
