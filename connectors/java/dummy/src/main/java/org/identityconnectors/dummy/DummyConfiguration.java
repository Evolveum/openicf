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
package org.identityconnectors.dummy;

import java.io.File;
import java.net.URI;
import org.identityconnectors.framework.spi.AbstractConfiguration;

public class DummyConfiguration extends AbstractConfiguration
{

    public DummyConfiguration()
    {
        _string = "String";
        _integer = new Integer(1);
        _long = new Long(2L);
        _boolean = Boolean.FALSE;
        _float = new Float(3F);
        _double = new Double(4D);
        _int = 5;
        _char = 'x';
        _character = Character.valueOf('C');
        _doublePrim = 6D;
        _floatPrim = 7F;
        _booleanPrim = false;
        _longPrim = 8L;
        try
        {
            _uri = new URI("http:/localhost:8080/foo");
            _file = new File("dummy.txt");
        }
        catch(Exception e) { }
        _booleanArray = (new Boolean[] {
            Boolean.TRUE, Boolean.FALSE
        });
        try
        {
            _uriArray = (new URI[] {
                new URI("http:/localhost:8080/foo"), new URI("http:/localhost:8080/bar")
            });
            _fileArray = (new File[] {
                new File("foo.txt"), new File("bar.txt")
            });
        }
        catch(Exception e) { }
    }

    public void validate()
    {
    }

    public String getString()
    {
        return _string;
    }

    public void setString(String _string)
    {
        this._string = _string;
    }

    public Integer getInteger()
    {
        return _integer;
    }

    public void setInteger(Integer _integer)
    {
        this._integer = _integer;
    }

    public Long getLong()
    {
        return _long;
    }

    public void setLong(Long _long)
    {
        this._long = _long;
    }

    public long getLongPrim()
    {
        return _longPrim;
    }

    public void setLong(long _longPrim)
    {
        this._longPrim = _longPrim;
    }

    public Boolean getBoolean()
    {
        return _boolean;
    }

    public void setBoolean(Boolean _boolean)
    {
        this._boolean = _boolean;
    }

    public Float getFloat()
    {
        return _float;
    }

    public void setFloat(Float _float)
    {
        this._float = _float;
    }

    public Double getDouble()
    {
        return _double;
    }

    public void setDouble(Double _double)
    {
        this._double = _double;
    }

    public URI getUri()
    {
        return _uri;
    }

    public void setUri(URI _uri)
    {
        this._uri = _uri;
    }

    public int getInt()
    {
        return _int;
    }

    public void setInt(int _int)
    {
        this._int = _int;
    }

    public char getChar()
    {
        return _char;
    }

    public void setChar(char _char)
    {
        this._char = _char;
    }

    public Character getCharacter()
    {
        return _character;
    }

    public void setCharacter(Character character)
    {
        _character = character;
    }

    public double getDoublePrim()
    {
        return _doublePrim;
    }

    public void setDoublePrim(double prim)
    {
        _doublePrim = prim;
    }

    public float getFloatPrim()
    {
        return _floatPrim;
    }

    public void setFloatPrim(float prim)
    {
        _floatPrim = prim;
    }

    public boolean isBooleanPrim()
    {
        return _booleanPrim;
    }

    public void setBooleanPrim(boolean prim)
    {
        _booleanPrim = prim;
    }

    public File getFile()
    {
        return _file;
    }

    public void setFile(File _file)
    {
        this._file = _file;
    }

    public char[] getCharArray()
    {
        return _charArray;
    }

    public void setCharArray(char array[])
    {
        _charArray = array;
    }

    public String[] getStringArray()
    {
        return _stringArray;
    }

    public void setStringArray(String _stringArray[])
    {
        this._stringArray = _stringArray;
    }

    public Integer[] getIntegerArray()
    {
        return _integerArray;
    }

    public void setIntegerArray(Integer _integerArray[])
    {
        this._integerArray = _integerArray;
    }

    public Long[] getLongArray()
    {
        return _longArray;
    }

    public void setLongArray(Long _longArray[])
    {
        this._longArray = _longArray;
    }

    public long[] getLongPrimArray()
    {
        return _longPrimArray;
    }

    public void setLongArray(long _longPrimArray[])
    {
        this._longPrimArray = _longPrimArray;
    }

    public Boolean[] getBooleanArray()
    {
        return _booleanArray;
    }

    public void setBooleanArray(Boolean _booleanArray[])
    {
        this._booleanArray = _booleanArray;
    }

    public Float[] getFloatArray()
    {
        return _floatArray;
    }

    public void setFloatArray(Float _floatArray[])
    {
        this._floatArray = _floatArray;
    }

    public Double[] getDoubleArray()
    {
        return _doubleArray;
    }

    public void setDoubleArray(Double _doubleArray[])
    {
        this._doubleArray = _doubleArray;
    }

    public URI[] getUriArray()
    {
        return _uriArray;
    }

    public void setUriArray(URI _uriArray[])
    {
        this._uriArray = _uriArray;
    }

    public int[] getIntArray()
    {
        return _intArray;
    }

    public void setIntArray(int _intArray[])
    {
        this._intArray = _intArray;
    }

    public Character[] getCharacterArray()
    {
        return _characterArray;
    }

    public void setCharacterArray(Character characterArray[])
    {
        _characterArray = characterArray;
    }

    public double[] getDoublePrimArray()
    {
        return _doublePrimArray;
    }

    public void setDoublePrimArray(double primArray[])
    {
        _doublePrimArray = primArray;
    }

    public float[] getFloatPrimArray()
    {
        return _floatPrimArray;
    }

    public void setFloatPrimArray(float primArray[])
    {
        _floatPrimArray = primArray;
    }

    public boolean[] getBooleanPrimArray()
    {
        return _booleanPrimArray;
    }

    public void setBooleanPrimArray(boolean primArray[])
    {
        _booleanPrimArray = primArray;
    }

    public File[] getFileArray()
    {
        return _fileArray;
    }

    public void setFileArray(File _fileArray[])
    {
        this._fileArray = _fileArray;
    }

    private char _charArray[] = {
        'c', 'h', 'a', 'r', 'A', 'r', 'r', 'a', 'y'
    };
    private String _string;
    private Integer _integer;
    private Long _long;
    private Boolean _boolean;
    private Float _float;
    private Double _double;
    private URI _uri;
    private int _int;
    private char _char;
    private Character _character;
    private double _doublePrim;
    private float _floatPrim;
    private boolean _booleanPrim;
    private long _longPrim;
    private File _file;
    private String _stringArray[] = {
        "String", "array"
    };
    private Integer _integerArray[] = {
        Integer.valueOf(1), Integer.valueOf(2)
    };
    private Long _longArray[] = {
        Long.valueOf(3L), Long.valueOf(4L)
    };
    private Boolean _booleanArray[];
    private Float _floatArray[] = {
        Float.valueOf(5F), Float.valueOf(6F)
    };
    private Double _doubleArray[] = {
        Double.valueOf(7D), Double.valueOf(8D)
    };
    private URI _uriArray[];
    private int _intArray[] = {
        15, 16
    };
    private Character _characterArray[] = {
        Character.valueOf('C'), Character.valueOf('h')
    };
    private double _doublePrimArray[] = {
        9D, 10D
    };
    private float _floatPrimArray[] = {
        11F, 12F
    };
    private boolean _booleanPrimArray[] = {
        true, false
    };
    private long _longPrimArray[] = {
        13L, 14L
    };
    private File _fileArray[];
}
