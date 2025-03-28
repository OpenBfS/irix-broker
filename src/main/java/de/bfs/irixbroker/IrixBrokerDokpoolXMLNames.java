/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */

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
    String TAG_ELANEVENTS = "Scenarios";
    String TAG_ELANEVENT = "Scenario";
    String TAG_SUBJECTS = "Subjects";
    String TAG_SUBJECT = "Subject";
    String TAG_AREA = "Area";
    String TAG_PURPOSE = "Purpose";
    String TAG_DURATION = "Duration";
    String TAG_NETWORKOPERATOR = "NetworkOperator";
    String TAG_SAMPLETYPE = "SampleType";
    String TAG_DOM = "Dom";
    String TAG_DATASOURCE = "DataSource";
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
    String TAG_REVISION = "Revision";
    String TAG_YEAR = "Year";
    String TAG_PERIOD = "Period";
    String TAG_NUCLEARINSTALLATION = "NuclearInstallation";
    String TAG_MEDIUM = "Medium";
    String TAG_REILEGALBASE = "ReiLegalBase";
    String TAG_ORIGIN = "Origin";
    String TAG_MST = "MSt";
    String TAG_AUTHORITY = "Authority";
    String TAG_PDFVERSION = "PDFVersion";

    // allowed mime types
    String MT_PDF = "application/pdf";
    String MT_GIF = "image/gif";
    String MT_PNG = "image/png";
    String MT_JPG = "image/jpg";
    String MT_JPEG = "image/jpeg";
    String MT_MP4 = "video/mp4";
    String MT_OGG = "video/ogg";
    String MT_WEBM = "video/webm";
    List<String> MT_IMAGES = Arrays.asList(MT_GIF, MT_JPEG, MT_JPG, MT_PNG);
    List<String> MT_MOVIES = Arrays.asList(MT_MP4, MT_OGG, MT_WEBM);

    //used strings
    String ID_CONF = "Free for Public Use";

}
