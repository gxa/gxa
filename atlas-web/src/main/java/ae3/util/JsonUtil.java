package ae3.util;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;

/**
 * @author Olga Melnichuk
 *         Date: 16/02/2011
 */
public class JsonUtil {

    private static final Logger log = LoggerFactory.getLogger(JsonUtil.class);

    public static String toJson(Object obj) {
        try {
            StringWriter out = new StringWriter();
            ObjectMapper mapper = new ObjectMapper();
            mapper.writeValue(out, obj);
            return out.toString();
        } catch (IOException e) {
            log.error("JSON serialization error", e);
        }
        return "null";
    }
}
