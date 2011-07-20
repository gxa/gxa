package uk.ac.ebi.gxa.utils;

import javax.annotation.concurrent.GuardedBy;
import java.util.List;

import static com.google.common.collect.Lists.newArrayList;
import static uk.ac.ebi.gxa.exceptions.LogUtil.createUnexpected;

/**
 * ThreadLocal enabling one to drop (almost) all of the instances it is holding.
 * <p/>
 * As of {@link ThreadLocal} documentation,
 * <blockquote>
 * Each thread holds an implicit reference to its copy of a thread-local
 * variable as long as the thread is alive and the <tt>ThreadLocal</tt>
 * instance is accessible; after a thread goes away, all of its copies of
 * thread-local instances are subject to garbage collection (unless other
 * references to these copies exist).
 * </blockquote>
 * <p/>
 * But since we have a thread pool, we cannot be sure the thread is going
 * to get away in any foreseeable future - hence, we should make every
 * effort to clean up our ThreadLocals.
 * <p/>
 * The footprint we leave is one object having one <code>null</code> reference.
 */
public class GCFriendlyThreadLocal<T> {
    private final Class<T> clazz;

    @GuardedBy("holders")
    private final List<Holder<T>> holders = newArrayList();
    private final ThreadLocal<Holder<T>> threadLocal = new ThreadLocal<Holder<T>>() {
        @Override
        protected synchronized Holder<T> initialValue() {
            try {
                final Holder<T> result = Holder.create(clazz.newInstance());
                synchronized (holders) {
                    holders.add(result);
                }
                return result;
            } catch (InstantiationException e) {
                throw createUnexpected("Cannot create an object of " + clazz, e);
            } catch (IllegalAccessException e) {
                throw createUnexpected("Cannot create an object of " + clazz, e);
            }
        }
    };

    /**
     * Creates a GC-friendly ThreadLocal instance
     *
     * @param clazz the class of initial value; must have public default constructor
     */
    public GCFriendlyThreadLocal(Class<T> clazz) {
        this.clazz = clazz;
    }

    /**
     * Returns the value in the current thread's copy of this
     * thread-local variable.  If the variable has no value for the
     * current thread, it is first initialized to an instance of
     * the class passed to the constructor.
     *
     * @return the current thread's value of this thread-local
     * @see {@link ThreadLocal#get}
     */
    public T get() {
        return threadLocal.get().getValue();
    }

    /**
     * Returns all the values associated with this thread-local.
     * <p/>
     * Note that the behavior of this method is undefined in multithreaded environment;
     * it is the caller's responsibility to ensure that all the {@link #get} calls
     * and the value processing <em>happen-before</em> call to this method.
     *
     * @return a list of values associated with this thread-local
     */
    public List<T> getAll() {
        List<T> result = newArrayList();
        synchronized (holders) {
            for (Holder<T> holder : holders) {
                result.add(holder.getValue());
            }
        }
        return result;
    }

    /**
     * Makes all the values associated with this thread-local eligible for GC.
     * <p/>
     * The method is destructive: the behaviour of the object after call to this method is undefined.
     * <p/>
     * Note that the behavior of this method is undefined in multithreaded environment;
     * it is the caller's responsibility to ensure that all the {@link #get} calls
     * and the value processing <em>happen-before</em> call to this method.
     */
    public void destroyAndAllowGC() {
        synchronized (holders) {
            for (Holder<T> holder : holders) {
                holder.clean();
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        destroyAndAllowGC();
        super.finalize();
    }

    /**
     * Simple holder structure
     * <p/>
     * Maintains an object pointer, allows one to be be reset to <code>null</code> thus
     * allowing the referenced object to be garbage collected.
     *
     * @param <T> the class of object to hold
     */
    private static class Holder<T> {
        private T value;

        private Holder(T value) {
            this.value = value;
        }

        /**
         * Creates a holder for a given value
         *
         * @param value the value to hold
         * @param <T>   class of the value
         * @return a {@link Holder} object holding the value
         */
        public static <T> Holder<T> create(T value) {
            return new Holder<T>(value);
        }

        public T getValue() {
            return value;
        }

        public void clean() {
            value = null;
        }
    }
}
