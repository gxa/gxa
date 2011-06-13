/*
 * Copyright 2008-2011 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.web.controller;

import javax.servlet.http.HttpServletResponse;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.io.ByteStreams.copy;
import static com.google.common.io.Closeables.closeQuietly;

/**
 * Created by IntelliJ IDEA.
 * User: rpetry
 * Date: 6/2/11
 * Time: 3:31 PM
 * This class stores an enumeration of valid resource mime types and their corresponding file extensions.
 * Its handle() method returns the requested experiment asset provided that its mime type matches one of the
 * mime types enumerated in this class.
 */
public enum ResourcePattern {
    CSS("text/css", "css"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    JPG("image/jpeg", "jpg");

    private String contentType;
    private Pattern pattern;

    private ResourcePattern(String contentType, String extension) {
        this.contentType = contentType;
        this.pattern = Pattern.compile("(:?[^\\.]+)\\." + extension);
    }

    public boolean handle(File dir, String resourceName, HttpServletResponse response) throws ResourceNotFoundException, IOException {
        Matcher m = pattern.matcher(resourceName);
        if (!m.matches()) {
            return false;
        }

        File f = new File(dir, resourceName);
        if (!f.exists()) {
            throw new ResourceNotFoundException("Resource doesn't exist: " + f.getAbsolutePath());
        }

        BufferedInputStream in = null;
        try {
            response.setContentType(contentType);
            in = new BufferedInputStream(new FileInputStream(f));
            copy(in, response.getOutputStream());
            response.getOutputStream().flush();
        } finally {
            closeQuietly(in);
        }

        return true;
    }
}
