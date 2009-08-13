package ae3.restresult;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author pashky
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestOut {
    String name() default "";
    Class profile() default Object.class;
    boolean empty() default true;
}
