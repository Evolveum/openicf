/**
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 ForgeRock AS. All Rights Reserved
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
 * 
 * @author Gael Allioux <gael.allioux@forgerock.com>
 *
 */
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.AttributeInfo.Flags;
import org.identityconnectors.framework.common.objects.AttributeInfoBuilder;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;

// Parameters:
// The connector sends the following:
// action: a string describing the action ("SCHEMA" here)
// log: a handler to the Log facility
// builder: SchemaBuilder instance for the connector

log.info("Entering "+action+" Script");

attrsInfo = new HashSet<AttributeInfo>();


//ORG ASSIGNEMENT (INFOTYPE P0001):
//PERNO, value:00200014
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:PERNO", String.class));
//INFOTYPE, value:0001
//SUBTYPE, value:
//OBJECT_ID, value:
//LOCK_IND, value:
//TO_DATE, value:Fri Dec 31 00:00:00 CET 9999
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:TO_DATE", String.class));
//FROM_DATE, value:Sun Jan 01 00:00:00 CET 2012
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:FROM_DATE", String.class));
//SEQNO, value:000
//CH_ON, (Last changed on), value:Tue Oct 25 00:00:00 CEST 2011
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:CH_ON", String.class));
//CHANGED_BY, value:ADMIN
//COMP_CODE, (Company Code), value:1000
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:COMP_CODE", String.class));
//PERS_AREA, (Personnel Area), value:1000
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:PERS_AREA", String.class));
//EGROUP, (Employee Group),value:T
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:EGROUP", String.class));
//ESUBGROUP, (Employee Subgroup)value:TA
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:ESUBGROUP", String.class));
//ORG_KEY, (Organizational Key), value:521101
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:ORG_KEY", String.class));
//BUS_AREA, value:
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:BUS_AREA", String.class));
//P_SUBAREA, value:1000
//LEG_PERSON, value:1000
//PAYAREA, value:A1
//CONTRACT, value:
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:CONTRACT", String.class));
//COSTCENTER, value:
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:COSTCENTER", String.class));
//ORG_UNIT, value:00000000
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:ORG_UNIT", String.class));
//POSITION, value:99999999
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:POSITION", String.class));
//JOB, value:00000000
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:JOB", String.class));
//SUPERVISOR, value:
//PAYR_ADMIN, value:
//PERS_ADMIN, value:ASC
//TIME_ADMIN, value:
//SORT_NAME, (full name sorted: LASTN FIRSTN), value:DOE JOHN
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:SORT_NAME", String.class));
//NAME,(full name), value:Prof. John Doe
attrsInfo.add(AttributeInfoBuilder.build("ORG_ASSIGNMENT:NAME", String.class));
//HIST_FLAG, value:
//TEXTFLAG, value:
//REF_FLAG, value:
//CNFRM_FLAG, value:
//SCREENCTRL, value:
//REASON, value:
//FLAG1, value:
//FLAG2, value:
//FLAG3, value:
//FLAG4, value:
//RESERVED1, value:
//RESERVED2, value:


//*************PERSONAL DATA (INFOTYPE P0002):
//PERNO: employee number, example value:00200014
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:PERNO", String.class));
//FIRSTNAME, example value:John
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:FIRSTNAME", String.class));
//LAST_NAME, example value:Doe
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:LAST_NAME", String.class));
//INITIALS, value:
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:INITIALS", String.class));
//TITLE, value:Prof. 
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:TITLE", String.class));
//KNOWN_AS, value:
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:KNOWN_AS", String.class));
//ARI_TITLE, value:
//LAST_NAME2, value:
//TITLE_2, value:
//GENDER, value:1
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:GENDER", String.class));
//LANGU, value:D
//TO_DATE, (Employee's data valid to), value:Fri Dec 31 00:00:00 CET 9999
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:TO_DATE", String.class));
//FROM_DATE, (Employee's data valid from), value:Tue Mar 01 00:00:00 CET 2011
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:FROM_DATE", String.class));
//BIRTHDATE, (Employee's Birth date), value:Fri Jul 21 00:00:00 CET 1950
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:BIRTHDATE", String.class));
//BIRTHCTRY, (Employee's Birth country), value: DE
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:BIRTHCTRY", String.class));
//BIRTHSTATE, value:
//BIRTHPLACE, (Employee's Birth place), value:Berlin
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:BIRTHPLACE", String.class));
//NATIONAL, (Employee’s primary nationality),value:DE
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:NATIONAL", String.class));
//NATIONAL_2, (Employee’s second nationality), value:
attrsInfo.add(AttributeInfoBuilder.build("PERSONAL_DATA:NATIONAL_2", String.class));
//NATIONAL_3, value:
//BIRTHYEAR, (Employee's year of birth), value:1950
//BIRTHMONTH, (Employee's month of birth), value:07
//BIRTHDAY, (Employee's day of birth), value:21
//LASTNAME_M, value:DOE
//FSTNAME_M, value:JOHN
//INFOTYPE, value:0002
//SUBTYPE, value:
//OBJECT_ID, value:
//LOCK_IND, value:
//SEQNO, value:000
//CH_ON, value:Mon Oct 24 00:00:00 CEST 2011
//CHANGED_BY, value:ADMIN
//HIST_FLAG, value:
//TEXTFLAG, value:
//REF_FLAG, value:
//CNFRM_FLAG, value:
//SCREENCTRL, value:
//REASON, value:
//FLAG1, value:
//FLAG2, value:
//FLAG3, value:
//FLAG4, value:
//RESERVED1, value:
//RESERVED2, value:
//NAMEAFFIX, value:
//NAMEPREFIX, value:
//NAME_FORM, value:00
//FORMOFADR, value:1
//RELIGION, value:
//MAR_STATUS, value:0
//NO_O_CHLDR, value:0
//NAME_CON, value:
//PERMO, value:
//PERID, value:
//BIRTHDTPP, value:Fri Jul 21 00:00:00 CET 1950
//FST_NAME_K, value:
//LST_NAME_K, value:
//FST_NAME_R, value:
//LST_NAME_R, value:
//BIRTHNME_K, value:
//BIRTHNME_R, value:
//NICKNAME_K, value:
//NICKNAME_R, value:


//COMMUNICATION DATA (INFOTYPE P0105, SUBTYPE 0010) - EMAIL:
//EMPLOYEENO, (Employee number = PERNO)value:00200014
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:EMAIL:EMPLOYEENO", String.class));
//SUBTYPE, value:0010
//OBJECTID, value:
//LOCKINDIC, value:
//VALIDEND, (Email address valid till), value:Fri Dec 31 00:00:00 CET 9999
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:EMAIL:VALIDEND", String.class));
//VALIDBEGIN, (Email address valid from), value:Thu Oct 25 00:00:00 CEST 2012
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:EMAIL:VALIDBEGIN", String.class));
//RECORDNR, value:000
//COMMTYPE, value:0010
//NAMEOFCOMMTYPE, value:
//ID, (Email address), value:BOB.FLEMMING@FAST.COM
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:EMAIL:ID", String.class));
//

//COMMUNICATION DATA (INFOTYPE P0105, SUBTYPE 0001) - SYSTEM USER NAME
//EMPLOYEENO, (Employee number = PERNO)value:00200014
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:ACCOUNT:EMPLOYEENO", String.class));
//SUBTYPE, value:0010
//OBJECTID, value:
//LOCKINDIC, value:
//VALIDEND, (Email address valid till), value:Fri Dec 31 00:00:00 CET 9999
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:ACCOUNT:VALIDEND", String.class));
//VALIDBEGIN, (Email address valid from), value:Thu Oct 25 00:00:00 CEST 2012
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:ACCOUNT:VALIDBEGIN", String.class));
//RECORDNR, value:000
//COMMTYPE, value:0010
//NAMEOFCOMMTYPE, value:
//ID, (Account name), value:BFLEMMING
attrsInfo.add(AttributeInfoBuilder.build("COMMUNICATION:ACCOUNT:ID", String.class));



final ObjectClassInfo ociEmployee = new ObjectClassInfoBuilder().setType("EMPLOYEE").addAllAttributeInfo(attrsInfo).build();
builder.defineObjectClass(ociEmployee);
builder.removeSupportedObjectClass(org.identityconnectors.framework.spi.operations.CreateOp.class,ociEmployee)
builder.removeSupportedObjectClass(org.identityconnectors.framework.spi.operations.DeleteOp.class,ociEmployee)
builder.removeSupportedObjectClass(org.identityconnectors.framework.spi.operations.SyncOp.class,ociEmployee)

log.info("Schema script done");