package io.github.qwefgh90.handyfinder.memory.monitor;

import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Test class is for FunctionalLatch object
 * @author cheochangwon
 *
 */
public class FunctionalLatchTest {
	
	private final static Logger LOG = LoggerFactory
			.getLogger(FunctionalLatchTest.class);
	
	/**
	 * infinite waiting
	 * @throws InterruptedException 
	 */
	@Test
	public void infiniteWaiting() throws InterruptedException{
		final FunctionalLatch latch = new FunctionalLatch();
		final ChangesFlag flag = new ChangesFlag(false, 0);
		final Runnable run = new Runnable(){
			@Override
			public void run() {
				try {
					latch.await(() -> false, 1);
					synchronized(this){
						flag.count++;
						LOG.trace("Wake up in infinite waiting : " + flag.count);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		final int WAITING_COUNT = 100;
		for(int i=0; i<WAITING_COUNT; i++)
			new Thread(run).start();

		Thread.sleep(5000);
		assertThat(flag.count, is(0));
		latch.signalAll();
		Thread.sleep(2000);
		assertThat(flag.count, is(WAITING_COUNT));
		latch.terminate();
	}
	
	/**
	 * functional waiting
	 * @throws InterruptedException 
	 */
	@Test
	public void functionalWaiting() throws InterruptedException{
		final int WAITING_COUNT = 100;
		final FunctionalLatch latch = new FunctionalLatch(WAITING_COUNT);
		final ChangesFlag flag = new ChangesFlag(false, 0);
		final Long fromMilliSeconds = System.currentTimeMillis(); 
		final Runnable run = new Runnable(){
			@Override
			public void run() {
				try {
					latch.await(() -> { // release all threads. after 5 senconds
						if((System.currentTimeMillis() - fromMilliSeconds) / 1000 > 5)
							return true;
						else
							return false;
					}, 1);
					synchronized(this){
						flag.count++;
						LOG.trace("Wake up in finite waiting : " + flag.count);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		for(int i=0; i<WAITING_COUNT; i++)
			new Thread(run).start();
		Thread.sleep(2000);
		assertThat(flag.count, is(0));
		Thread.sleep(5000);	//plus 2 seconds
		assertThat(flag.count, is(WAITING_COUNT)); //after 5 seconds, release all threads.
		latch.terminate();
	}
	
	
	/**
	 * functional waiting
	 * @throws InterruptedException 
	 */
	@Test
	public void functionalWaitingHeavy() throws InterruptedException{
		final int WAITING_COUNT_FIRST = 500;
		final int WAITING_COUNT_SECOND = 1000;
		final FunctionalLatch latch = new FunctionalLatch(WAITING_COUNT_SECOND);
		final ChangesFlag flag = new ChangesFlag(false, 0);
		final Long fromMilliSeconds = System.currentTimeMillis();
		final Runnable shortRun = new Runnable(){
			@Override
			public void run() {
				try {
					latch.await(() -> { // release half threads, after 2 senconds
						if((System.currentTimeMillis() - fromMilliSeconds) / 1000 > 2)
							return true;
						else
							return false;
					}, 1);
					synchronized(this){
						flag.count++;
						LOG.trace("Wake up in functional waiting : " + flag.count);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		final Runnable longRun = new Runnable(){
			@Override
			public void run() {
				try {
					latch.await(() -> { // release remaining threads, after 6 senconds
						if((System.currentTimeMillis() - fromMilliSeconds) / 1000 > 6)
							return true;
						else
							return false;
					}, 1);
					synchronized(this){
						flag.count++;
						LOG.trace("Wake up in functional waiting : " + flag.count);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		for(int i=0; i<WAITING_COUNT_FIRST; i++)
			new Thread(shortRun).start();
		for(int i=0; i<WAITING_COUNT_SECOND - WAITING_COUNT_FIRST; i++)
			new Thread(longRun).start();
		Thread.sleep(1000);
		assertThat(flag.count, is(0));
		Thread.sleep(3000);	// plus 2 seconds to default
		assertThat(flag.count, is(WAITING_COUNT_FIRST));//after 2 seconds, return true
		Thread.sleep(4000); // plus 2 seconds
		assertThat(flag.count, is(WAITING_COUNT_SECOND));//after 6 seconds, return true
		latch.terminate();
	}
	
	/**
	 * functional waiting with timeout
	 * @throws InterruptedException 
	 */
	@Test
	public void functionalWaitingTimeout() throws InterruptedException{
		final int WAITING_COUNT_FIRST = 500;
		final int WAITING_COUNT_SECOND = 1000;
		final FunctionalLatch latch = new FunctionalLatch(WAITING_COUNT_SECOND);
		final ChangesFlag flag = new ChangesFlag(false, 0);
		final Long fromMilliSeconds = System.currentTimeMillis();
		final Runnable shortRun = new Runnable(){
			@Override
			public void run() {
				try {
					latch.await(() -> { // release half threads, after 2 senconds
						if((System.currentTimeMillis() - fromMilliSeconds) / 1000 > 2)
							return true;
						else
							return false;
					}, 1);
					synchronized(this){
						flag.count++;
						LOG.trace("Wake up in functional waiting : " + flag.count);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		final Runnable longRun = new Runnable(){
			@Override
			public void run() {
				try {
					latch.await(() -> { // release remaining threads, after 10 seconds
						if((System.currentTimeMillis() - fromMilliSeconds) / 1000 > 10)
							return true;
						else
							return false;
					}, 1, 5, TimeUnit.SECONDS); // timeout occurs, after 5 seconds
					synchronized(this){
						flag.count++;
						LOG.trace("Wake up in functional waiting : " + flag.count);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		};
		for(int i=0; i<WAITING_COUNT_FIRST; i++)
			new Thread(shortRun).start();
		for(int i=0; i<WAITING_COUNT_SECOND - WAITING_COUNT_FIRST; i++)
			new Thread(longRun).start();
		Thread.sleep(1000);
		assertThat(flag.count, is(0));
		Thread.sleep(3000);	// plus 2 seconds to default
		assertThat(flag.count, is(WAITING_COUNT_FIRST));//after 2 seconds, return true
		Thread.sleep(3000); // timeout occurs and release all threads, after 5 seconds
		assertThat(flag.count, is(WAITING_COUNT_SECOND));
		latch.terminate();
	}
	
	private static class ChangesFlag{
		public ChangesFlag(boolean change, int count){
			this.change = change;
			this.count = count;
		}
		volatile public boolean change = false;
		volatile public int count = 0;
	}
}
