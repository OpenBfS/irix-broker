/* Copyright (C) 2015 by Bundesamt fuer Strahlenschutz
 * Software engineering by Intevation GmbH
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE.txt for details.
 */

package de.bfs.irixbroker;

/**
 * Exception signaling any failure to while delivering an IRIX-XML report to a
 * recipient.
 *
 */
public class IrixBrokerException extends Exception {

    /**
     * @param message the error message.
     * @param cause the {@link Throwable}, which is the cause of the
     * exception.
     */
    public IrixBrokerException(String message, Throwable cause) {
        super(message, cause);
    }

}
