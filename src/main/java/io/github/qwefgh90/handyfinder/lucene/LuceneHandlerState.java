package io.github.qwefgh90.handyfinder.lucene;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * It manages a state of LuceneHandler's instance and It's FSM.
 * @author ccw
 *
 */
public class LuceneHandlerState {
	private final static Logger LOG = LoggerFactory
			.getLogger(LuceneHandlerState.class);
	
	public final static LuceneHandlerState self = new LuceneHandlerState();
	
	// indexing state (startIndex(), stopIndex() use state)
	static enum INDEX_WRITE_STATE {
		PROGRESS, STOPPING, READY, CAN_NOT_MATCH
	}
	
	static class TransitionObserver{
		/**
		 * 
		 * @param before
		 * @param after
		 * @param consumer if condition is true, execute it with self reference. 
		 */
		TransitionObserver(Optional<INDEX_WRITE_STATE> before, Optional<INDEX_WRITE_STATE> after, Consumer<TransitionObserver> consumer){
			this.before = before;
			this.after = after;
			this.consumer = consumer;
		}
		protected final Optional<INDEX_WRITE_STATE> before;
		protected final Optional<INDEX_WRITE_STATE> after;
		protected final Consumer<TransitionObserver> consumer;
	}
	private final List<TransitionObserver> listOfObservers = Collections.synchronizedList(new ArrayList<TransitionObserver>());
	private INDEX_WRITE_STATE writeStateInternal = INDEX_WRITE_STATE.READY; // no directly access
	
	void addObserverOfTransition(TransitionObserver observer){
		listOfObservers.add(observer);
	}
	
	void removeObserver(TransitionObserver observer){
		listOfObservers.remove(observer);
	}
	
	void clearObservers(){
		listOfObservers.clear();
	}
	
	/**
	 * Update handler's state synchronously
	 * 
	 * @param state
	 */
	@SuppressWarnings("incomplete-switch")
	private synchronized boolean updateWriteState(INDEX_WRITE_STATE state) {
		// progress
		boolean success = false;
		final INDEX_WRITE_STATE before = this.writeStateInternal;
		switch(state){			
		case READY:{
			this.writeStateInternal = state;
			LOG.debug("LuceneHandler is READY");
			success = true;
			break;
		}

		case PROGRESS:{
			if(writeStateInternal != INDEX_WRITE_STATE.STOPPING){
				this.writeStateInternal = state;
				LOG.debug("LuceneHandler is PROGRESS");
				success = true;
			}
			break;

		}
		case STOPPING:{
			if(writeStateInternal != INDEX_WRITE_STATE.READY){
				this.writeStateInternal = state;
				LOG.debug("LuceneHandler is STOPPING");
				success = true;
			}
			break;
		}
		}
		final INDEX_WRITE_STATE after = this.writeStateInternal;

		//Defensive copying
		listOfObservers.stream().collect(Collectors.toList()).forEach((TransitionObserver observer) -> {
			if((observer.before.orElse(INDEX_WRITE_STATE.CAN_NOT_MATCH) == before
					&& observer.after.orElse(INDEX_WRITE_STATE.CAN_NOT_MATCH) == after))
				observer.consumer.accept(observer);	//check both before and after
			else if(observer.before.orElse(INDEX_WRITE_STATE.CAN_NOT_MATCH) == before
					&& !observer.after.isPresent())
				observer.consumer.accept(observer);	//check only before
			else if(observer.after.orElse(INDEX_WRITE_STATE.CAN_NOT_MATCH) == after
					&& !observer.before.isPresent())
				observer.consumer.accept(observer);	//check only after
		});

		return success;
	}

	/**
	 * It's private API
	 * @return
	 */
	synchronized boolean isStopping(){
		if(writeStateInternal == LuceneHandlerState.INDEX_WRITE_STATE.STOPPING)
			return true;
		return false;
	}

	/**
	 * 
	 * It's private API
	 * @return
	 */
	synchronized boolean isReady(){
		if(writeStateInternal == LuceneHandlerState.INDEX_WRITE_STATE.PROGRESS 
				||writeStateInternal == LuceneHandlerState.INDEX_WRITE_STATE.STOPPING)
			return false;
		return true;
	}
	
	boolean progress(){
		return updateWriteState(LuceneHandlerState.INDEX_WRITE_STATE.PROGRESS);
	}
	
	boolean ready(){
		return updateWriteState(LuceneHandlerState.INDEX_WRITE_STATE.READY);
	}
	
	boolean stopping(){
		return updateWriteState(LuceneHandlerState.INDEX_WRITE_STATE.STOPPING);
	}
}
