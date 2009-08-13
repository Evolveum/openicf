package org.identityconnectors.oracle;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.test.common.TestHelpers;
import org.junit.Assert;
import org.junit.Test;

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
        Map<String, Object> map = parseMap("a=b,c=d");
        Assert.assertEquals(CollectionUtil.newMap(new String[]{"a","c"}, new String[]{"b","d"}), map);
        map = parseMap("{a=b,c=d}");
        Assert.assertEquals(CollectionUtil.newMap(new String[]{"a","c"}, new String[]{"b","d"}), map);
        map = parseMap("{name=Tom,surname=Scott,address={town=London,street=Some street,number={n1=10,n2=1234}}}");
        assertEquals("Tom",map.get("name"));
        assertEquals("Scott",map.get("surname"));
        final Map testMap = CollectionUtil.newMap("town", "London", "street", "Some street");
        testMap.put("number", CollectionUtil.newMap("n1","10","n2","1234"));
        assertEquals(testMap,map.get("address"));
        map = parseMap("a=,c=d");
        Assert.assertEquals(CollectionUtil.newMap("a",null,"c","d"), map);
        map = parseMap("");
        Assert.assertEquals(new HashMap(), map);
        map = parseMap(null);
        Assert.assertEquals(new HashMap(), map);
        map = parseMap("quates=");
        System.out.println(map);
    }
    
    /** Test fail of parsing */
    @Test
    public void testParseFail(){
        //Wish java had closures
        testFail(new Runnable(){public void run(){parseMap("{");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){parseMap("}");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){parseMap("a=b}");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){parseMap("a=b,");}}, "Must fail for invalid entry");
        testFail(new Runnable(){public void run(){parseMap("a=b,=");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){parseMap("a=b,=d");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){parseMap(",");}}, "Must fail for invalid comma");
        testFail(new Runnable(){public void run(){parseMap(",,");}}, "Must fail for invalid comma");
        testFail(new Runnable(){public void run(){parseMap("a=b,c={d");}}, "Must fail for invalid bracket");
        testFail(new Runnable(){public void run(){parseMap("a={b,c=d}");}}, "Must fail for invalid bracket");
    }
    
    private void testFail(Runnable runnable,String msg){
        try{
            runnable.run();
            Assert.fail(msg);
        }
        catch(RuntimeException e){}
    }
    
    private Map<String,Object> parseMap(String string){
    	return MapParser.parseMap(string, TestHelpers.createDummyMessages());
    }
    
}
