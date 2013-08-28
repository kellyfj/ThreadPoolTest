package com.dataxu;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Generic Resource Pool Implementation
 * 
 * @author kellyfj
 *
 * @param <T>
 */
public class ResoucePool<T> {

		private boolean open=false;
		private BlockingQueue < T > resourcesInUse;
		private BlockingQueue < T > resourcesIdle;
		private Object commonLock;
		
		public void ResourcePool()
		{
			resourcesInUse = new LinkedBlockingQueue < T >();
			resourcesIdle  = new LinkedBlockingQueue < T >();
		}
		
		public void open()
		{
			open=true;
		}
		
		public boolean isOpen()
		{
			return open;
		}

		public void close()
		{
			//Tag as closed first before waiting for all objects in use to be releases
			open = false;
			
			while(resourcesInUse.size() > 0)
			{
				try {
					Thread.sleep(10);
				} catch (InterruptedException ignore) {
					//Do nothing
				}
			}
			
			synchronized(commonLock)
			{
				resourcesInUse.clear();
				resourcesIdle.clear();
			}
		}
		
		public void closeNow()
		{
			open = false;
			synchronized(commonLock)
			{
				resourcesInUse.clear();
				resourcesIdle.clear();
			}
		}
		
		public boolean add(T r)
		{
			if(resourcesInUse.contains(r))
				throw new IllegalStateException("Cannot add resource to Pool as it is part of the pool already and already in use");
			
			//Returns true if the underlying collection changed as a result of the call
			return resourcesIdle.add(r);
		}
		
		public boolean remove(T r)
		{
			if(resourcesInUse.contains(r))
				throw new IllegalStateException("Cannot remove resource from Pool as it is  already in use");

			//true if an element was removed as a result of this call
			return resourcesIdle.remove(r);
		}
		
		public boolean removeNow(T r)
		{
			//true if an element was removed as a result of this call
			synchronized(commonLock)
			{
				boolean b1 = resourcesIdle.remove(r);
				boolean b2 = resourcesInUse.remove(r);
				return b1 || b2;
			}
		}
		
		public T acquire()
		{
			if(!open)
				throw new IllegalStateException("Unable to acquire resource as pool is closed");
			
			synchronized(commonLock)
			{
				T resource = resourcesIdle.poll();
				resourcesInUse.add(resource);
				return resource;
			}
		}
		
		/**
		 * Retrieves and removes the head of this queue, or returns null if this queue is empty.
		 * 
		 * @param timeout
		 * @param unit
		 * @return
		 */
		public T acquire(long timeout, java.util.concurrent.TimeUnit unit)
		{
			if(!open)
				throw new IllegalStateException("Unable to acquire resource as pool is closed");

			try {
				synchronized(commonLock)
				{
					T resource =  resourcesIdle.poll(timeout, unit);
					resourcesInUse.add(resource);
					return resource;
				}
			} catch (InterruptedException e) {
				//If thread interrupted then return null
				return null;
			}
		}
		
		public void release(T resource)
		{
			if(!resourcesInUse.contains(resource))
				throw new IllegalArgumentException("Cannot release resource as it is not in use");
			
			if(resourcesIdle.contains(resource))
				throw new IllegalArgumentException("Cannot release resources as it is idle");
			
			synchronized(commonLock)
			{
				resourcesInUse.remove(resource);
				resourcesIdle.add(resource);
			}
		}
		
}
