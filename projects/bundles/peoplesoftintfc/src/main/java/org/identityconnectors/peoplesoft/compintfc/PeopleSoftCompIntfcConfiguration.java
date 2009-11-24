/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */

package org.identityconnectors.peoplesoft.compintfc;

import org.identityconnectors.peoplesoft.*;


/**
 * @author kitko
 *
 */
public final class PeopleSoftCompIntfcConfiguration extends PeopleSoftAbstractConfiguration {
    private String rwCompIntfc;
    private String delCompIntfc;

    /**
     * @return the rwCompIntfc
     */
    public String getRwCompIntfc() {
        return rwCompIntfc;
    }

    /**
     * @param rwCompIntfc the rwCompIntfc to set
     */
    public PeopleSoftCompIntfcConfiguration setRwCompIntfc(String rwCompIntfc) {
        this.rwCompIntfc = rwCompIntfc;
        return this;
    }

    /**
     * @return the delCompIntfc
     */
    public String getDelCompIntfc() {
        return delCompIntfc;
    }

    /**
     * @param delCompIntfc the delCompIntfc to set
     */
    public PeopleSoftCompIntfcConfiguration setDelCompIntfc(String delCompIntfc) {
        this.delCompIntfc = delCompIntfc;
        return this;
    }

    @Override
    public void validate() {
    }
}
