package uk.ac.ebi.gxa.requesthandlers.base.restutil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * @author pashky
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestOut {
    String name() default "";
    Class forProfile() default Object.class;
    boolean exposeEmpty() default true;
    Class forRenderer() default RestResultRenderer.class;
    String xmlAttr() default "";
    String xmlItemName() default "";
    boolean asString() default false;
}
