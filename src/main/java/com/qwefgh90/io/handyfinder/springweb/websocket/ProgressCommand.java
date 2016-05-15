package com.qwefgh90.io.handyfinder.springweb.websocket;

import java.nio.file.Path;

import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * singleton class
 * Command pattern
 * @author choechangwon
 *
 */
@JsonIgnoreProperties({ "receiver", "state" })
public class ProgressCommand implements ICommand {
	public enum STATE {
		START, TERMINATE, PROGRESS
	}

	private STATE state;
	private int processIndex; // start from one
	private Path processPath;
	private int totalProcessCount;
	private IInteractionReceiver receiver;

	private ProgressCommand() {
	}

	private static ProgressCommand command;

	public static ProgressCommand getInstance(IInteractionReceiver receiver) {
		if (command == null)
			command = new ProgressCommand();
		command.receiver = receiver;
		return command;
	}

	@Override
	public void execute() {
		switch (this.state) {
		case START:
			// receiver.startProgressChannel();
			break;
		case PROGRESS:
			receiver.sendToProgressChannel(this);
			break;
		case TERMINATE:
			// receiver.terminateProgressChannel();
			break;
		default:
			break;
		}
	}

	public STATE getState() {
		return state;
	}

	public void setState(STATE state) {
		this.state = state;
	}

	public int getProcessIndex() {
		return processIndex;
	}

	public void setProcessIndex(int processIndex) {
		this.processIndex = processIndex;
	}

	public Path getProcessPath() {
		return processPath;
	}

	public void setProcessPath(Path processPath) {
		this.processPath = processPath;
	}

	public int getTotalProcessCount() {
		return totalProcessCount;
	}

	public void setTotalProcessCount(int totalProcessCount) {
		this.totalProcessCount = totalProcessCount;
	}

	public IInteractionReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(IInteractionReceiver receiver) {
		this.receiver = receiver;
	}

}
