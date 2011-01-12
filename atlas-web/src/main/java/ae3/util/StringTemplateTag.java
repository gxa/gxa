package ae3.util;

import org.antlr.stringtemplate.StringTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTagSupport;
import java.io.IOException;
import java.util.*;

/**
 * @author Olga Melnichuk
 *         Date: 05/01/2011
 */
public class StringTemplateTag extends BodyTagSupport {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String name;

    private final Map<String, Object> parameters = new HashMap<String, Object>();

    private boolean wrapping;

    private StringTemplateLoader templateLoader;

    public StringTemplateTag() {
        this(false);
    }

    public StringTemplateTag(boolean wrapping) {
        this.wrapping = wrapping;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void addParameter(String name, Object value) {
        parameters.put(name, value);
    }

    @Override
    public int doStartTag() throws JspException {
        WebApplicationContext webApplContext = WebApplicationContextUtils.
                getRequiredWebApplicationContext(pageContext.getServletContext());
        templateLoader = webApplContext.getBean("stringTemplateLoader", StringTemplateLoader.class);
        return EVAL_BODY_BUFFERED;
    }

    @Override
    public int doEndTag() throws JspException {
        try {
            writeBeginTemplate();
            writeBody();
            writeEndTemplate();
        } catch (IOException e) {
            throw new JspException(e);
        } catch (Exception e) {
            log.error("Template processing error", e);
        }
        return EVAL_PAGE;
    }

    private void writeBeginTemplate() throws IOException {
        if (wrapping) {
            writeTemplate(name + "_Begin");
        }
    }

    private void writeEndTemplate() throws IOException {
        if (wrapping) {
            writeTemplate(name + "_End");
        } else {
            writeTemplate(name);
        }
    }

    private void writeTemplate(String templateName) throws IOException {
        StringTemplate template = null;
        try {
            template = templateLoader.findTemplate(templateName);
        } catch (Exception ex) {
            log.error("Can't find template: " + templateName);
            return;
        }

        StringBuilder info = new StringBuilder().
                append(templateName).append("(");

        Map args = template.getFormalArguments();
        for (Object arg : args.keySet()) {
            String argName = (String) arg;
            Object argValue = findValue(argName);
            if (argValue == null) {
                log.error("Can't find argument value: " + templateName + "(" + argName + ")");
                return;
            } else {
                template.setAttribute(argName, argValue);
                info.append("\n").append(argName).append("=").append(argValue);
            }
        }

        String result = template.toString();
        log.debug(info.append(")=\n").append(result).toString());

        pageContext.getOut().write(result);
    }

    private void writeBody() throws IOException {
        BodyContent bc = getBodyContent();
        if (bc != null) {
            bc.writeOut(pageContext.getOut());
        }
    }

    private Object findValue(String key) {
        if (parameters.containsKey(key)) {
            return parameters.get(key);
        }
        return pageContext.findAttribute(key);
    }
}
