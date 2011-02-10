package uk.ac.ebi.gxa.anatomogram;

/**
 */
public class CSSUtil {
    static final String ANY_COLOR_REGEXP = "(\\#[0-9a-f]{6}|none)";

    static String replaceColor(String style, String attribute, String color) {
        if (!style.contains(attribute + ":"))
            return attribute + ":" + color + "; " + style;
        return style.replaceAll(attribute + ":" + ANY_COLOR_REGEXP, attribute + ":" + color);
    }
}
