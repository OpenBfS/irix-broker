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

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.time.ZonedDateTime;

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
import de.bfs.dokpool.client.base.DokpoolBaseService;
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

    public boolean sendToDokpool(ReportType report) throws IrixBrokerException {
        success = false;
        success = readIdentification(report.getIdentification());
        success = readAnnexes(report.getAnnexes());
        try {
            success = prepareDocAndSendToDokpool();
        } catch (Exception e) {
            throw new IrixBrokerException("DokpoolClient() not working as expected: ", e);
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

    /**
     * Gets the document pool corresponding to the ID specified
     * within the IRIX document. If no such pool exists, a pool
     * with ID specified IrixBrokerProperties is returned. If this
     * also does not exist, use the REST users default pool.
     */
    private DocumentPool getMyDocPool(DokpoolBaseService dpService, List<DocumentPool> myDocPools, Element dt) {
        //TODO use primaryDokpool (of irixauto) or configuration file "ploneDokpool"?
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String ploneDokpool = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOL");

        DocumentPool myDocPool = dpService.getPrimaryDocumentPool();
        Element myDocPoolNameElement = extractSingleElement(dokpoolmeta, TAG_DOKPOOLNAME);
        String myDocPoolName = (myDocPoolNameElement != null) ? myDocPoolNameElement.getTextContent() : null;
        boolean specificDocPoolFound = false;
        if (myDocPoolName != null) {
            for (DocumentPool sDocPool : myDocPools) {
                if (sDocPool.getId().equals(myDocPoolName)) {
                    myDocPool = sDocPool;
                    specificDocPoolFound = true;
                }
            }
        }
        if (!specificDocPoolFound) {
            for (DocumentPool sDocPool : myDocPools) {
                if (sDocPool.getId().equals(ploneDokpool)) {
                    myDocPool = sDocPool;
                }
            }
        }

        return myDocPool;
    }

/*    private DocumentPool getMyDocPool(DokpoolBaseService dpService, List<DocumentPool> myDocPools) {
        Element dt = new Element;
        return getMyDocPool(dpService, myDocPools, dt);
    }*/

    private Folder getMyGroupFolder(DokpoolBaseService dpService, DocumentPool myDocPool) {
        String ploneSite = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
        String ploneDokpool = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOL");
        String ploneGroupFolder = bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_GROUPFOLDER");

        Element myDocPoolGroupFolder = extractSingleElement(dokpoolmeta, TAG_DOKPOOLGROUPFOLDER);
        List<DocumentPool> myDocPools = dpService.getDocumentPools();
        Folder myGroupFolder = null;
        boolean folderFound = false;
        if (myDocPoolGroupFolder  == null) {
            log.log(WARNING, "Could not find Groupfolder in dokpoolmeta. Trying systemdefined Groupfolder.");
        } else {
            try {
                //myGroupFolder = myDocPool.getFolder(ploneSite + "/" + ploneDokpool + "/content/Groups/" + myDocPoolGroupFolder.getTextContent());
                myGroupFolder = myDocPool.getFolder("content/Groups/" + myDocPoolGroupFolder.getTextContent());
                folderFound = true;
            } catch (NullPointerException e) {
                // It's fine not to find a groufolder here
                // TODO give warning falling back to system configuration for import
                log.log(WARNING, "Could not find Groupfolder: " + myDocPoolGroupFolder.getTextContent() + ". Trying systemdefined Groupfolder.");
            }
        }
        if (!folderFound) {
            try {
                myGroupFolder = myDocPool.getFolder("content/Groups/" + ploneGroupFolder);
                folderFound = true;
            } catch (NullPointerException e) {
                // It's fine not to find a groufolder here but suspicious
                // TODO give warning falling back to first available groupfolder for import
                log.log(WARNING, "Could not find systemdefined Groupfolder: " + ploneGroupFolder + ". Trying first available Groupfolder.");
            }
        }
        if (!folderFound && (myDocPools.size() > 0)) {
            try {
                myGroupFolder = myDocPool.getGroupFolders().get(0);
            } catch (NullPointerException e) {
                throw new NullPointerException("Could not find a valid GroupFolder");
            }
        }

        return myGroupFolder;
    }

    /**
     * Turns all active behaviors in the IRIX document's DokpoolMeta
     * into a list of behaviors. "<IsBehavior>true</IsBehavior>" becomes the
     * list element "behavior". Behaviors are only added if the app is
     * supported by the current document pool.
     * @return Map where the only entry is the list of behaviors with key "local_behaviors".
     */
    private Map<String, Object> behaviors(DocumentPool documentPool) {
        List<String> docPoolBehaviors = documentPool.getSupportedApps();
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> behaviorsList = new ArrayList<String>();
        String[] behaviorsTagList = {TAG_ISELAN, TAG_ISRODOS, TAG_ISREI, TAG_ISDOKSYS};
        for (String behaviorTag : behaviorsTagList) {
            Element element = extractSingleElement(dokpoolmeta, behaviorTag);
            if (element != null && element.getTextContent().equalsIgnoreCase("true")) {
                String behavior = element.getTagName().replaceFirst("^Is", "").toLowerCase();
                if (docPoolBehaviors.contains(behavior)) {
                    behaviorsList.add(behavior);
                }
            }
        }

        if (behaviorsList.size() > 0) {
            properties.put("local_behaviors", behaviorsList);
        }
        return properties;
    }

    /**
     * Extract all subjects from the IRIX document and turn them into a list.
     * @return Map where the list is the only entry with key "subjects".
     */
    private Map<String, Object> subjects() {
        Map<String, Object> properties = new HashMap<String, Object>();
        List<String> propertiesList = new ArrayList<String>();
        appendDoksysSubjects(propertiesList);
        appendElanSubjects(propertiesList);
        appendRodosSubjects(propertiesList);
        appendReiSubjects(propertiesList);

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

    private void appendDoksysSubjects(List<String> propertiesList) {

        Element doksysmeta = extractSingleElement(dokpoolmeta, TAG_DOKSYS);
        //Element foo = extractSingleElement(doksysmeta, TAG_FOO);
        //propertiesList.add("no DOKSYS subjects");
    }

    private void appendElanSubjects(List<String> propertiesList) {

        Element elanmeta = extractSingleElement(dokpoolmeta, TAG_ELAN);
        //Element foo = extractSingleElement(elanmeta, TAG_FOO);
        //propertiesList.add("no ELAN subjects");
    }

    private void appendRodosSubjects(List<String> propertiesList) {

        Element rodosmeta = extractSingleElement(dokpoolmeta, TAG_RODOS);
        //Element foo = extractSingleElement(rodosmeta, TAG_FOO);
        //propertiesList.add("no RODOS subjects");
    }

    private void appendReiSubjects(List<String> propertiesList) {

        Element reimeta = extractSingleElement(dokpoolmeta, TAG_REI);
        //Element foo = extractSingleElement(reimeta, TAG_FOO);
        //propertiesList.add("no REI subjects");
    }

    /**
     * Extracts the Doksys properties from the DokpoolMeta element.
     * @return doksys properties as a Map.
     */
    private Map<String, Object> doksysProperties() {
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
            } catch (Exception gce) {
                log.log(ERROR, gce);
            }
            if (tagElement != null) {
                if (doksysDatetimeTagList.contains(tag)) {
                    try {
                        String value = tagElement.getTextContent();
                        ZonedDateTime zdt = ZonedDateTime.parse(value);
                        GregorianCalendar gcalval = GregorianCalendar.from(zdt);
                        doksysProperties.put(tag, gcalval.getTime());
                    } catch (Exception gce) {
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
            } catch (Exception gce) {
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

    /**
     * Extracts the ELAN properties from the DokpoolMeta element.
     * @return ELAN properties as a Map.
     */
    private Map<String, Object> elanProperties(DocumentPool myDocPool) {
        /** point the new Dokument to active or referenced scenarios of the dokpool
         * if scenarios are referenced in request: add those that exist in Dokpool/ELAN
         * and are active. If no scenarios are referenced in the request add all active
         * scenarios
         */
        Map<String, Object> elanProperties = new HashMap<String, Object>();
        Element elanmeta = extractSingleElement(dokpoolmeta, TAG_ELAN);
        if (elanmeta != null) {
        }
        return elanProperties;
    }

    /**
     * Extracts the ELAN events from the DokpoolMeta element
     * and assigns them to the document. If there are to events,
     * all active events from the documents dokpool are assigned.
     * @param d The document to which the events are assigned.
     * @return ELAN properties as a Map.
     */
    private void assignEvents(Document d) {
        /** point the new Dokument to active or referenced scenarios of the dokpool
         * if scenarios are referenced in request: add those that exist in Dokpool/ELAN
         * and are active. If no scenarios are referenced in the request add all active
         * scenarios
         */
        Element elanmeta = extractSingleElement(dokpoolmeta, TAG_ELAN);
        if (elanmeta != null) {
            Element myElanEvents = extractSingleElement(elanmeta, TAG_ELANEVENTS);
            NodeList myElanEventList = extractElementNodelist(elanmeta, TAG_ELANEVENT);
            if (myElanEventList != null) {
                List<String> evlist = new ArrayList<String>();
                for (int i = 0; i < myElanEventList.getLength(); i++) {
                    evlist.add(myElanEventList.item(i).getTextContent());
                }
                List<String> assigned = d.assignEventIdsUids(evlist);
                if (assigned.isEmpty()) {
                    d.assignEventIdsUids(List.of("routinemode"));
                }
            } else if (myElanEvents != null) {
                NodeList myElanEventsList = myElanEvents.getChildNodes();
                List<String> evlist = new ArrayList<String>();
                for (int i = 0; i < myElanEventsList.getLength(); i++) {
                    evlist.add(myElanEventsList.item(i).getTextContent());
                }
                List<String> assigned = d.assignEventIdsUids(evlist);
                if (assigned.isEmpty()) {
                    d.assignEventIdsUids(List.of("routinemode"));
                }
            } else {
                d.assignAllActiveEvents();
            }
        }
    }

    /**
     * Extracts the RODOS properties from the DokpoolMeta element.
     * @return RODOS properties as a Map.
     */
    private Map<String, Object> rodosProperties() {
        Map<String, Object> rodosProperties;
        Element rodosmeta = extractSingleElement(dokpoolmeta, TAG_RODOS);
        if (rodosmeta != null) {
            rodosProperties = extractChildElementsAsMap(rodosmeta);
        } else {
            rodosProperties = new HashMap<String, Object>();
        }
        return rodosProperties;
    }

    /**
     * Extracts the REI properties from the DokpoolMeta element.
     * @return REI properties as a Map.
     */
    private Map<String, Object> reiProperties() {
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
        String[] reiListTagList = {
                TAG_NUCLEARINSTALLATION,
                TAG_REILEGALBASE,
                TAG_ORIGIN
        };
        String[] reiSpecTagList = {
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

    /**
     * Creators as specified by IrixBokerProperties and
     * and contained in DcoumentOwner Element of the IRIX document.
     * @return Map with "creators" list as only entry.
     */
    private Map<String, Object> creators(DocumentPool documentPool) {
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

    /**
     * Prepares a Dokpool document from the IRIX document and
     * sends it to the Dokpool instance specified by
     * IrixBrokerProperties.
     * @return true iff sending succeeded.
     */
    private boolean prepareDocAndSendToDokpool() throws IrixBrokerException {
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
        DokpoolBaseService dpService = new DokpoolBaseService(proto + "://" + host + ":" + port + "/" + ploneSite, user, pw);
        // DocumentPool
        List<DocumentPool> myDocPools = dpService.getDocumentPools();
        DocumentPool myDocPool = getMyDocPool(dpService, myDocPools, dt);
        //GroupFolder
        List<Folder> groupFolders = myDocPool.getGroupFolders();
        Folder myGroupFolder = getMyGroupFolder(dpService, myDocPool);
        // hashmap to store the generic dokpool meta data
        Map<String, Object> docProperties = new HashMap<String, Object>();
        docProperties.put("title", title);
        docProperties.put("description", desc);
        docProperties.put("text", main_text);
        //WIP FIXME - here the problem seems to start or show up
        docProperties.put("docType", dt.getTextContent());
        docProperties.putAll(behaviors(myDocPool));
        docProperties.putAll(subjects());
        log.log(INFO, "Creating new Dokument in " + myGroupFolder.getFolderPath());
        Document d = myGroupFolder.createDPDocument(ReportId, docProperties);
        // updating document with doksys specific properties
        Element doksys = extractSingleElement(dokpoolmeta, TAG_ISDOKSYS);
        if (doksys != null && doksys.getTextContent().equalsIgnoreCase("true")) {
            d.update(doksysProperties());
        }
        // updating document with elan specific properties (inlc. events)
        Element elan = extractSingleElement(dokpoolmeta, TAG_ISELAN);
        if (elan != null && elan.getTextContent().equalsIgnoreCase("true")) {
            Map<String,Object> elanProp = elanProperties(myDocPool);
            if (!elanProp.isEmpty()) {
                d.update(elanProperties(myDocPool));
            }
            assignEvents(d);
        }
        // updating document with rodos specific properties
        Element rodos = extractSingleElement(dokpoolmeta, TAG_ISRODOS);
        if (rodos != null && rodos.getTextContent().equalsIgnoreCase("true")) {
            d.update(rodosProperties());
        }
        // updating document with rei specific properties
        Element rei = extractSingleElement(dokpoolmeta, TAG_ISREI);
        if (rei != null && rei.getTextContent().equalsIgnoreCase("true")) {
            d.update(reiProperties());
        }
        //DokpoolOwner to be used to change ownership of an created document
        Element dokpoolDocumentOwner = extractSingleElement(dokpoolmeta, TAG_DOKPOOLDOCUMENTOWNER);
        /*if (dokpoolDocumentOwner != null && !dokpoolDocumentOwner.getTextContent().equals("")) {
            d.update(creators(myDocPool));
        }*/
        d.update(creators(myDocPool));
        // add attachements
        for (int i = 0; i < fet.size(); i++) {
            String t = fet.get(i).getTitle();
            String aid = fet.get(i).getFileName();
            try {
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
                } else if (MT_MOVIES.contains(mimeType)) { // TODO separate handling of movie files
                    d.uploadFile(afn, t, t, fet.get(i).getEnclosedObject(), aid, mimeType);
                } else {
                    d.uploadFile(afn, t, t, fet.get(i).getEnclosedObject(), aid, mimeType);
                }
            } catch (UnsupportedEncodingException uee) {
                throw new IrixBrokerException("Could not Upload Attachement: ", uee);
            }
        }

        if (Confidentiality.equals(ID_CONF)) {
            publish(d);
        }
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

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
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

    public static Map<String, Object> extractChildElementsAsMap(Element parent) {
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

}
