/**
 * @authors bp-fr, lem-fr - German Federal Office for Radiation Protection www.bfs.de
 */

package de.bfs.irixbroker;

import java.util.*;

import java.lang.NullPointerException;

import javax.xml.datatype.XMLGregorianCalendar;

import org.apache.log4j.Logger;
import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.iaea._2012.irix.format.identification.EventIdentificationType;
import org.iaea._2012.irix.format.identification.IdentificationType;

import de.bfs.dokpool.client.Document;
import de.bfs.dokpool.client.DocumentPool;
import de.bfs.dokpool.client.Scenario;
import de.bfs.dokpool.client.DocpoolBaseService;
import de.bfs.dokpool.client.Folder;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class IrixBrokerDokpoolClient implements IrixBrokerDokpoolXMLNames {

    private static Logger log = Logger.getLogger(IrixBrokerDokpoolClient.class);

    private String OrganisationReporting;
    private XMLGregorianCalendar DateTime;
    private String ReportContext;
    private String Confidentiality = "Free for Public Use";
    private String scenario = "routinemode"; //first element from List EventIdentification
    private String[] scenarios = {scenario}; //first element from List EventIdentification

    private List<AnnotationType> annot;
    private List<FileEnclosureType> fet;
    private String title;
    private String main_text = "<b>eingestellt durch IRIX-Broker</b>";
    private String ReportId;
    private Element dokpoolmeta; //DOM Element with the full dokpoolmeta information
    private Properties bfsIrixBrokerProperties;

    private boolean success = false;


    public IrixBrokerDokpoolClient(Properties bfsIBP) {
        bfsIrixBrokerProperties = bfsIBP;
    }

    public boolean sendToDocpool(ReportType report) {
        success = false;
        success = readIdentification(report.getIdentification());
        success = readAnnexes(report.getAnnexes());
        success = DocPoolClient();
        return success;
    }

    private boolean readIdentification(IdentificationType ident) {
        boolean success = true;

        List<EventIdentificationType> eid = null;

        setOrganisationReporting(ident.getOrganisationReporting());
        setDateTime(ident.getDateAndTimeOfCreation());
        setReportContext(ident.getReportContext().value());
        if (ident.getConfidentiality() != null) {
            setConfidentiality(ident.getConfidentiality().value());
        }
        setReportId(ident.getReportUUID());

        if (ident.getEventIdentifications() != null) {
            eid = ident.getEventIdentifications().getEventIdentification();
            if (eid.isEmpty()) {
                log.warn("No eventidentification found!!");
                setScenario(scenario);
                success = false;
            } else {
                setScenario(eid.get(0).getValue());
                log.debug("eventidentification filled");
            }
        }

        return success;
    }

    private boolean readAnnexes(AnnexesType annex) {
        boolean success = true;
        /**
         * information for ELAN is only valid if there is one annotation with title and annotation text and file attachment
         * or title and enclosed file attachments. You need a file attachment because the Information Category is only in this element.
         */

        setAnnot(annex.getAnnotation());

        if (annot.isEmpty()) {
            success = false;
        } else {
            setTitle(annot.get(0).getTitle());
            if (annot.get(0).getText().getContent().size() > 0) {
                setMain_text((String) annot.get(0).getText().getContent().get(0));
            }
            // get the DokPool Meta data
            List<Element> el = annot.get(0).getAny();
            dokpoolmeta = el.get(0);
        }
        //get the attached files
        setFet(annex.getFileEnclosure());
        if (fet.isEmpty()) {
            success = false;
        }
        return success;
    }

    private boolean publish(Document d) {
        d.setWorkflowStatus("publish");
        return true;
    }

    private DocumentPool getMydokpool(DocpoolBaseService docpoolBaseService, List<DocumentPool> myDocpools) {
        //TODO use primaryDokpool (of irixauto) or configuration file "ploneDokpool"?
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String ploneDokpool = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOL");

        DocumentPool myDocpool = docpoolBaseService.getPrimaryDocumentPool();
        Element mydokpoolname = extractSingleElement(dokpoolmeta, TAG_DOKPOOLNAME);
        Boolean renewdokpool = true;
        if (renewdokpool && (mydokpoolname != null)) {
            for (DocumentPool sdokpool : myDocpools) {
                if (sdokpool.getFolderPath().matches("/" + ploneSite + "/" + mydokpoolname.getTextContent())) {
                    myDocpool = sdokpool;
                    renewdokpool = false;
                }
            }
        }
        if (renewdokpool) {
            for (DocumentPool sdokpool : myDocpools) {
                if (sdokpool.getFolderPath().matches("/" + ploneSite + "/" + ploneDokpool)) {
                    myDocpool = sdokpool;
                }
            }
        }

        return myDocpool;
    }

    private Folder getMyGroupFolder(DocpoolBaseService docpoolBaseService, DocumentPool myDocpool) {
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String ploneDokpool = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOL");
        String ploneGroupFolder = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_GROUPFOLDER");

        Boolean renewGroupFolder = true;
        Element myDocpoolGroupFolder = extractSingleElement(dokpoolmeta, TAG_DOKPOOLGROUPFOLDER);
        List<DocumentPool> myDocpools = docpoolBaseService.getDocumentPools();
        Folder myGroupFolder = null;
        try {
            myGroupFolder = myDocpool.getFolder(ploneSite + "/" + ploneDokpool + "/content/Groups/" + myDocpoolGroupFolder.getTextContent());
            renewGroupFolder = false;
        } catch (NullPointerException e) {
            // It's fine not to find a groufolder here
            // TODO give warning falling back to system configuration for import
            log.warn("Could not find Groupfolder: " + myDocpoolGroupFolder.getTextContent() + ". Trying systemdefined Groupfolder.");
        }
        if (renewGroupFolder) {
            try {
                myGroupFolder = myDocpool.getFolder(ploneSite + "/" + ploneDokpool + "/content/Groups/" + ploneGroupFolder);
                renewGroupFolder = false;
            } catch (NullPointerException e) {
                // It's fine not to find a groufolder here but suspicious
                // TODO give warning falling back to first available groupfolder for import
                log.warn("Could not find systemdefined Groupfolder: " + ploneGroupFolder + ". Trying first available Groufolder.");
            }
        }
        if (renewGroupFolder && (myDocpools.size() > 0)) {
            try {
                myGroupFolder = myDocpool.getGroupFolders().get(0);
            } catch (NullPointerException e) {
                throw new NullPointerException("Could not find a valid GroupFolder");
            }
        }

        return myGroupFolder;
    }

    private Map<String, Object> setBehaviors() {
        //TODO check if Dokpool supports behaviours before adding them
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> behaviorsList = new ArrayList<String>();
        String[] behaviorsTagList = {TAG_ISDOKSYS, TAG_ISELAN, TAG_ISRODOS, TAG_ISREI};

        for (String behaviorTag : behaviorsTagList) {
            Element element = extractSingleElement(dokpoolmeta, behaviorTag);
            if (element.getTextContent().equalsIgnoreCase("true")) {
                behaviorsList.add(element.getTagName().replaceFirst("^Is", "").toLowerCase());
            }
        }

        /*Element elan = extractSingleElement(dokpoolmeta, TAG_ISELAN);
        if (elan.getTextContent().equalsIgnoreCase("true")) {
            behaviorsList.add("elan");
            //properties.put("scenarios",scenarios);
            //d.update(new HashMap<String, Object>("scenarios", scenarios));
            //TODO other elan specific properties must be added later
        }
        Element rodos = extractSingleElement(dokpoolmeta, TAG_ISRODOS);
        if (rodos.getTextContent().equalsIgnoreCase("true")) {
            behaviorsList.add("rodos");
        }
        Element rei = extractSingleElement(dokpoolmeta, TAG_ISREI);
        if (rei.getTextContent().equalsIgnoreCase("true")) {
            behaviorsList.add("rei");
        }

        Element doksys = extractSingleElement(dokpoolmeta, TAG_ISDOKSYS);
        if (doksys.getTextContent().equalsIgnoreCase("true")) {
            behaviorsList.add("doksys");
        }*/

        if (behaviorsList.size() > 0) {
            properties.put("local_behaviors", behaviorsList);
        }
        return properties;
    }



    private Map<String, Object> setSubjects(){
        Map<String, Object> properties = new HashMap<String, Object>();
        Element purpose = extractSingleElement(dokpoolmeta, TAG_PURPOSE);
        Element network = extractSingleElement(dokpoolmeta, TAG_NETWORKOPERATOR);
        Element stid = extractSingleElement(dokpoolmeta, TAG_SAMPLETYPEID);
        Element st = extractSingleElement(dokpoolmeta, TAG_SAMPLETYPE);
        Element dom = extractSingleElement(dokpoolmeta, TAG_DOM);
        Element dtype = extractSingleElement(dokpoolmeta, TAG_DATATYPE);
        Element lbase = extractSingleElement(dokpoolmeta, TAG_LEGALBASE);
        Element mp = extractSingleElement(dokpoolmeta, TAG_MEASURINGPROGRAM);
        Element status = extractSingleElement(dokpoolmeta, TAG_STATUS);
        Element sbegin = extractSingleElement(dokpoolmeta, TAG_SAMPLINGBEGIN);
        Element send = extractSingleElement(dokpoolmeta, TAG_SAMPLINGEND);

        properties.put("subjects", new String[]{
                "SubjectTest von lem-fr",
                purpose.getTextContent(),
                network.getTextContent(),
                stid.getTextContent(),
                st.getTextContent(),
                dom.getTextContent(),
                dtype.getTextContent(),
                lbase.getTextContent(),
                mp.getTextContent(),
                status.getTextContent(),
                sbegin.getTextContent(),
                send.getTextContent()
        });

        return properties;
    }

    private Map<String, Object> setDoksysProperties(){
        Map<String, Object> doksysProperties = new HashMap<String, Object>();
        //getting the dokpool metainformation by tagname
        //TODO activate dokpoolname and dokpoolfolder
        Element purpose = extractSingleElement(dokpoolmeta, TAG_PURPOSE);
        Element network = extractSingleElement(dokpoolmeta, TAG_NETWORKOPERATOR);
        Element stid = extractSingleElement(dokpoolmeta, TAG_SAMPLETYPEID);
        Element st = extractSingleElement(dokpoolmeta, TAG_SAMPLETYPE);
        Element dom = extractSingleElement(dokpoolmeta, TAG_DOM);
        Element dtype = extractSingleElement(dokpoolmeta, TAG_DATATYPE);
        Element lbase = extractSingleElement(dokpoolmeta, TAG_LEGALBASE);
        Element mp = extractSingleElement(dokpoolmeta, TAG_MEASURINGPROGRAM);
        Element status = extractSingleElement(dokpoolmeta, TAG_STATUS);
        Element sbegin = extractSingleElement(dokpoolmeta, TAG_SAMPLINGBEGIN);
        Element send = extractSingleElement(dokpoolmeta, TAG_SAMPLINGEND);

        doksysProperties.put("purpose", purpose.getTextContent());
        doksysProperties.put("dom", dom.getTextContent());
        doksysProperties.put("lbase", lbase.getTextContent());
        doksysProperties.put("sbegin", sbegin.getTextContent());
        doksysProperties.put("status", status.getTextContent());
        return doksysProperties;
    }

    private Map<String, Object> setElanProperties(DocumentPool myDocpool){
        /** point the new Dokument to active or referenced scenarios of the dokpool
         *
         */
        //TODO support list of scenarios and properties settings as well!
        Map<String, Object> elanProperties = new HashMap<String, Object>();
        Element myElanScenarios = extractSingleElement(dokpoolmeta, TAG_ELANSCENARIOS);
        if (myElanScenarios != null) {
            NodeList myElanScenarioList = myElanScenarios.getElementsByTagName(TAG_ELANSCENARIO);
            List<String> sclist = new ArrayList<String>();
            for (int i = 0; i < myElanScenarioList.getLength(); i++) {
                sclist.add(myElanScenarioList.item(i).getTextContent());
            }
            //addScenariosfromDokpool(mydokpool, mydokpoolscenarios.getTextContent());
            addScenariosfromDokpool(myDocpool, sclist);
        } else {
            addActiveScenariosfromDokpool(myDocpool);
        }
        elanProperties.put("scenarios", scenarios);

        return elanProperties;
    }

    private Map<String, Object> setRodosProperties(){
        Map<String, Object> rodosProperties = new HashMap<String, Object>();
        return rodosProperties;
    }

    private Map<String, Object> setReiProperties(){
        Map<String, Object> reiProperties = new HashMap<String, Object>();
        return reiProperties;
    }

    private boolean DocPoolClient() {
        success = true;

        String proto = bfsIrixBrokerProperties.getProperty("irix-dokpool.PROTO");
        String host = bfsIrixBrokerProperties.getProperty("irix-dokpool.HOST");
        String port = bfsIrixBrokerProperties.getProperty("irix-dokpool.PORT");
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String user = bfsIrixBrokerProperties.getProperty("irix-dokpool.USER");
        String pw = bfsIrixBrokerProperties.getProperty("irix-dokpool.PW");

        String desc = "Original date: " + DateTime.toString() + " " + ReportContext + " " + Confidentiality;

        //connect to Dokpool using API (wsapi4plone/wsapi4elan)
        DocpoolBaseService docpoolBaseService = new DocpoolBaseService(proto + "://" + host + ":" + port + "/" + ploneSite, user, pw);

        // DocumentPool
        List<DocumentPool> myDocpools = docpoolBaseService.getDocumentPools();
        DocumentPool myDocpool = getMydokpool(docpoolBaseService, myDocpools);

        //GroupFolder
        List<Folder> groupFolders = myDocpool.getGroupFolders();
        Folder myGroupFolder = getMyGroupFolder(docpoolBaseService, myDocpool);

        /** hashmap to store the generic dokpool meta data
         *
         */
        Map<String, Object> docpoolProperties = new HashMap<String, Object>();
        docpoolProperties.put("title", title);
        docpoolProperties.put("description", desc);
        docpoolProperties.put("text", main_text);
        Element dt = extractSingleElement(dokpoolmeta, TAG_DOKPOOLCONTENTTYPE);
        docpoolProperties.put("docType", dt.getTextContent());
        docpoolProperties.putAll(setBehaviors());

        Document d = myGroupFolder.createDocument(ReportId, docpoolProperties);
        log.info(d.getTitle());

        // updating document with generic (docpool) properties
        d.update(setSubjects());

        // updating document with doksys specific properties
        Element doksys = extractSingleElement(dokpoolmeta, TAG_ISDOKSYS);
        if (doksys.getTextContent().equalsIgnoreCase("true")) {
            d.update(setDoksysProperties());
            //FIXME  may be for loop needed here?
            /*for (FOO key : doksysProperties.keySet()) {
                //d.setProperty(key, dokpoolProperties.get(key), "string");
                d.update(doksysProperty);
            }*/
        }
        // updating document with elan specific properties
        Element elan = extractSingleElement(dokpoolmeta, TAG_ISELAN);
        if (elan.getTextContent().equalsIgnoreCase("true")) {
            d.update(setElanProperties(myDocpool));
            //TODO other elan specific properties must be added later
        }
        // updating document with elan specific properties
        Element rodos = extractSingleElement(dokpoolmeta, TAG_ISRODOS);
        if (rodos.getTextContent().equalsIgnoreCase("true")) {
            d.update(setRodosProperties());
        }
        // updating document with rei specific properties
        Element rei = extractSingleElement(dokpoolmeta, TAG_ISREI);
        if (rei.getTextContent().equalsIgnoreCase("true")) {
            d.update(setReiProperties());
        }

        // add attachements
        for (int i = 0; i < fet.size(); i++) {
            String t = fet.get(i).getTitle();
            //FIXME path generation URL consistent!!
            String aid = fet.get(i).getFileName();
            log.info("Anhang" + i + ": " + t);
            if (MT_IMAGES.contains(fet.get(i).getMimeType())) {
                d.uploadImage(aid, t, t, fet.get(i).getEnclosedObject(), fet.get(i).getFileName());
            }
            // TODO separate handling of movie files
            else if (MT_MOVIES.contains(fet.get(i).getMimeType())) {
                d.uploadFile(aid, t, t, fet.get(i).getEnclosedObject(), fet.get(i).getFileName());
            } else {
                d.uploadFile(aid, t, t, fet.get(i).getEnclosedObject(), fet.get(i).getFileName());
            }
        }

        if (Confidentiality.equals(ID_CONF)) {
            publish(d);
        }
        return success;
    }

    public void addScenariosfromDokpool(DocumentPool dp) {

        List<Scenario> scen = dp.getScenarios();
        String[] sc = new String[scen.size()];
        for (int i = 0; i < scen.size(); i++)
            sc[i] = scen.get(i).getId();
        setScenarios(sc);
    }

    public void addActiveScenariosfromDokpool(DocumentPool dp) {

        List<Scenario> scen = dp.getScenarios();
        String[] sc = new String[scen.size()];
        for (int i = 0; i < scen.size(); i++) {
            String stat = scen.get(i).getStringAttribute("status");
            if (stat.equals("active"))
                sc[i] = scen.get(i).getId();
        }
        setScenarios(sc);
    }

    public void addScenariosfromDokpool(DocumentPool dp, String myscenario) {

        List<Scenario> scen = dp.getScenarios();
        String[] sc = new String[scen.size()];
        for (int i = 0; i < scen.size(); i++) {
            if (scen.get(i).getId().equals(myscenario)) {
                sc[i] = scen.get(i).getId();
            }
        }
        if (sc.length == 0) {
            sc[0] = "routinemode";
        }
        setScenarios(sc);
    }

    public void addScenariosfromDokpool(DocumentPool dp, List<String> myscenarios) {

        List<Scenario> scen = dp.getScenarios();
        ArrayList<String> sc = new ArrayList<String>();
        for (int i = 0; i < scen.size(); i++) {
            if (myscenarios.contains(scen.get(i).getId())) {
                sc.add(scen.get(i).getId());
            }
        }
        if (sc.size() == 0) {
            sc.add("routinemode");
        }
        setScenarios(sc.toArray((new String[sc.size()])));
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

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
