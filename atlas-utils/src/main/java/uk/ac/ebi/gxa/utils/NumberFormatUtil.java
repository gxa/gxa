package uk.ac.ebi.gxa.utils;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Number format utilities
 *
 * @author rpetry
 */
public class NumberFormatUtil {

    private static final String E = "E";
    private static final String ZERO = "0";
    private static final String E_REPLACEMENT = " * 10";
    private static final String pattern = "#.##" + E + ZERO;
    private static final List<String> ZERO_LIST = new ArrayList<String>(1);
    static {
        ZERO_LIST.add(ZERO);
    }

    private static final String SUP_PRE = "<span style=\"vertical-align: super;\">";
    private static final String SUP_POST = "</SPAN>";


    /**
     * @param number
     * @return number, formatted as 'significant digits ? 10<sup>exponent</sup>' (and '0' when significant digits == 0)
     */
    public static String prettyFloatFormat(Float number) {
        DecimalFormat df = new DecimalFormat(pattern);
        String auxFormat = df.format((double) number);
        // Examples values of auxFormat: 6.2E-3, 0E0
        // We now convert this format to 6.2*10<sup>-3</sup> (and 0 in the case of 0E0 specifically)
        List<String> formatParts = new ArrayList<String>(Arrays.asList(auxFormat.split(E)));
        if (formatParts.removeAll(ZERO_LIST) && formatParts.size() == 0) { // if the auxFormat = '0E0'
            return ZERO;
        }
        return formatParts.get(0) + E_REPLACEMENT + SUP_PRE + formatParts.get(1) + SUP_POST;
    }
}
