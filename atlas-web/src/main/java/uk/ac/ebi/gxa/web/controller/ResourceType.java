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

import java.util.regex.Pattern;

/**
 * This class stores an enumeration of valid resource mime types and their corresponding file extensions.
 * Its handle() method returns the requested experiment asset provided that its mime type matches one of the
 * mime types enumerated in this class.
 */
public enum ResourceType {
    CSS("text/css", "css"),
    PNG("image/png", "png"),
    GIF("image/gif", "gif"),
    JPG("image/jpeg", "jpg");

    private final String contentType;
    private final Pattern pattern;

    private ResourceType(String contentType, String extension) {
        this.contentType = contentType;
        this.pattern = Pattern.compile("[^\\.]+\\." + extension);
    }

    public String getContentType() {
        return contentType;
    }

    public static ResourceType getByFileName(String filename) {
        for (ResourceType type : values()) {
            if (type.pattern.matcher(filename).matches()) {
                return type;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "ResourceType{" + contentType + '}';
    }
}
