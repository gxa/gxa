package uk.ac.ebi.microarray.atlas.loader;

import uk.ac.ebi.microarray.atlas.loader.db.utils.AtlasDB;
import uk.ac.ebi.microarray.atlas.loader.model.Assay;
import uk.ac.ebi.microarray.atlas.loader.model.Experiment;
import uk.ac.ebi.microarray.atlas.loader.model.Sample;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * Created by IntelliJ IDEA. User: Andrey Date: Aug 27, 2009 Time: 10:46:28 AM
 * To change this template use File | Settings | File Templates.
 */
public class AtlasDBTest {

  public static void main(String[] args) throws Exception {

    String driverName = "oracle.jdbc.driver.OracleDriver";
    Class.forName(driverName);

    String serverName = "apu.ebi.ac.uk";
    String portNumber = "1521";
    String sid = "AEDWT";
    String url =
        "jdbc:oracle:thin:@" + serverName + ":" + portNumber + ":" + sid;
    String username = "AEMART";
    String password = "marte";

    Connection conn =
        DriverManager.getConnection(url, username, password);

    Experiment experiment = new Experiment();
    experiment.setAccession("EXP-1");
    experiment.setDescription("enee menee monee moe");
    experiment.setPerformer("Johnny Cash");
    experiment.setLab("Blue Suede Shoes");

    AtlasDB.writeExperiment(conn, experiment);

    /*Atlas.*/
    Assay assay = new Assay();
    assay.setAccession("ASSAY-1");
    assay.setArrayDesignAcession("A-AFFY-2");
    assay.setExperimentAccession("EXP-1");
    //assay.Properties = new ArrayList<Atlas.Property>(); can compose by hands or use shortcut
    assay.addProperty("celltype", "celltype", "alveolar macrophage", true);
    assay.addProperty("growthcondition", "growthcondition", "42 degree_C",
                      false);
    //etc.

    for (int i = 0; i != 1000; i++) {
      assay.addExpressionValue("266731_at", 123.45F);
    }

    AtlasDB.writeAssay(conn, assay);

    Sample sample = new Sample();

    sample
        .setAccession("ebi.ac.uk:MIAMExpress:BioSample:Extraction.HumanClone");
    sample.addAssayAccession("ASSAY-1");
    sample.addProperty("individual", "individual", "programmer N23", false);

    AtlasDB.writeSample(conn, sample);
  }
}
