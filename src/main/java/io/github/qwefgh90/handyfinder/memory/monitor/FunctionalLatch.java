package io.github.qwefgh90.handyfinder.memory.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import com.beust.jcommander.ParameterException;
/**
 * 
 * @author qwefgh90
 *
 */
public class FunctionalLatch {
	private final List<Locker> list = Collections.synchronizedList(new ArrayList<Locker>());
	private final Timer timer = new Timer(true); //make a daemon timer
	
	private volatile boolean terminated = false;
	private volatile int numberAtOnceForWakeUp;
	
	/**
	 * Initialize a functional latch.
	 * @param numberAtOnceForWakeUp
	 */
	public FunctionalLatch(){
		this(1);
	}
	
	/**
	 * Initialize a functional latch.
	 * @param numberAtOnceForWakeUp a number greater than zero
	 */
	public FunctionalLatch(int numberAtOnceForWakeUp){
		if(numberAtOnceForWakeUp <= 0)
			throw new ParameterException("numberAtOnceForWakeUp is less than one. " + numberAtOnceForWakeUp);
		this.numberAtOnceForWakeUp = numberAtOnceForWakeUp;
		timer.schedule(new ConditionChecker(), 0, ConditionChecker.CHECK_INTERVAL); // Start checking lockers
	}
	
	/**
	 * Setter for the number of thread which awakes at once.
	 * @param numberAtOnceForWakeUp number
	 */
	public void setNumberAtOnceWakeUp(int numberAtOnceForWakeUp){
		if(numberAtOnceForWakeUp <= 0)
			return;
		this.numberAtOnceForWakeUp = numberAtOnceForWakeUp;
	}
	
	/**
	 * Causes the current thread to wait until receiving a signal.
	 * @return
	 * @throws InterruptedException
	 * @throws IllegalStateException
	 */
	public void await() throws InterruptedException{
		if(terminated)
			throw new IllegalStateException("A latch is already terminated.");
		final CountDownLatch latch = new CountDownLatch(1);
		final Locker loc = new Locker(Optional.empty(), Optional.empty(), Optional.of(latch));
		list.add(loc);
		try{
			latch.await();
		}finally{
			list.remove(loc);
		}
	}
	
	/**
	 * Causes the current thread to wait until receiving a signal or conditions are met.
	 * @param f if true, wake up a thread
	 * @param interval second between 1 and 3600, how often do you check a function
	 * @param timeout
	 * @param unit unit for timeout
	 * @return
	 * @throws InterruptedException
	 * @throws IllegalStateException
	 * @throws NullPointerException
	 */
	public void await(Supplier<Boolean> f, long interval) throws InterruptedException{
		if(terminated)
			throw new IllegalStateException("A latch is already terminated.");
		if(f == null)
			throw new NullPointerException("Function parameter is null.");
		if(interval < 1 || interval > ConditionChecker.MAX_COUNT)
			throw new IllegalStateException("Invalid interval is found. " + interval);
		
		final CountDownLatch latch = new CountDownLatch(1);
		final Locker loc = new Locker(Optional.of(f), Optional.of(interval), Optional.of(latch));
		list.add(loc);
		try{
			latch.await();
		}finally{
			list.remove(loc);
		}
	}

	/**
	 * Causes the current thread to wait until receiving a signal or timeout or conditions are met.
	 * @param f if true, wake up a thread
	 * @param interval second between 1 and 3600. how often do you check a function
	 * @param timeout
	 * @param unit unit for timeout
	 * @return
	 * @throws InterruptedException
	 * @throws IllegalStateException
	 * @throws NullPointerException
	 */
	public void await(Supplier<Boolean> f, long interval, long timeout, TimeUnit unit) throws InterruptedException{
		if(terminated)
			throw new IllegalStateException("A latch is already terminated.");
		if(f == null || unit == null)
			throw new NullPointerException("Function parameter or time unit is null.");
		if(interval < 1 || interval > ConditionChecker.MAX_COUNT)
			throw new IllegalStateException("Invalid interval is found. " + interval);
		
		final CountDownLatch latch = new CountDownLatch(1);
		final Locker loc = new Locker(Optional.of(f), Optional.of(interval), Optional.of(latch));
		list.add(loc);
		try{
			latch.await(timeout, unit);
		}finally{
			list.remove(loc);
		}
	}
	
	/**
	 * Wake up all threads.
	 * @return a number of threads which receive signal.
	 */
	public synchronized int signalAll(){
		final Iterator<Locker> iter = list.iterator();
		int countOfActiveThreads = 0;
		while(iter.hasNext()){
			final Locker cond = iter.next();
			final boolean success =
					cond.latch.map(latch -> {
						latch.countDown();
						return true;
					}).orElse(false);
			if(success)
				countOfActiveThreads++;
			
		}
		return countOfActiveThreads;
	}
	
	/**
	 * Signal all latch and terminate the latch.
	 * This latch can't used after it's called.
	 */
	public synchronized void terminate(){
		//1) set a flag
		terminated = true;
		//2) cancel a timer
		timer.cancel();
		//3) signal all latch for awaking
		signalAll();
	}
	
	/**
	 * Locker is private class and contains function which return boolean value, latch, interval for checking.
	 * @author qwefgh90
	 *
	 */
	private class Locker{
		private final Optional<Supplier<Boolean>> f;
		private final Optional<Long> interval;
		private final Optional<CountDownLatch> latch;
		private Locker(Optional<Supplier<Boolean>> f, Optional<Long> interval, Optional<CountDownLatch> latch){
			this.f = f;
			this.interval = interval;
			this.latch = latch;
		}
	}
	
	/**
	 * This which is used by Timer iterates all elements and decides itself whether it will wake up thread or not.
	 * This class is a implementation of TimerTask.
	 * @author qwefgh90
	 *
	 */
	private class ConditionChecker extends TimerTask{
		private static final long MAX_COUNT = 3600L;
		private static final long CHECK_INTERVAL = 1000; // Check it every 1000 millisecond
		private volatile long count = 1;
		@Override
		public void run() {
			int numberAtOnceForWakeUp = FunctionalLatch.this.numberAtOnceForWakeUp;
			// Check for all lockers
			final Iterator<Locker> iter = list.iterator();
			while(iter.hasNext()){
				if(numberAtOnceForWakeUp == 0){
					break;
				}
				final Locker loc = iter.next();
				if((count % loc.interval.orElse(1L) == 0) && loc.f.orElse(() -> false).get() == true){
					loc.latch.ifPresent(latch -> {
						latch.countDown();// If the condition is true, wake up thread.
					});
				}
				numberAtOnceForWakeUp--;
			}

			count++;
			if(count > ConditionChecker.MAX_COUNT)
				count = 1;
		}
	}

	
}
