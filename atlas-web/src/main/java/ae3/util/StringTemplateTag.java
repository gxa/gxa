package ae3.util;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.AtlasPropertiesListener;

import javax.servlet.ServletContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Olga Melnichuk
 *         Date: 05/01/2011
 */
public class StringTemplateTag extends BodyTagSupport {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String name;

    private final Map<String, Object> parameters = new HashMap<String, Object>();

    public void setName(String name) {
        this.name = name;
    }

    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    @Override
    public int doStartTag() throws JspException {
        parameters.clear();
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        String content = "";

        try {
            StringTemplate template = findTemplate(name);

            if (template != null) {
                StringBuilder info = new StringBuilder().
                        append(name).append("(");

                Map args = template.getFormalArguments();

                if (args != null) {
                    for (Object arg : args.keySet()) {
                        String argName = (String) arg;
                        Object argValue = findValue(argName);
                        if (argValue != null) {
                            template.setAttribute(argName, argValue);
                            info.append("\n").append(argName).append("=").append(argValue);
                        } else {
                            logError("Value for argument \"" + argName + "\" not found");
                            break;
                        }
                    }
                }
                content = template.toString();
                log.debug(info.append(")=\n").append(content).toString());
            } else {
                logError("Template not found");
            }

        } catch (Exception ex) {
            logError("Template usage error", ex);
        }

        try {
            JspWriter out = pageContext.getOut();
            out.write(content);
        } catch (IOException e) {
            throw new JspException(e.getMessage());
        }

        return EVAL_PAGE;
    }

    private StringTemplate findTemplate(String name) {
        WebApplicationContext webApplContext = WebApplicationContextUtils.
                getRequiredWebApplicationContext(pageContext.getServletContext());
        return webApplContext.getBean("stringTemplateLoader", StringTemplateLoader.class).findTemplate(name);
    }

    private void logError(String errorMessage) {
        logError(errorMessage, null);
    }

    private void logError(String errorMessage, Exception ex) {
        errorMessage = "Template [" + name + "] - " + errorMessage;
        if (ex == null) {
            log.error(errorMessage);
        } else {
            log.error(errorMessage, ex);
        }
    }

    private Object findValue(String argName) {
        if (parameters.containsKey(argName)) {
            return parameters.get(argName);
        }
        return pageContext.findAttribute(argName);
    }
}
