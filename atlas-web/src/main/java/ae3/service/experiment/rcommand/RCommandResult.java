package ae3.service.experiment.rcommand;

import uk.ac.ebi.rcloud.server.RType.*;

/**
 * @author Olga Melnichuk
 *         Date: 25/03/2011
 */
public class RCommandResult {

    private final RDataFrame dataFrame;

    public RCommandResult(RDataFrame dataFrame) {
        this.dataFrame = dataFrame;
    }

    public int[] getIntValues(String valueName) {
        Object obj = ret(getValue(valueName));
        return obj == null ? new int[0] : (int[]) obj;
    }

    public double[] getNumericValues(String valueName) {
        Object obj = ret(getValue(valueName));
        return obj == null ? new double[0] : (double[]) obj;
    }

    public String[] getStringValues(String valueName) {
        Object obj = ret(getValue(valueName));
        return obj == null ? new String[0] : (String[]) obj;
    }

    public int[] getIntAttribute(String attrName) {
        Object obj = ret(getAttribute(attrName));
        return obj == null ? new int[0] : ((int[]) obj);
    }

    private RObject getValue(String valueName) {
        return dataFrame == null ? null : dataFrame.getData().getValueByName(valueName);
    }

    private RObject getAttribute(String attrName) {
        return dataFrame == null ? null : dataFrame.getAttributes().getValueByName(attrName);
    }

    private Object ret(Object obj) {
        if (obj == null || !(obj instanceof RObject)) {
            return obj;
        }

        if (obj instanceof RNumeric) {
            return ret(((RNumeric) obj).getValue());
        }
        if (obj instanceof RInteger) {
            return ret(((RInteger) obj).getValue());
        }
        if (obj instanceof RFactor) {
            return ret(((RFactor) obj).asData());
        }
        if (obj instanceof RChar) {
            return ret(((RChar) obj).getValue());
        }
        if (obj instanceof RArray) {
            return ret(((RArray) obj).getValue());
        }
        throw new IllegalArgumentException("Unsupported R obj type: " + obj.getClass());
    }

    public boolean isEmpty() {
        return dataFrame == null;
    }
}
