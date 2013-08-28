package com.dataxu;

import static org.junit.Assert.*;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class ResourcePoolTest {

	@Test
	public void testResourcePool() {
		ResourcePool<String> rp = new ResourcePool<String>();
		assertFalse(rp.isOpen());
	}

	@Test
	public void testOpen() {
		ResourcePool<String> rp = new ResourcePool<String>();
		assertFalse(rp.isOpen());
		rp.open();
		assertTrue(rp.isOpen());
	}

	@Test
	public void testClose() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		rp.close();
		assertFalse(rp.isOpen());
	}

	@Test
	public void testCloseNow() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		rp.closeNow();
		assertFalse(rp.isOpen());
	}

	@Test
	public void testAddAndEntireLifeCycle() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s = "Hello World!";
		assertTrue(rp.add(s));
		assertTrue(s.equals(rp.acquire()));
		rp.release(s);
		rp.remove(s);
		rp.close();
	}
	
	@Test
	public void testAddMultipleTimes() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s = "Hello World!";
		assertTrue(rp.add(s));
		assertFalse(rp.add(s));
		assertTrue(s.equals(rp.acquire()));

	}
	
	@Test
	public void testExhaustQueue() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s = "Hello World!";
		assertTrue(rp.add(s));
		assertTrue(s.equals(rp.acquire()));
		String reply = rp.acquire(1000, TimeUnit.MILLISECONDS);
		assertTrue(reply==null);
		rp.release(s);
		assertTrue(rp.remove(s));
		rp.close();
	}
	
	@Test
	public void testAddMultiple() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s1 = "Hello World!";
		String s2 = "Two";
		String s3 = "Three";
		assertTrue(rp.add(s1));
		assertTrue(rp.add(s2));
		assertTrue(rp.add(s3));
		assertTrue(s1.equals(rp.acquire()));
		assertTrue(s2.equals(rp.acquire()));
		assertTrue(s3.equals(rp.acquire()));
		rp.release(s1);
		rp.release(s2);
		rp.release(s3);
		rp.close();
	}

	@Test
	public void testAddAndReleaseOrder() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s1 = "One";
		String s2 = "Two";
		String s3 = "Three";
		assertTrue(rp.add(s1));
		assertTrue(rp.add(s2));
		assertTrue(rp.add(s3));
		assertTrue(s1.equals(rp.acquire()));
		assertTrue(s2.equals(rp.acquire()));
		assertTrue(s3.equals(rp.acquire()));
		rp.release(s3);
		assertTrue(s3.equals(rp.acquire()));
		rp.closeNow();
	}
	
	@Test
	public void testAddAndEntireLifeCycleWithCloseNow() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s = "Hello World!";
		rp.add(s);
		assertTrue(s.equals(rp.acquire()));
		rp.release(s);
		rp.remove(s);
		rp.closeNow();
	}
	
	@Test
	public void testAddAndEntireLifeCycleWithCloseNow_LeavingResourcesHanging() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s = "Hello World!";
		rp.add(s);
		assertTrue(s.equals(rp.acquire()));
		rp.closeNow();
	}
	
	@Test
	public void testAcquireLongTimeUnit() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s = "Hello World!";
		rp.add(s);
		assertTrue(s.equals(rp.acquire(1000,TimeUnit.MILLISECONDS)));
		rp.release(s);
		rp.remove(s);
		rp.close();
	}

	@Test (expected = IllegalStateException.class)
	public void testRemoveFailure_ResourceStillInUse() {
		ResourcePool<String> rp = new ResourcePool<String>();
		rp.open();
		String s = "Hello World!";
		rp.add(s);
		assertTrue(s.equals(rp.acquire()));
		rp.remove(s);
		rp.close();
	}

}
