/**
 *
 */
package de.bfs.irixbroker;

import java.util.List;
import java.util.Properties;

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
 * <p>
 * irixBroker has to decide what to do with the information from an IRIX report
 * e.g. information for ELAN, measurements for VDB, documents for IAEA,
 * EU or federal states
 */

public class IrixBroker {

    private static Logger log = Logger.getLogger(IrixBroker.class);

    @Resource
    private WebServiceContext context;

    //alternatively get dokpool credentials from file
    /**
     * Path to the IrixBroker Dokpool properties file.
     */
    private static final String DOKPOOL_CONN_LOC =
            "./bfs-irixbroker.properties";

    /**
     * Has {@link init()} successfully been called?
     */
    public boolean initialized;

    public final Properties bfsIBP;

    public IrixBroker(Properties bfsIrixBrokerProperties) {
        initialized = false;
        bfsIBP = bfsIrixBrokerProperties;
    }

    /**
     * Get servlet context and initialize log4j and other parameters from
     * configuration in web.xml.
     *
     * @throws IrixBrokerException if the servlet context cannot be obtained.
     */
    protected void init() throws IrixBrokerException {

        initialized = true;
    }

    /**
     * {@inheritDoc}
     */
    //@Override
    public void deliverIrixBroker(ReportType report) throws IrixBrokerException {
        if (!initialized) {
            // Necessary because the servlet context is not accessible in ctor
            init();
            // TODO move connection test to Client classes
            testRecipientConnection();
        }

        if (report == null)
            System.exit(-1);
        else {
            IdentificationType ident = report.getIdentification();
            ReportingBasesType base = ident.getReportingBases();
            List<String> bases;
            bases = base.getReportingBasis();

            if (bases.isEmpty()) {
                System.out.println("No reporting bases!");
                System.exit(-2);
            }
            for (int i = 0; i < bases.size(); i++) {
                String b = bases.get(i);

                //documents for Dokpool
                if (b.equals("ESD")) {
                    IrixBrokerDokpoolClient iec = new IrixBrokerDokpoolClient(bfsIBP);
                    iec.sendToDocpool(report);
                    log.debug(iec.getReportContext());
                    log.debug("iec created");
                }
                //data for VDB
                if (b.equals("VDB")) {
                    //IrixBrokerVDBClient ivc = new IrixBrokerVDBClient(report);
                    //ivc.sendToVDB();
                    log.debug("TODO: create IrixBrokerVDBClient");
                }
                //data for IAEA/USIE
                if (b.equals("IAEA")) {
                    IrixBrokerIAEAClient iic = new IrixBrokerIAEAClient(bfsIBP);
                    iic.sendToIAEA(report);
                    log.debug(iic.getReportContext());
                    log.debug("IrixBrokerIAEAClient created");
                    log.debug("TODO: implement workflow in IrixBrokerIAEAClient");
                }
                //data for EU/ECURIE
                if (b.equals("EU")) {
                    IrixBrokerEUClient ieuc = new IrixBrokerEUClient(bfsIBP);
                    ieuc.sendToEU(report);
                    log.debug(ieuc.getReportContext());
                    log.debug("IrixBrokerEUClient created");
                    log.debug("TODO: implement workflow in IrixBrokerEUClient");
                }
                //data for federal state Baden-Wuerttemberg (BW, ...)
                //FIXME could this be done more generic and robust?
                if (b.startsWith("BL_")) {
                    String bl = b.replaceFirst("^BL_", "");
                    log.debug("BL extracted: " + bl);
                    IrixBrokerBLClient iblc = new IrixBrokerBLClient(bfsIBP);
                    iblc.sendToBL(report, bl);
                    log.debug("TODO: create IrixBrokerBLClient");
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

    /** TODO add void main() to make it usable via CLI */
}
