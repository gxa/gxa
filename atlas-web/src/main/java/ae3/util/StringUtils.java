package ae3.util;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Sep 14, 2009
 * Time: 10:38:27 AM
 * To change this template use File | Settings | File Templates.
 */
public class StringUtils {
    public static String quoteComma(String value){
        if(value.contains(","))
            return "\"" + value + "\"";
        else
            return value;
    }

    public static String pluralize(String value){
        if(!value.endsWith("s"))
            return value+"s";
        else
            return value;
    }

    public static String decapitalise(String value){

        if(value.length()>1)
            if(!Character.isUpperCase(value.charAt(1)))
                value = value.toLowerCase();

        return value;
    }

    public static String ReplaceLast(String value, String OldValue, String NewValue){
        if(value.endsWith(OldValue))
            return value.substring(0,value.lastIndexOf(OldValue)) + NewValue;
        else
            return value;
    }
}
