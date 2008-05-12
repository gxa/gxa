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
public class IndexBuilderTest extends AbstractIndexBuilderTest
{
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
		super.setUp();
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
	 * Method 
	 * Test method for {@link uk.ac.ebi.ae3.indexbuilder.App#parse(java.lang.String[])}.
	 */
	@Test
	public void test_parse()
	{
		App indexBuilder = new App();
		//Test parse input arguments. 
		//No parameters. Method should return false
		String[] args1={};	
		assertFalse(indexBuilder.parse(args1));
		//Wrong parameters. Method should return false		
		String[] args2={"--prox","sosos"};
		assertFalse(indexBuilder.parse(args2));	
		//Correct parameters
		String[] args3={"--property","resource//indexbuilder.properties"};
		assertTrue(indexBuilder.parse(args3));	
		
	}
	
	/**
	 * Test method for {@link uk.ac.ebi.ae3.indexbuilder.App#run()}.
	 */
	@Test
	public void test_run()
	{
		fail("Not yet implemented");
	}
	
	/**
	 * Test method for {@link uk.ac.ebi.ae3.indexbuilder.App#main(java.lang.String[])}.
	 */
	@Test
	public void test_main()
	{
		fail("Not yet implemented");
	}
	
	
	
}
