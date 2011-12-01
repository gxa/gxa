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

import com.google.common.base.Function;
import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;

import javax.annotation.Nonnull;
import java.net.URL;
import java.util.Collection;

import static com.google.common.base.Joiner.on;
import static com.google.common.collect.Collections2.transform;

/**
 * Experiment loading step that parses IDF and SDRF into a MAGETABInvestigation object.
 * Based on the original code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 */
public class ParsingStep {
    private final static Logger log = LoggerFactory.getLogger(ParsingStep.class);

    public static String displayName() {
        return "Parsing IDF & SDRF files";
    }

    public MAGETABInvestigation parse(URL idfFileLocation) throws AtlasLoaderException {
        MAGETABInvestigation investigation = null;
        MAGETABParser parser = new MAGETABParser();
        parser.setStripEscaping(true);

        // register an error item listener
        parser.addErrorItemListener(new ErrorItemListener() {
            public void errorOccurred(ErrorItem item) {
                // lookup message
                String message = "";
                for (ErrorCode ec : ErrorCode.values()) {
                    if (item.getErrorCode() == ec.getIntegerValue()) {
                        message = ec.getErrorMessage();
                        break;
                    }
                }
                String comment = item.getComment();
                if (message.equals("")) {
                    message = comment.equals("") ? "Unknown error" : comment;
                } else if (!comment.equals("")) {
                    message += " (" + comment + ")";
                }

                // log the error
                // todo: this should go to a different log stream, part of loader report -
                // probably should dynamically creating an appender that writes to the magetab directory
                log.error(
                        "Parser reported:\n\t" +
                                item.getErrorCode() + ": " + message + "\n\t\t- " +
                                "occurred in parsing " + item.getParsedFile() + " " +
                                "[line " + item.getLine() + ", column " + item.getCol() + "].", item
                );
            }
        });

        try {
            investigation = parser.parse(idfFileLocation);
            log.info("Parsing finished");
        } catch (ParseException e) {
            // something went wrong - no objects have been created though
            log.error("There was a problem whilst trying to parse " + idfFileLocation, e);
            throw new AtlasLoaderException("Parse error: " + getParseErrors(e.getErrorItems()), e);

        }

        return investigation;
    }

    public String getParseErrors(Collection<ErrorItem> errorItems) {
        return on(',').join(transform(errorItems, new Function<ErrorItem, String>() {
            @Override
            public String apply(@Nonnull ErrorItem errorItem) {
                return errorItem.reportString();
            }
        }));
    }
}
