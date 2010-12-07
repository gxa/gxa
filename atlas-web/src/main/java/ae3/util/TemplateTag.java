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

package ae3.util;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import javax.servlet.*;
import javax.servlet.jsp.tagext.*;
import org.slf4j.*;

import uk.ac.ebi.gxa.properties.*;

/**
 * JSP tag that includes our own HTML templates
 *
 * @author geometer
 */
public class TemplateTag extends TagSupport {
    private static class Cache implements AtlasPropertiesListener {
        private final HashMap<String,String> map = new HashMap<String,String>();
        private String directoryPath;
        private boolean isCacheEnabled;

        Cache(AtlasProperties atlasProperties) {
            atlasProperties.registerListener(this);
            init(atlasProperties);
        }

        private synchronized void init(AtlasProperties atlasProperties) {
            directoryPath = atlasProperties.getConfigurationDirectoryPath();
            isCacheEnabled = atlasProperties.isLookCacheEnabled();
            map.clear();
        }

        public void onAtlasPropertiesUpdate(AtlasProperties atlasProperties) {
            if (directoryPath == null ||
                !directoryPath.equals(atlasProperties.getConfigurationDirectoryPath()) ||
                isCacheEnabled != atlasProperties.isLookCacheEnabled()) {
                init(atlasProperties);
            }
        }

        public synchronized String getDirectoryPath() {
            return directoryPath;
        }

        public synchronized String getTextForFilename(String fileName) {
            return isCacheEnabled ? map.get(fileName) : null;
        }

        public synchronized void setTextForFilename(String fileName, String text) {
            if (isCacheEnabled) {
                map.put(fileName, text);
            }
        }
    }

    private Cache cache() {
        final String KEY = "HTML_TEMPLATE_CACHE";
        ServletContext context = pageContext.getServletContext();
        Cache c = (Cache)context.getAttribute(KEY);
        if (c == null) {
            synchronized (context) {
                c = (Cache)context.getAttribute(KEY);
                if (c == null) {
                    c = new Cache(
                        (AtlasProperties)pageContext.getServletContext().getAttribute("atlasProperties")
                    );
                    context.setAttribute(KEY, c);
                }
            }
        }
        return c;
    }

    private static final Pattern beanPattern = Pattern.compile("#\\{[^\\}]+\\}");
    private static final Pattern varPattern = Pattern.compile("#\\([^)]+\\)");
    private static final Pattern definePattern = Pattern.compile("^\\s*#define\\s");
    private static final Pattern includePattern = Pattern.compile("^\\s*#include\\s");

    private final Logger logger = LoggerFactory.getLogger(getClass());
    private AtlasProperties atlasProperties;

    private String fileName;

    public String getFile() {
        return fileName;
    }

    public void setFile(String fileName) {
        this.fileName = fileName;
    }

    public int doStartTag() throws javax.servlet.jsp.JspException {
        try {
            Cache c = cache();
            String text = c.getTextForFilename(fileName);
            if (text == null) {
                CharSequence data = preprocessedTextFromFile(fileName);
                if (data != null) {
                    text = data.toString();
                    c.setTextForFilename(fileName, text);
                }
            }
            if (text != null) {
                pageContext.getOut().print(doCompile(text));
            }
        } catch (IOException e) {
        }
        return Tag.SKIP_BODY;
    }

    private CharSequence evaluate(String variable) {
        try {
            String[] names = variable.split("\\.");
            if (names.length == 0) {
                throw new RuntimeException("Missing variable name");
            }
            ServletContext context = pageContext.getServletContext();
            Object bean = context.getAttribute(names[0]);
            if (bean == null) {
                ServletRequest request = pageContext.getRequest();
                if (request != null) {
                    bean = request.getAttribute(names[0]);
                }
            }
            if (bean == null) {
                throw new RuntimeException("Value of " + names[0] + " is null");
            }
            for (int i = 1; i < names.length; ++i) {
                String methodName = "get" + names[i].substring(0, 1).toUpperCase() + names[i].substring(1);
                Method method = bean.getClass().getMethod(methodName);
                bean = method.invoke(bean);
                if (bean == null) {
                    StringBuilder builder = new StringBuilder();
                    builder.append("Value of ");
                    builder.append(names[0]);
                    for (int j = 1; j < i; ++j) {
                        builder.append(".");
                        builder.append(names[j]);
                    }
                    builder.append(" is null");
                    throw new RuntimeException(builder.toString());
                }
            }
            return bean.toString();
        } catch (Throwable e) {
            logger.error("Bean accessing problem (" + e.getMessage() + ")", e);
            return "NULL";
        }
    }

    private String doCompile(String text) {
        Matcher m = beanPattern.matcher(text);
        StringBuilder builder = null;
        int endIndex = 0;
        while (m.find()) {
            if (builder == null) {
                builder = new StringBuilder();
            }
            int startIndex = m.start();
            builder.append(text.substring(endIndex, startIndex));
            endIndex = m.end();
            builder.append(evaluate(text.substring(startIndex + 2, endIndex - 1)));
        }

        if (builder == null) {
            return text;
        }
        builder.append(text.substring(endIndex));
        return builder.toString();    
    }

    private String path(String baseFilePath, String relativePath) {
        // TODO: normalize path (remove /../, /./, etc.)
        return new File(new File(baseFilePath).getParent(), relativePath).getPath();
    }

    private CharSequence doPreprocess(String line, List<String> pathStack, Map<String, String> vars) {
        Matcher m = varPattern.matcher(line);
        StringBuilder builder = null;
        int endIndex = 0;
        while (m.find()) {
            if (builder == null) {
                builder = new StringBuilder();
            }
            int startIndex = m.start();
            builder.append(line.substring(endIndex, startIndex));
            endIndex = m.end();
            String name = line.substring(startIndex + 2, endIndex - 1);
            String value = vars.get(name);
            if (value == null) {
                logger.error("Variable not defined: " + name);
                value = "NULL";
            }
            builder.append(value);
        }

        if (builder != null) {
            builder.append(line.substring(endIndex));
            line = builder.toString();    
        }

        if (definePattern.matcher(line).find()) {
            String definition = definePattern.split(line)[1].trim();
            String[] array = definition.split("\\s", 2);
            if (array.length >= 2) {
                String name = array[0].trim();
                String value = array[1].trim();
                if (value.charAt(0) == '"' && value.charAt(value.length() - 1) == '"') {
                    value = value.substring(1, value.length() - 1);
                }
                if (vars.get(name) != null) {
                    logger.error("Variable redefined: " + name);
                }
                vars.put(name, value);
            }
            return "";
        }
        if (includePattern.matcher(line).find()) {
            String[] filePathArray = includePattern.split(line);
            return preprocessedTextFromFile(
                path(pathStack.get(pathStack.size() - 1), filePathArray[1].trim()), pathStack, vars
            );
        }
        return line;
    }

    private CharSequence preprocessedTextFromFile(String path) {
        return preprocessedTextFromFile(path, new ArrayList<String>(), new HashMap<String,String>());
    }

    private CharSequence preprocessedTextFromFile(
        String path,
        List<String> pathStack,
        Map<String, String> vars
    ) {
        if (pathStack.contains(path)) {
            logger.error("cycle in file inclusions for " + path);
            return null;
        }
        pathStack.add(path);

        try {
            String text = plainTextFromFile(path);
            if (text == null) {
                return null;
            }
        
            String[] lines = text.split("\r\n|\r|\n");
            StringBuilder preprocessed = new StringBuilder();
            for (String line : lines) {
                CharSequence s = doPreprocess(line, pathStack, vars);
                if (s != null) {
                    preprocessed.append(s);
                }
            }
            return preprocessed;
        } finally {
            pathStack.remove(pathStack.size() - 1);
        }
    }

    private String plainTextFromFile(String path) {
        try {
            InputStream stream = null;
            try {
                stream = new FileInputStream(cache().getDirectoryPath() + '/' + path);
            } catch (IOException e) {
            }
            if (stream == null) {
                stream = getClass().getClassLoader().getResourceAsStream(path);
            }
            if (stream == null) {
                logger.error("Cannot open file: " + path);
                return null;
            }

            InputStreamReader reader = new InputStreamReader(stream);
            StringBuilder valueBuilder = new StringBuilder();
            char[] buf = new char[8192];
            while (true) {
                int len = reader.read(buf);
                if (len <= 0) {
                    break;
                }
                valueBuilder.append(buf, 0, len);
            }
            reader.close();
            return valueBuilder.toString();
        } catch (IOException e) {
               logger.error("File reading problem: " + path);
        }
        return null;
    }
}
