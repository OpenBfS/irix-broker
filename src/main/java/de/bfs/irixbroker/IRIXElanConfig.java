/* Copyright (C) 2015-2025 by Bundesamt fuer Strahlenschutz
 *
 * This file is Free Software under the GNU GPL (v>=3)
 * and comes with ABSOLUTELY NO WARRANTY!
 * See LICENSE for details.
 */
/**
 * Class for configuring the mapping between IRIX InformationCategory and ELAN document types
 */
package de.bfs.irixbroker;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Peter Bieringer
 * @version 0.1 02.03.2015
 * FIXME is this file obsolete?? lem-fr, 2018-10-11;
 *
 */
public class IRIXElanConfig {

    private Map<String, String> cat;

    public IRIXElanConfig() {
        cat = initCategoryMap();
    }

    /**
     * At the moment constant mapping.
     * Must be changed to database access for mapping!!!!
     *
     * @return
     */
    private Map<String, String> initCategoryMap() {
        Map<String, String> category = new HashMap<String, String>();


        /* At the moment constant mapping.
           Must be changed to database access for mapping!!!!  */

        category.put("Event Information", "eventinformation");
        category.put("Installation Status Information", "nppinformation");
        category.put("Release Information", "nppinformation");
        category.put("Meteorological Information", "weatherinformation");
        category.put("Measurement Data", "None");
        category.put("Protective Actions Information", "protectivactions");
        category.put("Response Information", "protectivactions");
        category.put("Public Information", "instructions");
        category.put("Public Information - Press Release", "mediarelease");
        category.put("Modelling Results", "otherprojection");
        category.put("Modelling Results - Averted Dose", "otherprojection");
        category.put("Modelling Results - Cloud Arrival Time",
            "otherprojection");

        return category;
    }

    public String getESDdoctype(String irixCat) {
        return cat.get(irixCat);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

        IRIXElanConfig IEC = new IRIXElanConfig();

    }

}
