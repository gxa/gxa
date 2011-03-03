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

import org.mged.magetab.error.ErrorCode;
import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.arrayexpress2.magetab.datamodel.MAGETABInvestigation;
import uk.ac.ebi.arrayexpress2.magetab.exception.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.handler.ParserMode;
import uk.ac.ebi.arrayexpress2.magetab.parser.MAGETABParser;
import uk.ac.ebi.gxa.loader.AtlasLoaderException;

import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Experiment loading step that parses IDF and SDRF into a MAGETABInvestigation object.
 * Based on the original code by Tony Burdett.
 *
 * @author Nikolay Pultsin
 * @date Aug-2010
 */


public class ParsingStep implements Step {
    private final URL idfFileLocation;
    private final MAGETABInvestigation investigation;
    private final Logger log = LoggerFactory.getLogger(this.getClass());

    public ParsingStep(URL idfFileLocation, MAGETABInvestigation investigation) {
        this.idfFileLocation = idfFileLocation;
        this.investigation = investigation;
    }

    public String displayName() {
        return "Parsing IDF & SDRF files";
    }

    public void run() throws AtlasLoaderException {
        MAGETABParser parser = new MAGETABParser();

        parser.setParsingMode(ParserMode.READ_AND_WRITE);
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

        ExecutorService parseService =
                Executors.newFixedThreadPool(2);
        ExecutorService idfService =
                Executors.newFixedThreadPool(2);
        ExecutorService sdrfService =
                Executors.newFixedThreadPool(2);
        try {
            parser.parse(idfFileLocation, investigation, parseService, idfService, sdrfService);
            log.info("Parsing finished");
        } catch (ParseException e) {
            // something went wrong - no objects have been created though
            log.error("There was a problem whilst trying to parse " + idfFileLocation, e);
            throw new AtlasLoaderException("Parse error: " + e.getErrorItem().toString(), e);
        } finally {
            parseService.shutdownNow();
            idfService.shutdownNow();
            sdrfService.shutdownNow();
        }
    }
}
