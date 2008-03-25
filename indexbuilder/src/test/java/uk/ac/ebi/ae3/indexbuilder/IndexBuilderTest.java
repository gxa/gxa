/**
 * 
 */
package uk.ac.ebi.ae3.indexbuilder;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author mdylag
 *
 */
public class IndexBuilderTest
{
	private String argsPropertry[]={};
	private String argsCli[]={};

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception
	{
		System.out.println("tear before");

	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception
	{
		System.out.println("tear down");

	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception
	{
		System.out.println("Start test");
	}
	
	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception
	{
	}
	
	/**
	 * Test method for {@link uk.ac.ebi.ae3.indexbuilder.IndexBuilder#parse(java.lang.String[])}.
	 */
	@Test
	public void testParse()
	{
	}
	
	/**
	 * Test method for {@link uk.ac.ebi.ae3.indexbuilder.IndexBuilder#run()}.
	 */
	@Test
	public void testRun()
	{
		fail("Not yet implemented");
	}
	
	/**
	 * Test method for {@link uk.ac.ebi.ae3.indexbuilder.IndexBuilder#main(java.lang.String[])}.
	 */
	@Test
	public void testMain()
	{
		fail("Not yet implemented");
	}
	
}
