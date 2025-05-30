/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */
 /**
 * @authors bp-fr, lem-fr - German Federal Office for Radiation Protection www.bfs.de
 */

package de.bfs.irixbroker;

import static java.lang.System.Logger.Level.DEBUG;
import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

//still part of Java 21
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.List;
import java.util.Properties;

public class IrixBrokerIAEAClient implements IrixBrokerDokpoolXMLNames {

    private static System.Logger log = System.getLogger(IrixBrokerIAEAClient.class.getName());

    private String OrganisationReporting;
    private XMLGregorianCalendar DateTime;
    private String ReportContext;
    private String Confidentiality = "Free for Public Use";
    private String scenario = "routinemode"; //first element from List EventIdentification
    private String[] scenarios = {scenario}; //first element from List EventIdentification
    private List<AnnotationType> annot;
    private List<FileEnclosureType> fet;
    private String title;
    private String main_text = "<b>bereit gestellt durch IRIX-Broker (IRIXBrokerBLClient)</b>";
    private String ReportId;
    private Properties bfsIrixBrokerProperties;
    private boolean success = false;

    public IrixBrokerIAEAClient(Properties bfsIBP) {
        bfsIrixBrokerProperties = bfsIBP;
    }

    /**
     *
     * Do all the work and deliver at the end
     * TODO implement workflow
     * @param report
     * @return boolean
     */
    public boolean sendToIAEA(ReportType report) {
        success = false;
        success = IrixBLClient();
        return success;
    }

    private boolean IrixBLClient() {
        success = true;

        String desc = "Original date: " + DateTime.toString() + " " + ReportContext + " " + Confidentiality;
        log.log(DEBUG, "No delivery workflow implemented for EU");

        return success;
    }

    public String getOrganisationReporting() {
        return OrganisationReporting;
    }

    public void setOrganisationReporting(String organisationReporting) {
        OrganisationReporting = organisationReporting;
    }

    public XMLGregorianCalendar getDateTime() {
        return DateTime;
    }

    public void setDateTime(XMLGregorianCalendar dateTime) {
        DateTime = dateTime;
    }

    public String getReportContext() {
        return ReportContext;
    }

    public void setReportContext(String reportContext) {
        ReportContext = reportContext;
    }

    public String getConfidentiality() {
        return Confidentiality;
    }

    public void setConfidentiality(String confidentiality) {
        Confidentiality = confidentiality;
    }

    public String getScenario() {
        return scenario;
    }

    public void setScenario(String scenario) {
        this.scenario = scenario;
    }

    public String[] getScenarios() {
        return scenarios;
    }

    public void setScenarios(String[] scenarios) {
        this.scenarios = scenarios;
    }

    public List<AnnotationType> getAnnot() {
        return annot;
    }

    public void setAnnot(List<AnnotationType> annot) {
        this.annot = annot;
    }

    public List<FileEnclosureType> getFet() {
        return fet;
    }

    public void setFet(List<FileEnclosureType> fet) {
        this.fet = fet;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMain_text() {
        return main_text;
    }

    public void setMain_text(String main_text) {
        this.main_text = main_text;
    }

    public String getReportId() {
        return ReportId;
    }

    public void setReportId(String reportId) {
        ReportId = reportId;
    }

    public static Element extractSingleElement(Element element, String elementName) {
        // Kindelement mit dem gewünschten Tag-Namen abholen
        final NodeList childElements = element.getElementsByTagName(elementName);

        // Anzahl der Ergebnisse abfragen
        final int count = childElements.getLength();

        // Wenn kein Kindelement gefunden, null zur�ck geben
        if (count == 0) {
            return null;
        }

        // Falls mehr als ein Kindelement gefunden, Fehler werfen
        if (count > 1) {
            throw new IllegalArgumentException("No single child element <" + elementName + "> found! Found " + count + " instances!");
        }

        // Das erste Kindelement zur�ck geben
        return (Element) childElements.item(0);
    }

}
