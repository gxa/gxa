package uk.ac.ebi.gxa.utils;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
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

    private static PValueFormat pvalueFormat = new PValueFormat();
    private static TValueFormat tvalueFormat = new TValueFormat();

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
    public static String formatPValue(Float number) {
        return pvalueFormat.format(number);
    }

    /**
     * Rounds the number for two significant digits and returns result as HTML String
     *
     * @param number T-value
     * @return a rounded T-value as HTML String
     */
    public static String formatTValue(Float number) {
        return tvalueFormat.format(number);
    }

    private static abstract class PrettyFloatFormat {

        String format(Float number) {
            if (number == null) {
                throw new NullPointerException();
            }

            double doubleValue = number.doubleValue();

            return new StringBuilder()
                    .append(NOBR_START)
                    .append((Double.isNaN(doubleValue) ? "N/A" : format(doubleValue)))
                    .append(NOBR_END)
                    .toString();
        }

        abstract String format(double number);
    }

    private static class TValueFormat extends PrettyFloatFormat {

        @Override
        String format(double number) {
            double abs = Math.abs(number);

            if (abs < 10) {
                abs = abs < 1e-10 ? 0.0 : abs;

                double sign = number < 0 ? -1.0 : 1.0;
                double scale = Math.pow(10, Math.floor(Math.log10(abs)) - 1);
                double rounded = sign *
                        new BigDecimal(scale * Math.round(abs / scale))
                                .round(new MathContext(2, RoundingMode.HALF_EVEN))
                                .doubleValue();
                if (Math.abs(rounded - Math.round(rounded)) > 0) {
                    return Double.toString(rounded);
                }
                number = rounded;
            }
            return new DecimalFormat("#").format(Math.round(number));
        }
    }

    private static class PValueFormat extends PrettyFloatFormat {

        @Override
        String format(double number) {
            return (number < MIN_REPORTED_VALUE) ?
                    "&lt;" + formatNumber(MIN_REPORTED_VALUE) : formatNumber(number);

        }

        String formatNumber(double number) {

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
