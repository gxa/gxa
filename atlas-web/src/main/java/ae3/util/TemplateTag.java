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
import java.util.regex.*;
import javax.servlet.ServletContext;
import javax.servlet.jsp.tagext.*;

import uk.ac.ebi.gxa.properties.AtlasProperties;

/**
 * JSP tag that includes our own HTML templates
 *
 * @author geometer
 */
public class TemplateTag extends TagSupport {
    private final Pattern varPattern = Pattern.compile("\\$\\{[^\\}]+\\}");

	private String fileName;

	public String getFile() {
		return fileName;
	}

	public void setFile(String fileName) {
		this.fileName = fileName;
	}

    public int doStartTag() throws javax.servlet.jsp.JspException {
		try {
			String text = compileTemplate(textFromFile(fileName));
			if (text != null) {
        		pageContext.getOut().print(text);
			}
		} catch (IOException e) {
		}
        return Tag.SKIP_BODY;
    }

	private String getDirectoryPathPrefix() {
		ServletContext context = pageContext.getServletContext();
    	AtlasProperties props = (AtlasProperties)pageContext.getServletContext().getAttribute("atlasProperties");
		return props.getConfigurationDirectoryPath();
	}

    private CharSequence evaluate(String variable) {
        String[] names = variable.split("\\.");
		if (names.length == 0) {
			return "NULL";
		}
		ServletContext context = pageContext.getServletContext();
		Object bean = context.getAttribute(names[0]);
		if (bean == null) {
			return "NULL";
		}
		try {
			for (int i = 1; i < names.length; ++i) {
				String methodName = "get" + names[i].substring(0, 1).toUpperCase() + names[i].substring(1);
				Method method = bean.getClass().getDeclaredMethod(methodName);
				bean = method.invoke(bean);
			}
			return bean.toString();
		} catch (Exception e) {
			return "NULL";
		}
    }

    private String compileTemplate(String text) {
		if (text == null) {
			return null;
		}

        Matcher m = varPattern.matcher(text);
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

    private String textFromFile(String name) {
        try {
            InputStream stream = null;
			try {
                stream = new FileInputStream(getDirectoryPathPrefix() + '/' + fileName);
			} catch (IOException e) {
			}
			if (stream == null) {
                stream = getClass().getClassLoader().getResourceAsStream(fileName);
            }
            if (stream == null) {
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
        }
        return null;
    }
}
