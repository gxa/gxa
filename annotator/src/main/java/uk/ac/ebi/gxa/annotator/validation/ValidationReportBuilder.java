/*
 * Copyright 2008-2012 Microarray Informatics Team, EMBL-European Bioinformatics Institute
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

package uk.ac.ebi.gxa.annotator.validation;

import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * User: nsklyar
 * Date: 01/02/2012
 */
public class ValidationReportBuilder {
    private Collection<String> messages = new ArrayList<String>();

    public void addMessage(String message) {
        if (!message.isEmpty()) {
            messages.add(message);
        }
    }

    public Collection<String> getMessages() {
        return Collections.unmodifiableCollection(messages);
    }

    public boolean isEmpty() {
        return messages.isEmpty();
    }

    public String getSummary(String headMessage, String separator) {
        return getSummary("", headMessage, separator);
    }

    public String  getSummary(String messageIfValid, String headMessage, String separator) {
        if (!isEmpty()) {
            StringBuilder sb = new StringBuilder();
            sb.append(headMessage);
            sb.append(": ");
            return Joiner.on(separator).appendTo(sb, messages).toString();
        }
        return messageIfValid;
    }

}
