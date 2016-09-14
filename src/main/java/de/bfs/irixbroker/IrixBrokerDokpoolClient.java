/**
 * 
 */
package de.bfs.irixbroker;

import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.xml.datatype.XMLGregorianCalendar;

import org.iaea._2012.irix.format.ReportType;
import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.iaea._2012.irix.format.identification.EventIdentificationType;
import org.iaea._2012.irix.format.identification.IdentificationType;

import de.bfs.elan.client.Document;
//import de.bfs.elan.client.ELANService;
//import de.bfs.elan.client.ESD;
import de.bfs.elan.client.Folder;

/**
 * @authors bp-fr, lem-fr - German Federal Office for Radiation Protection www.bfs.de
 *
 */
public class IrixBrokerDokpoolClient {
	
	private String OrganisationReporting;
	private XMLGregorianCalendar DateTime;
	private String ReportContext;
	private String Confidentiality;
	private String scenario; //first element from List EventIdentification
	
	private List<AnnotationType> annot;
	private List<FileEnclosureType> fet;
	private String title;
	private String main_text;
	private String ReportId;
	
	
	public IrixBrokerDokpoolClient(ReportType report)
	{
		//ReportType report = this.report;
	}
	
	private boolean readIdentification(IdentificationType ident)
	{
		boolean success=true;
		
		setOrganisationReporting(ident.getOrganisationReporting());
		setDateTime(ident.getDateAndTimeOfCreation());
		setReportContext(ident.getReportContext().value());
		setConfidentiality(ident.getConfidentiality().value());
		setReportId(ident.getReportUUID());
		
		List<EventIdentificationType> eid= ident.getEventIdentifications().getEventIdentification();
		
		if(eid.isEmpty())
		{
			System.out.println("No eventidentification found!!");
			success = false;
		}
		else
		{
			setScenario(eid.get(0).getValue());
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
			setMain_text((String) annot.get(0).getText().getContent().get(0)) ;
			
			
			
		}
		//get the attached files
		setFet(annex.getFileEnclosure());
		if(fet.isEmpty())
		{
			success=false;
		}
		
		
		
		return success;
	}
	
	private boolean ELANClient()
	{
		boolean success=true;
		
		
		// read host, port, user etc from the properties file
		ResourceBundle resources;
		
		try {
            resources = ResourceBundle.getBundle("resources.IrixBrokerDokpoolClient",
                                                 Locale.getDefault());
        } catch (MissingResourceException mre) {
            System.err.println("resources/IrixBrokerDokpoolClient.properties not found");
            return false;
        }

		String proto=resources.getString("PROTO");
		String host=resources.getString("HOST");
		String port=resources.getString("PORT");
		String ploneSite=resources.getString("PLONE_SITE");
		String user=resources.getString("USER");
		String pw =resources.getString("PW");
		
		String desc="Original date: "+DateTime.toString()+" "+ReportContext+ " "+ Confidentiality;
		
		//connect to wsapi4plone
		/*ELANService elan = new ELANService(proto+"://"+host+":"+port+"/"+ploneSite,user,pw);
		
		ESD myesd = elan.getPrimaryESD();
		Folder userfolder = myesd.getUserFolder();
		
		// Fileencloser List first element for the Information category
		
		String cat= (String) fet.get(0).getInformationCategoryOrInformationCategoryDescription().get(0).getValue();
		
		IRIXElanConfig iec=new IRIXElanConfig();
		
		Document d = userfolder.createDocument(ReportId, title, desc, main_text, iec.getESDdoctype(cat), scenario);
		
		for(int i =0; i<fet.size(); i++ )
		{
			fet.get(i);
		}*/
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

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
