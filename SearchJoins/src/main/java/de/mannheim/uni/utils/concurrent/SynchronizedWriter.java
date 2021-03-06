package de.mannheim.uni.utils.concurrent;

import java.util.concurrent.ConcurrentLinkedQueue;

import scala.Math;

public abstract class SynchronizedWriter<T>
	implements Runnable
{
	private ConcurrentLinkedQueue<T> input;
	private boolean shouldStop;
	private boolean stopAfterFlush;
	private Thread thread;
	//private BufferedWriter writer;
	
	public SynchronizedWriter(String file) throws Exception
	{	
		input = new ConcurrentLinkedQueue<T>();
		shouldStop=false;
		stopAfterFlush=false;
		//writer = new BufferedWriter(new FileWriter(file, true));
		createWriter(file);
		
		thread = new Thread(this);
		thread.start();
	}

	protected abstract void createWriter(String file) throws Exception;
	protected abstract void writeData(T data) throws Exception;
	protected abstract void flushWriter() throws Exception;
	protected abstract void closeWriter() throws Exception;
	public abstract long getApproximateLength() throws Exception;
	
	public void write(T data)
	{
		input.add(data);
	}
	
	public void stop()
	{
		shouldStop=true;
	}
	
	public void stopAndBlock() throws InterruptedException
	{
		stop();
		thread.join();
	}

	public void stopAfterFlush()
	{
		stopAfterFlush=true;
	}
	
	public void flushAndBlock() throws InterruptedException
	{
		stopAfterFlush();
		thread.join();
	}
	
	public void run() {
		T data = null;
		
		long sleep = 100;
		
		while(!shouldStop && !(stopAfterFlush && input.isEmpty()))
		{
			data = input.poll();
			
			if(data!=null)
			{
				sleep = 100;
				
				try {
					writeData(data);
				} catch (Exception e) {
					e.printStackTrace();
					// failed to write, add to queue again ...
					write(data);
				}
			} else
				try {
					flushWriter();
					
					Thread.sleep(sleep);
					sleep *= 2; // if the input queue is not filled, increase the time to wait before the next poll
					sleep = Math.max(sleep, 2000);
				} catch (Exception e) {
					e.printStackTrace();
				}
		}
		

		try {
			closeWriter();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
}
