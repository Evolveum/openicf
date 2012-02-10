/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright © 2012 ForgeRock AS. All rights reserved.
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
 * $Id$
 */
package org.forgerock.openicf.salesforce.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.identityconnectors.framework.common.objects.AttributeInfo;

import org.forgerock.openicf.salesforce.translators.EmptyTranslator;
import org.forgerock.openicf.salesforce.translators.IFieldTranslator;


@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ICAttribute {

    /**
     * Defines the name of the attribute. The name parameter is used by
     * the Identity Connector framework to reference the attribute.
     */
    public String name();

    /**
     * The <code>bean</code> property that should be mapped to this attribute.
     */
    public String property();

    /**
     * Defines a local property reference that will override the 'real' property
     * on read/write operations. The 'real' {@link #property()} must still be
     * defined since it is used to generate queries on.
     */
    public String local() default "";

    /**
     * Whether or not this attribute can be queried from client code.
     * Default is <code>true</code>.
     */
    public boolean queryable() default true;

    /**
     * Optional field for overriding database columns. Queries will be generated
     * using column fields defined in the entity bean. Setting the custom field
     * will override the column name and optionally provide additional table joins.
     */
    public Custom custom() default @Custom(field = "");

    /**
     * The java type of the mapped attribute. Defaults to {@link String}.
     */
    public Class<?> type() default String.class;

    /**
     * Optional field for specifying Identity Connector flags.
     * Defaults to an empty array.
     */
    public AttributeInfo.Flags[] flags() default {};

    /**
     * Optional field for specifying value translators. If the external
     * value type differ from the internal value type, than {@link IFieldTranslator}'s
     * may be used to convert to and from.
     * Defaults to {@link EmptyTranslator}
     */
    public Class<? extends IFieldTranslator<?, ?>> translator() default EmptyTranslator.class;
}
