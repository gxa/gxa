package ae3.restresult;

import uk.ac.ebi.gxa.utils.FilterIterator;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.AnnotatedElement;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;

/**
 * REST renderer utility class
 * @author pashky
 */
class Util {

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

    private static RestOut[] getAnnos(AnnotatedElement ae) {
        RestOuts restOuts = ae.getAnnotation(RestOuts.class);
        if(restOuts != null)
            return restOuts.value();
        RestOut restOut = ae.getAnnotation(RestOut.class);
        if(restOut != null)
            return new RestOut[] { restOut };
        return new RestOut[0];
    }

    public static RestOut getAnno(AnnotatedElement ae, Class renderer, Class profile) {
        for(RestOut a : getAnnos(ae)) {
            if((a.forProfile() == Object.class || a.forProfile().isAssignableFrom(profile))
                    && (a.forRenderer() == RestResultRenderer.class || a.forRenderer() == renderer))
                return a;
        }
        return null;
    }

    public static RestOut mergeAnno(RestOut a, AnnotatedElement ae, Class renderer, Class profile) {
        if(a == null)
            return Util.getAnno(ae, renderer, profile);
        return a;
    }

    static class Prop {
        String name;
        Object value;
        RestOut outProp;

        Prop(String name, Object value, RestOut outProp) {
            this.name = name;
            this.value = value;
            this.outProp = outProp;
        }
    }

    static boolean isEmpty(Object o) {
        if(o instanceof String)
            return "".equals(o);
        if(o instanceof Collection)
            return ((Collection)o).isEmpty();
        if(o instanceof Iterable)
            return ((Iterable)o).iterator().hasNext();
        if(o instanceof Iterator)
            return ((Iterator)o).hasNext();
        if(o instanceof Map)
            return ((Map)o).isEmpty();
        return false;
    }

    static Iterable<Prop> iterableProperties(final Object o, final Class profile, final RestResultRenderer renderer) {
        final Class rendererClass = renderer.getClass();
        if(o instanceof Map)
            return new Iterable<Prop>() {
                public Iterator<Prop> iterator() {
                    @SuppressWarnings("unchecked")
                    Iterator<Map.Entry> fromiter = ((Map) o).entrySet().iterator();
                    return new FilterIterator<Map.Entry,Prop>(fromiter) {
                        public Prop map(Map.Entry e) {
                            return e.getValue() != null ? new Prop(e.getKey().toString(), e.getValue(), null) : null;
                        }
                    };
                }
            };

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
