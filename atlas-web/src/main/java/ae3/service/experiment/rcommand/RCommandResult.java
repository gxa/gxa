package ae3.service.experiment.rcommand;

import uk.ac.ebi.rcloud.server.RType.*;

/**
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
        return dataFrame == null ? null : dataFrame.getData().getValueByName(valueName);
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
        return dataFrame == null;
    }
}
