package uk.ac.ebi.gxa.jmx;

import org.springframework.jmx.export.naming.ObjectNamingStrategy;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.BeansException;

import javax.management.ObjectName;
import javax.management.MalformedObjectNameException;
import java.util.Hashtable;

/**
 * @author pashky
 */
public class PerInstanceNamingStrategy implements ObjectNamingStrategy, ApplicationContextAware {
    private String domain;
    private String contextId;

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        // fixme: any ideas on what to use as unqiue instance id?
        contextId = Integer.toHexString(applicationContext.hashCode()) + Integer.toHexString(this.hashCode());
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public ObjectName getObjectName(Object o, String s) throws MalformedObjectNameException {
        Hashtable<String,String> table = new Hashtable<String, String>();
        table.put("name", s);
        table.put("instance", contextId);
        return ObjectName.getInstance(domain, table);
    }
}
