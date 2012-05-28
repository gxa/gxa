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

package uk.ac.ebi.gxa.web.view.dsv;

import uk.ac.ebi.gxa.export.dsv.ExperimentTableDsv;
import uk.ac.ebi.gxa.service.experiment.ExperimentAnalytics;
import uk.ac.ebi.gxa.spring.view.dsv.AbstractTsvView;
import uk.ac.ebi.gxa.utils.dsv.DsvRowIterator;

import java.util.Map;

/**
 * @author Olga Melnichuk
 */
public class ExperimentTableTsvView extends AbstractTsvView<ExperimentAnalytics.TableRow> {

    @Override
    protected DsvRowIterator<ExperimentAnalytics.TableRow> buildDsvDocument(Map<String, Object> model) {
        return ExperimentTableDsv.createDsvDocument(model);
    }

}
