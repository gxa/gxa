package ae3.restresult;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author pashky
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface AsMap {
    String attr() default "id";
    String item() default "";
    String anonName() default "";
}
