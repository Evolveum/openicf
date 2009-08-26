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

package org.identityconnectors.googleapps;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to compute the change set for lists
 * For example - when doing an update on nicknames, we need to compute
 * the set of names to add, and the set of names to delete.
 *
 * This can save on bandwidth. The trivial implementation is to delete
 * all of the current list items, then add in the new ones - but this
 * can cause additional traffic to the resource. This class computes the
 * minimal change set (minimal set of things to add, and delete).
 *
 * @author warrenstrange
 */
public class ChangeSet {

    private List<String> toAdd;
    private List<String> toRemove;


    public ChangeSet(List<String> existingList, List<String> updatedList) {

        // make copies so as not to touch the orignial lists
        toAdd = new ArrayList<String>(updatedList);
        toRemove = new ArrayList<String>(existingList);

        // remove all the names from toAdd that already exist
        //  (there no need to add them again);
        toAdd.removeAll(existingList);


        // If a name currently exists, and is NOT
        // on the update list (list of to be names)
        // then we need to remove it
        // by subtracting the names that should be on the list
        // we are left with just the names that should NOT be on the list
        toRemove.removeAll(updatedList);

    }

    public List<String> itemsToBeRemoved() {
        return toRemove;
    }

    public List<String> itemsToBeAdded() {
        return toAdd;
    }
}

