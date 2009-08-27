package uk.ac.ebi.microarray.atlas.loader;

import java.sql.DriverManager;

/**
 * Created by IntelliJ IDEA.
 * User: Andrey
 * Date: Aug 27, 2009
 * Time: 10:46:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDBTest {

    public static void main(String[] args) throws Exception {

                String driverName = "oracle.jdbc.driver.OracleDriver";
                  Class.forName(driverName);

                  String serverName = "apu.ebi.ac.uk";
                  String portNumber = "1521";
                  String sid = "AEDWT";
                  String url = "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
                  String username = "AEMART";
                  String password = "marte";

                  AtlasDB.Connection = DriverManager.getConnection(url, username, password);

                  /*Atlas.*/Experiment experiment = new /*Atlas.*/Experiment();
                    experiment.Accession ="EXP-1";
                    experiment.Description = "enee menee monee moe";
                    experiment.Performer = "Johnny Cash";
                    experiment.Lab = "Blue Suede Shoes";

                  AtlasDB.ExperimentSet(experiment);

                /*Atlas.*/Assay assay = new /*Atlas.*/Assay();
                  assay.Accession ="ASSAY-1";
                  assay.ArrayDesignAcession="A-AFFY-2";
                  assay.ExperimentAccession="EXP-1";
                  //assay.Properties = new ArrayList<Atlas.Property>(); can compose by hands or use shortcut
                  assay.AddProperty("celltype", "celltype", "alveolar macrophage", true);
                  assay.AddProperty("growthcondition", "growthcondition","42 degree_C", false);
                  //etc.

                  for(int i=0;i!=1000;i++)
                  {
                    assay.AddExpressionValue("266731_at",123.45F);
                  }

                AtlasDB.AssaySet(assay);

                /*Atlas.*/Sample sample = new /*Atlas.*/Sample();

                sample.Accession = "ebi.ac.uk:MIAMExpress:BioSample:Extraction.HumanClone";
                sample.AddAssayAccession("ASSAY-1");
                sample.AddProperty("individual","individual","programmer N23",false);

                AtlasDB.SampleSet(sample);
        
    }
}
