package ae3.restresult;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author pashky
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AsObject {
    String anonName() default "";
}
