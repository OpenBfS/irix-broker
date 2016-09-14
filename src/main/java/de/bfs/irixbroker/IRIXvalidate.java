/**
 *
 */
package de.bfs.irixbroker;

import javax.xml.bind.JAXBException;
import org.iaea._2012.irix.format.*;
import org.iaea._2012.irix.format.ReportType;
import org.xml.sax.SAXException;

/**
 * Class for validation of an XML-file against the IRIX schema
 *
 * @author Peter Bieringer
 * @version 0.1
 *
 * date 2015-01-22
 *
 */
public class IRIXvalidate {
	
	private ReportType report;
	boolean result = false;
	
	public ReportType getReport() {
		return report;
	}

	public void setReport(ReportType report) {
		this.report = report;
	}

	public boolean isResult() {
		return result;
	}

	public void setResult(boolean result) {
		this.result = result;
	}

	public  IRIXvalidate(String file)
	{
		
		
		result= validate(file);
		
	}
	
	/**
	 * public method validate
	 * @param filename
	 * @return boolean value (valid or not valid)
	 */
	public boolean validate( String filename)
	{
		try {
			report = (ReportType) JaxbMarshalUnmarshalUtil.unmarshal( "IRIX.xsd", filename, Class.forName( "org.iaea._2012.irix.format.ReportType" ), true );
		} catch (ClassNotFoundException | JAXBException | SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}
		
		return true;
	}

	/**
	 * main
	 *
	 * @param args Filename from the command line
	 */
	public static void main(String[] args) {
		if( args.length != 1 ) {
	         System.out.println( "\nPlease give the filename of the IRIX report as command line argument" );
	         return;
	      }
	      System.out.println( "\nXML-document: " + args[0] + "\n" );
	
	      IRIXvalidate val = new IRIXvalidate(args[0]);
	
	      if(val.isResult())
	    	  System.out.println("IRIX report is valid!");
	      else
	    	  System.out.println("Not a valid IRIX report!");

	}

}
