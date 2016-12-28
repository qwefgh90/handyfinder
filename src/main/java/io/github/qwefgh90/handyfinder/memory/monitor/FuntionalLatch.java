package io.github.qwefgh90.handyfinder.memory.monitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;
/**
 * 
 * boolean await() causes the current thread to wait until the latch meet condition, unless the thread is interrupted.
 * boolean await(Supplier<Boolean>, int interval) causes the current thread to wait until the latch meet condition, unless the thread is interrupted.
 * int signalAll(Supplier<Boolean>) wake up all waiting threads which return true, if it meets condition
 * int signalAll() wake up all waiting threads which return true
 * String getInfo() all waiting thread information
 * void terminate() wake up all waiting threads which return false
 * @author qwefgh90
 *
 */
public class FuntionalLatch {
	private final List<Condition> list = Collections.synchronizedList(new ArrayList<Condition>());
	private volatile boolean alreadyTerminated = false;
	private class Condition{
		private final Optional<Supplier<Boolean>> f;
		private final Optional<Long> interval;
		private final Optional<CountDownLatch> latch;
		private Condition(Optional<Supplier<Boolean>> f, Optional<Long> interval, Optional<CountDownLatch> latch){
			this.f = f;
			this.interval = interval;
			this.latch = latch;
		}
	}
	
	public boolean await() throws InterruptedException{
		final CountDownLatch latch = new CountDownLatch(1);
		list.add(new Condition(Optional.empty(), Optional.empty(), Optional.of(latch)));
		latch.await();
		return !alreadyTerminated;
	}
	
	public boolean await(Supplier<Boolean> f, long interval) throws InterruptedException{
		final CountDownLatch latch = new CountDownLatch(1);
		list.add(new Condition(Optional.of(f), Optional.of(interval), Optional.of(latch)));
		latch.await();
		return !alreadyTerminated;
	}
	
	public synchronized int signalAll(){
		final Iterator<Condition> iter = list.iterator();
		int removed = 0;
		while(iter.hasNext()){
			final Condition cond = iter.next();
			final boolean success =
					cond.latch.map(latch -> {
						latch.countDown();
						iter.remove();
						return true;
					}).orElse(false);
			if(success)
				removed++;
			
		}
		return removed;
	}
	
	private class ConditionChecker extends TimerTask{
		volatile long count = 0;
		@Override
		public void run() {
			final Iterator<Condition> iter = list.iterator();
			while(iter.hasNext()){
				final Condition cond = iter.next();
				if((count % cond.interval.orElse(1L) == 0) && cond.f.orElse(() -> false).get()){
					cond.latch.ifPresent(latch -> {
						latch.countDown();
						iter.remove();
					});
				}

			}

			count++;
			if(count > 3600)
				count = 0;
		}
	}

	
}
