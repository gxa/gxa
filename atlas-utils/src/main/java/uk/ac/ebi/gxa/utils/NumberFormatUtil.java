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

    // P-values less than 10E-10 are shown as '< 10^-10'
    private static final Float MIN_REPORTED_VALUE = 10E-10f;
    private static final String MIN_EXPONENT = "-10";
    private static final String LESS_THAN = "< ";
    private static final String TEN = "10";
    private static final String MULTIPLY_HTML_CODE = " &#0215; ";
    private static final String E_PATTERN = "#.##E0";
    private static final String E = "E";
    private static final String SUP_PRE = "<span style=\"vertical-align: super;\">";
    private static final String SUP_POST = "</span>";
    private static final String NOBR_START = "<nobr>";
    private static final String NOBR_END = "</nobr>";
    private static final String PLUS = "+";


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
        String exponent = formatParts.get(1); // // in 6.2E-3, exponent= -3
         // Don't show '+' in non-negative exponents, '10^+2' should be shown as '10^2'
        if (exponent.startsWith(PLUS)) {
            exponent = exponent.substring(1);
        }

        String pre = mantissa + MULTIPLY_HTML_CODE; // e.g '6.2 * '
        if (number < MIN_REPORTED_VALUE) {
            // if number < 10E-10 forget its mantissa and show it simply as '< 10^-10'
            pre = LESS_THAN;
            exponent = MIN_EXPONENT;
        }
        return NOBR_START + pre + TEN + SUP_PRE + exponent + SUP_POST + NOBR_END;
    }
}
