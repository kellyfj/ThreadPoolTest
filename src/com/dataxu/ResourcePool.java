package com.dataxu;

import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.Collections;
import java.util.HashSet;

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
	private Set<T> resourcesInUse;

	private Object commonLock;

	public ResourcePool() {
		commonLock = new Object();
		resourcesIdle = new LinkedBlockingQueue<T>();
		resourcesInUse = Collections.synchronizedSet(new HashSet<T>());
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
		// Tag as closed first before waiting for all objects in use to be
		// released
		open = false;

		while (resourcesInUse.size() > 0) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException ignore) {
				// Do nothing
			}
		}

		synchronized (commonLock) {
			resourcesInUse.clear();
			resourcesIdle.clear();
		}
	}

	/**
	 * Non-blocking call to close the pool.
	 */
	public void closeNow() {
		open = false;
		synchronized (commonLock) {
			resourcesInUse.clear();
			resourcesIdle.clear();
		}
	}

	/**
	 * Adds a resource to the pool
	 * @param r
	 * @return boolean indicating whether the resource was added to the pool (true) or if the resource was already in the pool (false)
	 */
	public boolean add(T r) {
		if (resourcesInUse.contains(r))
			throw new IllegalStateException(
					"Cannot add resource to Pool as it is part of the pool already and already in use");

		boolean alreadyPresent = resourcesIdle.contains(r);

		if (!alreadyPresent) {
			resourcesIdle.add(r);
			return true;
		} else {
			return false;
		}

	}

	public boolean remove(T r) {
		if (resourcesInUse.contains(r))
			throw new IllegalStateException("Cannot remove resource from Pool as it is  already in use");

		boolean returnValue = resourcesIdle.contains(r);

		resourcesIdle.remove(r);

		return returnValue;
	}

	/**
	 * Removes a resource from the pool whether the resource is in use or not.
	 * @param r
	 * @return
	 */
	public boolean removeNow(T r) {
		synchronized (commonLock) {
			boolean b1 = resourcesIdle.remove(r);
			boolean b2 = resourcesInUse.remove(r);
			// true if an element was removed as a result of this call
			return b1 || b2;
		}
	}

	/**
	 * Borrows a resource from the pool. Blocks until resource is available
	 * @return the resource
	 */
	public T acquire() {
		if (!open)
			throw new IllegalStateException("Unable to acquire resource as pool is closed");

		synchronized (commonLock) {
			T resource = resourcesIdle.poll();
			if (resource != null)
				resourcesInUse.add(resource);
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
			synchronized (commonLock) {
				T resource = resourcesIdle.poll(timeout, unit);
				if (resource != null)
					resourcesInUse.add(resource);
				return resource;
			}
		} catch (InterruptedException e) {
			// If thread interrupted then return null
			return null;
		}
	}

	/**
	 * Releases the resource back to the pool
	 * 
	 * @param resource
	 */
	public void release(T resource) {
		if (!resourcesInUse.contains(resource))
			throw new IllegalArgumentException("Cannot release resource as it is not in use");

		if (resourcesIdle.contains(resource))
			throw new IllegalArgumentException("Cannot release resources as it is idle");

		synchronized (commonLock) {
			resourcesInUse.remove(resource);
			resourcesIdle.add(resource);
		}
	}

}
