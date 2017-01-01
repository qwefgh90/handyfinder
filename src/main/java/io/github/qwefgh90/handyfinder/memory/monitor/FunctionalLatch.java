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
 * FunctionalLatch class provides functional tools for awaiting and release.
 * Default value which is for release is 1. You can set this value at any time.
 * <br><br>Every second, a internal timer executes functions and release threads which return true.
 * 
 * @author qwefgh90
 *
 */
public class FunctionalLatch {
	private final List<Locker> lockerList = Collections.synchronizedList(new ArrayList<Locker>());
	private final Timer timer = new Timer(true); //make a daemon thread	
	private volatile boolean terminated = false;
	private volatile int numberAtOnceForRelease;
	
	/**
	 * Initialize a functional latch.
	 * At once, one thread is released.
	 */
	public FunctionalLatch(){
		this(1);
	}

	/**
	 * Initialize a functional latch.
	 * @param numberAtOnceForRelease a number greater than zero
	 */
	public FunctionalLatch(int numberAtOnceForRelease){
		if(numberAtOnceForRelease <= 0)
			throw new ParameterException("numberAtOnceForRelease is less than one. " + numberAtOnceForRelease);
		this.numberAtOnceForRelease = numberAtOnceForRelease;
		timer.schedule(new ConditionChecker(), 0, ConditionChecker.CHECK_INTERVAL); // Start checking lockers
	}

	/**
	 * Setter for the number of thread which will be released at once.
	 * @param numberAtOnceForRelease number
	 */
	public void setNumberAtOnceRelease(int numberAtOnceForRelease){
		if(numberAtOnceForRelease <= 0)
			return;
		this.numberAtOnceForRelease = numberAtOnceForRelease;
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
		lockerList.add(loc);
		latch.await();

	}

	/**
	 * Causes the current thread to wait until receiving a signal or conditions are met.
	 * @param f it's for release. if returning true, releases a thread
	 * @param interval second between 1 and 3600. it's related to how often a timer executes a function
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
		if(interval < 1 || interval > ConditionChecker.END_OF_SECONDS)
			throw new IllegalStateException("Invalid interval is found. " + interval);

		final CountDownLatch latch = new CountDownLatch(1);
		final Locker loc = new Locker(Optional.of(f), Optional.of(interval), Optional.of(latch));
		lockerList.add(loc);
		latch.await();		
	}

	/**
	 * Causes the current thread to wait until receiving a signal or timeout or conditions are met.
	 * @param f it's for release. if returning true, releases a thread
	 * @param interval second between 1 and 3600. it's related to how often a timer executes a function
	 * @param timeout maximum waiting time
	 * @param unit it's for timeout
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
		if(interval < 1 || interval > ConditionChecker.END_OF_SECONDS)
			throw new IllegalStateException("Invalid interval is found. " + interval);

		final CountDownLatch latch = new CountDownLatch(1);
		final Locker loc = new Locker(Optional.of(f), Optional.of(interval), Optional.of(latch));
		lockerList.add(loc);
		latch.await(timeout, unit);
	}

	/**
	 * Releases all threads.
	 * @return a number of threads which receive signal.
	 */
	public int signalAll(){
		int countOfActiveThreads = 0;
		//https://docs.oracle.com/javase/7/docs/api/java/util/Collections.html#synchronizedList(java.util.List)
		//It's imperative.
		synchronized(lockerList){
			final Iterator<Locker> iter = lockerList.iterator();
			while(iter.hasNext()){
				final Locker cond = iter.next();
				final boolean success =
						cond.latch.map(latch -> {
							iter.remove();
							latch.countDown();
							return true;
						}).orElse(false);
				if(success)
					countOfActiveThreads++;

			}
		}
		return countOfActiveThreads;
	}

	/**
	 * Signal the latch and terminate the latch.
	 * This latch can't used after it's called.
	 */
	public void terminate(){
		//1) set a flag
		terminated = true;
		//2) cancel a timer (release all waiting threads.)
		timer.cancel();
		//3) signal all latch for release
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
	 * This which is used by a timer iterates all elements and decides whether to release threads in elements by itself.
	 * <br><br>This class is a implementation of TimerTask.
	 * @author qwefgh90
	 *
	 */
	private class ConditionChecker extends TimerTask{
		private static final long END_OF_SECONDS = 3600L;
		private static final long CHECK_INTERVAL = 1000; // Check it every 1000 millisecond
		private volatile long count = 1;
		@Override
		public void run() {
			int numberAtOnceForRelease = FunctionalLatch.this.numberAtOnceForRelease;
			// Check for all lockers
			synchronized(lockerList){
				final Iterator<Locker> iter = lockerList.iterator();
				while(iter.hasNext()){
					if(numberAtOnceForRelease == 0){
						break;
					}
					final Locker loc = iter.next();
					if((count % loc.interval.orElse(1L) == 0) && loc.f.orElse(() -> false).get() == true){
						boolean success = loc.latch.map(latch -> {
							iter.remove();
							latch.countDown();// If the condition is true, release thread.
							return true;
						}).orElse(false);
						if(success)
							numberAtOnceForRelease--;
					}
				}

				count++;
				if(count > ConditionChecker.END_OF_SECONDS)
					count = 1;
			}
		}
	}
}


