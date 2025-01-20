/**
 *
 */
package de.bfs.irixbroker;

/**
 * @author Torsten Horn
 * http://www.torsten-horn.de/techdocs/java-xml-jaxb.htm
 *
 */
import java.io.*;

//still part of Java 21
import javax.xml.XMLConstants;
import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.*;
import javax.xml.validation.Validator;

import jakarta.xml.bind.*;
import org.xml.sax.SAXException;

/**
 * Hilfsklasse zum Parsen grosser XML-Dateien (durch Kombination von JAXB mit StAX).<br>
 * Es wird nicht das StartElement geparst, aber alle darunter angeordneten Elemente.
 */
public class XmlParserMitStaxUndJaxb
{
   /**
    * Interface fuer Callback-Klassen fuer die Verarbeitung der einzelnen XML-Elemente.
    */
   public interface ElementeVerarbeitung
   {
      void verarbeite( Object element );
   }

   /**
    * Hilfsmethode zum Parsen grosser XML-Dateien (durch Kombination von JAXB mit StAX).<br>
    * Es koennen alle im angegebenen Package vorhandenen Klassen geparst werden, welche eine XmlRootElement-Annotation haben.<br>
    *
    * @param  xsdDatei    Schema-XSD-Datei
    * @param  xmlDatei    XML-Datei
    * @param  encoding    Character-Encoding, z.B. UTF-8
    * @param  packageName Package mit den zu lesenden Java-Klassen
    * @param  elemVerarb  Callback-Objekt fuer die Verarbeitung der einzelnen Elemente
    * @return Anzahl der gefundenen Elemente
    */
   public static long parseXmlElemente( String xsdDatei, String xmlDatei, String encoding, String packageName, ElementeVerarbeitung elemVerarb ) throws Exception
   {
      if( xsdDatei != null && xsdDatei.trim().length() > 0 ) {
         Reader xml = new InputStreamReader( new FileInputStream( xmlDatei ), encoding );
         try { validate( xsdDatei, xml ); } finally { xml.close(); }
      }
      Reader xml = new InputStreamReader( new FileInputStream( xmlDatei ), encoding );
      try { return parseXmlElemente( xml, packageName, elemVerarb ); } finally { xml.close(); }
   }

   /**
    * Hilfsmethode zum Parsen grosser XML-Dateien (durch Kombination von JAXB mit StAX).<br>
    * Es koennen alle im angegebenen Package vorhandenen Klassen geparst werden, welche eine XmlRootElement-Annotation haben.<br>
    * <b>Achtung:</b> Zum XML-Reader wird normalerweise kein close() aufgerufen, ausser siehe Bug:
    * {@link http://bugs.sun.com/view_bug.do?bug_id=6539065}
    *
    * @param  xml         Reader zur XML-Datei
    * @param  packageName Package mit den zu lesenden Java-Klassen
    * @param  elemVerarb  Callback-Objekt fuer die Verarbeitung der einzelnen Elemente
    * @return Anzahl der gefundenen Elemente
    */
   public static long parseXmlElemente( Reader xml, String packageName, ElementeVerarbeitung elemVerarb ) throws XMLStreamException, JAXBException
   {
      // StAX:
      EventFilter startElementFilter = new EventFilter() {
         @Override public boolean accept( XMLEvent event ) {
            return event.isStartElement();
         }
      };
      long            anzahlElem   = 0;
      XMLInputFactory staxFactory  = XMLInputFactory.newInstance();
      XMLEventReader  staxReader   = staxFactory.createXMLEventReader( xml );
      XMLEventReader  staxFiltRd   = staxFactory.createFilteredReader( staxReader, startElementFilter );
      // Ueberspringe StartElement:
      staxFiltRd.nextEvent();
      // JAXB:
      JAXBContext     jaxbContext  = JAXBContext.newInstance( packageName );
      Unmarshaller    unmarshaller = jaxbContext.createUnmarshaller();
      // Parsing:
      while( staxFiltRd.peek() != null ) {
         Object element = unmarshaller.unmarshal( staxReader );
         elemVerarb.verarbeite( element );
         anzahlElem++;
      }
      return anzahlElem;
   }

   /**
    * Hilfsmethode zum Parsen grosser XML-Dateien (durch Kombination von JAXB mit StAX).<br>
    * Es kann nur die eine angegebene Klasse geparst werden (eine XmlRootElement-Annotation wird nicht benoetigt).<br>
    * <b>Achtung:</b> Zum XML-Reader wird normalerweise kein close() aufgerufen, ausser siehe Bug:
    * {@link http://bugs.sun.com/view_bug.do?bug_id=6539065}
    *
    * @param  xml          Reader zur XML-Datei
    * @param  elementClass Die zu parsende Java-Klasse
    * @param  elemVerarb   Callback-Objekt fuer die Verarbeitung der einzelnen Elemente
    * @return Anzahl der gefundenen Elemente
    */
   public static long parseXmlElemente( Reader xml, Class<?> elementClass, ElementeVerarbeitung elemVerarb ) throws XMLStreamException, JAXBException
   {
      // StAX:
      EventFilter startElementFilter = new EventFilter() {
         @Override public boolean accept( XMLEvent event ) {
            return event.isStartElement();
         }
      };
      long            anzahlElem   = 0;
      XMLInputFactory staxFactory  = XMLInputFactory.newInstance();
      XMLEventReader  staxReader   = staxFactory.createXMLEventReader( xml );
      XMLEventReader  staxFiltRd   = staxFactory.createFilteredReader( staxReader, startElementFilter );
      // Ueberspringe StartElement:
      staxFiltRd.nextEvent();
      // JAXB:
      JAXBContext     jaxbContext  = JAXBContext.newInstance( elementClass );
      Unmarshaller    unmarshaller = jaxbContext.createUnmarshaller();
      // Parsing:
      while( staxFiltRd.peek() != null ) {
         Object element = unmarshaller.unmarshal( staxReader, elementClass );
         if( element instanceof JAXBElement && ((JAXBElement<?>) element).getValue() != null ) {
            element = ((JAXBElement<?>) element).getValue();
         }
         elemVerarb.verarbeite( element );
         anzahlElem++;
      }
      return anzahlElem;
   }

   /**
    * Validierung des XML-Readers gegen ein XSD-Schema.<br>
    * <b>Achtung:</b> Zum XML-Reader wird kein close() aufgerufen.
    *
    * @param xsdSchema XSD-Schema
    * @param xml       XML-Reader
    */
   public static void validate( String xsdSchema, Reader xml ) throws SAXException, IOException
   {
      SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
      Schema        schema = schemaFactory.newSchema( new File( xsdSchema ) );
      Validator     validator = schema.newValidator();
      validator.validate( new StreamSource( xml ) );
   }
}

