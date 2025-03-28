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

import java.util.GregorianCalendar;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;


//still part of Java 21
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.XMLGregorianCalendar;

import jakarta.xml.bind.DatatypeConverter;

import static java.lang.System.Logger.Level.ERROR;



/**
 * Static helper methods to work as IRIX Broker.
 *
 * This class provides helper methods to work
 *
 */
public final class IrixBrokerUtils {
    private static System.Logger log = System.getLogger(IrixBrokerUtils.class.getName());
    private IrixBrokerUtils() {
        // hidden constructor to avoid instantiation.
    }

    /**
     * Create a XMLGregorianCalendar from a GregorianCalendar object.
     *
     * This method is necessary to fulfil the date spec of the
     * IRIX Schema.
     *
     * @param cal The Gregorian calendar.
     * @return An XMLGregorianCalendar with msces set to undefined.
     */
    protected static XMLGregorianCalendar createXMLCalFromGregCal(
            GregorianCalendar cal) {
        cal.setTimeZone(TimeZone.getTimeZone("utc"));
        try {
            XMLGregorianCalendar date = DatatypeFactory.newInstance().
                    newXMLGregorianCalendar(cal);
            date.setFractionalSecond(null);
            return date;
        } catch (DatatypeConfigurationException e) {
            log.log(ERROR, "Exception converting to XMLGregorianCalendar");
            return null;
        }
    }

    /**
     * Helper method to obtain the current datetime as XMLGregorianCalendar.
     *
     * @return a {@link javax.xml.datatype.XMLGregorianCalendar} object.
     */
    public static XMLGregorianCalendar getXMLGregorianNow() {
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(new Date(System.currentTimeMillis()));
        return createXMLCalFromGregCal(c);
    }

    /**
     * Parses an XML Calendar string into an XMLGregorianCalendar object.
     *
     * @param str An  ISO 8601 DateTime like: 2015-05-28T15:35:54.168+02:00
     * @return a {@link javax.xml.datatype.XMLGregorianCalendar} object.
     */
    public static XMLGregorianCalendar xmlCalendarFromString(String str) {
        Calendar cal = DatatypeConverter.parseDateTime(str);
        GregorianCalendar c = new GregorianCalendar();
        c.setTime(cal.getTime());
        return createXMLCalFromGregCal(c);
    }
};
