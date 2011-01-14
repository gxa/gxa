package ae3.util;

import org.antlr.stringtemplate.StringTemplate;
import org.antlr.stringtemplate.StringTemplateErrorListener;
import org.antlr.stringtemplate.StringTemplateGroup;
import org.antlr.stringtemplate.language.DefaultTemplateLexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import uk.ac.ebi.gxa.properties.AtlasProperties;
import uk.ac.ebi.gxa.properties.AtlasPropertiesListener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Olga Melnichuk
 *         Date: 10/01/2011
 */
public class StringTemplateLoader implements AtlasPropertiesListener, StringTemplateErrorListener {

    private static final Pattern CLASSPATH = Pattern.compile("^classpath\\:(.+)");

    private final Logger log = LoggerFactory.getLogger(getClass());

    private String path2Templates;

    private boolean isCacheEnabled;

    private StringTemplateGroup cachedGroup;

    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    private final Lock read = readWriteLock.readLock();

    private final Lock write = readWriteLock.writeLock();

    @Autowired
    public StringTemplateLoader(AtlasProperties atlasProperties) {
        atlasProperties.registerListener(this);
        load(atlasProperties);
    }

    private void load(AtlasProperties props) {
        load(props.getLafTemplatesPath(), props.isLafCacheEnabled());
    }

    private void load(String path2Templates, boolean isCacheEnabled) {
        write.lock();
        try {
            if ((path2Templates != null && !path2Templates.equals(this.path2Templates)) ||
                    (isCacheEnabled != this.isCacheEnabled)) {

                this.path2Templates = path2Templates;
                this.isCacheEnabled = isCacheEnabled;
                log.info("StringTemplateLoader updated: path=" + path2Templates + ", isCacheEnabled=" + isCacheEnabled);

                cachedGroup = loadGroup();
            }
        } finally {
            write.unlock();
        }
    }

    private InputStream openStream(String filePath) {
        Matcher m = CLASSPATH.matcher(filePath);

        if (m.matches()) {
            filePath = m.group(1);
            InputStream in = getClass().getClassLoader().getResourceAsStream(filePath);
            if (in == null) {
                log.error("Can't find templates in classpath: " + filePath);
            }
            return in;
        }

        try {
            return new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            log.error("Can't find templates in path: " + filePath, e);
        }
        return null;
    }

    private StringTemplateGroup loadGroup() {
        InputStream in = openStream(path2Templates);
        if (in != null) {
            return new StringTemplateGroup(new InputStreamReader(in), DefaultTemplateLexer.class, this);
        }
        return null;
    }

    public StringTemplate findTemplate(String templateName) {
        read.lock();
        try {
            StringTemplateGroup group = isCacheEnabled ? cachedGroup : loadGroup();
            return group == null ? null : group.getInstanceOf(templateName);
        } finally {
            read.unlock();
        }
    }

    public void onAtlasPropertiesUpdate(AtlasProperties atlasProperties) {
        load(atlasProperties);
    }

    public void error(String msg, Throwable e) {
        log.error(msg, e);
    }

    public void warning(String msg) {
        log.warn(msg);
    }
}



