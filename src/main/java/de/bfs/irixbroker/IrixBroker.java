/**
 * 
 */
package de.bfs.irixbroker;

import java.util.List;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.identification.IdentificationType;
import org.iaea._2012.irix.format.identification.ReportingBasesType;

import javax.annotation.Resource;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 * @author Peter Bieringer, Marco Lechner
 * 
 * irixBroker has to decide what to do with the information from an IRIX report
 * e.g. information for ELAN or measurements for VDB
 *
 */

public class IrixBroker {

    private static Logger log = Logger.getLogger(IrixBroker.class);

    @Resource
    private WebServiceContext context;

    //alternatively get dokpool credentials from file
    /** Path to the IrixBroker Dokpool properties file. */
    private static final String DOKPOOL_CONN_LOC =
            "/WEB-INF/irixbroker-dokpool.properties";

    /** Has {@link init()} successfully been called? */
    public boolean initialized;

    public IrixBroker() {

        initialized = false;
    }

    /**
     * Get servlet context and initialize log4j and other parameters from
     * configuration in web.xml.
     *
     * @throws IrixBrokerException if the servlet context cannot be obtained.
     */
    protected void init() throws IrixBrokerException {
        WebServiceContext sc;
        try {
            sc = (WebServiceContext) context.getMessageContext().get(
                    MessageContext.SERVLET_CONTEXT);
        } catch (IllegalStateException e) {
            System.err.println("Failed to get servlet context.");
            throw new IrixBrokerException(
                    "Failed to get servlet context.", e);
        }

        //String file = sc.getInitParameter("log4j-properties");
        //String log4jProperties = sc.getRealPath(file);
        //PropertyConfigurator.configure(log4jProperties);

        //String dokpoolProp = sc.getInitParameter("irixbroker-dokpool-properties");
        //String dokpoolProp = sc.getInitParameter("irixbroker-dokpool-properties");
        //log.debug("Using: " + dokpoolProp + " to get Dokpool connection.");

        initialized = true;
    }

    /** {@inheritDoc} */
    //@Override
	public void deliverIrixBroker(ReportType report) throws IrixBrokerException {
        //if (!initialized) {
        if (initialized) {
            // Necessary because the servlet context is not accessible in ctor
            init();
            testRecipientConnection();
        }

		if(report == null)
			System.exit(-1);
		else
		{
			IdentificationType ident= report.getIdentification();
			ReportingBasesType base= ident.getReportingBases();
			List<String> bases;
			bases = base.getReportingBasis();
			
			if(bases.isEmpty())
			{
				System.out.println("No reporting bases!");
				System.exit(-2);
			}
			for(int i=0;i<bases.size();i++)
			{
				String b=bases.get(i);
				
				//documents for ELAN
				if(b.equals("ESD"))
				{
                    IrixBrokerDokpoolClient iec = new IrixBrokerDokpoolClient(report);
                    log.debug(iec.getReportContext());
                    log.debug("iec created");
				}
			}
			
		}
	}

    /**
     * TODO Test if recipient is available.
     *
     * @throws IrixBrokerException if the directory cannot be created.
     */
    protected void testRecipientConnection() throws IrixBrokerException {
        log.debug("Testing if recipient is available.");
    }
}
