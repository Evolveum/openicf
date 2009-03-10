package org.identityconnectors.oracle;

import static org.junit.Assert.assertEquals;

import java.util.*;

import org.identityconnectors.common.CollectionUtil;
import org.junit.*;

/** 
 * Tests for {@link MapParser}
 * @author kitko
 */
public class MapParserTest {
    
    
    /**
     * Successful parsing 
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testParseOk(){
        Map<String, Object> map = MapParser.parseMap("a=b,c=d");
        Assert.assertEquals(CollectionUtil.newMap(new String[]{"a","c"}, new String[]{"b","d"}), map);
        map = MapParser.parseMap("{a=b,c=d}");
        Assert.assertEquals(CollectionUtil.newMap(new String[]{"a","c"}, new String[]{"b","d"}), map);
        map = MapParser.parseMap("{name=Tom,surname=Scott,address={town=London,street=Some street,number={n1=10,n2=1234}}}");
        assertEquals("Tom",map.get("name"));
        assertEquals("Scott",map.get("surname"));
        final Map testMap = CollectionUtil.newMap("town", "London", "street", "Some street");
        testMap.put("number", CollectionUtil.newMap("n1","10","n2","1234"));
        assertEquals(testMap,map.get("address"));
        map = MapParser.parseMap("a=,c=d");
        Assert.assertEquals(CollectionUtil.newMap("a",null,"c","d"), map);
        map = MapParser.parseMap("");
        Assert.assertEquals(new HashMap(), map);
        map = MapParser.parseMap(null);
        Assert.assertEquals(new HashMap(), map);
        map = MapParser.parseMap("quates=");
        System.out.println(map);
    }
    
    /** Test fail of parsing */
    @Test
    public void testParseFail(){
        //Wish java had closures
        testFail(new Runnable(){public void run(){MapParser.parseMap("{");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){MapParser.parseMap("}");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){MapParser.parseMap("a=b}");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){MapParser.parseMap("a=b,");}}, "Must fail for invalid entry");
        testFail(new Runnable(){public void run(){MapParser.parseMap("a=b,=");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){MapParser.parseMap("a=b,=d");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){MapParser.parseMap(",");}}, "Must fail for invalid comma");
        testFail(new Runnable(){public void run(){MapParser.parseMap(",,");}}, "Must fail for invalid comma");
        testFail(new Runnable(){public void run(){MapParser.parseMap("a=b,c={d");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){MapParser.parseMap("a={b,c=d}");}}, "Must fail for invalid bracket");
    }
    
    private void testFail(Runnable runnable,String msg){
        try{
            runnable.run();
            Assert.fail(msg);
        }
        catch(RuntimeException e){}
    }
    
}
