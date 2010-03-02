/*
 * Copyright 2008-2010 Microarray Informatics Team, EMBL-European Bioinformatics Institute
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * For further details of the Gene Expression Atlas project, including source code,
 * downloads and documentation, please see:
 *
 * http://gxa.github.com/gxa
 */

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
