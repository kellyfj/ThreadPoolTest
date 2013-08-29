package com.dataxu;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Generic Resource Pool Implementation
 * 
 * @author kellyfj
 * 
 * @param <T>
 */
public class ResourcePool<T> {

	private boolean open = false;
	private BlockingQueue<T> resourcesIdle;
	//private Set<T> resourcesInUse;

	private Object syncPoint;
	
	//Lock & Condition to tell threads blocking on acquiring a resource if none available
	final private Lock resourcesAvailableLock = new ReentrantLock();
	final private Condition resourcesAvailable = resourcesAvailableLock.newCondition(); 
	
	//Lock & Condition to tell threads when no resources are in use (to close pool)
	final private Lock noResourcesInUseLock = new ReentrantLock();
	final private Condition noResourcesInUse = noResourcesInUseLock.newCondition();
	
	//Lock & Conditions for each object
	private ConcurrentMap<T, Lock> resourceLocks;
	private ConcurrentMap<T, Condition> resourceConditions;

	public ResourcePool() {
		syncPoint = new Object();
		resourcesIdle = new LinkedBlockingQueue<T>();
		resourceLocks = new ConcurrentHashMap<T,Lock>();
		resourceConditions = new ConcurrentHashMap<T,Condition>();
	}

	/**
	 * Opens the pool allowing acquisition and release of elements in the pool
	 */
	public void open() {
		open = true;
	}

	/**
	 * Indicates whether the pool is open or not
	 * @return boolean indicating whether the pool is open or not
	 */
	public boolean isOpen() {
		return open;
	}

	/**
	 * Closes the pool. Blocks and waits for all objects in use to be released
	 */
	public void close() {
		// Tag as closed first before waiting for all objects in use to be released
		open = false;

		waitForAResourceToBeAvailable();

		synchronized (syncPoint) {
			resourceLocks.clear();
			resourceConditions.clear();
			resourcesIdle.clear();
		}
	}

	private void waitForAResourceToBeAvailable() {

		if (resourcesIdle.size()==0 && resourceLocks.size() > 0) {
			try{
				noResourcesInUseLock.lock();
				noResourcesInUse.await();
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread interrupted");
			} finally {
				noResourcesInUseLock.unlock();
			}
		}
	}

	/**
	 * Non-blocking call to close the pool.
	 */
	public void closeNow() {
		open = false;
		synchronized (syncPoint) {
			resourceLocks.clear();
			resourceConditions.clear();
			resourcesIdle.clear();
		}
	}

	/**
	 * Adds a resource to the pool
	 * @param r
	 * @return boolean indicating whether the resource was added to the pool (true) or if the resource was already in the pool (false)
	 */
	public boolean add(T r) {
		if (resourceLocks.containsKey(r))
			throw new IllegalStateException(
					"Cannot add resource to Pool as it is part of the pool already and already in use");

		boolean alreadyPresent = resourcesIdle.contains(r);

		if (!alreadyPresent) {
			signalThatAResourceIsNowAvailable(r);
  		    return true;
		} else {
			return false;
		}

	}

	private void signalThatAResourceIsNowAvailable(T r) {
		resourcesIdle.add(r);
		resourcesAvailableLock.lock();
		try{
			resourcesAvailable.signal();
		} finally{ 
			resourcesAvailableLock.unlock();
		}
	}

	public boolean remove(T r) {
		//If resource in use wait for resource to be released
		waitForThisResourceToBeAvailable(r);
	
		boolean returnValue = resourcesIdle.contains(r);
		resourcesIdle.remove(r);

		return returnValue;
	}

	private void waitForThisResourceToBeAvailable(T r) {
		if (resourceLocks.containsKey(r)) {
			Lock l = resourceLocks.get(r);
			l.lock();
			Condition c = resourceConditions.get(r);
			try {
				c.await();
			} catch (InterruptedException e) {
				throw new RuntimeException("Thread interrupted");
			} finally {
				l.unlock();
			}
		}
	}

	/**
	 * Removes a resource from the pool whether the resource is in use or not.
	 * @param r
	 * @return
	 */
	public boolean removeNow(T r) {
		synchronized (syncPoint) {
			boolean b1 = resourcesIdle.remove(r);
			Lock l = resourceLocks.remove(r);
			// true if an element was removed as a result of this call
			return b1 || (l!=null);
		}
	}

	/**
	 * Borrows a resource from the pool. Blocks until resource is available
	 * @return the resource
	 */
	public T acquire() {
		if (!open)
			throw new IllegalStateException("Unable to acquire resource as pool is closed");
		
			waitForAResourceToBeAvailable();
		
			synchronized (syncPoint) {
				T resource = resourcesIdle.poll();
				storeLocksForResource(resource);
				return resource;
			}

	}

	/**
	 *  Borrows a resource from the pool. Blocks for the specified time until resource is available
	 * @param timeout
	 * @param unit
	 * @return the resource
	 */
	public T acquire(long timeout, java.util.concurrent.TimeUnit unit) {
		if (!open)
			throw new IllegalStateException("Unable to acquire resource as pool is closed");

		try {
			synchronized (syncPoint) {
				T resource = resourcesIdle.poll(timeout, unit);
				storeLocksForResource(resource);
				return resource;
			}
		} catch (InterruptedException e) {
			// If thread interrupted then return null
			return null;
		}
	}

	private void storeLocksForResource(T resource) {
		if (resource != null)
		{
			Lock l = new ReentrantLock();
			Condition c = l.newCondition();
			resourceLocks.put(resource, l);
			resourceConditions.put(resource, c);
		}
	}

	/**
	 * Releases the resource back to the pool
	 * 
	 * @param resource
	 */
	public void release(T resource) {
		if (!resourceLocks.containsKey(resource))
			throw new IllegalArgumentException("Cannot release resource as it is not in use");

		if (resourcesIdle.contains(resource))
			throw new IllegalArgumentException("Cannot release resources as it is idle");

		synchronized (syncPoint) {
			//Signal that THIS resource is available
			signalThatThisResourceIsNowAvailable(resource);
			
			signalThatAResourceIsNowAvailable(resource);
						
			//Signal if no resource is in use
			if(resourceLocks.size()==0)
			{
				signalThatNoResourceIsInUse();
			}
		}
	}

	private void signalThatNoResourceIsInUse() {
		noResourcesInUseLock.lock();
		noResourcesInUse.signal();
		noResourcesInUseLock.unlock();
	}

	private void signalThatThisResourceIsNowAvailable(T resource) {
		Lock l = resourceLocks.remove(resource);
		l.lock();
		Condition c = resourceConditions.remove(resource);
		c.signal();
		l.unlock();
	}

}
