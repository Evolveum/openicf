package org.identityconnectors.oracle;

import static org.junit.Assert.*;

import org.junit.Test;

public class PairTest {

	@Test
	public void testHashCode() {
		Pair<Integer,String> pair1 = new Pair<Integer, String>(1,"one");
		Pair<Integer,String> pair2 = new Pair<Integer, String>(1,"one");
		assertEquals(pair1.hashCode(),pair2.hashCode());
		pair2 = new Pair<Integer, String>(1,"two");
		assertFalse(pair1.hashCode() == pair2.hashCode());
		pair1 = new Pair<Integer, String>(null,"one");
		pair2 = new Pair<Integer, String>(null,"one");
		assertEquals(pair1.hashCode(),pair2.hashCode());
		pair1 = new Pair<Integer, String>(1,null);
		pair2 = new Pair<Integer, String>(1,null);
		assertEquals(pair1.hashCode(),pair2.hashCode());
	}

	@Test
	public void testPair() {
		Pair<Integer,String> pair = new Pair<Integer, String>(1,"one");
		assertEquals(new Integer(1), pair.getFirst());
		assertEquals("one", pair.getSecond());
	}

	@Test
	public void testEqualsObject() {
		Pair<Integer,String> pair1 = new Pair<Integer, String>(1,"one");
		Pair<Integer,String> pair2 = new Pair<Integer, String>(1,"one");
		assertEquals(pair1,pair2);
		assertEquals(pair2,pair1);
		pair1 = pair2;
		assertEquals(pair1,pair2);
		assertEquals(pair2,pair1);
		pair2 = new Pair<Integer, String>(1,"two");
		assertFalse(pair1.equals(pair2));
		assertFalse(pair2.equals(pair1));
		pair1 = new Pair<Integer, String>(null,"one");
		pair2 = new Pair<Integer, String>(null,"one");
		assertEquals(pair1,pair2);
		assertEquals(pair2,pair1);
		pair1 = new Pair<Integer, String>(1,null);
		pair2 = new Pair<Integer, String>(1,null);
		assertEquals(pair1,pair2);
		assertEquals(pair2,pair1);
		
	}

}
