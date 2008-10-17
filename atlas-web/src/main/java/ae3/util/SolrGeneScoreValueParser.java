package ae3.util;

import org.apache.solr.search.ValueSourceParser;
import org.apache.solr.search.FunctionQParser;
import org.apache.solr.search.function.ValueSource;
import org.apache.solr.search.function.DocValues;
import org.apache.solr.common.util.NamedList;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.index.IndexReader;

import java.util.List;
import java.util.Arrays;
import java.io.IOException;

/**
 * @author pashky
 */
public class SolrGeneScoreValueParser extends ValueSourceParser {
    private static class Function extends ValueSource {
        private final ValueSource[] sources;

        public Function(ValueSource[] sources) {
            this.sources = sources;
        }

        private String name() { return "genescore"; }

        public String description() {
            StringBuilder sb = new StringBuilder();
            sb.append(name()).append('(');
            boolean firstTime=true;
            for (ValueSource source : sources) {
                if (firstTime) {
                    firstTime=false;
                } else {
                    sb.append(',');
                }
                sb.append(source);
            }
            sb.append(')');
            return sb.toString();
        }

        public DocValues getValues(IndexReader reader) throws IOException {
            final DocValues[] valsArr = new DocValues[sources.length];
            for (int i=0; i<sources.length; i++) {
                valsArr[i] = sources[i].getValues(reader);
            }

            return new DocValues() {
                public float floatVal(int doc) {
                    float val = 0.0f;
//                    valsArr.
                    for (DocValues vals : valsArr) {
                        val += vals.floatVal(doc);
                    }
                    return val;
                }
                public int intVal(int doc) {
                    return (int)floatVal(doc);
                }
                public long longVal(int doc) {
                    return (long)floatVal(doc);
                }
                public double doubleVal(int doc) {
                    return (double)floatVal(doc);
                }
                public String strVal(int doc) {
                    return Float.toString(floatVal(doc));
                }
                public String toString(int doc) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(name()).append('(');
                    boolean firstTime=true;
                    for (DocValues vals : valsArr) {
                        if (firstTime) {
                            firstTime=false;
                        } else {
                            sb.append(',');
                        }
                        sb.append(vals.toString(doc));
                    }
                    sb.append(')');
                    return sb.toString();
                }
            };
        }

        public int hashCode() {
            return Arrays.hashCode(sources) + name().hashCode();
        }

        public boolean equals(Object o) {
            if (this.getClass() != o.getClass()) return false;
            Function other = (Function)o;
            return this.name().equals(other.name())
                    && Arrays.equals(this.sources, other.sources);
        }
    }

    public void init(NamedList args) {
    }

    public ValueSource parse(FunctionQParser fp) throws ParseException {
        List<ValueSource> sources = fp.parseValueSourceList();
        return new Function(sources.toArray(new ValueSource[sources.size()]));
    }
}
