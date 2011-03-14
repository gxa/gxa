package uk.ac.ebi.gxa.naming.factory;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import java.net.URI;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 3/14/11
 * Time: 9:09 AM
 * An {@link ObjectFactory} that constructs {@link URI} objects and makes them available as JNDI Resources.
 */
public class URIFactory implements ObjectFactory {
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        URI uri = null;

        Reference reference = (Reference) o;
        Enumeration<RefAddr> refAddrs = reference.getAll();
        while (refAddrs.hasMoreElements()) {
            RefAddr refAddr = refAddrs.nextElement();

            // the propety to read - should be "path"
            String typeName = refAddr.getType();
            // the value of this property - should be the URI String
            Object content = refAddr.getContent();

            if (typeName.equals("path")) {
                uri = new URI(content.toString());
            }
        }

        // check file isn't null - if it is, our declaration is bad
        if (uri == null) {
            throw new NamingException("Bad declaration - the URI 'path' must be specified");
        }

        return uri;
    }
}
