package ae3.restresult;

import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Iterator;

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

                    {
                        i = 0;
                        skip();
                    }

                    public boolean hasNext() {
                        return i < methods.length;
                    }

                    public Prop next() {
                        Method m = methods[i];
                        RestOut ak = m.getAnnotation(RestOut.class);
                        String name;
                        if(ak != null && ak.name().length() != 0)
                            name = ak.name();
                        else
                            name = methodToProperty(m.getName());

                        Object value;
                        try {
                            value = m.invoke(o, (Object[])null);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        }

                        ++i;
                        skip();

                        return new Prop(name, value, m);
                    }

                    private void skip() {
                        while(i < methods.length &&
                                (methods[i].getParameterTypes().length > 0 ||
                                        (checkAnno && (!methods[i].isAnnotationPresent(RestOut.class)
                                                || !methods[i].getAnnotation(RestOut.class).profile().isAssignableFrom(profile))) ||
                                        (!checkAnno && !methods[i].getName().startsWith("get") && !methods[i].getName().startsWith("is")) ||
                                        (!checkAnno && methods[i].getName().equals("getClass"))
                                ))
                            ++i;
                    }

                    public void remove() { }
                };
            }
        };
    }
}
