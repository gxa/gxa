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

    // P-values with exponents less than MIN_EXPONENT are shown as '< 10^-10'
    private static final Integer MIN_EXPONENT = -10;
    private static final String LESS_THAN = "< ";
    private static final String E = "E";
    private static final String ZERO = "0";
    private static final String TEN = "10";
    private static final String MULTIPLY_HTML_CODE = " &#0215 ";
    private static final String E_PATTERN = "#.##" + E + ZERO;
    private static final String SUP_PRE = "<span style=\"vertical-align: super;\">";
    private static final String SUP_POST = "</span>";


    /**
     * @param number P-value
     * @return number, formatted as 'mantissa ? 10^exponent' (and '0' when mantissa == 0)
     *         N.B. P-values with exponents less than MIN_EXPONENT are shown as '< 10^-10
     */
    public static String prettyFloatFormat(Float number) {
        DecimalFormat df = new DecimalFormat(E_PATTERN);
        // Examples values of auxFormat: 6.2E-3, 0E0
        String auxFormat = df.format((double) number);

        // We now convert this format to 6.2*10^-3 (and 0 in the case of 0E0 specifically)
        List<String> formatParts = new ArrayList<String>(Arrays.asList(auxFormat.split(E)));
        String mantissa = formatParts.get(0); // in 6.2E-3, mantissa = 6.2
        Integer exponent = Integer.parseInt(formatParts.get(1)); // // in 6.2E-3, exponent= -3

        String pre = mantissa + MULTIPLY_HTML_CODE; // e.g 6.2 * 10
        if ((mantissa.equals(ZERO) && exponent == 0) || (exponent < MIN_EXPONENT)) {
            // if number < 10E-10 (including a round 0E0) forget its mantissa and show it simply as '< 10^-10'
            pre = LESS_THAN;
            exponent = MIN_EXPONENT;
        }
        return pre + TEN + SUP_PRE + exponent + SUP_POST;
    }
}
