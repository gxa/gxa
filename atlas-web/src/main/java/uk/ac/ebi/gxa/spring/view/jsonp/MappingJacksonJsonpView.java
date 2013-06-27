package uk.ac.ebi.gxa.spring.view.jsonp;

import org.springframework.web.servlet.view.json.MappingJacksonJsonView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Robert Petryszak
 *
 * Class taken from http://vivin.net/2011/07/01/implementing-jsonp-in-spring-mvc-3-0-x/
 */
public class MappingJacksonJsonpView extends MappingJacksonJsonView {

    /**
     * Default content type. Overridable as bean property.
     */
    public static final String DEFAULT_CONTENT_TYPE = "application/javascript";

    @Override
	public String getContentType() {
        return DEFAULT_CONTENT_TYPE;
    }

    /**
     * Prepares the view given the specified model, merging it with static
     * attributes and a RequestContext attribute, if necessary.
     * Delegates to renderMergedOutputModel for the actual rendering.
     * @see #renderMergedOutputModel
     */
    @Override
	public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        if("GET".equals(request.getMethod().toUpperCase())) {
            @SuppressWarnings("unchecked")
		Map<String, String[]> params = request.getParameterMap();

            if(params.containsKey("callback")) {
                response.getOutputStream().write(new String(params.get("callback")[0] + "(").getBytes());
                super.render(model, request, response);
                response.getOutputStream().write(new String(");").getBytes());
                response.setContentType("application/javascript");
            }

            else {
                super.render(model, request, response);
            }
        }

        else {
            super.render(model, request, response);
        }
    }
}