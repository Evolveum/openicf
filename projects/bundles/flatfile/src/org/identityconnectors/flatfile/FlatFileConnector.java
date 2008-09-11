/*
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved.
 * 
 * U.S. Government Rights - Commercial software. Government users 
 * are subject to the Sun Microsystems, Inc. standard license agreement
 * and applicable provisions of the FAR and its supplements.
 * 
 * Use is subject to license terms.
 * 
 * This distribution may include materials developed by third parties.
 * Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
 * Connectors are trademarks or registered trademarks of Sun 
 * Microsystems, Inc. or its subsidiaries in the U.S. and other
 * countries.
 * 
 * UNIX is a registered trademark in the U.S. and other countries,
 * exclusively licensed through X/Open Company, Ltd. 
 * 
 * -----------
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
 * 
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License(CDDL) (the License).  You may not use this file
 * except in  compliance with the License. 
 * 
 * You can obtain a copy of the License at
 * http://identityconnectors.dev.java.net/CDDLv1.0.html
 * See the License for the specific language governing permissions and 
 * limitations under the License.  
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each
 * file and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * -----------
 */
package org.identityconnectors.flatfile;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.identityconnectors.common.IOUtil;
import org.identityconnectors.common.StringUtil;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ConnectorObjectBuilder;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;


/**
 * Only implements search since this connector is only used to do sync.
 */
@ConnectorClass(configurationClass=FlatFileConfiguration.class,
                  displayNameKey="FlatFile") //TODO: l10n
public class FlatFileConnector implements Connector, SearchOp<String>, SchemaOp {
    /**
     * Setup {@link Connector} based logging.
     */
    private static Log log = Log.getLog(FlatFileConnector.class);

    // ===================================================================
    // Constants
    // ===================================================================
    private final static String MSG_SKIPPING = "Skipping blank line.";

    // =======================================================================
    // Fields
    // =======================================================================
    /**
     * Configuration information passed back to the {@link Connector} by the method
     * {@link Connector#init(Configuration)}.
     */
    private FlatFileConfiguration cfg;
    
    public Configuration getConfiguration() {
        return this.cfg;
    }

    /**
     * Saves the configuration for use in later calls.
     * 
     * @see org.identityconnectors.framework.Connector#init(org.identityconnectors.framework.Configuration)
     */
    public void init(Configuration cfg) {
        this.cfg = (FlatFileConfiguration) cfg;
    }

    /**
     * Nothing to do since there's not resources used.
     * 
     * @see org.identityconnectors.framework.Connector#dispose()
     */
    public void dispose() {
        // in this matter do nothing..
    }

    /**
     * Read the header from the file and determine the attributes for the
     * account object this resource supports.
     * 
     * @see org.identityconnectors.framework.Connector#getSupportedObjectTypes()
     */
    public Schema schema() {
        // read the header to construct an ConnectionObjectInfo..
        final SchemaBuilder bld = new SchemaBuilder(getClass());
        BufferedReader rdr = null;
        try {
            // open the file for reading..
            rdr = this.cfg.newFileReader();
            // build the connector info object..
            // read the header..
            Set<AttributeInfo> attrInfos = new HashSet<AttributeInfo>(); 
            List<String> fieldNames = readHeader(rdr, this.cfg
                    .getFieldDelimiter(), this.cfg.getTextQualifier(), this.cfg
                    .getUniqueAttributeName());
            for (String fieldName : fieldNames) {
                AttributeInfoBuilder abld = new AttributeInfoBuilder();
                abld.setName(fieldName);
                abld.setCreateable(false);
                abld.setUpdateable(false);
                attrInfos.add(abld.build());
            }
            // set it to object class account..
            bld.defineObjectClass(ObjectClass.ACCOUNT_NAME, attrInfos);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } finally {
            IOUtil.quietClose(rdr);
        }
        // return the new schema object..
        return bld.build();
    }
    
    public FilterTranslator<String> createFilterTranslator(ObjectClass oclass, OperationOptions options) {
        //flat files are not queryable - don't translate anything
        return new AbstractFilterTranslator<String>(){};
    }

    /**
     * Searches for objects to return based on the filter provided with offset
     * and limit features.
     */
    public void executeQuery(ObjectClass oclass, String query, ResultsHandler handler, OperationOptions options) {
        /**
         * Track the number of lines processed.
         */
        long lines = 0;


        /**
         * Text qualifier character.
         */
        final char textQualifier = cfg.getTextQualifier();

        /**
         * Field delimiter.
         */
        final char fieldSeparator = cfg.getFieldDelimiter();

        /**
         * Unique identifier field.
         */
        final String uniqueIdField = cfg.getUniqueAttributeName();

        /**
         * Internal reader initialized in the constructor.
         */
        BufferedReader rdr = null;
        
        try {
	        rdr = cfg.newFileReader();
	        /**
	         * Fields names read from the header.
	         */
	        List<String> fieldNames = FlatFileConnector.readHeader(rdr, fieldSeparator, textQualifier,
	                uniqueIdField);
	        
            String line;
            while ((line = rdr.readLine()) != null) {
                ++lines;
                if (line.trim().length() == 0) {
                    log.info(MSG_SKIPPING);
                    continue;
                }
                log.ok("Processing Data Line: {0}", line);
                List<String> fieldValues = 
                	StringUtil.parseLine(line, fieldSeparator, textQualifier);
                if (fieldValues == null) {
                    log.error("Error: {0}", line);
                    break;
                } else {
                    ConnectorObjectBuilder bld = new ConnectorObjectBuilder();
                    for (int i = 0; i < fieldValues.size(); ++i) {
                        String name = fieldNames.get(i);
                        String value = fieldValues.get(i);
                        if (name.equals(uniqueIdField)) {
                            bld.setUid(value);
                            bld.setName(value);
                        } else {
                            bld.addAttribute(name, value);
                        }
                    }
                    // create the connector object..
                    ConnectorObject ret = bld.build();
                    if (!handler.handle(ret)) {
	                    break;
                    }
                }
            }
        }
        catch (IOException e) {
            throw new ConnectorIOException(e);
        }
        finally {
        	IOUtil.quietClose(rdr);
        }
    }

    // =======================================================================
    // Helper Classes
    // =======================================================================


    /**
     * Common code needed to determine the headers names for AttributeInfo and
     * ConnectorObjectInfo. Reads the header to determine the field names that
     * are supported.
     */
    static List<String> readHeader(final BufferedReader rdr,
            final char fieldSeparator, final char textQualifier,
            final String uniqueAttribute) throws IOException {
        String line;
        List<String> ret = null;
        while ((line = rdr.readLine()) != null) {
            if (line.trim().length() == 0) {
                log.info(MSG_SKIPPING);
                continue;
            }
            log.ok("Processing Header Line: {0}", line);
            ret = StringUtil.parseLine(line, fieldSeparator, textQualifier);
            if (ret == null) {
                final String msg = "Error Parsing field names: Line ";
                throw new IllegalStateException(msg + line);
            }
            if (!ret.contains(uniqueAttribute)) {
                final String msg = "Error unique attribute field does not exist: Line ";
                throw new IllegalStateException(msg + line);
            }

            log.info("Found Field Names: {0}", ret);
            break;
        }
        return ret;
    }

}
