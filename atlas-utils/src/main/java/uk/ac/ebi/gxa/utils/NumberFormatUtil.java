package uk.ac.ebi.gxa.utils;

import java.text.DecimalFormat;

/**
 * Number format utilities
 *
 * @author rpetry
 */
public class NumberFormatUtil {

    // P-values less than 10E-10 are shown as '< 10^-10'
    private static final Float MIN_REPORTED_VALUE = 1E-10f;
    private static final String TEN = "10";
    private static final String MULTIPLY_HTML_CODE = " &#0215; ";
    private static final String E_PATTERN = "#.##E0";
    private static final String E = "E";
    private static final String SUP_PRE = "<span style=\"vertical-align: super;\">";
    private static final String SUP_POST = "</span>";
    private static final String NOBR_START = "<nobr>";
    private static final String NOBR_END = "</nobr>";

    private static PrettyFormat prettyFormat = new PrettyFormat();

    /**
     * @param number P-value
     * @return number converted to String according to the following rules:
     *         1. Return fValue in format eg 3.4e-12 as 3.4 * 10^-12
     *         2. Numbers less than 1e-10 are returned as '< 10^-10'
     *         3.3. Number with exponent >= -3 and <= 0 is left as is, i.e. not converted to mantissa * 10^exponent format
     *         This function replicates in .jsp world what jquery.flot.atlas.js.prettyFloatFormat() method
     *         provides in .js world
     * @throws NullPointerException if the given number is null
     */
    public static String prettyFloatFormat(Number number) {
        if (number == null) {
            throw new NullPointerException();
        }
        return prettyFormat.format(number.doubleValue());
    }

    private static class PrettyFormat {

        String format(double number) {
            String formattedValue =
                    (number < MIN_REPORTED_VALUE) ?
                            "&lt;" + formatNumber(MIN_REPORTED_VALUE) : formatNumber(number);

            return new StringBuilder()
                    .append(NOBR_START)
                    .append(formattedValue)
                    .append(NOBR_END)
                    .toString();
        }

        /**
         * Formats double value into html string.
         * <p/>
         * If mantissa of a value is smaller than -3 the format is:
         * _mantissa_ * 10 <span style="vertical-align: super;">_exponent_</span>
         * <p/>
         * otherwise: new DecimalFormat("#.###") is used.
         *
         * @param number a double value to format
         * @return a formatted number
         */
        String formatNumber(double number) {
            if (Double.isNaN(number))
                return "N/A";

            DecimalFormat df = new DecimalFormat(E_PATTERN);
            // Examples values of auxFormat: 6.2E-3, 0E0
            String auxFormat = df.format(number);

            // We now convert this format to 6.2*10^-3 (and 0 in the case of 0E0 specifically)
            String[] formatParts = auxFormat.split(E);
            String mantissa = formatParts[0]; // in 6.2E-3, mantissa = 6.2
            int exponent = Integer.parseInt(formatParts[1]); // in 6.2E-3, exponent= -3
            if (exponent >= -3 && exponent <= 0) {
                return new DecimalFormat("#.###").format(number);
            }

            return new StringBuilder()
                    .append(mantissa)
                    .append(MULTIPLY_HTML_CODE)
                    .append(TEN)
                    .append(SUP_PRE)
                    .append(exponent)
                    .append(SUP_POST).toString();
        }
    }

}
