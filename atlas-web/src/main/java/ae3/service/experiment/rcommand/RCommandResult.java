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
        return ret((RInteger) getValue(valueName));
    }

    public double[] getNumericValues(String valueName) {
        return ret((RNumeric) getValue(valueName));
    }

    public String[] getStringValues(String valueName) {
        return ret((RFactor) getValue(valueName));
    }

    public int[] getIntAttribute(String attrName) {
        return ret((RInteger)getAttribute(attrName));
    }

    private RObject getValue(String valueName) {
        return dataFrame == null ? null : dataFrame.getData().getValueByName(valueName);
    }

    private RObject getAttribute(String attrName) {
        return dataFrame == null ? null : dataFrame.getAttributes().getValueByName(attrName);
    }

    private int[] ret(RInteger val) {
        return val == null ? new int[0] : val.getValue();
    }

    private double[] ret(RNumeric val) {
        return val == null ? new double[0] : val.getValue();
    }

    private String[] ret(RFactor val) {
        return val == null ? new String[0] : val.asData();
    }

    public boolean isEmpty() {
        return dataFrame == null;
    }
}
