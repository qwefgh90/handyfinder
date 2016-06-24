package io.github.qwefgh90.handyfinder.springweb.websocket;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * singleton pattern
 * @author cheochangwon
 *
 */
@JsonIgnoreProperties({ "receiver", "singleton" })
public class UpdateSummaryCommand implements ICommand {

	public enum STATE {
		START, TERMINATE
	}
	
	private int countOfDeleted;
	private int countOfExcluded;
	private int countOfModified;
	private STATE state;
	private ICommandReceiver receiver;

	private static UpdateSummaryCommand singleton;
	
	private UpdateSummaryCommand() {
		// TODO Auto-generated constructor stub
	}
	
	public static UpdateSummaryCommand getInstance(ICommandReceiver receiver){
		if(singleton == null)
			singleton = new UpdateSummaryCommand();
		singleton.receiver = receiver;
		return singleton;
	}
	
	@Override
	public void execute() {
		receiver.sendToUpdateSummary(this);
	}
	
	public STATE getState() {
		return state;
	}

	public void setState(STATE state) {
		this.state = state;
	}

	public int getCountOfDeleted() {
		return countOfDeleted;
	}

	public void setCountOfDeleted(int countOfDeleted) {
		this.countOfDeleted = countOfDeleted;
	}

	public int getCountOfExcluded() {
		return countOfExcluded;
	}

	public void setCountOfExcluded(int countOfExcluded) {
		this.countOfExcluded = countOfExcluded;
	}

	public int getCountOfModified() {
		return countOfModified;
	}

	public void setCountOfModified(int countOfModified) {
		this.countOfModified = countOfModified;
	}

	
}
