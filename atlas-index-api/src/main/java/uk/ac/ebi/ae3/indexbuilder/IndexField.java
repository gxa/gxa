package uk.ac.ebi.ae3.indexbuilder;

import java.io.UnsupportedEncodingException;

/**
 * @author pashky
 */
public class IndexField {
    public static String encode(String ef, String efv) {
        return encode(ef) + "_" + encode(efv);
    }
    
    public static String encode(String v) {
        try {
            StringBuffer r = new StringBuffer();
            for(char x : v.toCharArray())
            {
                if(Character.isJavaIdentifierPart(x))
                    r.append(x);
                else
                    for(byte b : Character.toString(x).getBytes("UTF-8"))
                        r.append("_").append(String.format("%x", b));
            }
            return r.toString();
        } catch(UnsupportedEncodingException e){
            throw new IllegalArgumentException("Unable to encode EFV in UTF-8", e);
        }
    }

    public static int nullzero(Short i)
    {
        return i == null ? 0 : i;
    }

    public static double nullzero(Float d)
    {
        return d == null ? 0.0 : d;
    }
}
