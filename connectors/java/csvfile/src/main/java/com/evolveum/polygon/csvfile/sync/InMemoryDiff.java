/*
 *
 * Copyright (c) 2010 ForgeRock Inc. All Rights Reserved
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1.php or
 * OpenIDM/legal/CDDLv1.0.txt
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at OpenIDM/legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted 2010 [name of copyright owner]"
 *
 * Portions Copyrighted 2011 Viliam Repan (lazyman)
 *
 * $Id$
 */
package com.evolveum.polygon.csvfile.sync;

import static com.evolveum.polygon.csvfile.util.Utils.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

import com.evolveum.polygon.csvfile.util.PositionedCsvItem;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;

import com.evolveum.polygon.csvfile.CSVFileConfiguration;
import com.evolveum.polygon.csvfile.util.CSVSchemaException;
import com.evolveum.polygon.csvfile.util.CsvItem;
import com.evolveum.polygon.csvfile.util.Utils;

/**
 *
 * @author Viliam Repan (lazyman)
 */
public class InMemoryDiff {

    private static final Log log = Log.getLog(InMemoryDiff.class);

    private CSVFileConfiguration configuration;
    private Pattern linePattern;
    private File oldFile;
    private File newFile = null;

    public InMemoryDiff(File oldFile, File newFile, Pattern linePattern, CSVFileConfiguration configuration) {
        notNullArgument(newFile, "newFile");
        notNullArgument(linePattern, "linePattern");
        notNullArgument(configuration, "configuration");

        this.oldFile = oldFile;
        this.newFile = newFile;
        this.configuration = configuration;
        this.linePattern = linePattern;
    }

    @SuppressWarnings("unchecked")
    public List<Change> diff() throws DiffException {
        log.info("Computing diff from old {0} ({1}) and new {2} ({3}).",
                (oldFile != null ? oldFile.getName() : "null"), (oldFile != null ? oldFile.length() : 0),
                newFile.getName(), newFile.length());

        List<Change> changes = new ArrayList<Change>();
        try {
            if (oldFile != null) {
                testHeaders(newFile, oldFile);
            }

            RecordSet newRecordSet = createRecordSet(newFile);
            int uidIndex = newRecordSet.getHeaders().indexOf(configuration.getUniqueAttribute());
            if (oldFile != null) {
                RecordSet oldRecordSet = createRecordSet(oldFile);
                //compare records from both record sets
                List<PositionedCsvItem> newList = new ArrayList<>(newRecordSet.getRecords());
                List<PositionedCsvItem> oldList = new ArrayList<>(oldRecordSet.getRecords());

                log.info("Record set size, new: {0}, old: {1}", newList.size(), oldList.size());

                if (oldList.isEmpty()) {
                    createOneTypeChanges(newRecordSet, uidIndex, changes, Change.Type.CREATE);
                } else if (newList.isEmpty()) {
                    createOneTypeChanges(oldRecordSet, uidIndex, changes, Change.Type.DELETE);
                } else {
                    changes.addAll(findChanges(uidIndex, newRecordSet.getHeaders(), newList, oldList));
                }
            } else {
                //everything will be add
                createOneTypeChanges(newRecordSet, uidIndex, changes, Change.Type.CREATE);
            }
        } catch (IOException ex) {
            throw new ConnectorIOException(ex.getMessage(), ex);
        } catch (ConnectorException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new DiffException("Can't create csv diff, reason: " + ex.getMessage(), ex);
        }

		Collections.sort(changes, new ChangeComparator());
        return changes;
    }

    private void createOneTypeChanges(RecordSet recordSet, int uidIndex, List<Change> changes, Change.Type type) {
        Set<PositionedCsvItem> items = recordSet.getRecords();
        Iterator<PositionedCsvItem> iterator = items.iterator();
        Change change;
        while (iterator.hasNext()) {
            PositionedCsvItem item = iterator.next();

            change = new Change(item.getAttribute(uidIndex), type,
                    recordSet.getHeaders(), item.getAttributes(), item.getPosition());
            changes.add(change);
        }
    }

    private List<Change> findChanges(int uidIndex, List<String> headers, List<PositionedCsvItem> newList, List<PositionedCsvItem> oldList) {
        List<Change> changes = new ArrayList<>();
        Change change;
        int newIndex = 0, oldIndex = 0;
        String oldUid, newUid;

        outer:
        for (; newIndex < newList.size(); newIndex++) {
            PositionedCsvItem newItem = newList.get(newIndex);
            newUid = newItem.getAttribute(uidIndex);

            if (oldIndex >= oldList.size()) {
                break;
            }

            for (; oldIndex < oldList.size();) {
                PositionedCsvItem oldItem = oldList.get(oldIndex);
                oldUid = oldItem.getAttribute(uidIndex);

                int compare = String.CASE_INSENSITIVE_ORDER.compare(newUid, oldUid);
                if (compare < 0) {
                    PositionedCsvItem item = newList.get(newIndex);
                    change = new Change(item.getAttribute(uidIndex), Change.Type.CREATE,
                            headers, item.getAttributes(), item.getPosition());
                    changes.add(change);
                    break;
                } else if (compare > 0) {
                    PositionedCsvItem item = oldList.get(oldIndex);
                    change = new Change(item.getAttribute(uidIndex), Change.Type.DELETE,
                            headers, item.getAttributes(), item.getPosition());
                    changes.add(change);
                    oldIndex++;
                } else {
                    if (!isEqual(newItem, oldItem)) {
                        change = new Change(newItem.getAttribute(uidIndex), Change.Type.MODIFY,
                                headers, newItem.getAttributes(), newItem.getPosition());
                        changes.add(change);
                    }
                    oldIndex++;
                    break;
                }

                if (oldIndex >= oldList.size()) {
                    break outer;
                }
            }
        }

        for (; newIndex < newList.size(); newIndex++) {
            PositionedCsvItem item = newList.get(newIndex);
            change = new Change(item.getAttribute(uidIndex), Change.Type.CREATE,
                    headers, item.getAttributes(), item.getPosition());
            changes.add(change);
        }
        for (; oldIndex < oldList.size(); oldIndex++) {
            PositionedCsvItem item = oldList.get(oldIndex);
            change = new Change(item.getAttribute(uidIndex), Change.Type.DELETE,
                    headers, item.getAttributes(), item.getPosition());
            changes.add(change);
        }

        return changes;
    }

    private boolean isEqual(CsvItem item1, CsvItem item2) {
        return Arrays.equals(item1.getAttributes().toArray(), item2.getAttributes().toArray());
    }

    private RecordSet createRecordSet(File file) throws IOException {
        RecordSet recordSet = null;

        BufferedReader reader = null;
        try {
            reader = createReader(file, configuration);
            List<String> headers = readHeader(reader, linePattern, configuration);
            int index = headers.indexOf(configuration.getUniqueAttribute());
            if (index < 0 || index >= headers.size()) {
                throw new CSVSchemaException("Header in '" + file.getAbsolutePath()
                        + "' doesn't contain unique attribute '" + configuration.getUniqueAttribute()
                        + "' as defined in configuration.");
            }
            Set<PositionedCsvItem> set = new TreeSet<>(new CsvItemComparator(index));
            String line;
            int lineNumber = 1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (isEmptyOrComment(line)) {
                    continue;
                }

                set.add(Utils.createCsvItem(headers, line, lineNumber, linePattern, configuration));
            }

            recordSet = new RecordSet(headers, set);
        } finally {
            if (reader != null) {
                closeReader(reader, null);
            }
        }

        return recordSet;
    }

    private void testHeaders(File newFile, File oldFile) throws IOException, CSVSchemaException {
        List<String> newHeaders = null;
        List<String> oldHeaders = null;

        BufferedReader newReader = null;
        BufferedReader oldReader = null;
        try {
            newReader = createReader(newFile, configuration);
            newHeaders = readHeader(newReader, linePattern, configuration);

            oldReader = createReader(oldFile, configuration);
            oldHeaders = readHeader(oldReader, linePattern, configuration);
        } finally {
            closeReader(newReader, null);
            closeReader(oldReader, null);
        }

        if (newHeaders == null || oldHeaders == null || !Arrays.equals(newHeaders.toArray(),
                oldHeaders.toArray())) {
            throw new CSVSchemaException("Headers in files '" + newFile.getPath()
                    + "' and '" + oldFile.getPath() + "' doesn't match.");
        }
    }

	private class ChangeComparator implements Comparator<Change> {
		@Override
		public int compare(Change o1, Change o2) {
			// first create/modify entries, then delete ones
			if ((o1.getType() == Change.Type.CREATE || o1.getType() == Change.Type.MODIFY) && o2.getType() == Change.Type.DELETE) {
				return -1;
			}
			if ((o2.getType() == Change.Type.CREATE || o2.getType() == Change.Type.MODIFY) && o1.getType() == Change.Type.DELETE) {
				return 1;
			}
			return Integer.compare(o1.getPosition(), o2.getPosition());
		}
	}
}
