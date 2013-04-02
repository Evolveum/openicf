/*
 * DO NOT REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://forgerock.org/license/CDDLv1.0.html
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at http://forgerock.org/license/CDDLv1.0.html
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 */

package org.forgerock.openicf.maven;

import java.util.Comparator;


/**
 * A ClassComparator compares the name (CASE_INSENSITIVE) of {@link Class}.
 * 
 * @author Laszlo Hordos
 */
public class ClassComparator implements Comparator<Class> {
    public int compare(Class o1, Class o2) {
        return String.CASE_INSENSITIVE_ORDER.compare(o1.getSimpleName(), o2.getSimpleName());
    }
}