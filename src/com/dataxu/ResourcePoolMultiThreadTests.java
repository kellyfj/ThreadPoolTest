package com.dataxu;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ResourcePoolMultiThreadTests {

	static ResourcePool<String> poolUnderTest;

	public static void main(String[] args) {
		// Test 1
		poolUnderTest = new ResourcePool<String>();
		poolUnderTest.open();
		poolUnderTest.add("Uno");
		poolUnderTest.add("Dos");
		poolUnderTest.add("Tres");
		poolUnderTest.add("Quattro");

		Thread t = new Thread(new BasicAcquireLoopThread());
		t.start();

		// Wait for thread to die
		try {
			t.join();
		} catch (InterruptedException ignore) {
		}

		// Test 2
		Thread t1 = new Thread(new BasicAcquireAndReleaseLoopThread("T1"));
		t1.start();
		Thread t2 = new Thread(new BasicAcquireAndReleaseLoopThread("T2"));
		t2.start();
		try {
			t1.join();
			t2.join();
		} catch (InterruptedException ignore) {
		}

		// Test 3

		int numThreads = 4;
		ExecutorService pool = Executors.newFixedThreadPool(numThreads);
		for (int i = 0; i < numThreads; i++) {
			pool.execute(new BasicAcquireAndReleaseLoopThread("Exec"));

		}

	}

	private static class BasicAcquireLoopThread implements Runnable {

		public void run() {

			boolean continueLoop = true;
			List<String> releaseList = new ArrayList<String>();
			while (continueLoop) {
				String s = poolUnderTest.acquire();
				if (s != null)
					releaseList.add(s);

				System.out.println(s);
				if (s == null)
					continueLoop = false;
			}

			for (String s : releaseList) {
				poolUnderTest.release(s);
			}
		}

	}

	private static class BasicAcquireAndReleaseLoopThread implements Runnable {

		private String name;

		public BasicAcquireAndReleaseLoopThread(String name) {
			this.name = name;
		}

		public void run() {

			int count = 0;
			while (count < 1000) {
				String s = poolUnderTest.acquire();

				// Pretend to do something useful
				double sleepTime = 50 * Math.random();
				try {
					Thread.sleep((long) sleepTime);
				} catch (InterruptedException ignored) {
				}
				System.out.println(this.name + " " + count + " " + s);
				count++;

				poolUnderTest.release(s);
			}
		}

	}
}
