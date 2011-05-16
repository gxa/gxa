package ae3.service.experiment.rcommand;

import uk.ac.ebi.rcloud.server.RType.*;

/**
 * RDataFrame wrapper class to easily access variables and attributes.
 * <p/>
 * If the data frame is empty, all values considered to be empty (not null!), even not existed one.
 * Otherwise an exception (ClassCastException or IllegalArgumentException) is thrown if the type or variable name
 * specified incorrectly.
 * <p/>
 * Attribute values considered to be empty for undefined attributes. R doesn't add empty attributes
 * (NULL, c(), integer(0) etc.) to the frame.
 *
 * @author Olga Melnichuk
 */
public class RCommandResult {
    private final RDataFrame dataFrame;

    public RCommandResult(RDataFrame dataFrame) {
        this.dataFrame = dataFrame;
    }

    public int[] getIntValues(String valueName) {
        Object obj = toJava(getValue(valueName));
        return obj == null ? new int[0] : (int[]) obj;
    }

    public double[] getNumericValues(String valueName) {
        Object obj = toJava(getValue(valueName));
        return obj == null ? new double[0] : (double[]) obj;
    }

    public String[] getStringValues(String valueName) {
        Object obj = toJava(getValue(valueName));
        return obj == null ? new String[0] : (String[]) obj;
    }

    public int[] getIntAttribute(String attrName) {
        Object obj = toJava(getAttribute(attrName));
        return obj == null ? new int[0] : ((int[]) obj);
    }

    private RObject getValue(String valueName) {
        if (isEmpty()) {
            return null;
        }

        for (String name : dataFrame.getData().getNames()) {
            if (name.equals(valueName)) {
                return dataFrame.getData().getValueByName(valueName);
            }
        }

        throw new IllegalArgumentException("No such variable name found: " + valueName);
    }

    private RObject getAttribute(String attrName) {
        return dataFrame == null ? null : dataFrame.getAttributes().getValueByName(attrName);
    }

    private Object toJava(Object rObject) {
        if (!(rObject instanceof RObject)) {
            return rObject;
        }
        if (rObject instanceof RArray) {
            final RArray array = (RArray) rObject;
            return toJava(array.getValue());
        }
        if (rObject instanceof RNumeric) {
            final RNumeric numeric = (RNumeric) rObject;
            return numeric.getValue();
        }
        if (rObject instanceof RInteger) {
            final RInteger integer = (RInteger) rObject;
            return integer.getValue();
        }
        if (rObject instanceof RFactor) {
            final RFactor factor = (RFactor) rObject;
            return factor.asData();
        }
        if (rObject instanceof RChar) {
            final RChar rChar = (RChar) rObject;
            return rChar.getValue();
        }
        throw new IllegalArgumentException("Unsupported R obj type: " + rObject.getClass());
    }

    public boolean isEmpty() {
        return dataFrame == null || dataFrame.getRowNames().length == 0;
    }
}
