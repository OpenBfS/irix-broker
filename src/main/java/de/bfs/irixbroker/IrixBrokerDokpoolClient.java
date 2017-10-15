/**
 * @authors bp-fr, lem-fr - German Federal Office for Radiation Protection www.bfs.de
 *
 */

package de.bfs.irixbroker;

import java.util.*;

import java.lang.NullPointerException;

import javax.xml.datatype.XMLGregorianCalendar;

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
	
	private String OrganisationReporting;
	private XMLGregorianCalendar DateTime;
	private String ReportContext;
	private String Confidentiality="Free for Public Use";
	private String scenario = "routinemode"; //first element from List EventIdentification
	private String[] scenarios = {scenario}; //first element from List EventIdentification
	
	private List<AnnotationType> annot;
	private List<FileEnclosureType> fet;
	private String title;
	private String main_text="empty";
	private String ReportId;
	private Element dokpoolmeta; //DOM Element with the full dokpoolmeta information
	private Properties bfsIrixBrokerProperties;

	private boolean success = false;


	public IrixBrokerDokpoolClient(Properties bfsIBP)
	{
		bfsIrixBrokerProperties = bfsIBP;
	}

	public boolean doTheWork(ReportType report){
		success = false;

		success = readIdentification(report.getIdentification());
		success = readAnnexes(report.getAnnexes());

		success = DocPoolClient();
		return success;
	}
	
	private boolean readIdentification(IdentificationType ident)
	{
		boolean success=true;

		List<EventIdentificationType> eid=null;

		setOrganisationReporting(ident.getOrganisationReporting());
		setDateTime(ident.getDateAndTimeOfCreation());
		setReportContext(ident.getReportContext().value());
		if(ident.getConfidentiality() != null) {
			setConfidentiality(ident.getConfidentiality().value());
		}
		setReportId(ident.getReportUUID());

		if(ident.getEventIdentifications() != null){
			eid= ident.getEventIdentifications().getEventIdentification();
			if(eid.isEmpty()) {
				System.out.println("No eventidentification found!!");
				setScenario(scenario);
				success = false;
			} else {
				setScenario(eid.get(0).getValue());
				System.out.println("eventidentification filled");
			}
		}
		
		return success;
	}
	
	private boolean readAnnexes(AnnexesType annex)
	{
		boolean success=true;
		/**
		 * information for ELAN is only valid if there is one annotation with title and annotation text and file attachment
		 * or title and enclosed file attachments. You need a file attachment because the Information Category is only in this element.
		 */
		
		setAnnot(annex.getAnnotation());
		
		if(annot.isEmpty())
		{
			success=false;
		}
		else
		{
			setTitle(annot.get(0).getTitle());
			if(annot.get(0).getText().getContent().size() >0) {
				setMain_text((String) annot.get(0).getText().getContent().get(0)) ;
			}

			// get the DokPool Meta data
			List<Element> el = annot.get(0).getAny();
			dokpoolmeta = el.get(0);

			
		}
		//get the attached files
		setFet(annex.getFileEnclosure());
		if(fet.isEmpty())
		{
			success=false;
		}
		return success;
	}

	private boolean publish(Document d){
		d.setWorkflowStatus("publish");
		return true;
	}
	
	private boolean DocPoolClient()
	{
		success = true;

		String proto=bfsIrixBrokerProperties.getProperty("irix-dokpool.PROTO");
		String host=bfsIrixBrokerProperties.getProperty("irix-dokpool.HOST");
		String port=bfsIrixBrokerProperties.getProperty("irix-dokpool.PORT");
		String ploneSite=bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_SITE");
		String ploneDokpool=bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_DOKPOOL");
		String ploneGroupFolder=bfsIrixBrokerProperties.getProperty("irix-dokpool.PLONE_GROUPFOLDER");
		String user=bfsIrixBrokerProperties.getProperty("irix-dokpool.USER");
		String pw=bfsIrixBrokerProperties.getProperty("irix-dokpool.PW");
		
		String desc="Original date: "+DateTime.toString()+" "+ReportContext+ " "+ Confidentiality;
		
		//connect to wsapi4plone
		DocpoolBaseService dokpool = new DocpoolBaseService(proto+"://"+host+":"+port+"/"+ploneSite,user,pw);

		//TODO use primaryDokpool (of irixauto) or configuration file "ploneDokpool"?
		DocumentPool mydokpool = dokpool.getPrimaryDocumentPool();
		List <DocumentPool> mydokpools = dokpool.getDocumentPools();
		Element mydokpoolname = extractSingleElement(dokpoolmeta, TAG_DOKPOOLNAME);
		Boolean renewdokpool = true;
		if (renewdokpool && (mydokpoolname != null)){
			for (DocumentPool sdokpool : mydokpools) {
				if (sdokpool.getFolderPath().matches("/" + ploneSite + "/"+mydokpoolname.getTextContent())){
					mydokpool = sdokpool;
					renewdokpool = false;
				}
			}
		}
		if (renewdokpool) {
			for (DocumentPool sdokpool : mydokpools) {
				if (sdokpool.getFolderPath().matches("/" + ploneSite + "/"+ploneDokpool)){
					mydokpool = sdokpool;
				}
			}
		}

		List<Folder> groupFolders = mydokpool.getGroupFolders();
		Boolean renewgroupfolder = true;
		Element mydokpoolgroupfolder = extractSingleElement(dokpoolmeta, TAG_DOKPOOLGROUPFOLDER);
		Folder mygroupfolder = null;
                try {
                    mygroupfolder = mydokpool.getFolder( ploneSite+"/"+ploneDokpool+"/content/Groups/"+mydokpoolgroupfolder.getTextContent() );
		    renewgroupfolder = false;
		} catch (NullPointerException e) {
                    // It's fine not to find a groufolder here
                    // TODO give warning falling back to system configuration for import
                    System.out.println("[WARNING] Could not find Groupfolder: " + mydokpoolgroupfolder.getTextContent() + ". Trying systemdefined Groupfolder.");
                }
		if (renewgroupfolder){
                    try {
                        mygroupfolder = mydokpool.getFolder( ploneSite+"/"+ploneDokpool+"/content/Groups/"+ploneGroupFolder );
		        renewgroupfolder = false;
		    } catch (NullPointerException e) {
                        // It's fine not to find a groufolder here but suspicious
                        // TODO give warning falling back to first available groupfolder for import
                        System.out.println("[WARNING] Could not find systemdefined Groupfolder: " + ploneGroupFolder + ". Trying first available Groufolder.");
                    }
		}
		if (renewgroupfolder && (mydokpools.size() > 0)){
                    try {
			mygroupfolder = mydokpool.getGroupFolders().get(0);
                    } catch (NullPointerException e) {
                        throw new NullPointerException("Could not find a valid GroupFolder");
                    }
		}

		/** point the new Dokument to active or referenced scenarios of the dokpool
		 *
		 */
		//TODO support list of scenarios and properties settings as well!
		Element mydokpoolscenarios = extractSingleElement(dokpoolmeta, TAG_ELANSCENARIOS);
		if ( mydokpoolscenarios != null ) {
			NodeList mydokpoolscenariolist = mydokpoolscenarios.getElementsByTagName(TAG_ELANSCENARIO);
			List<String> sclist = new ArrayList<String>();
			for ( int i =0; i<mydokpoolscenariolist.getLength(); i++ ) {
				sclist.add(mydokpoolscenariolist.item(i).getTextContent());
			}
			//addScenariosfromDokpool(mydokpool, mydokpoolscenarios.getTextContent());
			addScenariosfromDokpool(mydokpool, sclist);
		} else {
		    addActiveScenariosfromDokpool(mydokpool);
		}
		
		/** hashmap to store the dokpool meta data
		 *
		 */
		Map<String, Object> properties = new HashMap<String, Object>();
		Map<String, String> dokpoolProperties = new HashMap<String, String>();
		properties.put("title",title);
		properties.put("description",desc);
		properties.put("text","<b>eingestellt durch IRIX-Broker</b>");
		Element dt = extractSingleElement(dokpoolmeta, TAG_DOKPOOLCONTENTTYPE);
		properties.put("docType",dt.getTextContent());
		properties.put("scenarios",scenarios);

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

		properties.put("subjects", new String[]{purpose.getTextContent(),
												network.getTextContent(),
												stid.getTextContent(),
												st.getTextContent(),
												dom.getTextContent(),
												dtype.getTextContent(),
												lbase.getTextContent(),
												mp.getTextContent(),
												status.getTextContent(),
												sbegin.getTextContent(),
												send.getTextContent()});

		dokpoolProperties.put("purpose", purpose.getTextContent());
		dokpoolProperties.put("dom", dom.getTextContent());
		dokpoolProperties.put("lbase", lbase.getTextContent());
		dokpoolProperties.put("sbegin", sbegin.getTextContent());
		dokpoolProperties.put("status", status.getTextContent());

		Element elan = extractSingleElement(dokpoolmeta, TAG_ISELAN);
		if(elan.getTextContent().equalsIgnoreCase("true")){
			properties.put("local_behaviors", new String[]{"elan"});
			//TODO other behaviors must be added later
		}

		Element rodos = extractSingleElement(dokpoolmeta, TAG_ISRODOS);
		if(rodos.getTextContent().equalsIgnoreCase("true")){
            System.out.println("[INFO] RODOS behavior not yet available in Dokpool. Ignoring!");
			//TODO properties.put("local_behaviors", new String[]{"rodos"});
		}

		Element rei = extractSingleElement(dokpoolmeta, TAG_ISREI);
		if(rei.getTextContent().equalsIgnoreCase("true")){
			System.out.println("[INFO] REI behavior not yet available in Dokpool. Ignoring!");
			//TODO properties.put("local_behaviors", new String[]{"rei"});
		}

		Element doksys = extractSingleElement(dokpoolmeta, TAG_ISDOKSYS);
		if(doksys.getTextContent().equalsIgnoreCase("true")){
			System.out.println("[INFO] DOKSYS behavior not yet available in Dokpool. Ignoring!");
			//TODO properties.put("local_behaviors", new String[]{"doksys"});
		}

		Document d = mygroupfolder.createDocument(ReportId, properties);
		System.out.println(d.getTitle());
		for (String key : dokpoolProperties.keySet()){
			d.setProperty(key, dokpoolProperties.get(key), "string");
		}

		for(int i =0; i<fet.size(); i++ )
		{
			String t=fet.get(i).getTitle();
			System.out.println("Anhang"+i+": "+t);

			if (MT_IMAGES.contains(fet.get(i).getMimeType()))
			{
				String aid = fet.get(i).getFileName(); //object-id is filename - needed from template simpleviz
				d.uploadImage(aid, t, t, fet.get(i).getEnclosedObject(), fet.get(i).getFileName());
			}
			// TODO separate handling of movie files
			else if (MT_MOVIES.contains(fet.get(i).getMimeType()))
			{
				String aid = fet.get(i).getFileName(); //object-id is filename - needed from template simpleviz
				d.uploadFile(aid, t, t, fet.get(i).getEnclosedObject(), fet.get(i).getFileName());
			}
			else
			{
				String aid = fet.get(i).getFileName(); //object-id is filename - needed from template simpleviz
				d.uploadFile(aid, t, t, fet.get(i).getEnclosedObject(), fet.get(i).getFileName());
			}


		}
		if (Confidentiality.equals(ID_CONF))
		{
			publish(d);
		}
		return success;
	}

	public void addScenariosfromDokpool(DocumentPool dp){

		List<Scenario> scen = dp.getScenarios();
		String [] sc = new String [scen.size()];
		for (int i = 0; i<scen.size();i++)
			sc[i]=scen.get(i).getId();
		setScenarios(sc);
	}

	public void addActiveScenariosfromDokpool(DocumentPool dp){

		List<Scenario> scen = dp.getScenarios();
		String [] sc = new String [scen.size()];
		for (int i = 0; i<scen.size();i++){
			String stat=scen.get(i).getStringAttribute("status");
			if(stat.equals("active"))
				sc[i]=scen.get(i).getId();
		}
		setScenarios(sc);
	}

	public void addScenariosfromDokpool(DocumentPool dp, String myscenario){

		List<Scenario> scen = dp.getScenarios();
		String [] sc = new String [scen.size()];
		for (int i = 0; i<scen.size();i++){
			if(scen.get(i).getId().equals(myscenario)) {
				sc[i] = scen.get(i).getId();
			}
		}
		if ( sc.length == 0 ){
			sc[0] = "routinemode";
		}
		setScenarios(sc);
	}

	public void addScenariosfromDokpool(DocumentPool dp, List<String> myscenarios){

		List<Scenario> scen = dp.getScenarios();
		String [] sc = new String [scen.size()];
		for (int i = 0; i<scen.size();i++){
			if (myscenarios.contains(scen.get(i).getId())){
				sc[i] = scen.get(i).getId();
			}
		}
		if ( sc.length == 0 ){
			sc[0] = "routinemode";
		}
		setScenarios(sc);
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

	public static Element extractSingleElement(Element element, String elementName)  {
		// Kindelement mit dem gewünschten Tag-Namen abholen
		final NodeList childElements = element.getElementsByTagName(elementName);

		// Anzahl der Ergebnisse abfragen
		final int count = childElements.getLength();

		// Wenn kein Kindelement gefunden, null zur�ck geben
		if(count == 0)
		{
			return null;
		}

		// Falls mehr als ein Kindelement gefunden, Fehler werfen
		if(count > 1)
		{
			throw new IllegalArgumentException("No single child element <" + elementName + "> found! Found " + count + " instances!");
		}

		// Das erste Kindelement zur�ck geben
		return (Element)childElements.item(0);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
