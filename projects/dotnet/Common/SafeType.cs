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

using System;

using System.Reflection;

namespace Org.IdentityConnectors.Common
{
   /// <summary>
   /// The equivalent of java's Class&lt;? extends...%gt; syntax.
   /// Allows you to restrict a Type to a certain class hierarchy.
   /// </summary>
   public abstract class SafeType<T>
       where T : class
   {
       /// <summary>
       /// Make private so no one else can subclass this.
       /// </summary>
       private SafeType()
       {
       }
       
       /// <summary>
       /// Returns the member of the group by its C# type
       /// </summary>
       /// <param name="type"></param>
       /// <returns></returns>
       public static SafeType<T> ForRawType(Type type)
       {
           if (!typeof(T).IsAssignableFrom(type)) {
               throw new ArgumentException("Type: "+type+" is not a subclass of"+typeof(T));
           }
           Type safeType = typeof(SafeType<T>);
           MethodInfo info = safeType.GetMethod("Get");
           info = info.MakeGenericMethod(new Type[]{type});                
           Object rv = info.Invoke(null,null);
           return (SafeType<T>)rv;
       }
       
       /// <summary>
       /// Gets an instance of the safe type.
       /// </summary>
       /// <returns>The instance of the safe type</returns>
       public static SafeType<T> Get<U>()
           where U : T
       {
           return new Impl<U>();
       }
       
       /// <summary>
       /// Returns the underlying C# type
       /// </summary>
       public abstract Type RawType {get;}
       /// <summary>
       /// Returns true iff these represent the same underlying type
       /// and the SafeType has the same parent type.
       /// </summary>
       /// <param name="o">The other</param>
       /// <returns>true iff these represent the same underylying type
       /// and the TypeGroup has the same parent type</returns>
       public override bool Equals(object o)
       {
           if ( o is SafeType<T> ) {
               SafeType<T> other = (SafeType<T>)o;
               return RawType.Equals(other.RawType);
           }
           return false;
       }
       /// <summary>
       /// Returns a hash of the type
       /// </summary>
       /// <returns>a hash of the type</returns>
       public override int GetHashCode()
       {
           return RawType.GetHashCode();
       }
       /// <summary>
       /// Returns a string representation of the member type
       /// </summary>
       /// <returns>a string representation of the member type</returns>
       public override string ToString()
       {
           return RawType.ToString();
       }
       /// <summary>
       /// Implementation
       /// </summary>
       private class Impl<U> : SafeType<T>
           where U : T
       {
           /// <summary>
           /// Creates the implementation
           /// </summary>
           public Impl()
           {
           }
           /// <summary>
           /// Returns the underlying raw type
           /// </summary>
           public override Type RawType
           {
               get
               {
                   return typeof(U);
               }
           }
       }
   }

    
}
