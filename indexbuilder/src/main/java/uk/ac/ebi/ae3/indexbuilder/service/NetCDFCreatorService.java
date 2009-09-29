package uk.ac.ebi.ae3.indexbuilder.service;

import ucar.ma2.*;
import ucar.nc2.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.io.*;
import java.sql.*;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;


/**
 * Creates NetCDF files on the disk for data in the Warehouse
 * @author ugis
 * @author ostolop
 */
public class NetCDFCreatorService {

    DataSource dataSource;
    protected final Logger log = LoggerFactory.getLogger(getClass());

    String netcdfPath;

    boolean skipExisting = true;

    private final String CREATENETCDF_VERSION = "AtlasNetCDF SVN Revision $Rev: 8073 $";
    private static final int NUM_THREADS = 16;

    public NetCDFCreatorService() {

    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getNetcdfPath() {
        return netcdfPath;
    }

    public void setNetcdfPath(String netcdfPath) {
        this.netcdfPath = netcdfPath;
    }

    /**
     * This utility class should be called as
     * <pre>AEDWloader.CreateNetCDF accnum|all properties.txt mappings.txt</pre>
     * where accnum would be the experiment accession number and properties.txt is the file
     * with configuration properties in it. mappings.txt is the file mapping
     * attributes (e.g. experimental factor names) to tables in the database.
     *
     * If the utility class is called with 1st argument 'all', then all will be created.
     */
    public void run (String what) {
        try {
            Connection connection = getDataSource().getConnection();

            try {

                if ( what == null || what.equals ( "all" ) || what.equals("update") ) {
                    try {
                        String query = what != null && what.equals("update") ?
                                "select accession from LOAD_MONITOR where netcdf ='pending' AND load_type='experiment' AND status = 'loaded'" :
                        "select unique experiment_accession from AE1__EXPERIMENT__MAIN where experiment_accession is not null";

                        final ExecutorService tpool = Executors.newFixedThreadPool(NUM_THREADS);
                        final ResourcePool<ExperimentCreator> cpool = new ResourcePool<ExperimentCreator>(NUM_THREADS) {
                            @Override
                            public void closeResource(ExperimentCreator experimentCreator) {
                                try {
                                    experimentCreator.closeStatements();
                                } catch (SQLException e) {
                                    log.error("Can't close statement", e);
                                }
                            }

                            @Override
                            public ExperimentCreator createResource() {
                                try {
                                    ExperimentCreator creator = new ExperimentCreator();
                                    creator.prepareStatements();
                                    return creator;
                                } catch(SQLException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        };

                        Statement stmt = connection.createStatement ();
                        ResultSet rset=stmt.executeQuery (query);
                        while (rset.next ()) {
                            final String experiment_accession = rset.getString (1);
                            tpool.submit(new Runnable() {
                                public void run() {
                                    try {
                                        ExperimentCreator creator = cpool.getItem();
                                        creator.createForExperiment(experiment_accession);
                                        cpool.putItem(creator);
                                    } catch(Exception e) {
                                        log.error("Exception in worker", e);
                                        throw new RuntimeException("Exception in the worker", e);
                                    }
                                }
                            });
                        }
                        rset.close ();
                        if (stmt != null)
                            stmt.close ();

                        log.info("Waiting for workers...");
                        tpool.shutdown();
                        tpool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
                        cpool.close();
                        log.info("Finished, committing");

                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    ExperimentCreator creator = new ExperimentCreator();
                    creator.prepareStatements();
                    creator.createForExperiment(what);
                    creator.closeStatements();
                }
            } catch(Exception e) {
                throw new RuntimeException(e);
            } finally {
                if(connection != null)
                    connection.close();
            }
        }  catch(SQLException e) {
            throw new RuntimeException(e);

        }
    }

    public class ExperimentCreator {

        private PreparedStatement stmtGetExpQuantType;
        private PreparedStatement stmtGetAryDes;
        private PreparedStatement stmtGetGenes;
        private PreparedStatement stmtGetSamples;
        private PreparedStatement stmtGetAssays;
        private PreparedStatement stmtGetAtlas;
        private PreparedStatement stmtGetDataAbs;
        private PreparedStatement stmtGetDataRat;
        private Vector fNames = new Vector();
        private Vector characteristics = new Vector();
        private HashMap transHash = new HashMap ();
        private HashMap transHash2 = new HashMap ();
        private HashMap transHash3 = new HashMap ();
        private Connection connection;

        public void prepareStatements() throws SQLException {
            connection = getDataSource().getConnection();

            stmtGetExpQuantType = connection.prepareStatement("select exp.experiment_id_key, meas.value " +
                    "from AE1__EXPERIMENT__MAIN exp, ae1__experiment_meastype__dm meas " +
                    "where experiment_accession = ? "+
                    "and exp.experiment_id_key = meas.experiment_id_key ");
            stmtGetAryDes = connection.prepareStatement("select UNIQUE m.ARRAYDESIGN_ID from AE1__ASSAY__MAIN m where m.ARRAYDESIGN_ID is not null AND EXPERIMENT_ACCESSION = ?");
            stmtGetGenes = connection.prepareStatement("select gene_identifier, designelement_id_key, gene_id_key from AE2__DESIGNELEMENT__MAIN t where t.ARRAYDESIGN_ID = ? order by gene_id_key, designelement_id_key");
            stmtGetSamples = connection.prepareStatement("select UNIQUE sample_id_key, arraydesign_accession, arraydesign_name, assay_id_key from AE1__SAMPLE__MAIN t where t.ARRAYDESIGN_ID = ? AND experiment_id_key = ? order by sample_id_key");
            stmtGetAssays = connection.prepareStatement("select UNIQUE assay_id_key from AE1__ASSAY__MAIN t where t.ARRAYDESIGN_ID = ? AND experiment_id_key = ? order by assay_id_key");
            stmtGetAtlas = connection.prepareStatement("select designelement_id_key, gene_id_key, updn_pvaladj, updn_tstat from atlas where experiment_id_key = ? AND arraydesign_id_key = ? AND ef = ? AND efv = ?");
            stmtGetDataAbs = connection.prepareStatement("select v.absolute, v.designelement_id_key, v.sample_id, de.gene_id_key, v.assay_id from AE2__EXPRESSIONVALUE__MAIN v, ae2__designelement__main de where v.ARRAYDESIGN_ID = ? and v.experiment_id = ? and de.arraydesign_id=v.ARRAYDESIGN_ID and v.absolute is not null and v.designelement_id_key is not null and v.sample_id is not null and de.designelement_id_key=v.designelement_id_key");
            stmtGetDataRat = connection.prepareStatement("select v.ratio, v.designelement_id_key, v.sample_id, de.gene_id_key, v.assay_id  from AE2__EXPRESSIONVALUE__MAIN v, ae2__designelement__main de where v.ARRAYDESIGN_ID = ? and v.experiment_id = ? and de.arraydesign_id=v.ARRAYDESIGN_ID and v.ratio is not null and v.designelement_id_key is not null and v.assay_id is not null and de.designelement_id_key=v.designelement_id_key");

            getAssaySampleProperties(connection);
            Collections.sort(fNames);
            Collections.sort(characteristics);

        }

        public void closeStatements() throws SQLException {
            stmtGetExpQuantType.close();
            stmtGetAryDes.close();
            stmtGetGenes.close();
            stmtGetAssays.close();
            stmtGetAtlas.close();
            stmtGetDataAbs.close();
            stmtGetDataRat.close();

            for(Object ps : transHash.values())
                ((PreparedStatement)ps).close();
            for(Object ps : transHash2.values())
                ((PreparedStatement)ps).close();
            for(Object ps : transHash3.values())
                ((PreparedStatement)ps).close();

            connection.close();
        }


        /**
         * Creates NetCDFs for an experiment, for all array designs.
         * @param accession experiment accession number
         */
        public void createForExperiment (String accession) throws SQLException, IOException, InvalidRangeException {
            log.info("[exp:" + accession + "] " + "Processing " + accession);
            String experiment_id = null;
            String quantType = null;
            stmtGetExpQuantType.setString(1, accession);
            ResultSet rset= stmtGetExpQuantType.executeQuery();
            log.info("[exp:" + accession + "] " + "here");
            if (rset.next ()){
                experiment_id = rset.getString (1);
                quantType = rset.getString(2);
            }
            else
                return;
            rset.close ();

            stmtGetAryDes.setString(1, accession);
            rset= stmtGetAryDes.executeQuery();
            while (rset.next ()) {
                String array_id = rset.getString (1);
                createForArray (array_id, experiment_id, accession, quantType);
            }
            rset.close ();
        }

        /**
         * Creates NetCDFs for the specified arraydesign + experiment pair.
         * @param array_id arraydesign id (DW internal DB id)
         * @param experiment_id experiment id (DW internal DB id)
         * @param accession experiment accession (public)
         */
        void createForArray (String array_id, String experiment_id, String accession, String qtType) throws SQLException, IOException,  InvalidRangeException {

            log.info("[exp:" + accession + "] " + "..Array " + array_id + " Experiment " + experiment_id);

            if(skipExisting)
            {
                String fileName1 = netcdfPath + File.separator + experiment_id + "_" + array_id + ".nc";
                String fileName2 = netcdfPath + File.separator + experiment_id + "_" + array_id + "_ratios.nc";
                if(new File(fileName1).exists() || new File(fileName2).exists())
                {
                    log.info("[exp:" + accession + "] " + "Already exists, skipping");
                    return;
                }
            }


            log.info("[exp:" + accession + "] " + "....Fetching genes");
            TreeMap des = new TreeMap (new Comparator() { public int compare (Object o1, Object o2) {return ((Integer[]) o1)[0].compareTo(((Integer[]) o2)[0]);}} );
            Vector des_vector = new Vector();
            stmtGetGenes.setString(1, array_id);
            ResultSet rset=stmtGetGenes.executeQuery();
            int gene_counter = 0;
            while (rset.next ()) {
                int designelement_id_key = rset.getInt (2);
                int gene_id_key          = rset.getInt (3);
                Integer[] dege = new Integer[] { new Integer (designelement_id_key), new Integer(gene_id_key) };

                des.put (dege, new Integer (gene_counter));
                des_vector.add (dege);
                gene_counter++;
            }
            rset.close ();
            log.info("[exp:" + accession + "] " + "......" + Integer.toString (gene_counter));


            log.info("[exp:" + accession + "] " + "....Fetching samples");
            TreeMap samples = new TreeMap ();
            TreeMap samples2assays = new TreeMap();
            stmtGetSamples.setString(1, array_id);
            stmtGetSamples.setString(2, experiment_id);
            rset=stmtGetSamples.executeQuery ();
            int sample_counter = 0;
            String adAccession="", adName="";
            while (rset.next ()) {
              // sample_id_key, arraydesign_accession, arraydesign_name, assay_id_key
                int sample_id_key = rset.getInt (1);
                adAccession = rset.getString(2);
                adName = rset.getString(3);
                samples.put (new Integer (sample_id_key),new Integer (sample_counter));
                samples2assays.put(sample_id_key, rset.getString(4));
                sample_counter++;
            }
            rset.close ();
            log.info("[exp:" + accession + "] " + "......" + Integer.toString (sample_counter));

            log.info("[exp:" + accession + "] " + "....Fetching assays");
            TreeMap assays = new TreeMap ();

            stmtGetAssays.setString(1, array_id);
            stmtGetAssays.setString(2, experiment_id);
            rset=stmtGetAssays.executeQuery();
            int assay_counter = 0;
            while (rset.next ()) {
                int assay_id_key = rset.getInt (1);
                assays.put (new Integer (assay_id_key),new Integer (assay_counter));
                assay_counter++;
            }
            rset.close ();
            log.info("[exp:" + accession + "] " + "......" + Integer.toString (assay_counter));

            // Experimental Factor Values

            Iterator fIter = fNames.iterator ();
            Iterator cIter = characteristics.iterator();

            TreeMap efvs4assays = new TreeMap ();
            TreeMap efvs4samples = new TreeMap ();
            TreeMap characValues4samples = new TreeMap();

            TreeMap uniqEfvsNums = new TreeMap();
            Vector uniqEfvs = new Vector();

            while (fIter.hasNext ()) {
                // factor name
                String fName = (String)(fIter.next ());

                //factor values - for all relevant bioassays/samples (ratios/absolutes)
                Vector fValuesForAssays  = getEFValuesForAssays ( fName, experiment_id, array_id);
                Vector fValuesForSamples = getEFValuesForSamples ( fName, experiment_id, array_id);

                if ( fValuesForAssays.size () > 0 ) {
                    efvs4assays.put (fName,fValuesForAssays);
                    Set efvSet = new HashSet();
                    efvSet.addAll(fValuesForAssays);
                    uniqEfvs.addAll(efvSet);
                    uniqEfvsNums.put(fName, efvSet.size());
                }

                if ( fValuesForSamples.size () > 0 ) efvs4samples.put (fName,fValuesForSamples);

            }

            while (cIter.hasNext()){
                String characteristic = cIter.next().toString();

                LinkedHashMap<String, ArrayList<String>> charValuesForSamples = getCharValuesForSamples(characteristic,experiment_id,array_id);
                if(charValuesForSamples.size() > 0)
                    characValues4samples.put(characteristic,charValuesForSamples);
            }

            log.info("[exp:" + accession + "] " + "Fetching atlas");
            stmtGetAtlas.setInt(1, Integer.valueOf(experiment_id));
            stmtGetAtlas.setInt(2, Integer.valueOf(array_id));

            Vector pvalues = new Vector();
            pvalues.setSize(uniqEfvs.size() * des_vector.size());
            Vector tstats = new Vector();
            tstats.setSize(uniqEfvs.size() * des_vector.size());
            Iterator efvIter = uniqEfvs.iterator();
            int efvPos = 0;
            for(fIter = efvs4assays.keySet().iterator(); fIter.hasNext(); ) {
                String ef = (String)fIter.next();
                stmtGetAtlas.setString(3, ef.substring(3));
                for(int i = 0; i < (Integer)uniqEfvsNums.get(ef); ++i) {
                    String efv = (String)efvIter.next();
                    stmtGetAtlas.setString(4, efv);
                    rset = stmtGetAtlas.executeQuery();
                    while(rset.next()) {
                        Integer[] dege = new Integer[] { rset.getInt(1), rset.getInt(2) };
                        Integer depos = (Integer)des.get(dege);
                        Double pvalue = rset.getDouble(3);
                        Double tstat = rset.getDouble(4);
                        pvalues.set(depos * uniqEfvs.size() + efvPos, pvalue);
                        tstats.set(depos * uniqEfvs.size() + efvPos, tstat);
                    }
                    ++efvPos;
                    rset.close();
                }
            }
            log.info("[exp:" + accession + "] " + "end");

            for(int i = 0; i < pvalues.size(); ++i)
                if(pvalues.get(i) == null)
                    pvalues.set(i, Double.valueOf(0));

            for(int i = 0; i < tstats.size(); ++i)
                if(tstats.get(i) == null)
                    tstats.set(i, Double.valueOf(0));

            /* absolutes - map to BioSamples */

            stmtGetDataAbs.setString(1, array_id);
            stmtGetDataAbs.setString(2, experiment_id);
            createMatrix (netcdfPath + File.separator + experiment_id + "_" + array_id + ".nc", sample_counter, assay_counter, gene_counter, samples, assays,
                    des, des_vector, efvs4assays, characValues4samples, stmtGetDataAbs,
                    accession, qtType, adAccession, adName,samples2assays, uniqEfvsNums, uniqEfvs, pvalues, tstats);

            /* ratios - map to BioAssays */

            stmtGetDataRat.setString(1, array_id);
            stmtGetDataRat.setString(2, experiment_id);
            createMatrix (netcdfPath + File.separator + experiment_id + "_" + array_id + "_ratios.nc", sample_counter, assay_counter, gene_counter, samples, assays,
                    des, des_vector, efvs4assays, characValues4samples, stmtGetDataRat,
                    accession, qtType, adAccession, adName, samples2assays, uniqEfvsNums, uniqEfvs, pvalues, tstats);

        }

        /**
         * returns all possible experimental factor names
         */
        void getAssaySampleProperties (Connection connection) throws SQLException {
            BufferedReader br = null;
            try {
                br = new BufferedReader (new InputStreamReader(getClass().getClassLoader().getResourceAsStream("mappings.txt")));
            } catch (Exception e) {
                log.info("Exception while opening mappings");
            }

            try {
                String line = br.readLine ();
                while (line != null && line.compareTo ("") != 0) {
                    String filterName = line.substring (0,line.indexOf (' '));
                    String attributeName = line.substring (line.indexOf (' ')+1);

                    if(attributeName.toLowerCase().contains("assay")) {
                        transHash.put (filterName, connection.prepareStatement("select unique ef.value, a.assay_id_key from " + attributeName + " ef, ae1__assay__main a" +
                                " where ef.EXPERIMENT_ID_KEY=?" +
                                " and ef.experiment_id_key = a.experiment_id_key" +
                                " and ef.assay_id_key = a.assay_id_key" +
                                " and a.arraydesign_id = ?" +
                                " ORDER by a.assay_id_key"));

                        transHash2.put (filterName, connection.prepareStatement("select unique ef.value, s.sample_id_key from " + attributeName + " ef, ae1__sample__main s" +
                                " where ef.EXPERIMENT_ID_KEY=?" +
                                " and ef.experiment_id_key = s.experiment_id_key" +
                                " and ef.assay_id_key = s.assay_id_key" +
                                " and s.arraydesign_id = ?" +
                                " ORDER by s.sample_id_key"));
                    }
                    if(attributeName.toLowerCase().contains("sample")) {
                        transHash3.put(filterName, connection.prepareStatement("select unique charac.value, s.sample_id_key " +
                                "from " + attributeName + " charac, ae1__sample__main s"+
                                " where charac.sample_id_key = s.sample_id_key "+
                                " and charac.experiment_id_key = s.experiment_id_key "+
                                " and s.experiment_id_key = ?" +
                                " and s.arraydesign_id = ?" +
                                " order by s.sample_id_key"));
                    }

                    if ( filterName.startsWith ("ba_")) {
                        fNames.add (filterName);
                    }
                    if ( filterName.startsWith ("bs_")) {
                        characteristics.add (filterName);
                    }

                    line = br.readLine ();
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }


        /**
         * returns all experimental factor values for a given factor name/experiment/bioassays combination
         */
        Vector getEFValuesForAssays ( String fName, String experiment_id, String array_id) throws SQLException {

            PreparedStatement stmt = (PreparedStatement) transHash.get(fName);
            stmt.setString(1, experiment_id);
            stmt.setString(2, array_id);
            ResultSet rset = stmt.executeQuery ();

            Vector efvs = new Vector ();
            int nulls_ctr = 0;
            while ( rset.next () ) {
                String efv = rset.getString (1);

                if ( efv == null ) {
                    efv = "";
                    nulls_ctr++;
                }
                efvs.add (efv);
            }

            rset.close ();
            // return empty Vector if all returned values were null
            if ( nulls_ctr == efvs.size ()) return new Vector ();

            return efvs;
        }

        /**
         * returns all experimental factor values for a given factor name/experiment/biosamples combination
         */
        Vector getEFValuesForSamples ( String fName, String experiment_id, String array_id) throws SQLException {
            PreparedStatement stmt = (PreparedStatement) transHash2.get(fName);
            stmt.setString(1, experiment_id);
            stmt.setString(2, array_id);
            ResultSet rset = stmt.executeQuery ();

            Vector efvs = new Vector ();
            int nulls_ctr = 0;
            while ( rset.next () ) {
                String efv = rset.getString (1);

                if ( efv == null ) {
                    efv = "";
                    nulls_ctr++;
                }
                efvs.add (efv);
            }

            rset.close ();

            // return empty Vector if all returned values were null
            if ( nulls_ctr == efvs.size ()) return new Vector ();

            return efvs;
        }


        LinkedHashMap<String, ArrayList<String>> getCharValuesForSamples(String characteristic, String experiment_id, String array_id) throws SQLException{

            PreparedStatement stmt = (PreparedStatement) transHash3.get(characteristic);
            stmt.setString(1, experiment_id);
            stmt.setString(2, array_id);
            ResultSet rset = stmt.executeQuery ();

//    Vector charValues = new Vector();
            int nulls_ctr=0 , rows = 0;
            LinkedHashMap<String, ArrayList<String>> charValues = new LinkedHashMap<String, ArrayList<String>>();
            ArrayList<String> values;
            while(rset.next()){
                rows++;
                String sample_id_key = rset.getString(2);
                if(charValues.containsKey(sample_id_key)){
                    values = charValues.get(sample_id_key);
                    //values+=", ";
                }
                else{
                    values = new ArrayList<String>();
                    charValues.put(sample_id_key, values);
                }
                String charValue = rset.getString(1);
                if(charValue == null){
                    charValue="";
                    nulls_ctr++;
                }
                values.add(charValue);
            }
            rset.close();

            if ( nulls_ctr == rows) return new LinkedHashMap<String, ArrayList<String>> ();

            return charValues;
        }
        /**
         * Writes the data matrix to NetCDF file (given by fileName).
         * @param fileName filename to write NetCDF to
         * @param geneCount number of genes in the matrix
         * @param designElements design elements - genes in the matrix
         * @param designElementsVector - vector of design elements, preserving BDC row order
         * @param stmt query to retrieve data for the experiment/assays combination
         * @throws java.lang.Exception ...
         */
        void createMatrix (String fileName, int sampleCount, int assayCount, int geneCount, TreeMap samples, TreeMap assays,
                           TreeMap designElements, Vector designElementsVector, TreeMap expFactorValues, TreeMap sampleCharacValues,
                           PreparedStatement stmt, String accession, String qtType, String adAccession, String adName,
                           TreeMap samples2assays,
                           TreeMap uniqEfvsNums, Vector uniqEfvs, Vector pvalues, Vector tstats) throws SQLException, IOException, InvalidRangeException {

            // Checking that absolutes/ratios have been provided for this experiment and array

            ResultSet rset=stmt.executeQuery();

            if (!rset.next ()) {
                rset.close ();
                return;
            }

            int efCount = expFactorValues.size ();
            int efLen   = 0;
            int sampleCharacCount = sampleCharacValues.size();
            int characLen=0;

            log.info("[exp:" + accession + "] " + "File is " + fileName);
            NetcdfFileWriteable ncfile = NetcdfFileWriteable.createNew (fileName, false);

            ncfile.addGlobalAttribute("CreateNetCDF_VERSION", CREATENETCDF_VERSION);
            ncfile.addGlobalAttribute("experiment_accession",accession);
            ncfile.addGlobalAttribute("quantitationType",qtType);
            ncfile.addGlobalAttribute("ADaccession",adAccession);
            ncfile.addGlobalAttribute("ADname",adName);

            Dimension BSdim = ncfile.addDimension ("BS",sampleCount);
            Dimension ASdim = ncfile.addDimension("AS", assayCount);
            Dimension DEdim = ncfile.addDimension ("DE",geneCount);

            Dimension uEFVdim = ncfile.addDimension("uEFV", uniqEfvs.size());

            //Creating EF dimension
            if ( efCount > 0 ) {
                Dimension EFdim, EFlen;
                Dimension[] dim4, dim3;
                String[] efs = (String[]) expFactorValues.keySet ().toArray (new String[0]);
                for ( int i = 0; i < efCount; i++ ) {
                    if ( efs[i].length() > efLen ) efLen = efs[i].length();
                    Vector efvs = (Vector) expFactorValues.get (efs[i]);
                    for ( int j = 0; j < assayCount; j++ ) {
                        String efv = (String) efvs.get(j);
                        if ( efv.length() > efLen ) efLen = efv.length();
                    }
                }

                efLen++;

                System.out.println("Max EFV length: " + efLen);

                EFdim = ncfile.addDimension ("EF",efCount);
                EFlen = ncfile.addDimension ("EFlen",efLen);

                dim4 = new Dimension[2];
                dim4[0] = EFdim;
                dim4[1] = EFlen;

                dim3 = new Dimension[3];
                dim3[0] = EFdim;
                dim3[1] = ASdim;
                dim3[2] = EFlen;

                Dimension[] uEFVvar = new Dimension[2];
                uEFVvar[0] = uEFVdim;
                uEFVvar[1] = EFlen;

                ncfile.addVariable ("EF", DataType.CHAR, dim4);
                ncfile.addVariable ("EFV", DataType.CHAR, dim3);
                ncfile.addVariable ("uEFV", DataType.CHAR, uEFVvar);
                ncfile.addVariable ("uEFVnum", DataType.INT, new Dimension[] { EFdim });
            }




            //Creating sample characteristics dimension
            if ( sampleCharacCount > 0 ) {
                Dimension SCdim, SClen;
                Dimension[] SCharVar, SCharValueVar;
                String[] sampleCharacteristics = (String[]) sampleCharacValues.keySet ().toArray (new String[0]);
                String charVal="";
                for ( int i = 0; i < sampleCharacCount; i++ ) {
                    if ( sampleCharacteristics[i].length() > characLen ) characLen = sampleCharacteristics[i].length();
                    LinkedHashMap<String, ArrayList<String>> sCharVals = (LinkedHashMap<String, ArrayList<String>>) sampleCharacValues.get (sampleCharacteristics[i]);
                    for(ArrayList<String> values:sCharVals.values()){
                        charVal = StringUtils.join(values.iterator(),",");
                        if ( charVal.length() > characLen ) characLen = charVal.length();
                    }
                }

                characLen++;

                System.out.println("Max CharcteriticValue length: " + characLen);

                SCdim = ncfile.addDimension ("SC",sampleCharacCount);
                SClen = ncfile.addDimension ("SClen",characLen);

                SCharVar = new Dimension[2];
                SCharVar[0] = SCdim;
                SCharVar[1] = SClen;

                SCharValueVar = new Dimension[3];
                SCharValueVar[0] = SCdim;
                SCharValueVar[1] = BSdim;
                SCharValueVar[2] = SClen;

                ncfile.addVariable ("SC", DataType.CHAR, SCharVar);
                ncfile.addVariable ("SCV", DataType.CHAR, SCharValueVar);
            }

            Dimension[] BDCvar = new Dimension[2];
            BDCvar[0] = DEdim;
            BDCvar[1] = ASdim;

            Dimension[] BS2ASvar = new Dimension[2];
            BS2ASvar[0] = BSdim;
            BS2ASvar[1] = ASdim;

            Dimension[] ASSAYS = new Dimension[1];
            ASSAYS[0] = ASdim;

            Dimension[] SAMPLES = new Dimension[1];
            SAMPLES[0] = BSdim;

            Dimension[] dim0 = new Dimension[1];
            dim0[0] = DEdim;

            Dimension[] pValDim = new Dimension[2];
            pValDim[0] = DEdim;
            pValDim[1] = uEFVdim;


            ncfile.addVariable ("BDC", DataType.DOUBLE, BDCvar);
            ncfile.addVariable ("BS2AS",DataType.INT, BS2ASvar);
            ncfile.addVariable ("BS", DataType.INT, SAMPLES);
            ncfile.addVariable ("AS", DataType.INT, ASSAYS);
            ncfile.addVariable ("DE", DataType.INT, dim0);
            ncfile.addVariable ("GN", DataType.INT, dim0);
            ncfile.addVariable ("PVAL", DataType.DOUBLE, pValDim);
            ncfile.addVariable ("TSTAT", DataType.DOUBLE, pValDim);

            ncfile.create ();

            /* filling up biosample id vector */
            ArrayInt BS = new ArrayInt.D1 (sampleCount);
            IndexIterator iter = BS.getIndexIterator ();

            Iterator it = samples.keySet ().iterator ();
            while (it.hasNext ()) {
                Integer i = (Integer)(it.next ());
                iter.setIntNext (i.intValue ());
            }

            /* filling up assays id vector */
            ArrayInt AS = new ArrayInt.D1 (assayCount);
            IndexIterator asIter = AS.getIndexIterator ();

            Iterator ait = assays.keySet ().iterator ();
            while (ait.hasNext ()) {
                Integer i = (Integer)(ait.next ());
                asIter.setIntNext (i.intValue ());
            }

            /* filling up designelement id vector */
            ArrayInt DE = new ArrayInt.D1 (geneCount);
            ArrayInt GN = new ArrayInt.D1 (geneCount);

            IndexIterator iter2 = DE.getIndexIterator ();
            IndexIterator iter3 = GN.getIndexIterator ();

            Iterator it2 = designElementsVector.iterator ();
            int jj  = 0;
            while (it2.hasNext ()) {
                Integer[] i = (Integer[])(it2.next ());
                iter2.setIntNext (i[0].intValue ());
                iter3.setIntNext (i[1].intValue ());
            }

            //fill up samples to assays map with zero/one vector for each sample map to assay
            ArrayInt BS2AS = new ArrayInt.D2(sampleCount,assayCount);
            //Initialize the matrix with zeros
            IndexIterator iterbs = BS2AS.getIndexIterator();
            while(iterbs.hasNext()) iterbs.setIntNext(0);

            Index mapIndex = BS2AS.getIndex();
            Iterator mapiter = samples2assays.entrySet().iterator();
            while(mapiter.hasNext()){
                Map.Entry entry = ((Map.Entry)mapiter.next());
                String sample_id_key = entry.getKey().toString();
                String assay_id_key = entry.getValue().toString();
                int sIndex = (Integer)samples.get(new Integer(sample_id_key));
                int aIndex = (Integer)assays.get(new Integer(assay_id_key));
                BS2AS.setInt(mapIndex.set(sIndex,aIndex), 1);
            }

            ArrayDouble PVAL = new ArrayDouble.D2 (geneCount, uniqEfvs.size());
            iter = PVAL.getIndexIterator();
            int k = 0;
            while(iter.hasNext()) iter.setDoubleNext((Double)pvalues.get(k++));

            ArrayDouble TSTAT = new ArrayDouble.D2 (geneCount, uniqEfvs.size());
            iter = TSTAT.getIndexIterator();
            k = 0;
            while(iter.hasNext()) iter.setDoubleNext((Double)tstats.get(k++));

            /*fill up matrix*/
            ArrayDouble BDC = new ArrayDouble.D2 (geneCount, assayCount);

            /* defaults */
            iter = BDC.getIndexIterator();
            while (iter.hasNext()) iter.setDoubleNext(-1000000);

            /* data */
            Index ncIdx = BDC.getIndex ();

            log.info("[exp:" + accession + "] " + "....Fetching values");

            log.info("[exp:" + accession + "] " + "......creating NetCDF " + fileName);
            int total = geneCount * assayCount;
            log.info("[exp:" + accession + "] " + "Expecting... " + total + " elements");
            do {
                double dataValue = rset.getDouble (1);
                int designelement_id_key = rset.getInt (2);
                int assay_id = rset.getInt ("assay_id");
                int gene_id_key = rset.getInt("gene_id_key");

                Integer[] dege = new Integer[] { new Integer(designelement_id_key), new Integer(gene_id_key) };
                Integer deIdInteger = (Integer)(designElements.get (dege));

                if (deIdInteger==null)
                    continue;
                int deId = deIdInteger.intValue ();
                Integer assayIdInteger = (Integer)(assays.get (new Integer (assay_id)));
                if (assayIdInteger==null)
                    continue;
                int assayId = assayIdInteger.intValue ();
                BDC.setDouble (ncIdx.set (deId,assayId), dataValue);

            } while(rset.next ());
            rset.close ();

            log.info("[exp:" + accession + "] " + "......done");

            ncfile.write ("BDC",BDC);
            ncfile.write ("BS2AS", BS2AS);
            ncfile.write ("BS",BS);
            ncfile.write ("AS",AS);
            ncfile.write ("DE",DE);
            ncfile.write ("GN", GN);
            ncfile.write ("PVAL", PVAL);
            ncfile.write ("TSTAT", TSTAT);

            if (efCount > 0) {
                /* filling in EF */
                String[] efs = (String[]) expFactorValues.keySet ().toArray (new String[0]);

                ArrayChar EF = new ArrayChar.D2 (efCount,efLen);
                for ( int i = 0; i < efCount; i++ )
                    EF.setString ( i, efs[i] );

                /* filling up EFV */
                ArrayChar EFV = new ArrayChar.D3 (efCount,assayCount,efLen);
                Index ncEFVIdx = EFV.getIndex ();
                for ( int i = 0; i < efCount; i++ ) {
                    Vector efvs = (Vector) expFactorValues.get (efs[i]);
                    log.info("[exp:" + accession + "] " + "Adding EFVs for " + efs[i] );
                    for ( int j = 0; j < assayCount; j++ ) {
                        EFV.setString (ncEFVIdx.set (i,j),(String)efvs.get (j));
                    }
                }

                ArrayChar uEFV = new ArrayChar.D2 (uniqEfvs.size(), efLen);
                Iterator i = uniqEfvs.iterator();
                k = 0;
                while(i.hasNext() && i.hasNext())
                    uEFV.setString(k++, (String)i.next());

                ArrayInt uEFVnum = new ArrayInt.D1 (efCount);
                i = uniqEfvsNums.values().iterator();
                iter = uEFVnum.getIndexIterator();
                while(i.hasNext() && iter.hasNext())
                    iter.setIntNext((Integer)i.next());

                ncfile.write ("EFV",EFV);
                ncfile.write ("EF",EF);
                ncfile.write ("uEFV",uEFV);
                ncfile.write ("uEFVnum", uEFVnum);
            }

            if (sampleCharacCount > 0) {
                /* filling in SC */
                String[] scs = (String[]) sampleCharacValues.keySet ().toArray (new String[0]);

                ArrayChar SC = new ArrayChar.D2 (sampleCharacCount,characLen);
                for ( int i = 0; i < sampleCharacCount; i++ )
                    SC.setString ( i, scs[i] );

                /* filling up SCV */

                ArrayChar SCV = new ArrayChar.D3 (sampleCharacCount,sampleCount,characLen);
                Index ncSCVIdx = SCV.getIndex ();
                String charVal="";

                for ( int i = 0; i < sampleCharacCount; i++ ) {
                    int j=0;
                    LinkedHashMap<String, ArrayList<String>> sCharVals = (LinkedHashMap<String, ArrayList<String>>) sampleCharacValues.get (scs[i]);
                    log.info("[exp:" + accession + "] " + "Adding characteristic values for " + scs[i] );

                    for(ArrayList<String> values:sCharVals.values()){
                        charVal = StringUtils.join(values.iterator(),",");
                        SCV.setString (ncSCVIdx.set (i,j),charVal);
                        j++;
                    }
                }

                ncfile.write ("SCV",SCV);
                ncfile.write ("SC",SC);
            }

            ncfile.close ();
        }
    }
}