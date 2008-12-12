/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2007-2008 Sun Microsystems, Inc. All rights reserved.     
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
using System;

using System.Collections.Generic;

namespace Org.IdentityConnectors.Common.Pooling
{
    /**
     * Configuration for pooling objects
     */
    public sealed class ObjectPoolConfiguration {
    
        /**
         * Max objects (idle+active). 
         */
        private int _maxObjects = 10;
    
        /**
         * Max idle objects.
         */
        private int _maxIdle = 10;
    
        /**
         * Max time to wait if the pool is waiting for a free object to become
         * available before failing. Zero means don't wait
         */
        private long _maxWait = 150 * 1000;
    
        /**
         * Minimum time to wait before evicting an idle object.
         * Zero means don't wait
         */
        private long _minEvictableIdleTimeMillis = 120 * 1000;
    
        /**
         * Minimum number of idle objects.
         */
        private int _minIdle = 1;
    
    
        /**
         * Get the set number of maximum objects (idle+active)
         */
        public int MaxObjects {
            get {
                return _maxObjects;
            }
            set {
                _maxObjects = value;
            }
        }
        
        /**
         * Get the maximum number of idle objects.
         */
        public int MaxIdle {
            get {
                return _maxIdle;
            }
            set {
                _maxIdle = value;
            }
        }
        
        /**
         * Max time to wait if the pool is waiting for a free object to become
         * available before failing. Zero means don't wait
         */
        public long MaxWait {
            get {
                return _maxWait;
            }
            set {
                _maxWait = value;
            }
        }
        
        /**
         * Minimum time to wait before evicting an idle object.
         * Zero means don't wait
         */
        public long MinEvictableIdleTimeMillis {
            get {
                return _minEvictableIdleTimeMillis;
            }
            set {
                _minEvictableIdleTimeMillis = value;
            }
        }
        
        /**
         * Minimum number of idle objects.
         */
        public int MinIdle {
            get {
                return _minIdle;
            }
            set {
                _minIdle = value;
            }
        }
            
        public void Validate() {
            if (_minIdle < 0) {
                throw new InvalidOperationException("Min idle is less than zero.");
            }
            if (_maxObjects < 0) {
                throw new InvalidOperationException("Max active is less than zero.");
            }
            if (_maxIdle < 0) {
                throw new InvalidOperationException("Max idle is less than zero.");
            }
            if (_maxWait < 0) {
                throw new InvalidOperationException("Max wait is less than zero.");
            }
            if (_minEvictableIdleTimeMillis < 0) {
                throw new InvalidOperationException("Min evictable idle time millis less than zero.");
            }
            if ( _minIdle > _maxIdle ) {
                throw new InvalidOperationException("Min idle is greater than max idle.");            
            }
            if ( _maxIdle > _maxObjects ) {
                throw new InvalidOperationException("Max idle is greater than max objects.");                        
            }
        }
    
        public override int GetHashCode() {
            unchecked {
                return (int)(MaxObjects+MaxIdle+MaxWait+MinEvictableIdleTimeMillis+MinIdle);        
            }
        }
    
        public override bool Equals(Object obj) {
            if ( obj is ObjectPoolConfiguration) {
                ObjectPoolConfiguration other = (ObjectPoolConfiguration)obj;
                
                if (MaxObjects != other.MaxObjects) {
                    return false;
                }
                if (MaxIdle != other.MaxIdle) {
                    return false;
                }
                if (MaxWait != other.MaxWait) {
                    return false;
                }
                if (MinEvictableIdleTimeMillis != other.MinEvictableIdleTimeMillis) {
                    return false;
                }
                if (MinIdle != other.MinIdle) {
                    return false;
                }
                return true;
            }
            return false;
        }
        
        public override String ToString() {
            // poor man's toString()
            IDictionary<String, Object> bld = new Dictionary<String, Object>();
            bld["MaxObjects"] = MaxObjects;
            bld["MaxIdle"] = MaxIdle;
            bld["MaxWait"] = MaxWait;
            bld["MinEvictableIdleTimeMillis"] = MinEvictableIdleTimeMillis;
            bld["MinIdle"] = MinIdle;
            return bld.ToString();
        }
    }
}
