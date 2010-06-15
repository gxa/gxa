/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

package uk.ac.ebi.gxa.requesthandlers.base.restutil;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * REST serializer annotation.
 * This annotation is used to provide hints for serializer on how to handle property or method
 * @author pashky
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface RestOut {
    /**
     * Override property name
     * @return property name
     */
    String name() default "";

    /**
     * Restrict the property or class only for specific profile (or any its subclass!)
     * @return profile class
     */
    Class forProfile() default Object.class;

    /**
     * Defines if empty values should be exposed at all or not
     * @return Expose empty values at all or not
     */
    boolean exposeEmpty() default true;

    /**
     * Restrict object only for specific renderer class
     * @return renderer class
     */
    Class forRenderer() default RestResultRenderer.class;

    /**
     * XML renderer specific: defines xml attribute name to put property to
     * @return attribute name
     */
    String xmlAttr() default "";

    /**
     * XML renderer specific: defines xml item name for arrays and maps
     * @return
     */
    String xmlItemName() default "";

    /**
     * Defines if property should be serialized as its toString() value and not by common rules
     * @return true if stringified
     */
    boolean asString() default false;
}
