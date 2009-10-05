package uk.ac.ebi.ae3.indexbuilder.dao;

import org.springframework.jdbc.core.simple.ParameterizedRowMapper;

import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * The row mapper from the SQL statement which return XML string. 
 * TODO: Document ME
 * @author mdylag
 *
 */
@Deprecated
public class RowMapperExperimentXml implements ParameterizedRowMapper<String>
{
	/**
	 * Map row from ResultSet to XML String
	 */
	public String mapRow(ResultSet rst, int arg1) throws SQLException
	{
		Clob clob=rst.getClob(1);
		String str=clob.getSubString(1,(int)clob.length());
		return str;
	}
	
}
 
/* */
