package ae3.service.experiment.rcommand;

import com.google.common.base.Function;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Iterables.transform;

/**
 * @author Olga Melnichuk
 *         Date: 25/03/2011
 */
public class RCommandStatement {

    private final String functionName;
    private final List<String> params = new ArrayList<String>();

    public RCommandStatement(String functionName) {
        this.functionName = functionName;
    }

    public RCommandStatement addParam(Number param) {
        return add(wrap(param));
    }

    public RCommandStatement addParam(String param) {
        return add(wrap(param));
    }

    public RCommandStatement addParam(Iterable<?> iterable) {
        return add(wrap(iterable));
    }

    @Override
    public String toString() {
        return functionName + "(" + on(",").join(params) + ")";
    }

    private RCommandStatement add(String str) {
        params.add(str);
        return this;
    }

    private <T> String wrap(Iterable<T> iterable) {
        return "c(" + on(",").join(transform(iterable, wrapper())) + ")";
    }

    private <T> Function<T, String> wrapper() {
        return new Function<T, String>() {
            public String apply(@Nonnull T input) {
                if (input instanceof Number) {
                    return wrap((Number) input);
                }
                return wrap(input);
            }
        };
    }

    private String wrap(Number num) {
        return num.toString();
    }

    private String wrap(Object obj) {
        return "'" + obj.toString() + "'";
    }

}
