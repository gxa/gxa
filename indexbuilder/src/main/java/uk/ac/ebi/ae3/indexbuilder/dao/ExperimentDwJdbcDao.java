package uk.ac.ebi.ae3.indexbuilder.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;

import javax.sql.DataSource;

import oracle.sql.CLOB;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import uk.ac.ebi.ae3.indexbuilder.model.Experiment;


/**
 * The class prepares the sql statement and gets data for experiments from DW database.
 * The class uses 
 * 
 * @author mdylag
 * @version 1.0
 * 
 *
 */
public class ExperimentDwJdbcDao
{
	/** The instance of JdbcTemplate **/
	private JdbcTemplate jdbcTemplate;

    private static final String SQL_EXPERIMENT="select Experiment_id_key, experiment_identifier  FROM ae1__experiment__main experiment WHERE experiment.experiment_accession=?";
	private static String sqlPendingExperiments = "select db_id_key, accession from load_monitor " +
												  "where searchindex='pending' " +
												  "and load_type='experiment' " +
												  "and status = 'loaded'";
	private static String sqlAEWexperiments="select experiment_id_key, experiment_identifier  FROM ae1__experiment__main experiment";
	protected static final Logger log = LoggerFactory.getLogger(ExperimentDwJdbcDao.class);

	
	/** An instance of JDBC RowMapper for SQL_ASXML**/
	private RowMapper rowMapper = new RowMapperExperimentXml();
	private RowMapper rowMapperExp = new RowMapperExperiments();

	/** An instance of JDBC RowMapper for sqlExperiments from load monitor**/
	private RowMapper rowExpLoadMonitorMapper = new RowMapperExpLoadMonitor();
	private DataSource _dataSource;

    /**
	 * Set the default DataSource to be used by the ExperimentDwJdbcDao.
	 * @param dataSource -an instance of DataSource class
	 */
	public void setDataSource(DataSource dataSource)
	{
		this.jdbcTemplate = new JdbcTemplate(dataSource);
		_dataSource = dataSource;
	}
	public DataSource getDataSource() {
        return _dataSource;
    }
	
	/**
	 * 
	 * @param experiment
	 * @return
	 */
	public String getExperimentAsXml(Experiment experiment) throws Exception
	{
		Connection conn = getDataSource().getConnection();
		String expXML="";
        PreparedStatement expXMLstmt = conn.prepareStatement("select e.solr_xml.getClobVal() "+
				"FROM experiment_xml e, ae1__experiment__main m " +
				"WHERE e.experiment_id_key = m.experiment_id_key " +
				"AND m.experiment_accession = '"+experiment.getAccession()+"'");
        ResultSet expRS =  expXMLstmt.executeQuery();
        while(expRS.next()){
        	CLOB doc_clob = (CLOB)expRS.getObject(1);
        	expXML = doc_clob.getSubString(1, (int)doc_clob.length());
        }
        expRS.close();
        expXMLstmt.close();
        conn.close();
        return expXML;
	}
	
	public Collection<Experiment> getPendingExperiments() throws Exception{
		Collection<Experiment> colection=this.jdbcTemplate.query(sqlPendingExperiments, rowExpLoadMonitorMapper);
		return colection;
	}
	
	public Collection<Experiment> getExperiments() throws Exception{
		Collection<Experiment> colection=this.jdbcTemplate.query(sqlAEWexperiments, rowExpLoadMonitorMapper);
		return colection;
	}
	
	public boolean experimentExists(Experiment experiment)
	{
		try{
		List<String> l= this.jdbcTemplate.query(SQL_EXPERIMENT, new Object[] {experiment.getAccession()}, rowMapperExp);
	
		if (l.size()!= 0)
		{
			return true;
		}
		return false;
		}catch (Exception e){
			System.out.println(e);
			return false;
		}
	}
	class RowMapperExperiments implements ParameterizedRowMapper<Experiment>
	{
		/**
		 * Maps result set of SQL whci is in sqlExperiments to the Experiment object.
		 */		
		public Experiment mapRow(ResultSet rst, int arg1) throws SQLException
		{
			
			Experiment exp = new Experiment();
			exp.setId(rst.getLong(1));
			exp.setAccession(rst.getString(2));
			return exp;
		}
		
	}
	class RowMapperExpLoadMonitor implements ParameterizedRowMapper<Experiment>
	{
			
		public Experiment mapRow(ResultSet rst, int arg1) throws SQLException
		{
			Experiment exp = new Experiment();
			exp.setId(rst.getLong(1));
			exp.setAccession(rst.getString(2));

			return exp;
		}
		
	}
}
