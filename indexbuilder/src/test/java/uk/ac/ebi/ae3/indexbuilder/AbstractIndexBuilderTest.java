/**
 * 
 */
package uk.ac.ebi.ae3.indexbuilder;

import junit.framework.TestCase;
/**
 * An Abstract base class that makes JUnit tests easier. 
 * @author mdylag
 * <p>
 * 
 * </p>
 * @see #setUp
 * @see #tearDown
 */
public abstract class AbstractIndexBuilderTest extends TestCase
{
    private App indexBuilder;
    private boolean runSetUp = false;
	@Override
	protected void setUp() throws Exception
	{
		super.setUp();
		if (runSetUp)
		{
			
		}
	}
	
	@Override
	protected void tearDown() throws Exception
	{
		if (runSetUp)
		{
			
		}
	}
	/**
	 * 
	 * @return location of the property file
	 */
	public String getPropertyFileLocation()
	{
		return "resource/indexbuilder.properties";
	}

}
