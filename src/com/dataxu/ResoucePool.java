package com.dataxu;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Generic Resource Pool
 * 
 * @author kellyfj
 *
 * @param <T>
 */
public class ResoucePool<T> {

		private boolean open=false;
		private BlockingQueue < T > resources;
		
		public void ResourcePool()
		{
			resources = new LinkedBlockingQueue < T >();
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
			open = false;
		}
		
		public void closeNow()
		{
			
		}
		
		public boolean add(T resource)
		{
			//Returns true if the underlying collection changed as a result of the call
			return resources.add(resource);
		}
		
		public boolean remove(T r)
		{
			//true if an element was removed as a result of this call
			return resources.remove(r);
		}
		
		public boolean removeNow(T r)
		{
			//true if an element was removed as a result of this call
			return resources.remove(r);
		}
		
		public T acquire()
		{
			if(!open)
				throw new IllegalStateException("Unable to acquire resource as pool is closed");
			
			try {
				return resources.take();
			} catch (InterruptedException e) {
				//If thread interrupted then return null
				return null;
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
				return resources.poll(timeout, unit);
			} catch (InterruptedException e) {
				//If thread interrupted then return null
				return null;
			}
		}
		
		public void release(T resource)
		{
			
		}
		
}
