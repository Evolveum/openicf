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
package org.identityconnectors.rw3270;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

import expect4j.IOPair;

public class RW3270IOPair implements IOPair {
    private RW3270Reader            _reader;
    private RW3270Writer            _writer;
    
    public RW3270IOPair(RW3270Connection rw3270) {
        _reader = new RW3270Reader(rw3270);
        _writer = new RW3270Writer(rw3270);
    }

    public void close() {
        try {
            _reader.close();
            _writer.close();
        } catch (IOException e) {
        }
    }

    public Reader getReader() {
        return _reader;
    }

    public Writer getWriter() {
        return _writer;
    }

    public void reset() {
        _reader.reset();
    }
}

class RW3270Reader extends Reader {
    private RW3270Connection        _rw3270;
    private StringBuffer            _pendingOutput;
    
    public RW3270Reader(RW3270Connection rw3270) {
        _rw3270 = rw3270;
        _pendingOutput = new StringBuffer();
    }
    
    @Override
    public void close() throws IOException {
        _rw3270.dispose();
    }

    @Override
    public void reset() {
        _pendingOutput.setLength(0);
    }

    public int read(char[] cbuf, int off, int len) throws IOException {
        if (_pendingOutput.length()==0)
            _pendingOutput.append(_rw3270.waitForInput());
        int bytesToMove = Math.min(len, _pendingOutput.length());
        _pendingOutput.getChars(0, bytesToMove, cbuf, 0);
        _pendingOutput.delete(0, bytesToMove);
        return bytesToMove;
    }
}

class RW3270Writer extends Writer {
    private RW3270Connection        _rw3270;

    public RW3270Writer(RW3270Connection rw3270) {
        _rw3270 = rw3270;
    }
    
    @Override
    public void close() throws IOException {
        _rw3270.dispose();
    }

    @Override
    public void flush() throws IOException {
    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        char[] subArray = new char[len];
        System.arraycopy(cbuf, off, subArray, 0, len);
        _rw3270.sendFromIOPair(new String(subArray));
        Arrays.fill(subArray, ' ');
    }
}
