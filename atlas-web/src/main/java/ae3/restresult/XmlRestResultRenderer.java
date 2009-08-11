package ae3.restresult;

import com.jamesmurty.utils.XMLBuilder;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Properties;
import java.util.Iterator;
import java.lang.annotation.Annotation;

/**
 * @author pashky
 */
public class XmlRestResultRenderer implements RestResultRenderer {
    private XMLBuilder xml;

    private boolean indent = false;
    private int indentAmount = 4;
    private Class profile;

    public XmlRestResultRenderer(boolean indent, int indentAmount) {
        this.indent = indent;
        this.indentAmount = indentAmount;
    }


    public void render(Object object, Appendable where, final Class profile) throws RenderException, IOException {
        try {
            xml = XMLBuilder.create("atlasResponse");

            this.profile = profile;
            process(object, null, null);

            Properties props = new Properties();
            if (indent) {
                props.put("indent", "yes");
                props.put("{http://xml.apache.org/xalan}indent-amount", String.valueOf(indentAmount));
            }

            where.append(xml.asString(indent, indentAmount));
        } catch (ParserConfigurationException e) {
            throw new RenderException(e);
        }
    }

    private void process(Object o, String iname, Annotation[] a) throws IOException, RenderException {
        if(o instanceof Number || o instanceof Boolean || o instanceof String || o instanceof Enum || o.getClass().isAnnotationPresent(AsString.class)) {
            xml = xml.t(o.toString());
        } else if(o instanceof Iterable || o instanceof Iterator) {
            processArray(o, iname, a);
        } else {
            processMap(o, iname, a);
        }
    }

    private void processMap(Object o, String iname, Annotation[] as) throws IOException, RenderException {
        AsMap am = null;
        AsObject ao = null;
        if(as != null)
            for(Annotation ia : as)
                if(ia instanceof AsMap) {
                    am = (AsMap)ia;
                    break;
                } else if(ia instanceof AsObject) {
                    ao = (AsObject)ia;
                    break;
                }

        if(am == null && ao == null) {
            am = o.getClass().getAnnotation(AsMap.class);
            ao = o.getClass().getAnnotation(AsObject.class);
        }

        String itemName = null;
        if(am != null) {
            if(am.item().length() > 0)
                itemName = am.item();
            else if(iname != null && iname.length() > 1 && iname.endsWith("s"))
                itemName = iname.substring(0, iname.length() - 1);
            else if(iname == null && am.anonName().length() > 1 && am.anonName().endsWith("s"))
                itemName = am.anonName().substring(0, am.anonName().length() - 1);
            else
                itemName = "item";
        }

        boolean wrapped = true;
        if(iname == null && am != null && am.anonName().length() > 0)
            xml = xml.e(am.anonName());
        else if(iname == null && ao != null && ao.anonName().length() > 0)
            xml = xml.e(ao.anonName());
        else
            wrapped = false;

        for(Util.Prop p : Util.iterableProperties(o, profile)) {
            if(p.value == null)
                continue;
            
            if(am != null)
                xml = xml.e(itemName).a(am.attr(), p.name);
            else
                xml = xml.e(p.name);

            if(p.method != null && p.method.isAnnotationPresent(AsString.class)) {
                xml = xml.t(p.value.toString());
            } else {
                process(p.value, p.name, p.method != null ? p.method.getDeclaredAnnotations() : null);
            }

            xml = xml.up();
        }

        if(wrapped)
            xml = xml.up();
    }

    private void processArray(Object oi, String iname, Annotation[] as) throws RenderException, IOException {
        AsArray a = null;
        if(as != null)
            for(Annotation aa : as)
                if(aa instanceof AsArray) {
                    a = (AsArray)aa;
                    break;
                }

        if(a == null)
            a = oi.getClass().getAnnotation(AsArray.class);
        
        boolean wrapped = true;
        if(iname == null && a != null && a.anonName().length() > 0)
            xml = xml.e(a.anonName());
        else
            wrapped = false;

        String itemName;
        if(a != null && a.item().length() > 0)
            itemName = a.item();
        else if(iname != null && iname.length() > 1 && iname.endsWith("s"))
            itemName = iname.substring(0, iname.length() - 1);
        else if(iname == null && a != null && a.anonName().length() > 1 && a.anonName().endsWith("s"))
            itemName = a.anonName().substring(0, a.anonName().length() - 1);
        else
            itemName = "item";

        Iterator i = oi instanceof Iterator ? (Iterator)oi : ((Iterable)oi).iterator();
        while(i.hasNext()) {
            Object object = i.next();
            xml = xml.e(itemName);
            if(object != null)
                process(object, iname, null);
            xml = xml.up();
        }

        if(wrapped)
            xml = xml.up();
    }

}
