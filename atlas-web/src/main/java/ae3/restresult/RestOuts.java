package ae3.restresult;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation allows to specify several {@link ae3.restresult.RestOut} annotation on some element
 * e.g. to specify different rules for different profiles and/or formatters. The first matching annotation
 * is going to be chosen then, if there're no matching annotations, element is ignored during output
 * @author pashky
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestOuts {
    /**
     * Array of REST options annotations
     * @return array of annotations
     */
    RestOut[] value();
}
