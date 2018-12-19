package de.bfs.irixbroker;

import java.util.List;
import java.util.Arrays;

public interface IrixBrokerDokpoolXMLNames {
	
	// Tag-Namen
	String TAG_DOKPOOLNAME = "DokpoolName";
	String TAG_DOKPOOLGROUPFOLDER = "DokpoolGroupFolder";
	String TAG_DOKPOOLPRIVATEFOLDER = "DokpoolPrivateFolder";
	String TAG_DOKPOOLTRANSFERFOLDER = "DokpoolTransferFolder";
	String TAG_DOKPOOLCONTENTTYPE = "DokpoolContentType";
	String TAG_DOKPOOLDOCUMENTOWNER = "DokpoolDocumentOwner";
	String TAG_ISELAN = "IsElan";
	String TAG_ISDOKSYS = "IsDoksys";
	String TAG_ISRODOS = "IsRodos";
	String TAG_ISREI = "IsRei";
	String TAG_DOKSYS = "DOKSYS";
	String TAG_ELAN = "ELAN";
	String TAG_RODOS = "RODOS";
	String TAG_REI = "REI";
	String TAG_ELANSCENARIOS = "Scenarios";
	String TAG_ELANSCENARIO = "Scenario";
	String TAG_SUBJECTS = "Subjects";
	String TAG_AREA = "Area";
	String TAG_PURPOSE = "Purpose";
	String TAG_DURATION = "Duration";
	String TAG_NETWORKOPERATOR = "NetworkOperator" ;
	String TAG_SAMPLETYPEID = "SampleTypeId";
	String TAG_SAMPLETYPE = "SampleType";
	String TAG_DOM = "Dom";
	String TAG_DATATYPE = "DataType";
	String TAG_LEGALBASE = "LegalBase";
	String TAG_MEASURINGPROGRAM = "MeasuringProgram";
	String TAG_MEASUREMENTCATEGORY = "MeasurementCategory";
	String TAG_OPERATIONMODE = "OperationMode";
	String TAG_TRAJECTORYSTARTLOCATION = "TrajectoryStartLocation";
	String TAG_TRAJECTORYENDLOCATION = "TrajectoryEndLocation";
	String TAG_TRAJECTORYSTARTTIME = "TrajectoryStartTime";
	String TAG_TRAJECTORYENDTIME = "TrajectoryEndTime";
	String TAG_STATUS = "Status";
	String TAG_INFOTYPE = "InfoType";
	String TAG_SAMPLINGBEGIN = "SamplingBegin";
	String TAG_SAMPLINGEND = "SamplingEnd";
	
	// allowed mime types
	String MT_PDF = "application/pdf";
	String MT_PNG = "image/png";
	String MT_JPG = "image/jpg";
	String MT_JPEG = "image/jpeg";
	String MT_MP4 = "video/mp4";
	String MT_OGG = "video/ogg";
	String MT_WEBM = "video/webm";
	List<String> MT_IMAGES = Arrays.asList(MT_JPEG, MT_JPG, MT_PNG);
	List<String> MT_MOVIES = Arrays.asList(MT_MP4, MT_OGG, MT_WEBM);
	
	//used strings
	String ID_CONF = "Free for Public Use";

}
