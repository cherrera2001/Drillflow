/**
 * Copyright © 2018-2018 Hashmap, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hashmapinc.tempus.witsml.valve.dot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import com.hashmapinc.tempus.WitsmlObjects.AbstractWitsmlObject;
import com.hashmapinc.tempus.WitsmlObjects.Util.WitsmlMarshal;

import com.hashmapinc.tempus.witsml.valve.ValveException;
import org.json.JSONObject;

/**
 * ABANDON ALL HOPE, YE WHO ENTER HERE
 */
public class DotTranslator {
    private static final Logger LOG = Logger.getLogger(DotTranslator.class.getName());

    /**
     * This function serializes the object to a 1.4.1.1 JSON string
     * @param obj - object to serialize
     * @return jsonString - String serialization of a JSON version of the 1.4.1.1 witsml object
     */
    public static String get1411JSONString(AbstractWitsmlObject obj) {
        LOG.info("Getting 1.4.1.1 json string for object: " + obj.toString());
        return obj.getJSONString("1.4.1.1");
    }

    /**
     * returns a valid 1311 XML string for the 1411 obj
     * @param obj1411 - 1411 AbstractWitsmlObject to serialize to xml
     */
    // TODO: delete this method and use the AbstractWitsmlObject.getXMLString method when version 1.1.5 fixes the namespace bug.
    public static String get1311XMLString(
        AbstractWitsmlObject obj1411
    ) throws ValveException {
        LOG.info("getting 1.3.1.1 XML string for object: " + obj1411.toString());

        // get 1311 string
        String xml1311 = obj1411.getXMLString("1.3.1.1");

        // convert to 1311 object
        try {
            AbstractWitsmlObject obj1311;
            switch (obj1411.getObjectType()) {
                case "well":
                    obj1311 = ((com.hashmapinc.tempus.WitsmlObjects.v1311.ObjWells) WitsmlMarshal.deserialize(
                            xml1311, com.hashmapinc.tempus.WitsmlObjects.v1311.ObjWells.class)
                    ).getWell().get(0);
                    return obj1311.getXMLString("1.3.1.1");
                default:
                    throw new ValveException("unsupported object type: " + obj1411.getObjectType());
            }
        } catch (Exception e) {
            throw new ValveException(e.getMessage());
        }
    }

    /**
     * merges response into query and returns the parsed 1.4.1.1 object
     * 
     * @param query    - JSON object representing the query
     * @param response - JSON object representing the response from DoT
     * @return obj - parsed abstract object
     */
    public static AbstractWitsmlObject translateQueryResponse(
        JSONObject query, 
        JSONObject response
    ) throws IOException {
        LOG.info("Translating query response.");

        // merge the responseJSON with the query
        LOG.info("Merging query and response into single object");
        for (Object key : query.keySet()) { // copy missing query fields from response
            String keyString = (String) key;
            LOG.info("Merging key <" + keyString + ">");
            if (query.get(keyString).equals("")) { // only fill in missing values
                Object val = response.get(keyString);
                query.put(keyString, val);
            }
        }

        // convert the queryJSON back to valid xml
        LOG.info("Converting merged query JSON to valid XML string");
        return WitsmlMarshal.deserializeFromJSON(query.toString(), com.hashmapinc.tempus.WitsmlObjects.v1411.ObjWell.class);
    }

    /**
     * Consolidates each object under 1 parent and serializes
     * the consolidated object into an XML string in the proper
     * WITSML version format
     *
     * @param witsmlObjects - list of objects to consolidate
     * @param version - witsml version to serialize to
     * @return = serialized parent object in requested WITSML version format
     * @throws ValveException
     */
    public static String consolidateObjectsToXML(
        ArrayList<AbstractWitsmlObject> witsmlObjects,
        String version
    ) throws ValveException {
        // validate version
        if(!"1.3.1.1".equals(version) && !"1.4.1.1".equals(version)) {
            throw new ValveException("Unsupported client version <" + version + "> in DoT GET");
        }

        // makes if statements more legible
        boolean is1411 = "1.4.1.1".equals(version);

        // get xmlString
        String xmlString;
        switch (witsmlObjects.get(0).getObjectType()) {
            case "well": // no consolidation needed for wells
                xmlString = is1411 ? witsmlObjects.get(0).getXMLString("1.4.1.1") : get1311XMLString(witsmlObjects.get(0));
                break;
            case "wellbore": // no consolidation needed for wells
                xmlString = is1411 ? consolidate1411WellboresToXML(witsmlObjects) : consolidate1311WellboresToXML(witsmlObjects);
                break;
            default:
                throw new ValveException("Unsupported object type: " + witsmlObjects.get(0).getObjectType());
        }

        return xmlString;
    }

    private static String consolidate1311WellboresToXML(
        ArrayList<AbstractWitsmlObject> witsmlObjects
    ) throws ValveException {
        try {
            // get parent object from first child
            com.hashmapinc.tempus.WitsmlObjects.v1311.ObjWellbores parent = 
                new com.hashmapinc.tempus.WitsmlObjects.v1311.ObjWellbores();

            // consolidate children
            for (AbstractWitsmlObject child : witsmlObjects) {
                parent.addWellbore(
                    (com.hashmapinc.tempus.WitsmlObjects.v1311.ObjWellbore) child
                );
            }

            // return xml
            return WitsmlMarshal.serialize(parent);
        } catch (Exception e ) {
            throw new ValveException(e.getMessage());
        }
    }

    private static String consolidate1411WellboresToXML(
        ArrayList<AbstractWitsmlObject> witsmlObjects
    ) throws ValveException {
        try {
            // get parent object from first child
            com.hashmapinc.tempus.WitsmlObjects.v1411.ObjWellbores parent = 
                new com.hashmapinc.tempus.WitsmlObjects.v1411.ObjWellbores();

            // consolidate children
            for (AbstractWitsmlObject child : witsmlObjects) {
                parent.addWellbore(
                    (com.hashmapinc.tempus.WitsmlObjects.v1411.ObjWellbore) child
                );
            }

            // return xml
            return WitsmlMarshal.serialize(parent);
        } catch (Exception e ) {
            throw new ValveException(e.getMessage());
        }
    }
}