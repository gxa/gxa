package ae3.util;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.BodyTagSupport;
import javax.servlet.jsp.tagext.Tag;

/**
 * @author Olga Melnichuk
 *         Date: 06/01/2011
 */
public class StringTemplateParamTag extends BodyTagSupport {

    private String name;

    private Object value;

    public void setName(String name) {
        this.name = name;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public int doEndTag() throws JspException {
        Tag t = findAncestorWithClass(this, StringTemplateTag.class);
        if (t == null) {
            throw new JspTagException("Param tag is outside of template tag");
        }
        StringTemplateTag parent = (StringTemplateTag) t;
        parent.addParameter(name, value);
        return EVAL_PAGE;
    }
}

