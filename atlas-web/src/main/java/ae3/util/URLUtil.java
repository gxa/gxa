package ae3.util;

import javax.servlet.http.HttpServletRequest;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Jul 6, 2009
 * Time: 11:39:15 AM
 * To change this template use File | Settings | File Templates.
 */
public class URLUtil {
    public static String getGeneURL(HttpServletRequest request, String GeneId)
    {
        //return "lol.txt";
        return(request.getContextPath()+"/gene/"+GeneId);
    }

    public static String getDasUrl(HttpServletRequest request)
    {
       return "http://" + AtlasProperties.getProperty("atlas.host") + request.getContextPath() + "/das/" + AtlasProperties.getProperty("atlas.dasdsn");
    }
}

