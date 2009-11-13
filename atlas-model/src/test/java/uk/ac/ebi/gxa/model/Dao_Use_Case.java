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
