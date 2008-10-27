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