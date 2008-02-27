package ae3.magetab;

import org.apache.commons.lang.StringUtils;

import java.util.*;

/**
 * User: ostolop
 * Date: 18-Feb-2008
 * <p/>
 * EBI Microarray Informatics Team (c) 2007
 */
public class MageTabDocument {
    private final HashMap<String, List<String>> fields = new HashMap<String,List<String>>();
    private final HashMap<String,Integer> allFieldValues = new HashMap<String,Integer>();

    public void setField(final String fieldName, final List<String> fieldValues) {
        fields.put(fieldName.replaceAll("\"",""), fieldValues);
    }

    public List<String> getFieldValues(final String fieldName) {
        return fields.get(fieldName);
    }

    public Map<String,List<String>> getFields() {
        return fields;
    }

    public String getAllFieldValuesAsString() {
        return StringUtils.join(allFieldValues.keySet(), ' ');
    }

    public void addToAllFieldValues(String[] values) {
        for(String val : values)
        allFieldValues.put(val, 0);
    }

    public void addToField(String field, String value) {
        List<String> fieldValues = fields.get(field);
        if (fieldValues == null) {
            fieldValues = new Vector<String>();
            fields.put(field, fieldValues);
        }

        fieldValues.add(value);
    }
}
