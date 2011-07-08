package uk.ac.ebi.gxa.utils;

import com.google.common.base.Function;
import uk.ac.ebi.gxa.exceptions.LogUtil;

import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * @author Misha Kapushesky
 */
public class GuavaUtil {
    /**
     * Creates a {@link Function} that transforms instances of A to B using B's constructor
     * of the form <code>B(A a)</code>.
     */
    public static <A,B> Function<A,B> instanceTransformer(final Class<A> fromA, final Class<B> toB) {
        return new Function<A,B> () {
            @Override
            public B apply(@Nullable A input) {
                try {
                    Constructor<B> con = toB.getConstructor(fromA);
                    return con.newInstance(input);
                } catch (NoSuchMethodException e) {
                    throw LogUtil.createUnexpected("Unexpected!", e);
                } catch (InvocationTargetException e) {
                    throw LogUtil.createUnexpected("Unexpected!", e.getTargetException());
                } catch (InstantiationException e) {
                    throw LogUtil.createUnexpected("Unexpected!", e);
                } catch (IllegalAccessException e) {
                    throw LogUtil.createUnexpected("Unexpected!", e);
                }
            }
        };
    }
}
