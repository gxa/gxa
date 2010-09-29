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

package uk.ac.ebi.gxa.loader.steps;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * utils for data processing steps
 *
 * @author Nikolay Pultsin
 * @date Aug-2010
 */


class DataUtils {
    private static final String AE_PREFIX =
        "ftp://ftp.ebi.ac.uk/pub/databases/microarray/data/experiment/";
    private static final Pattern AE_HACK_PATTERN =
        Pattern.compile(AE_PREFIX + "(.*)/(.*)/\\1/\\2/\\2\\.(.*zip)");

    static String fixZipURL(String original) {
        Matcher m = AE_HACK_PATTERN.matcher(original);
        if (m.matches()) {
            return AE_PREFIX + m.group(1) + "/" + m.group(2) + "/" + m.group(2) + "." + m.group(3);
        }
        return original;
    }
}
