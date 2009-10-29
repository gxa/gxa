package uk.ac.ebi.gxa.naming.factory;

import javax.naming.*;
import javax.naming.spi.ObjectFactory;
import java.io.File;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * An {@link ObjectFactory} that constructs {@link File} objects and makes them available as JNDI Resources.  The
 * resource must be accessible on the local file system for this to work.  This uses native Java fie bindings, not other
 * methods for remote directory lookups such as LDAP.  This will work with NAS mounts or other systems, as long as the
 * file appears to be physically located on the same filesystem as the JVM.
 *
 * @author Tony Burdett
 * @date 29-Oct-2009
 */
public class FileFactory implements ObjectFactory {
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        File file = null;

        Reference reference = (Reference) o;
        Enumeration<RefAddr> refAddrs = reference.getAll();
        while (refAddrs.hasMoreElements()) {
            RefAddr refAddr = refAddrs.nextElement();

            // the propety to read - should be "path"
            String typeName = refAddr.getType();
            // the value of this property - should be the file's location
            Object content = refAddr.getContent();

            if (typeName.equals("path")) {
                file = new File(content.toString());
            }
        }

        // check file isn't null - if it is, our declaration is bad
        if (file == null) {
            throw new NamingException("Bad declaration - the file 'path' must be specified");
        }

        return file;
    }
}
