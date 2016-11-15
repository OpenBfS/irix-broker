/**
 * @authors bp-fr, lem-fr - German Federal Office for Radiation Protection www.bfs.de
 *
 */

package de.bfs.irixbroker;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.Arrays;
import java.util.Properties;

import javax.xml.datatype.XMLGregorianCalendar;

import de.bfs.dokpool.client.DocumentPool;
import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.iaea._2012.irix.format.identification.EventIdentificationType;
import org.iaea._2012.irix.format.identification.IdentificationType;

import de.bfs.dokpool.client.Document;
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
		String user=bfsIrixBrokerProperties.getProperty("irix-dokpool.USER");
		String pw=bfsIrixBrokerProperties.getProperty("irix-dokpool.PW");
		
		String desc="Original date: "+DateTime.toString()+" "+ReportContext+ " "+ Confidentiality;

		
		//connect to wsapi4plone
		DocpoolBaseService dokpool = new DocpoolBaseService(proto+"://"+host+":"+port+"/"+ploneSite,user,pw);
		
		DocumentPool mydokpool = dokpool.getPrimaryDocumentPool();
		List <DocumentPool> mydokpools = dokpool.getDocumentPools();
		/** no userfolder needed
		Folder userfolder = mydokpool.getUserFolder();
		 */
		List<Folder> groupfolder = mydokpool.getGroupFolders();
		Folder mygroupfolder = groupfolder.get(0);

		/** hashmap to store the dokpool meta data
		 *
		 */
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put("title",title);
		properties.put("description",desc);
		properties.put("text","<b>WebGisKlient</b>");
		Element dt = extractSingleElement(dokpoolmeta, TAG_DOKPOOLCONTENTTYPE);
		properties.put("docType",dt.getTextContent());
		properties.put("scenarios",scenarios);

		//getting the dokpool metainformation by tagname
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

		Element elan = extractSingleElement(dokpoolmeta, TAG_ISELAN);
		if(elan.getTextContent().equalsIgnoreCase("true")) //other behaviors must be added later
		properties.put("local_behaviors", new String[]{"elan"});

		Document d = mygroupfolder.createDocument(ReportId, properties);
		System.out.println(d.getTitle());

		for(int i =0; i<fet.size(); i++ )
		{
			String t=fet.get(i).getTitle();
			System.out.println("Anhang"+i+": "+t);

			if(fet.get(i).getMimeType().equalsIgnoreCase(MT_PNG))
			{
				// String aid = ReportId+Integer.toString(i);
				String aid = fet.get(i).getFileName(); //object-id is filename - needed from template simpleviz
				System.out.println(aid);
				d.uploadImage(aid, t, t, fet.get(i).getEnclosedObject(), fet.get(i).getFileName());
			}
			else
			{
				//String aid = ReportId+Integer.toString(i);
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
