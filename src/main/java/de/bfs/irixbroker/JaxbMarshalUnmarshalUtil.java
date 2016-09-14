/**
 *
 */
package de.bfs.irixbroker;

/**
 * @author peter
 *
 */

import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.bind.*;
import javax.xml.validation.*;

import org.xml.sax.SAXException;
import org.iaea._2012.irix.format.*;
import org.iaea._2012.irix.format.annexes.AnnexesType;
import org.iaea._2012.irix.format.annexes.AnnotationType;
import org.iaea._2012.irix.format.annexes.FileEnclosureType;
import org.iaea._2012.irix.format.identification.IdentificationType;


public class JaxbMarshalUnmarshalUtil
{
   static final DecimalFormat DF_2 = new DecimalFormat( "#,##0.00" );

   public static <T> T unmarshal( String xsdSchema, String xmlDatei, Class<T> clss, boolean validate )
   throws JAXBException, SAXException
   {
      // Schema und JAXBContext sind multithreadingsicher ("thread safe"):
      SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
      Schema        schema        = ( xsdSchema == null || xsdSchema.trim().length() == 0 )
                                    ? null : schemaFactory.newSchema( new File( xsdSchema ) );
      JAXBContext   jaxbContext   = JAXBContext.newInstance( clss.getPackage().getName() );
   //   System.out.println(jaxbContext.toString());
      return unmarshal( jaxbContext, schema, xmlDatei, clss, validate );
   }


   public static <T> T unmarshal( JAXBContext jaxbContext, Schema schema, String xmlDatei, Class<T> clss, boolean validate )
   throws JAXBException
   {
      // Unmarshaller ist nicht multithreadingsicher:
      Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

      if(validate) // Validierung gegen das XML Schema
      unmarshaller.setSchema( schema );

      JAXBElement rep=null;
	try {
		rep = (JAXBElement) unmarshaller.unmarshal( new File( xmlDatei ) );
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	
  //   Object o = unmarshaller.unmarshal( new File( xmlDatei ) );
 //     ReportType rep = (ReportType) ((JAXBElement) unmarshaller.unmarshal( new File( xmlDatei ) )).getValue();
//      System.out.println("---->"+JAXBIntrospector.getValue(o));
//      System.out.println("Class ->"+o.getClass());
//      clss.cast(o);
//      return clss.cast(((JAXBElement) unmarshaller.unmarshal( new File( xmlDatei ) )).getValue() );
      return  clss.cast(rep.getValue());
   }

   public static void marshal( String xsdSchema, String xmlDatei, Object jaxbElement )
   throws JAXBException, SAXException
   {
      SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
      Schema        schema        = ( xsdSchema == null || xsdSchema.trim().length() == 0 )
                                    ? null : schemaFactory.newSchema( new File( xsdSchema ) );
      JAXBContext   jaxbContext   = JAXBContext.newInstance( jaxbElement.getClass().getPackage().getName() );
      marshal( jaxbContext, schema, xmlDatei, jaxbElement );
   }

   public static void marshal( JAXBContext jaxbContext, Schema schema, String xmlDatei, Object jaxbElement )
   throws JAXBException
   {
      Marshaller marshaller = jaxbContext.createMarshaller();
      marshaller.setSchema( schema );
      marshaller.setProperty( Marshaller.JAXB_ENCODING, "ISO-8859-1" );
      marshaller.setProperty( Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE );
      marshaller.marshal( jaxbElement, new File( xmlDatei ) );
   }

   /** Die main()-Methode ist nur fuer Testzwecke */
   public static void main( String[] args ) throws JAXBException, SAXException, ClassNotFoundException
   {
      if( args.length != 3 ) {
         System.out.println( "\nBitte XSD-Schema, XML-Dokument und Zielklasse angeben." );
         return;
      }
      System.out.println( "\nSchema: " + args[0] + ", XML-Dokument: " + args[1] + ", Zielklasse: " + args[2] + "\n" );

      // Unmarshalling-Test:
      long startSpeicherverbrauch = ermittleSpeicherverbrauch();
      long startZeit = System.nanoTime();
      Object obj = unmarshal( args[0], args[1], Class.forName( args[2] ),false );
      String dauer = ermittleDauer( startZeit );
      String speicherverbrauch = formatiereSpeichergroesse( ermittleSpeicherverbrauch() - startSpeicherverbrauch );
      System.out.println( "Parsingspeicherverbrauch = " + speicherverbrauch + ", Parsingdauer = " + dauer );
      System.out.println( obj.getClass() );
      // Die folgende Ausgabe macht nur Sinn, wenn es eine sinnvolle toString()-Methode gibt:
     ReportType report= (ReportType) obj;
     IdentificationType ident= report.getIdentification();
     System.out.println("Reporting Organisation:  "+ident.getOrganisationReporting());
     System.out.println("Date+time:   "+ident.getDateAndTimeOfCreation());
     System.out.println("Confidentiality:  "+ident.getConfidentiality());
     System.out.println("Context:  "+ident.getReportContext());
     AnnexesType annex = report.getAnnexes();
     List <AnnotationType> annot=annex.getAnnotation();
     if(!annot.isEmpty())
     {
     System.out.println(annot.get(0).getTitle());
     System.out.println(annot.get(0).getText());
     }

     List <FileEnclosureType> files= annex.getFileEnclosure();

     String fn = files.get(0).getFileName();

     try {
		FileOutputStream of = new FileOutputStream(fn);
		of.write(files.get(0).getEnclosedObject());
		of.close();
	} catch (Exception e) {
		// TODO: handle exception
	}
	// Marshalling-Test:
      startZeit = System.nanoTime();
    // marshal( args[0], args[1] + "-output.xml", obj );
    //  dauer = ermittleDauer( startZeit );
    //  System.out.println( "\n'" + args[1] + "-output.xml' erzeugt in " + dauer + "." );
   }

   static String ermittleDauer( long startZeitNanoSek )
   {
      long dauerMs = (System.nanoTime() - startZeitNanoSek) / 1000 / 1000;
      if( dauerMs < 1000 ) return "" + dauerMs + " ms";
      return DF_2.format( dauerMs / 1000. ) + " s";
   }

   static long ermittleSpeicherverbrauch()
   {
      System.gc();
      System.gc();
      return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
   }

   static String formatiereSpeichergroesse( long bytes )
   {
      if( bytes < 0 ) return "0 Byte";
      if( bytes < 1024 ) return "" + bytes + " Byte";
      double b = bytes / 1024.;
      if( b < 1024. ) return DF_2.format( b ) + " KByte";
      return DF_2.format( b / 1024. ) + " MByte";
   }
}
