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

package uk.ac.ebi.gxa.db.utils;

import oracle.sql.ARRAY;
import oracle.sql.ArrayDescriptor;
import oracle.sql.STRUCT;
import oracle.sql.StructDescriptor;
import uk.ac.ebi.microarray.atlas.model.*;
import uk.ac.ebi.microarray.atlas.model.Assay;
import uk.ac.ebi.microarray.atlas.model.Experiment;
import uk.ac.ebi.microarray.atlas.model.Property;
import uk.ac.ebi.microarray.atlas.model.Sample;
import uk.ac.ebi.gxa.model.*;
import uk.ac.ebi.gxa.model.impl.AtlasDao;

import java.sql.*;
import java.util.Map;

/**
 * Utils for writing atlas loader API objects (see also {@link uk.ac.ebi.microarray.atlas.model}) to a database.  Should
 * be supplied a connection
 *
 * @author Andrey Zorin
 * @date Aug 26, 2009 Time: 5:14:19 PM
 */
public class AtlasDB {
    // fixme: this connection object MUST be an OracleConnection, else we'll get a ClassCastException from ArrayDescriptor.createDescriptor()
    public static Array toSqlArray(Connection connection,
                                   String typeName,
                                   Object[] value)
            throws SQLException {
        ArrayDescriptor adExpressionValueTable =
                ArrayDescriptor.createDescriptor(typeName, connection);

        return new ARRAY(adExpressionValueTable, connection, value);
    }

    // fixme: this connection object MUST be an OracleConnection, else we'll get a ClassCastException from StructDescriptor.createDescriptor()
    public static STRUCT toSqlStruct(Connection connection, String typeName,
                                     Object[] value)
            throws SQLException {
        StructDescriptor sdExpressionValue =
                StructDescriptor.createDescriptor(typeName, connection);

        return new STRUCT(sdExpressionValue, connection, value);
    }

    // bind arrayDesignQuery to query parameter
    public static void setArrayDesignQuery(CallableStatement stmt, int ordinal, ArrayDesignQuery arrayDesignQuery)
            throws SQLException {

        //nested accession query
        Object[] acc = new Object[2];

        acc[0] = arrayDesignQuery.getId();
        acc[1] = arrayDesignQuery.getAccession();

        //array design query
        Object[] arr = new Object[4];
        arr[0] = AtlasDB.toSqlStruct(stmt.getConnection(), "ACCESSIONQUERY", acc);  //AccessionQuery
        arr[1] = arrayDesignQuery.getName();
        arr[2] = arrayDesignQuery.getType();
        arr[3] = arrayDesignQuery.getProvider();

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "ARRAYDESIGNQUERY", arr));
    }

    public static void setAssayQuery(CallableStatement stmt, int ordinal, AssayQuery assayQuery, AtlasDao dao)
            throws SQLException, GxaException {
        Object[] PropertyIDs = null;

        //pull IDs from DB - IO heavy
        if (null != assayQuery.getPropertyQuery()) {
            PropertyIDs = ToObjectArray(stmt.getConnection(),
                                        dao.getPropertyIDs(assayQuery.getPropertyQuery())); //first query
        }

        //nested accession query
        Object[] acc = new Object[2];

        acc[0] = assayQuery.getId();
        acc[1] = assayQuery.getAccession();

        Object[] asq = new Object[2];
        asq[0] = AtlasDB.toSqlStruct(stmt.getConnection(), "ACCESSIONQUERY", acc);
        asq[1] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", PropertyIDs);

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "ASSAYQUERY", asq));
    }

    public static void setSampleQuery(CallableStatement stmt, int ordinal, SampleQuery sampleQuery, AtlasDao dao)
            throws SQLException, GxaException {
        Object[] PropertyIDs = null;

        //pull IDs from DB - IO heavy
        if (null != sampleQuery.getPropertyQuery()) {
            PropertyIDs = ToObjectArray(stmt.getConnection(),
                                        dao.getPropertyIDs(sampleQuery.getPropertyQuery())); //first query
        }

        //nested accession query
        Object[] acc = new Object[2];

        acc[0] = sampleQuery.getId();
        acc[1] = sampleQuery.getAccession();

        Object[] asq = new Object[2];
        asq[0] = AtlasDB.toSqlStruct(stmt.getConnection(), "ACCESSIONQUERY", acc);
        asq[1] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", PropertyIDs);

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "SAMPLEQUERY", asq));
    }

    public static void setExperimentQuery(CallableStatement stmt,
                                          int ordinal,
                                          ExperimentQuery experimentQuery,
                                          AtlasDao dao) throws SQLException, GxaException {
        Object[] PropertyIDs = null;
        Object[] GeneIDs = null;

        //pull IDs from DB - IO heavy
        if (null != experimentQuery.getPropertyQuery()) {
            PropertyIDs = ToObjectArray(stmt.getConnection(),
                                        dao.getPropertyIDs(experimentQuery.getPropertyQuery())); //first query
        }

        if (null != experimentQuery.getGeneQuery()) {
            GeneIDs = ToObjectArray(stmt.getConnection(), dao.getGeneIDs(experimentQuery.getGeneQuery())); //first query
        }

        //nested accession query
        Object[] acc = new Object[2];

        acc[0] = experimentQuery.getId();
        acc[1] = experimentQuery.getAccession();

        Object[] asq = new Object[5];
        asq[0] = AtlasDB.toSqlStruct(stmt.getConnection(), "ACCESSIONQUERY", acc);
        asq[1] = experimentQuery.getLab();
        asq[2] = experimentQuery.getPerformer();
        asq[3] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", GeneIDs);
        asq[4] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", PropertyIDs);

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "EXPERIMENTQUERY", asq));
    }

    public static void setGeneQuery(CallableStatement stmt, int ordinal, GeneQuery geneQuery, AtlasDao dao)
            throws SQLException, GxaException {
        Object[] PropertyIDs = null;

        //pull IDs from DB - IO heavy
        if (null != geneQuery.getPropertyQueries() && (0 < geneQuery.getPropertyQueries().size())) {
            PropertyIDs = ToObjectArray(stmt.getConnection(),
                                        dao.getGenePropertyIDs(geneQuery.getPropertyQueries().get(0))); //first query
        }

        //nested accession query
        Object[] acc = new Object[2];

        acc[0] = geneQuery.getId();
        acc[1] = geneQuery.getAccession();

        Object[] asq = new Object[3];
        asq[0] = AtlasDB.toSqlStruct(stmt.getConnection(), "ACCESSIONQUERY", acc);
        asq[1] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", PropertyIDs);
        asq[2] = geneQuery.getSpecies();

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "GENEQUERY", asq));
    }

    public static Object[] ToObjectArray(Connection con, Integer[] ids) throws SQLException {
        if (null == ids) {
            return null;
        }

        Object[] result = new Object[ids.length];

        for (int i = 0; i != ids.length; i++) {
            Object[] PropertyID = new Object[1];
            PropertyID[0] = ids[i];

            result[i] = AtlasDB.toSqlStruct(con, "INTRECORD", PropertyID);
        }

        return result;
    }

    public static void setPropertyQuery(CallableStatement stmt, int ordinal, PropertyQuery propertyQuery, AtlasDao dao)
            throws SQLException, GxaException {
        Object[] AssayIDs = null;
        Object[] SampleIDs = null;
        Object[] ExperimentIDs = null;

        //pull IDs from DB - IO heavy
        if ((null != propertyQuery.getAssayQueries()) && (0 < propertyQuery.getAssayQueries().size())) {
            AssayIDs = ToObjectArray(stmt.getConnection(),
                                     dao.getAssayIDs(propertyQuery.getAssayQueries().get(0))); //first query
        }

        if ((null != propertyQuery.getSampleQueries()) && (0 < propertyQuery.getSampleQueries().size())) {
            SampleIDs = ToObjectArray(stmt.getConnection(),
                                      dao.getSampleIDs(propertyQuery.getSampleQueries().get(0))); //first query
        }

        if ((null != propertyQuery.getExperimentQueries()) && (0 < propertyQuery.getExperimentQueries().size())) {
            ExperimentIDs = ToObjectArray(stmt.getConnection(), dao.getExperimentIDs(
                    propertyQuery.getExperimentQueries().get(0))); //first query
        }

        Object[] asq = new Object[8];
        asq[0] = propertyQuery.getId();
        asq[1] = propertyQuery.getValue();
        asq[2] = propertyQuery.getFullTextQuery();
        asq[3] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", SampleIDs);
        asq[4] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", AssayIDs);
        asq[5] = AtlasDB.toSqlArray(stmt.getConnection(), "TBLINT", ExperimentIDs);

        if(null!=propertyQuery.isAssayProperty())
            asq[6] = propertyQuery.isAssayProperty() ? 1 : 0;

        if(null!=propertyQuery.isSampleProperty())
            asq[7] = propertyQuery.isSampleProperty() ? 1 : 0;

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "PROPERTYQUERY", asq));
    }


    public static void setGenePropertyQuery(CallableStatement stmt, int ordinal, GenePropertyQuery propertyQuery, AtlasDao dao)
       throws SQLException, GxaException {

        Object[] asq = new Object[8];
        asq[0] = propertyQuery.getId();
        asq[1] = propertyQuery.getValue();
        asq[2] = propertyQuery.getFullTextQuery();

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "GENEPROPERTYQUERY", asq));
    }

    // bind PageSortParam to query parameter
    public static void setPageSortParams(CallableStatement stmt, int ordinal, PageSortParams pageSortParams)
            throws SQLException {

        //PageSortParams
        Object[] arr = new Object[3];
        arr[0] = pageSortParams.getStart();
        arr[1] = pageSortParams.getRows();
        arr[2] = pageSortParams.getOrderBy();

        stmt.setObject(ordinal, AtlasDB.toSqlStruct(stmt.getConnection(), "PAGESORTPARAMS", arr));
    }

    @Deprecated
    public static void writeExperiment(Connection connection, Experiment value)
            throws SQLException {
        CallableStatement stmt = null;
        try {
            //1  TheAccession varchar2
            //2  TheDescription varchar2
            //3  ThePerformer varchar2
            //4  TheLab varchar2
            stmt = connection.prepareCall("{call AtlasLdr.a2_ExperimentSet(?,?,?,?)}");

            stmt.setString(1, value.getAccession());
            stmt.setString(2, value.getDescription());
            stmt.setString(3, value.getPerformer());
            stmt.setString(4, value.getLab());  //properties

            // execute statement
            stmt.execute();
        }
        finally {
            if (stmt != null) {
                // close statement
                stmt.close();
            }
        }
    }

    @Deprecated
    public static void writeAnalytics(Connection connection, String experimentAccession, String property, String propertyValue, Map<Integer, ExpressionAnalyticsStatistic> expressionValues)
            throws SQLException {
        CallableStatement stmt = null;
        try {
            //1 Accession varchar2
            //2 Property
            //3 PropertyValue
            //2 ExpressionAnalytics ExpressionAnalyticsTable

            stmt = connection.prepareCall("{call AtlasLdr.a2_AnalyticsSet(?,?,?,?)}");

            // replacing expression value lookup with mapped values lookup
            Object[] expressionValuesArray =
                    new Object[null == expressionValues ? 0
                            : expressionValues.size()];
            //placeholders for all properties of ExpressionValue structure
            Object[] members = new Object[2];

            if (expressionValues != null) {
                int i = 0;
                for (Integer ea : expressionValues.keySet()) {
                    members[1] = ea; //DesignElementID     integer
                    //members[2] = ea.getEfName();          //Property            varchar2
                    //members[3] = ea.getEfvName();         //PropertyValue       varchar2
                    members[2] = expressionValues.get(ea).getPvalue();    //PVALADJ             float
                    members[3] = expressionValues.get(ea).getTstat();    //TSTAT               float

                    expressionValuesArray[i++] =
                            toSqlStruct(connection, "EXPRESSIONANALYTICS", members);
                }
            }

            stmt.setString(1, experimentAccession);
            stmt.setString(2, property);
            stmt.setString(3, propertyValue);
            stmt.setArray(4, toSqlArray(connection, "EXPRESSIONANALYTICSTABLE",
                                        expressionValuesArray));

            // execute statement
            stmt.execute();
        }
        finally {
            if (stmt != null) {
                // close statement
                stmt.close();
            }
        }
    }


    @Deprecated
    public static void writeSample(Connection connection, Sample value)
            throws SQLException {
        CallableStatement stmt = null;
        try {
            //1  Accession varchar2
            //2  Assays AccessionTable
            //3  Properties PropertyTable
            //4  Species varchar2
            //5  Channel varchar2
            stmt = connection.prepareCall("{call AtlasLdr.a2_SampleSet(?,?,?,?,?)}");

            Object[] properties =
                    new Object[null == value.getProperties() ? 0
                            : value.getProperties().size()];
            Object[] members =
                    new Object[4]; //placeholders for all properties of ExpressionValue structure

            if (value.getProperties() != null) {
                int i = 0;
                for (Property v : value.getProperties()) {
                    members[0] = v.getAccession(); //accession
                    members[1] = v.getName();
                    members[2] = v.getValue();
                    members[3] = v.isFactorValue();

                    properties[i++] = toSqlStruct(connection, "PROPERTY", members);
                }
            }

            Object[] assayAccessions = new Object[null == value.getAssayAccessions()
                    ? 0
                    : value.getAssayAccessions().size()];
            if (value.getAssayAccessions() != null) {
                int i = 0;
                for (String v : value.getAssayAccessions()) {
                    assayAccessions[i++] = v;
                }
            }

            stmt.setString(1, value.getAccession());
            stmt.setArray(2,
                          toSqlArray(connection, "ACCESSIONTABLE", assayAccessions));
            stmt.setArray(3, toSqlArray(connection, "PROPERTYTABLE", properties));
            stmt.setString(4, value.getSpecies());  //properties
            stmt.setString(5, value.getChannel());

            // execute statement
            stmt.execute();
        }
        finally {
            if (stmt != null) {
                // close statement
                stmt.close();
            }
        }
    }

    /**
     * A convenience method for querying for the presence of an experiment in the atlas database.  This method returns
     * true if the experiment already exists in the database, and false if it could not be found.  You should not reload
     * an experiment that already exists without first deleting the old one.
     *
     * @param connection the connection to the atlas database
     * @param accession  the accession number of the experiment to query for
     * @return true if thie experiment already exists in the database, false otherwise
     * @throws java.sql.SQLException if there was a problem communicating with the database
     */
    public static boolean experimentExists(Connection connection,
                                           String accession) throws SQLException {
        Statement stmt = null;
        try {
            stmt = connection.prepareStatement(
                    "select count(*) from A2_EXPERIMENT " +
                            "where accession='?'");

            // execute the query
            ResultSet rs = stmt.executeQuery(accession);

            int size = (rs.next() ? rs.getInt(0) : 0);

            return size > 0;
        }
        finally {
            if (stmt != null) {
                // close statement
                stmt.close();
            }
        }
    }

    public static void deleteExperiment(Connection connection, String accession) throws SQLException {
            CallableStatement stmt = null;
            try {
                //1  Accession varchar2
                //2  Assays AccessionTable
                //3  Properties PropertyTable
                //4  Species varchar2
                //5  Channel varchar2
                stmt = connection.prepareCall("{call AtlasLdr.a2_ExperimentDelete(?)}");

                stmt.setString(1, accession);
                // execute statement
                stmt.execute();
            }
            finally {
                if (stmt != null) {
                    // close statement
                    stmt.close();
                }
            }
        }

}
