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

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;

import javax.annotation.Nonnull;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * REST renderer utility class
 * @author pashky
 */
class RestResultRendererUtil {

    /**
     * Converts method name to property name according to Joava Beans rules
     * or just by camelcasing it if it's not JavaBeans style name.
     * @param name method name
     * @return property name
     */
    static String methodToProperty(String name) {
        if (name.startsWith("get")) {
            name = name.substring(3);
        } else if (name.startsWith("is")) {
            name = name.substring(2);
        } else {
            return name;
        }
        if (Character.isUpperCase(name.charAt(0))) {
            if(name.length() > 1)
                name = name.substring(0, 1).toLowerCase() + name.substring(1);
            else
                name = name.toLowerCase();
        }
        return name;
    }

    /**
     * Get all RestOut annotations applied to some element
     * @param ae annotated element
     * @return array of RestOut
     */
    private static RestOut[] getAnnos(AnnotatedElement ae) {
        RestOuts restOuts = ae.getAnnotation(RestOuts.class);
        if(restOuts != null)
            return restOuts.value();
        RestOut restOut = ae.getAnnotation(RestOut.class);
        if(restOut != null)
            return new RestOut[] { restOut };
        return new RestOut[0];
    }

    /**
     * Returns correct annotation for specific element, profile class and renderer class
     * @param ae element
     * @param renderer renderer class
     * @param profile profile class
     * @return just one first matching RestOut annotation
     */
    public static RestOut getAnno(AnnotatedElement ae, Class renderer, Class profile) {
        for(RestOut a : getAnnos(ae)) {
            if((a.forProfile() == Object.class || a.forProfile().isAssignableFrom(profile))
                    && (a.forRenderer() == RestResultRenderer.class || a.forRenderer() == renderer))
                return a;
        }
        return null;
    }

    /**
     * Merge annotations for element and some other one
     * @param a custom annotation
     * @param ae element
     * @param renderer renderer class
     * @param profile profile class
     * @return just one first matching RestOut annotation
     */
    public static RestOut mergeAnno(RestOut a, AnnotatedElement ae, Class renderer, Class profile) {
        if(a == null)
            return RestResultRendererUtil.getAnno(ae, renderer, profile);
        return a;
    }

    /**
     * Property comtainer class
     */
    static class Prop {
        /**
         * Name
         */
        String name;
        /**
         * Value
         */
        Object value;
        /**
         * Annotation
         */
        RestOut outProp;

        /**
         * Constructor
         * @param name name
         * @param value value
         * @param outProp annotation
         */
        Prop(String name, Object value, RestOut outProp) {
            this.name = name;
            this.value = value;
            this.outProp = outProp;
        }
    }

    /**
     * Check if objkect value is empty in generic sense (string or collection)
     * @param o object
     * @return true if empty
     */
    static boolean isEmpty(Object o) {
        if (o instanceof String)
            return "".equals(o);
        if (o instanceof Collection)
            return ((Collection) o).isEmpty();
        if (o instanceof Iterable)
            return ((Iterable) o).iterator().hasNext();
        if (o instanceof Iterator)
            return ((Iterator) o).hasNext();
        return o instanceof Map && ((Map) o).isEmpty();
    }

    /**
     * Iterate over object's propertties (handles objects and collections) handling only those
     * unrestricted by annotations
     *
     * @param o object
     * @param profile profile class
     * @param renderer renderer class
     * @return iterable of properties and values
     */
    static Iterable<Prop> iterableProperties(final Object o, final Class profile, final RestResultRenderer renderer) {
        final Class rendererClass = renderer.getClass();
        if (o instanceof Map) {
            @SuppressWarnings("unchecked")
            Set<Map.Entry> entries = ((Map) o).entrySet();
            return Iterables.transform(
                    Iterables.filter(entries,
                            new Predicate<Map.Entry>() {
                                public boolean apply(@Nonnull Map.Entry entry) {
                                    return entry.getValue() != null;
                                }
                            }),
                    new Function<Map.Entry, Prop>() {
                        public Prop apply(@Nonnull Map.Entry entry) {
                            return new Prop(entry.getKey().toString(), entry.getValue(), null);
                        }
                    });
        }

        return new Iterable<Prop>() {

            public Iterator<Prop> iterator() {
                final Method[] methods = o.getClass().getMethods();
                boolean noAnnos = true;
                for(Method m : methods)
                    if(m.isAnnotationPresent(RestOut.class))
                        noAnnos = false;
                final boolean checkAnno = !noAnnos;
                return new Iterator<Prop>() {
                    int i;
                    Object value;
                    RestOut restOut;

                    {
                        i = 0;
                        skip();
                    }

                    public boolean hasNext() {
                        return i < methods.length;
                    }

                    public Prop next() {
                        Method m = methods[i];
                        Object value = this.value;
                        RestOut restOut = this.restOut;
                        String name;
                        if(restOut != null && restOut.name().length() != 0)
                            name = restOut.name();
                        else
                            name = methodToProperty(m.getName());

                        ++i;
                        skip();

                        return new Prop(name, value, restOut);
                    }


                    private void skip() {
                        while(i < methods.length) {
                            if(methods[i].getParameterTypes().length == 0) {
                                if(checkAnno) {
                                    restOut = getAnno(methods[i], rendererClass, profile);
                                    if(restOut != null) {
                                        try {
                                            value = methods[i].invoke(o, (Object[])null);
                                            if(value != null) {
                                                if(restOut.exposeEmpty() || !isEmpty(value)) {
                                                    return;
                                                }
                                            }
                                        } catch (IllegalAccessException e) {
                                            throw new RuntimeException(e);
                                        } catch (InvocationTargetException e) {
                                            throw new RuntimeException(e);
                                        }

                                    }
                                } else if((methods[i].getName().startsWith("get") || methods[i].getName().startsWith("is"))
                                                && !methods[i].getName().equals("getClass")) {
                                    try {
                                        try {
                                            value = methods[i].invoke(o, (Object[])null);
                                        } catch(IllegalAccessException e) {
                                            methods[i].setAccessible(true);
                                            value = methods[i].invoke(o, (Object[])null);
                                        }
                                        if(value != null) {
                                            restOut = null;
                                            return;
                                        }
                                    } catch (IllegalAccessException e) {
                                        throw new RuntimeException(e);
                                    } catch (InvocationTargetException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                            ++i;
                        }
                    }

                    public void remove() { }
                };
            }
        };
    }
}
