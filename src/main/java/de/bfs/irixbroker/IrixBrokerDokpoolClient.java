/**
 * @authors bp-fr, lem-fr - German Federal Office for Radiation Protection www.bfs.de
 */

package de.bfs.irixbroker;

import java.io.UnsupportedEncodingException;
import java.util.*;
import java.time.ZonedDateTime;

import java.lang.NullPointerException;

import java.net.URLEncoder;

//still part of Java 21
import javax.xml.datatype.XMLGregorianCalendar;

import de.bfs.irix.extensions.dokpool.DokpoolMeta;
import static java.lang.System.Logger.Level.DEBUG;
import static java.lang.System.Logger.Level.WARNING;
import static java.lang.System.Logger.Level.ERROR;
import static java.lang.System.Logger.Level.INFO;
import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.iaea._2012.irix.format.identification.EventIdentificationType;
import org.iaea._2012.irix.format.identification.IdentificationType;

import de.bfs.dokpool.client.content.Document;
import de.bfs.dokpool.client.content.DocumentPool;
import de.bfs.dokpool.client.content.Event;
import de.bfs.dokpool.client.base.DocpoolBaseService;
import de.bfs.dokpool.client.content.Folder;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class IrixBrokerDokpoolClient implements IrixBrokerDokpoolXMLNames {

    private static System.Logger log = System.getLogger(IrixBrokerDokpoolClient.class.getName());

    private String OrganisationReporting;
    private XMLGregorianCalendar DateTime;
    private String ReportContext;
    private String Confidentiality = "Free for Public Use";
    private String event = "routinemode"; //first element from List EventIdentification
    private String[] events = {event}; //first element from List EventIdentification

    private List<AnnotationType> annot;
    private List<FileEnclosureType> fet;
    private String title;
    private String main_text = "<b>eingestellt durch IRIX-Broker</b>";
    private String ReportId;
    private Element dokpoolmeta; //DOM Element with the full dokpoolmeta information
    private DokpoolMeta dokpoolMeta;
    private Properties bfsIrixBrokerProperties;

    private boolean success = false;


    public IrixBrokerDokpoolClient(Properties bfsIBP) {
        bfsIrixBrokerProperties = bfsIBP;
    }

    public boolean sendToDocpool(ReportType report) throws  IrixBrokerException {
        success = false;
        success = readIdentification(report.getIdentification());
        success = readAnnexes(report.getAnnexes());
        try {
            success = DocPoolClient();
        } catch (Exception e){
            throw new IrixBrokerException( "DocPoolClient() not working as expected: ", e);
        }
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
                log.log(WARNING, "No eventidentification found!!");
                setEvent(event);
                success = false;
            } else {
                setEvent(eid.get(0).getValue());
                log.log(DEBUG, "eventidentification filled");
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

    private DocumentPool getMyDocpool(DocpoolBaseService docpoolBaseService, List<DocumentPool> myDocpools, Element dt) {
        //TODO use primaryDokpool (of irixauto) or configuration file "ploneDokpool"?
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String ploneDokpool = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOL");

        DocumentPool myDocpool = docpoolBaseService.getPrimaryDocumentPool();
        Element myDocpoolName = extractSingleElement(dokpoolmeta, TAG_DOKPOOLNAME);
        Boolean renewDocpool = true;
        if (renewDocpool && (myDocpoolName != null)) {
            for (DocumentPool sDocpool : myDocpools) {
                if (sDocpool.getFolderPath().matches("/" + ploneSite + "/" + myDocpoolName.getTextContent())) {
                    myDocpool = sDocpool;
                    renewDocpool = false;
                }
            }
        }
        if (renewDocpool) {
            for (DocumentPool sDocpool : myDocpools) {
                if (sDocpool.getFolderPath().matches("/" + ploneSite + "/" + ploneDokpool)) {
                    myDocpool = sDocpool;
                }
            }
        }

        return myDocpool;
    }

/*    private DocumentPool getMyDocpool(DocpoolBaseService docpoolBaseService, List<DocumentPool> myDocpools) {
        Element dt = new Element;
        return getMyDocpool(docpoolBaseService, myDocpools, dt);
    }*/

    private Folder getMyGroupFolder(DocpoolBaseService docpoolBaseService, DocumentPool myDocpool) {
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String ploneDokpool = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOL");
        String ploneGroupFolder = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_GROUPFOLDER");

        Boolean renewGroupFolder = true;
        Element myDocpoolGroupFolder = extractSingleElement(dokpoolmeta, TAG_DOKPOOLGROUPFOLDER);
        List<DocumentPool> myDocpools = docpoolBaseService.getDocumentPools();
        Folder myGroupFolder = null;
        if (myDocpoolGroupFolder  == null) {
            log.log(WARNING, "Could not find Groupfolder in dokpoolmeta. Trying systemdefined Groupfolder.");
        } else {
            try {
                //myGroupFolder = myDocpool.getFolder(ploneSite + "/" + ploneDokpool + "/content/Groups/" + myDocpoolGroupFolder.getTextContent());
                myGroupFolder = myDocpool.getFolder("content/Groups/" + myDocpoolGroupFolder.getTextContent());
                renewGroupFolder = false;
            } catch (NullPointerException e) {
                // It's fine not to find a groufolder here
                // TODO give warning falling back to system configuration for import
                log.log(WARNING, "Could not find Groupfolder: " + myDocpoolGroupFolder.getTextContent() + ". Trying systemdefined Groupfolder.");
            }
        }
        if (renewGroupFolder) {
            try {
                myGroupFolder = myDocpool.getFolder("content/Groups/" + ploneGroupFolder);
                renewGroupFolder = false;
            } catch (NullPointerException e) {
                // It's fine not to find a groufolder here but suspicious
                // TODO give warning falling back to first available groupfolder for import
                log.log(WARNING, "Could not find systemdefined Groupfolder: " + ploneGroupFolder + ". Trying first available Groupfolder.");
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

    private List <String> getBehaviors(DocumentPool docpool) {
        List<String> behaviorList = new ArrayList();
        // TODO get this working
        return behaviorList;
    }


    private Map<String, Object> setBehaviors(DocumentPool documentPool) {
        //TODO check if Dokpool supports behaviours before adding them
        List <String> docpoolBehaviors = getBehaviors(documentPool);
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> behaviorsList = new ArrayList<String>();
        //String[] behaviorsTagList = {TAG_ISDOKSYS, TAG_ISELAN, TAG_ISRODOS, TAG_ISREI};
        String[] behaviorsTagList = {TAG_ISELAN, TAG_ISRODOS, TAG_ISREI, TAG_ISDOKSYS};
        //FIXME allow doksys as well - breaks Dokpool at the moment!
        for (String behaviorTag : behaviorsTagList) {
            Element element = extractSingleElement(dokpoolmeta, behaviorTag);
            if (element != null && element.getTextContent().equalsIgnoreCase("true")) {
                String behavior = element.getTagName().replaceFirst("^Is", "").toLowerCase();
                behaviorsList.add(behavior);
            }
        }


        if (behaviorsList.size() > 0) {
            properties.put("local_behaviors", behaviorsList);
        }
        return properties;
    }


    private Map<String, Object> setSubjects(){
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> propertiesList = new ArrayList<String>();
        getDoksysSubjects(propertiesList);
        getElanSubjects(propertiesList);
        getRodosSubjects(propertiesList);
        getReiSubjects(propertiesList);

        Element mySubjects = extractSingleElement(dokpoolmeta, TAG_SUBJECTS);
        //Map<String, Object> dokpoolSubjects = new HashMap<String, Object>();
        if (mySubjects != null) {
            NodeList mySubjectsList = mySubjects.getChildNodes();
            for (int i = 0; i <  mySubjectsList.getLength(); i++) {
                String mySubjectContent = mySubjectsList.item(i).getTextContent().trim();
                if (!mySubjectContent.isEmpty()) {
                    propertiesList.add(mySubjectContent);
                }
            }
        }
        NodeList mySubjectList = dokpoolmeta.getElementsByTagName(TAG_SUBJECT);
        for (int i = 0; i <  mySubjectList.getLength(); i++) {
            String mySubjectContent = mySubjectList.item(i).getTextContent().trim();
            if (!mySubjectContent.isEmpty()) {
                propertiesList.add(mySubjectContent);
            }
        }

        properties.put("subjects", propertiesList);
        return properties;
    }

    private void getDoksysSubjects(List<String> propertiesList){

        Element doksysmeta = extractSingleElement(dokpoolmeta, TAG_DOKSYS);
        //Element foo = extractSingleElement(doksysmeta, TAG_FOO);
        //propertiesList.add("no DOKSYS subjects");
    }

    private void getElanSubjects(List<String> propertiesList){

        Element elanmeta = extractSingleElement(dokpoolmeta, TAG_ELAN);
        //Element foo = extractSingleElement(elanmeta, TAG_FOO);
        //propertiesList.add("no ELAN subjects");
    }

    private void getRodosSubjects(List<String> propertiesList){

        Element rodosmeta = extractSingleElement(dokpoolmeta, TAG_RODOS);
        //Element foo = extractSingleElement(rodosmeta, TAG_FOO);
        //propertiesList.add("no RODOS subjects");
    }

    private void getReiSubjects(List<String> propertiesList){

        Element reimeta = extractSingleElement(dokpoolmeta, TAG_REI);
        //Element foo = extractSingleElement(reimeta, TAG_FOO);
        //propertiesList.add("no REI subjects");
    }

    private Map<String, Object> setDoksysProperties(){
        Map<String, Object> doksysProperties = new HashMap<String, Object>();
        Element doksysmeta = extractSingleElement(dokpoolmeta, TAG_DOKSYS);
        String[] doksysSingleTagList = {
                TAG_AREA,
                TAG_SAMPLINGBEGIN,
                TAG_SAMPLINGEND,
                TAG_INFOTYPE,
                TAG_STATUS,
                TAG_DURATION,
                TAG_OPERATIONMODE,
                TAG_TRAJECTORYSTARTLOCATION,
                TAG_TRAJECTORYENDLOCATION,
                TAG_TRAJECTORYSTARTTIME,
                TAG_TRAJECTORYENDTIME
        };
        String[] doksysListTagList = {
                TAG_PURPOSE,
                TAG_NETWORKOPERATOR,
                TAG_SAMPLETYPE,
                TAG_DOM,
                TAG_DATASOURCE,
                TAG_LEGALBASE,
                TAG_MEASURINGPROGRAM,
                TAG_MEASUREMENTCATEGORY,
        };
        List<String> doksysDatetimeTagList = Arrays.asList(
                TAG_SAMPLINGBEGIN,
                TAG_SAMPLINGEND,
                TAG_TRAJECTORYSTARTTIME,
                TAG_TRAJECTORYENDTIME
        );

        for (String tag: doksysSingleTagList) {
            log.log(DEBUG, tag);
            Element tagElement = null;
            try {
                tagElement = extractSingleElement(doksysmeta, tag);
            } catch(Exception gce) {
                log.log(ERROR, gce);
            }
            if (tagElement != null) {
                if (doksysDatetimeTagList.contains(tag)) {
                    try {
                        String value = tagElement.getTextContent();
                        ZonedDateTime zdt = ZonedDateTime.parse(value);
                        GregorianCalendar gcalval = GregorianCalendar.from(zdt);
                        doksysProperties.put(tag, gcalval.getTime());
                    } catch(Exception gce) {
                        log.log(ERROR, gce);
                    }
                } else {
                    doksysProperties.put(tag, tagElement.getTextContent());
                }
            }
        }
        for (String tag: doksysListTagList) {
            log.log(DEBUG, tag);
            NodeList tagElements = null;
            try {
                tagElements = extractElementNodelist(doksysmeta, tag);
            } catch(Exception gce) {
                log.log(ERROR, gce);
            }
            if (tagElements != null) {
                List<String> telist = new ArrayList<String>();
                for (int i = 0; i < tagElements.getLength(); i++) {
                    telist.add(tagElements.item(i).getTextContent());
                }
                doksysProperties.put(tag, telist);
            }
        }

        return doksysProperties;
    }

    private Map<String, Object> setElanProperties(DocumentPool myDocpool){
        /** point the new Dokument to active or referenced scenarios of the dokpool
         * if scenarios are referenced in request: add those that exist in Dokpool/ELAN
         * and are active. If no scenarios are referenced in the request add all active
         * scenarios
         */
        Map<String, Object> elanProperties = new HashMap<String, Object>();
        Element elanmeta = extractSingleElement(dokpoolmeta, TAG_ELAN);
        if (elanmeta != null) {
            Element myElanEvents = extractSingleElement(elanmeta, TAG_ELANEVENTS);
            NodeList myElanEventList = extractElementNodelist(elanmeta, TAG_ELANEVENT);
            if (myElanEventList != null) {
                List<String> evlist = new ArrayList<String>();
                for (int i = 0; i < myElanEventList.getLength(); i++) {
                    evlist.add(myElanEventList.item(i).getTextContent());
                }
                addEventsfromDokpool(myDocpool, evlist);
            } else if (myElanEvents != null) {
                NodeList myElanEventsList = myElanEvents.getChildNodes();
                List<String> evlist = new ArrayList<String>();
                for (int i = 0; i < myElanEventsList.getLength(); i++) {
                    evlist.add(myElanEventsList.item(i).getTextContent());
                }
                addEventsfromDokpool(myDocpool, evlist);
            } else {
                addActiveEventsfromDokpool(myDocpool);
            }
        }
        elanProperties.put("scenarios", events);
        elanProperties.put("events", events);
        return elanProperties;
    }

    private Map<String, Object> setRodosProperties(){
        Map<String, Object> rodosProperties;
        Element rodosmeta = extractSingleElement(dokpoolmeta, TAG_RODOS);
        if (rodosmeta != null) {
            rodosProperties = extractChildElementsAsMap(rodosmeta);
        } else {
            rodosProperties = new HashMap<String, Object>();
        }
        return rodosProperties;
    }

    private Map<String, Object> setReiProperties(){
        Map<String, Object> reiProperties = new HashMap<String, Object>();
        Element reimeta = extractSingleElement(dokpoolmeta, TAG_REI);
        String[] reiTagList = {
                TAG_REVISION,
                TAG_YEAR,
                TAG_PERIOD,
                TAG_MEDIUM,
                TAG_AUTHORITY,
                TAG_PDFVERSION
        };
        String [] reiListTagList = {
                TAG_NUCLEARINSTALLATION,
                TAG_REILEGALBASE,
                TAG_ORIGIN
        };
        String [] reiSpecTagList = {
                TAG_MST
        };
        for (String tag: reiSpecTagList) {
            Element tagElement = extractSingleElement(reimeta, tag);
            if (tagElement != null) {
                NodeList myReiTagList = tagElement.getChildNodes();
                List<String> telist = new ArrayList<String>();
                for (int i = 0; i < myReiTagList.getLength(); i++) {
                    // FIXME add support for MStName
                    Node myMSt = myReiTagList.item(i);
                    if (myMSt != null) {
                        NodeList myMStList = myMSt.getChildNodes();
                        telist.add(myMStList.item(0).getTextContent());
                    }
                }
                reiProperties.put(tagElement.getTagName(), telist);
            }
        }
        for (String tag: reiTagList) {
            Element tagElement = extractSingleElement(reimeta, tag);
            if (tagElement != null) {
                reiProperties.put(tag, tagElement.getTextContent());
            }
        }
        for (String tag: reiListTagList) {
            Element tagElement = extractSingleElement(reimeta, tag);
            if (tagElement != null) {
                NodeList myReiTagList = tagElement.getChildNodes();
                List<String> telist = new ArrayList<String>();
                for (int i = 0; i < myReiTagList.getLength(); i++) {
                    telist.add(myReiTagList.item(i).getTextContent());
                }
                reiProperties.put(tagElement.getTagName(), telist);
            }
        }
        return reiProperties;
    }

    private Map<String, Object> setCreators(DocumentPool documentPool){
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> creatorsList = new ArrayList<String>();
        // add irix system user for imports as default
        creatorsList.add(bfsIrixBrokerProperties.getProperty("irix-dokpool.USER"));
        Element myDocumentOwner = extractSingleElement(dokpoolmeta, TAG_DOKPOOLDOCUMENTOWNER);
        if (myDocumentOwner != null) {
            creatorsList.add(myDocumentOwner.getTextContent());
        }
        properties.put("creators", creatorsList);
        return properties;
    }


    private boolean DocPoolClient() throws IrixBrokerException {
        success = true;
        String proto = bfsIrixBrokerProperties.getProperty("irix-dokpool.PROTO");
        String host = bfsIrixBrokerProperties.getProperty("irix-dokpool.HOST");
        String port = bfsIrixBrokerProperties.getProperty("irix-dokpool.PORT");
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String documentOwner = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOLDOCUMENTOWNER");
        String user = bfsIrixBrokerProperties.getProperty("irix-dokpool.USER");
        String pw = bfsIrixBrokerProperties.getProperty("irix-dokpool.PW");
        Element dt = extractSingleElement(dokpoolmeta, TAG_DOKPOOLCONTENTTYPE);
        //FIXME remove this static String
        String desc = "Original date: " + DateTime.toString() + " " + ReportContext + " " + Confidentiality;

        //connect to Dokpool using API (wsapi4plone/wsapi4elan)
        DocpoolBaseService docpoolBaseService = new DocpoolBaseService(proto + "://" + host + ":" + port + "/" + ploneSite, user, pw);
        // DocumentPool
        List<DocumentPool> myDocpools = docpoolBaseService.getDocumentPools();
        DocumentPool myDocpool = getMyDocpool(docpoolBaseService, myDocpools, dt);
        //GroupFolder
        List<Folder> groupFolders = myDocpool.getGroupFolders();
        Folder myGroupFolder = getMyGroupFolder(docpoolBaseService, myDocpool);
        // hashmap to store the generic dokpool meta data
        Map<String, Object> docpoolProperties = new HashMap<String, Object>();
        docpoolProperties.put("title", title);
        docpoolProperties.put("description", desc);
        docpoolProperties.put("text", main_text);
        //WIP FIXME - here the problem seems to start or show up
        docpoolProperties.put("docType", dt.getTextContent());
        docpoolProperties.putAll(setBehaviors(myDocpool));
        docpoolProperties.putAll(setSubjects());
        log.log(INFO, "Creating new Dokument in " + myGroupFolder.getFolderPath());
        Document d = myGroupFolder.createDPDocument(ReportId, docpoolProperties);
        // updating document with doksys specific properties
        Element doksys = extractSingleElement(dokpoolmeta, TAG_ISDOKSYS);
        if (doksys != null && doksys.getTextContent().equalsIgnoreCase("true")) {
            d.update(setDoksysProperties());
        }
        // updating document with elan specific properties
        Element elan = extractSingleElement(dokpoolmeta, TAG_ISELAN);
        if (elan != null && elan.getTextContent().equalsIgnoreCase("true")) {
            d.update(setElanProperties(myDocpool));
        }
        // updating document with rodos specific properties
        Element rodos = extractSingleElement(dokpoolmeta, TAG_ISRODOS);
        if (rodos != null && rodos.getTextContent().equalsIgnoreCase("true")) {
            d.update(setRodosProperties());
        }
        // updating document with rei specific properties
        Element rei = extractSingleElement(dokpoolmeta, TAG_ISREI);
        if (rei != null && rei.getTextContent().equalsIgnoreCase("true")) {
            d.update(setReiProperties());
        }
        //DokpoolOwner to be used to change ownership of an created document
        Element dokpoolDocumentOwner = extractSingleElement(dokpoolmeta, TAG_DOKPOOLDOCUMENTOWNER);
        /*if (dokpoolDocumentOwner != null && !dokpoolDocumentOwner.getTextContent().equals("")) {
            d.update(setCreators(myDocpool));
        }*/
        d.update(setCreators(myDocpool));
        // add attachements
        for (int i = 0; i < fet.size(); i++) {
            String t = fet.get(i).getTitle();
            String aid = fet.get(i).getFileName();
            try{
                String afnurl = URLEncoder.encode(fet.get(i).getFileName(), "UTF-8");
                String afn = fet.get(i).getFileName()
                        .replaceAll("[^\\x00-\\x7F]", "")
                        .replace("(", "")
                        .replace(")", "")
                        .replace(" ", "")
                        .replace("/", "");
                log.log(INFO, "Anhang" + i + ": " + t);
                String mimeType = fet.get(i).getMimeType();
                if (MT_IMAGES.contains(mimeType)) {
                    d.uploadImage(afn, t, t, fet.get(i).getEnclosedObject(), aid, mimeType);
                }
                // TODO separate handling of movie files
                else if (MT_MOVIES.contains(mimeType)) {
                    d.uploadFile(afn, t, t, fet.get(i).getEnclosedObject(), aid, mimeType);
                } else {
                    d.uploadFile(afn, t, t, fet.get(i).getEnclosedObject(), aid, mimeType);
                }
            } catch (UnsupportedEncodingException uee){
                throw new IrixBrokerException("Could not Upload Attachement: ", uee);
            }
        }

        if (Confidentiality.equals(ID_CONF)) {
            publish(d);
        }
        return success;
    }

    public void addEventsfromDokpool(DocumentPool dp) {
        List<Event> events = dp.getEvents();
        String[] ev = new String[events.size()];
        for (int i = 0; i < events.size(); i++)
            ev[i] = events.get(i).getId();
        setEvents(ev);
    }

    public void addActiveEventsfromDokpool(DocumentPool dp) {
        List<Event> events = dp.getActiveEvents();
        String[] ev = new String[events.size()];
        for (int i = 0; i < events.size(); i++) {
            ev[i] = events.get(i).getId();
        }
        setEvents(ev);
    }

    public void addEventsfromDokpool(DocumentPool dp, String myevent) {
        List<Event> events = dp.getEvents();
        String[] ev = new String[events.size()];
        for (int i = 0; i < events.size(); i++) {
            if (events.get(i).getId().equals(myevent)) {
                ev[i] = events.get(i).getId();
            }
        }
        if (ev.length == 0) {
            ev[0] = "routinemode";
        }
        setEvents(ev);
    }

    public void addEventsfromDokpool(DocumentPool dp, List<String> myevents) {
        List<Event> events = dp.getEvents();
        ArrayList<String> ev = new ArrayList<String>();
        for (int i = 0; i < events.size(); i++) {
            if (myevents.contains(events.get(i).getId())) {
                ev.add(events.get(i).getId());
            }
        }
        if (ev.size() == 0) {
            ev.add("routinemode");
        }
        setEvents(ev.toArray((new String[ev.size()])));
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

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String[] getEvents() {
        return events;
    }

    public void setEvents(String[] events) {
        this.events = events;
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
        // Kindelement mit dem gewünschten Tag-Namen abholen - does not support NS!
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

    public static NodeList extractElementNodelist(Element element, String elementChildName) {
        // Kindelement mit dem gewünschten Tag-Namen abholen - does not support NS!
        final NodeList childElements = element.getElementsByTagName(elementChildName);
        if (childElements.getLength() > 0) {
            return childElements;
        } else  {
            return null;
        }
    }

    public static Map<String,Object> extractChildElementsAsMap(Element parent) {
        Map<String, Object> childrenMap = new HashMap<String, Object>();
        NodeList childNodes = parent.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node chNode = childNodes.item(i);
            if (chNode.getNodeType() == Node.ELEMENT_NODE) {
                //for element nodes:
                //getNodeName() == getTagName()
                //getTextContent() == concatenation of (text node) children
                childrenMap.put(chNode.getNodeName(), chNode.getTextContent());
            }
        }
        return childrenMap;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

}
