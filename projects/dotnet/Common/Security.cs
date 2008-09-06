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
using System.Security;
using System.Security.Cryptography;
using System.Runtime.InteropServices;
using System.Text;
namespace Org.IdentityConnectors.Common.Security
{
    #region UnmanagedArray
    /// <summary>
    /// Places an array facade on an unmanaged memory data structure that
    /// holds senstive data. (In C# placing senstive data in managed 
    /// memory is considered unsecure since the memory model allows
    /// data to be copied around).
    /// </summary>
    public interface UnmanagedArray<T> : IDisposable
    {
        int Length {get;}
        T this[int index] {get;set;}
    }
    #endregion
    
    /**
     * Secure string implementation that solves the problems associated with
     * keeping passwords as <code>java.lang.String</code>. That is, anything 
     * represented as a <code>String</code> is kept in memory as a clear
     * text password and stays in memory <b>at least</b> until it is garbage collected.
     * <p>
     * The GuardedString class alleviates this problem by storing the characters in
     * memory in an encrypted form. The encryption key will be a randomly-generated
     * key.
     * <p>
     * In their serialized form, GuardedString will be encrypted using a known
     * default key. This is to provide a minimum level of protection regardless
     * of the transport. For communications with the Remote Connector Framework
     * it is recommended that deployments enable SSL for true encryption.
     * <p>
     * Applications may also wish to persist GuardedStrings. In the case of 
     * Identity Manager, it should convert GuardedStrings to EncryptedData so
     * that they can be stored and managed using the Manage Encryption features
     * of Identity Manager. Other applications may wish to serialize APIConfiguration
     * as a whole. These applications are responsible for encrypting the APIConfiguration
     * blob for an additional layer of security (beyond the basic default key encryption
     * provided by GuardedString).
     */
    public sealed class GuardedString : IDisposable {
        /**
         * This method will be called with the clear text of the string.
         * After the call the clearChars array will be automatically zeroed
         * out, thus keeping the window of potential exposure to a bare-minimum.
         * @param clearChars
         */
        public delegate void Accessor(UnmanagedArray<char> clearChars); 
        
        
        private SecureString _target;
        private String _base64SHA1Hash;
        
        /**
         * Creates an empty secure string
         */
        public GuardedString() {
            _target = new SecureString();
            ComputeHash();
        }
        
        public GuardedString(SecureString str) {
            _target = str.Copy();
            ComputeHash();
        }
        
        
        /**
         * Provides access to the clear-text value of the string in a controlled fashion.
         * The clear-text characters will only be available for the duration of the call
         * and automatically zeroed out following the call. 
         * 
         * <p>
         * <b>NOTE:</b> Callers are encouraged to use {@link #verifyBase64SHA1Hash(String)}
         * where possible if the intended use is merely to verify the contents of
         * the string match an expected hash value.
         * @param accessor Accessor callback.
         * @throws IllegalStateException If the string has been disposed
         */
        public void Access(Accessor accessor) {
            using (SecureStringAdapter adapter = new SecureStringAdapter(_target)) {
                accessor(adapter);
            }
        }        
        
        /**
         * Appends a single clear-text character to the secure string.
         * The in-memory data will be decrypted, the character will be
         * appended, and then it will be re-encrypted.
         * @param c The character to append.
         * @throws IllegalStateException If the string is read-only
         * @throws IllegalStateException If the string has been disposed
         */
        public void AppendChar(char c) {
            _target.AppendChar(c);
            ComputeHash();
        }
        
        /**
         * Clears the in-memory representation of the string.
         */
        public void Dispose() {
            _target.Dispose();
        }
        
        /**
         * Returns true iff this string has been marked read-only
         * @return true iff this string has been marked read-only
         * @throws IllegalStateException If the string has been disposed
         */
        public bool IsReadOnly() {
            return _target.IsReadOnly();
        }
        
        /**
         * Mark this string as read-only.
         * @throws IllegalStateException If the string has been disposed
         */
        public void MakeReadOnly() {
            _target.MakeReadOnly();
        }
        
        /**
         * Create a copy of the string. If this instance is read-only,
         * the copy will not be read-only.
         * @return A copy of the string.
         * @throws IllegalStateException If the string has been disposed
         */
        public GuardedString Copy() {
            SecureString t2 = _target.Copy();
            GuardedString rv = new GuardedString(t2);
            return rv;
        }
        
        /**
         * Verifies that this base-64 encoded SHA1 hash of this string
         * matches the given value.
         * @param hash The hash to verify against.
         * @return True if the hash matches the given parameter.
         * @throws IllegalStateException If the string has been disposed
         */
        public bool VerifyBase64SHA1Hash(String hash) {
            CheckNotDisposed();
            return _base64SHA1Hash.Equals(hash);
        }
        
        public string GetBase64SHA1Hash() {
            CheckNotDisposed();
            return _base64SHA1Hash;            
        }
        
        
            
        private void CheckNotDisposed()
        {
            //this throws if disposed
            _target.IsReadOnly();
        }
        
    
        public override bool Equals(Object o) {
            if ( o is GuardedString ) {
                GuardedString other = (GuardedString)o;
                //not the true contract of equals. however,
                //due to the high mathematical improbability of
                //two unequal strings having the same secure hash,
                //this approach feels good. the alternative,
                //decrypting for comparison, is simply too
                //performance intensive to be used for equals
                return _base64SHA1Hash.Equals(other._base64SHA1Hash);
            }
            return false;
        }
        
        public override int GetHashCode() {
            return _base64SHA1Hash.GetHashCode();
        }
        
        public SecureString ToSecureString() {
            return _target.Copy();
        }
        
        private void ComputeHash()
        {
            Access(array=> {
                        _base64SHA1Hash = SecurityUtil.ComputeBase64SHA1Hash(array);
                  });            
        }
        
    }
    

    #region AbstractUnmanagedArray
    public abstract class AbstractUnmanagedArray<T> : UnmanagedArray<T>
    {
        private readonly int _length;
        private bool _disposed;
        public AbstractUnmanagedArray(int length) {
            if (length < 0) {
                throw new ArgumentException("Invalid length: "+length);
            }
            _length = length;
        }
        public int Length {
            get {
                if (_disposed) {
                    throw new ObjectDisposedException("UnmanagedArray");
                }
                return _length;               
            }
        }
        public T this[int index] {
            get {
                if (_disposed) {
                    throw new ObjectDisposedException("UnmanagedArray");
                }
                if ( index < 0 || index >= Length ) {
                    throw new IndexOutOfRangeException();
                }
                return GetValue(index);
            }
            set {
                if (_disposed) {
                    throw new ObjectDisposedException("SecureStringAdapter");
                }
                if ( index < 0 || index >= Length ) {
                    throw new IndexOutOfRangeException();
                }
                SetValue(index,value);
            }
        }
        public void Dispose() {
            if (!_disposed) {
                for ( int i = 0; i < Length; i++ ) {
                    this[i] = default(T);
                }
                _disposed = true;
                FreeMemory();
            }
        } 
        
        abstract protected T GetValue(int index);
        abstract protected void SetValue(int index, T val);
        abstract protected void FreeMemory();
    }
    #endregion
    
    #region SecureStringAdapter
    internal class SecureStringAdapter : AbstractUnmanagedArray<char>
    {
        private IntPtr _bstrPtr;
        public SecureStringAdapter(SecureString secureString) : base(secureString.Length) {
            Assertions.NullCheck(secureString,"secureString");
            _bstrPtr = Marshal.SecureStringToBSTR(secureString);
        }
        protected override char GetValue(int index)
        {            
            unsafe {
                char * charPtr = (char*)_bstrPtr;
                return *(charPtr+index);
            }
        }
        protected override void SetValue(int index, char c)
        {
            unsafe {
                char * charPtr = (char*)_bstrPtr;
                *(charPtr+index) = c;
            }                
        }
        protected override void FreeMemory()
        {
            Marshal.ZeroFreeBSTR( _bstrPtr );
        }
    }
    #endregion
    
    #region UnmanagedCharArray
    public class UnmanagedCharArray : AbstractUnmanagedArray<char>
    {
        private IntPtr _ptr;
        public UnmanagedCharArray(int length) : base(length) {
            unsafe {
                _ptr = Marshal.AllocHGlobal(length*sizeof(char));
            }
        }
        protected override char GetValue(int index)
        {            
            unsafe {
                char * charPtr = (char*)_ptr;
                return *(charPtr+index);
            }
        }
        protected override void SetValue(int index, char c)
        {
            unsafe {
                char * charPtr = (char*)_ptr;
                *(charPtr+index) = c;
            }                
        }
        protected override void FreeMemory()
        {
            Marshal.FreeHGlobal( _ptr );
        }        
    }
    #endregion
    
    #region UnmanagedByteArray
    public class UnmanagedByteArray : AbstractUnmanagedArray<byte>
    {
        private IntPtr _ptr;
        public UnmanagedByteArray(int length) : base(length) {
            unsafe {
                _ptr = Marshal.AllocHGlobal(length*sizeof(byte));
            }
        }
        protected override byte GetValue(int index)
        {            
            unsafe {
                byte * charPtr = (byte*)_ptr;
                return *(charPtr+index);
            }
        }
        protected override void SetValue(int index, byte c)
        {
            unsafe {
                byte * charPtr = (byte*)_ptr;
                *(charPtr+index) = c;
            }                
        }
        protected override void FreeMemory()
        {
            Marshal.FreeHGlobal( _ptr );
        }        
    }
    #endregion
    
    #region SecurityUtil
    /// <summary>
    /// Description of SecurityUtil.
    /// </summary>
    public static class SecurityUtil
    {
        
        /**
         * Converts chars to bytes without using any external functions
         * that might allocate additional buffers for the potentially
         * sensitive data. This guarantees the caller that they only
         * need to cleanup the input and result.
         * @param chars The chars
         * @return The bytes
         */
        public static UnmanagedArray<byte> CharsToBytes(UnmanagedArray<char> chars)
        {
            UnmanagedByteArray bytes = new UnmanagedByteArray(chars.Length*2);
            
            for ( int i = 0; i < chars.Length; i++ ) {
                char v = chars[i];
                bytes[i*2] = (byte)(0xff & (v >>  8));
                bytes[i*2+1] = (byte)(0xff & (v));
            }
            return bytes;
        }
        
        /**
         * Converts bytes to chars without using any external functions
         * that might allocate additional buffers for the potentially
         * sensitive data. This guarantees the caller that they only
         * need to cleanup the input and result.
         * @param chars The chars
         * @return The bytes
         */
        public static UnmanagedArray<char> BytesToChars(UnmanagedArray<byte> bytes)
        {
            UnmanagedCharArray chars = new UnmanagedCharArray(bytes.Length/2);
            for ( int i = 0; i < chars.Length; i++ ) {
                char v = (char)((bytes[i*2]<<8) | bytes[i*2+1]);
                chars[i] = v;
            }
            return chars;
        }

        
        public unsafe static string ComputeBase64SHA1Hash(UnmanagedArray<char> input)
        {            
            using (UnmanagedArray<byte> bytes = SecurityUtil.CharsToBytes(input)) {
                byte [] managedBytes = new byte[bytes.Length];
                fixed (byte*dummy=managedBytes) { //pin it
                    try {
                        //populate it in pinned block
                        SecurityUtil.UnmanagedBytesToManagedBytes(bytes,managedBytes);
                        SHA1 hasher = SHA1.Create(); 
                        byte[] data = hasher.ComputeHash(managedBytes);
                        return Convert.ToBase64String(data);
                    }
                    finally {
                        //clear it before we leave pinned block
                        SecurityUtil.Clear(managedBytes);
                    }
                }                
            }
        }
        
        /**
         * Copies an unmanaged byte array into a managed byte array.
         * NOTE: it is imperative for security reasons that this only
         * be done in a context where the byte array in question is pinned.
         * moreover, the byte array must be cleared prior to leaving the
         * pinned block
         */
        public static void UnmanagedBytesToManagedBytes(UnmanagedArray<byte> array,
                                                        byte [] bytes)
        {
            for ( int i = 0 ; i < array.Length; i++ )
            {
                bytes[i] = array[i];
            }
        }
        
        /**
         * Clears an array of potentially sensitive bytes
         * @param bytes The bytes. May be null.
         * NOTE: because this is C#, this alone is not enough. The
         * array must be pinned during the interval it is in-use or
         * it could be copied out from under you.
         */
        public static void Clear(byte [] bytes)
        {
            if ( bytes != null )
            {
                for ( int i = 0; i < bytes.Length; i++ )
                {
                    bytes[i] = 0;
                }
            }
        }
        
        /**
         * Clears an array of potentially sensitive chars
         * @param chars The characters. May be null.
         * NOTE: because this is C#, this alone is not enough. The
         * array must be pinned during the interval it is in-use or
         * it could be copied out from under you.
         */
        public static void Clear(char [] chars)
        {
            if ( chars != null ) 
            {
                for ( int i = 0; i < chars.Length; i++)
                {
                    chars[i] = (char)0;
                }
            }
        }
    
        public static bool VerifyBase64SHA1Hash(UnmanagedArray<char> input, string hash)
        {
            string inputHash = ComputeBase64SHA1Hash(input);
            return inputHash.Equals(hash);
        }
    }
    #endregion
    
    #region Encryptor
    /**
     * Responsible for encrypting/decrypting bytes. Implementations
     * are intended to be thread-safe.
     */
    public interface Encryptor {
        /**
         * Decrypts the given byte array
         * @param bytes The encrypted bytes
         * @return The decrypted bytes
         */
        UnmanagedArray<byte> Decrypt(byte [] bytes);
        
        /**
         * Encrypts the given byte array
         * @param bytes The clear bytes
         * @return The ecnrypted bytes
         */
        byte [] Encrypt(UnmanagedArray<byte> bytes);
    }
    #endregion

    #region EncryptorFactory
    public abstract class EncryptorFactory {
        private static readonly object LOCK = new object();
        
        // At some point we might make this pluggable, but for now, hard-code
        private const String IMPL_NAME = "Org.IdentityConnectors.Common.Security.Impl.EncryptorFactoryImpl";
    
        private static EncryptorFactory _instance;
    
        /**
         * Get the singleton instance of the {@link EncryptorFactory}.
         */
        public static EncryptorFactory GetInstance() {
            lock(LOCK) {
                if (_instance == null) {
                    Type type = FrameworkInternalBridge.LoadType(IMPL_NAME);
                    _instance = (EncryptorFactory)Activator.CreateInstance(type);
                }
                return _instance;
            }
        }
    
        /**
         * Default encryptor that encrypts/descrypts using a default key
         */
        public abstract Encryptor GetDefaultEncryptor();
        
    
    }
    #endregion

}
