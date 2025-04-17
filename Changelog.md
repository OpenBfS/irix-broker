# Changelog

## Version 3.x

### 3.3.0 *future*

 - BREAKING: removed setEvents, getEvents, addActiveEventsfromDokpool and addEventsfromDokpool from DokoolClient
   - all could only theoretically be used by external callers as they were internally used to 
     during ELAN property extraction from the IRIX document.
 - 

### 3.2.0 *2025-03-27*

 - pass on mimetype to dokpool-client
 - update rodos properties and pass everything rodos on to dokpool-client (also 3.2.0)

### 3.1 *2025-01-27*

 - jaxws, javax.xml.bind, jaxb to current versions (libs and plugins)
 - remove unneeded dependencies/imports (ArrayUtils/UrlPathHelper)
 - added dependency check to pom.xml
 - update Dokpool Client to 3.1.0 (XMLRPC->REST)
 - deployment tested with Java 11 and 21 (build version still 11)

### 3.0.0 *2023-10-24* (changes vs. 2.7.2)

 - start of upgrade to Java 11, Java build version: 11
 - dependency upgrades SpringBoot and jackson-annotations

## Version 2.x

### 2.8.0 *2024-12-16*

 - update Dokpool Client to 2.3.0 (XMLRPC->REST)

### 2.7.2 *2022-07-06*

 - added GIF mimetype to supported images
 - update log4j dependency

### 2.7.1 *2021-07-07*

 - commons.io+junit dependency updates

### 2.7.0 *2021-07-07*

 - list support and XML schema update for IRIX-Dokpool

### 2.6.0 *2020-07-10*

 - support for Länder-Dokpools (Dokpools for federal states)


### 2.5.0 *2019-12-18*

 - added support for MStIDs
 - IRIX-Dokpool schmema update
 - REI support WIP
 - Dokpool-Client 2.2.0

### 2.2.0 *2019-10-08*

 - chnage from Scenario to Event

### 2.1.1 *2018-12-19*

 - make broker more rebust

### 2.1.0 *2018-12-19*

 - Doksys support

### 2.0.0 *2018-12-19*

 - many breaking changes
 - Java 8 and Dokpool Client 2.x

## Version 1.x

### 1.4.2 *2017-08-30*

 - Dokpool Client 1.2

### 1.4.0 *2017-08-30*
 - initial stable version in this repo
