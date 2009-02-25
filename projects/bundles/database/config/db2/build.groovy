// -- START LICENSE
// Copyright 2008 Sun Microsystems, Inc. All rights reserved.
// 
// U.S. Government Rights - Commercial software. Government users 
// are subject to the Sun Microsystems, Inc. standard license agreement
// and applicable provisions of the FAR and its supplements.
// 
// Use is subject to license terms.
// 
// This distribution may include materials developed by third parties.
// Sun, Sun Microsystems, the Sun logo, Java and Project Identity 
// Connectors are trademarks or registered trademarks of Sun 
// Microsystems, Inc. or its subsidiaries in the U.S. and other
// countries.
// 
// UNIX is a registered trademark in the U.S. and other countries,
// exclusively licensed through X/Open Company, Ltd. 
// 
// -----------
// DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
// 
// Copyright 2008 Sun Microsystems, Inc. All rights reserved. 
// 
// The contents of this file are subject to the terms of the Common Development
// and Distribution License(CDDL) (the License).  You may not use this file
// except in  compliance with the License. 
// 
// You can obtain a copy of the License at
// http://identityconnectors.dev.java.net/CDDLv1.0.html
// See the License for the specific language governing permissions and 
// limitations under the License.  
// 
// When distributing the Covered Code, include this CDDL Header Notice in each
// file and include the License file at identityconnectors/legal/license.txt.
// If applicable, add the following below this CDDL Header, with the fields 
// enclosed by brackets [] replaced by your own identifying information: 
// "Portions Copyrighted [year] [name of copyright owner]"
// -----------
// -- END LICENSE
//
// @author Zdenek Louzensky, David Adam

/*  +---------------------------------------------------+
 *  ----------- Contract Tests configuration ------------
 *  +---------------------------------------------------+ 
 */

import org.identityconnectors.contract.data.groovy.Lazy
import org.identityconnectors.common.security.GuardedString

// Connector configuration
connector {
      jdbcDriver="com.ibm.db2.jcc.DB2Driver"
      keyColumn="ACCOUNTID"
      passwordColumn="PASSWORD"
      table="ACCOUNTS"
      user="__configureme__"
      password="__configureme__"
      jdbcUrlTemplate="jdbc:db2://%h:%p/%d"
      host="__configureme__"
      port="50000"
      database="SAMPLECT"      
}
