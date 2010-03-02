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

package uk.ac.ebi.gxa.model;

import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Oct 22, 2009
 * Time: 9:49:30 AM
 * To change this template use File | Settings | File Templates.
 */

/*
*/
public class Dao_Use_Case {
    public static void doit(Dao dao) throws GxaException{

        Iterable<Property> available_properties = dao.getProperty( new PropertyQuery()
                                               .isSampleProperty(true)
                                               .usedInExperiments(new ExperimentQuery()
                                                                      .hasPerformer("Kapush%"))).getItems();

        Property property = available_properties.iterator().next();

        for(String val : property.getValues()){
            //iterate through available values
        }

        Iterable<Sample> samples = dao.getSample( new SampleQuery()
                                                  .hasProperty( new PropertyQuery()
                                                                    .hasAccession("SAMPLE WIDTH")
                                                                    .hasValue("BIGG"))).getItems();

        Iterable<Gene> genes = dao.getGene( new GeneQuery()
                                                .usedInExperiments(new ExperimentQuery()
                                                                       .hasProperties(new PropertyQuery()
                                                                                          .hasAccession("Enligtment")
                                                                                          .hasValue("-5")))).getItems();


    }

}
