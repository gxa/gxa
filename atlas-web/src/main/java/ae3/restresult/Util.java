package ae3.restresult;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Iterator;
import java.util.Collection;

/**
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

    static class Prop {
        String name;
        Object value;
        Method method;

        Prop(String name, Object value, Method method) {
            this.name = name;
            this.value = value;
            this.method = method;
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

    static Iterable<Prop> iterableProperties(final Object o, final Class profile) {
        if(o instanceof Map)
            return new Iterable<Prop>() {
                public Iterator<Prop> iterator() {
                    return new Iterator<Prop>() {
                        Iterator i = ((Map)o).entrySet().iterator();
                        public boolean hasNext() {
                            return i.hasNext();
                        }

                        public Prop next() {
                            Map.Entry e = (Map.Entry)i.next();
                            return new Prop(e.getKey().toString(), e.getValue(), null);
                        }

                        public void remove() { }
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
                    Object v;

                    {
                        i = 0;
                        skip();
                    }

                    public boolean hasNext() {
                        return i < methods.length;
                    }

                    public Prop next() {
                        Method m = methods[i];
                        Object value = v;
                        RestOut a = m.getAnnotation(RestOut.class);
                        String name;
                        if(a != null && a.name().length() != 0)
                            name = a.name();
                        else
                            name = methodToProperty(m.getName());

                        ++i;
                        skip();

                        return new Prop(name, value, m);
                    }

                    private void skip() {
                        while(i < methods.length) {
                            if(methods[i].getParameterTypes().length == 0) {
                                if(checkAnno) {
                                    if(methods[i].isAnnotationPresent(RestOut.class)) {
                                        RestOut a = methods[i].getAnnotation(RestOut.class);
                                        if(a.profile() == Object.class || a.profile().isAssignableFrom(profile)) {
                                            try {
                                                v = methods[i].invoke(o, (Object[])null);
                                                if(v != null) {
                                                    if(a.empty() || !isEmpty(v))
                                                        return;
                                                }
                                            } catch (IllegalAccessException e) {
                                                throw new RuntimeException(e);
                                            } catch (InvocationTargetException e) {
                                                throw new RuntimeException(e);
                                            }

                                        }
                                    }
                                } else if((methods[i].getName().startsWith("get") || methods[i].getName().startsWith("is"))
                                                && !methods[i].getName().equals("getClass")) {
                                    i = i;
                                    try {
                                        v = methods[i].invoke(o, (Object[])null);
                                        if(v != null)
                                            return;
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
