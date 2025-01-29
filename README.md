# IRIXBroker

IRIXBroker is used as part of the IRIX-Webservice.

IRIXBroker has to decide what to do with the information from an IRIX report
e.g. information for Dokpool (e.g. ELAN), measurements for VDB, documents for IAEA, EU or federal states.
There is a dedicated client class for each server type.
The delivery workflows for EU, IAES and BL are currently not implemented, so IRIXBroker mainly acts as a Gateway to Dokpool via the Dokpool Client library (de.bfs.dokpool.client).

IRIXBroker depends on the package `org.iaea._2012.irix.format` which is generated from the XSD files in `src/main/webapp/WEB-INF/irix-schema/` (this is a subrepository).

The entry method for IRIXBroker is `deliverIrixBroker(ReportType report)` in [IrixBroker.java](src/main/java/de/bfs/irixbroker/IrixBroker.java).

further information:
 - [Changelog](Changelog.md)
 - [License](LICENSE)

## Building

To create irix-broker simply build it with:

    mvn package

and find from:

    target/bfs-irixbroker.jar

To go on with building irix-webservice install it into your local maven repo ~/.m2:

    mvn install

To build the Javadocs run:

    mvn javadoc:javadoc

The documentation can then be found at [apidocs](apidocs/index.html).

Unfortunately, tests are not up to date and do not currently work.
