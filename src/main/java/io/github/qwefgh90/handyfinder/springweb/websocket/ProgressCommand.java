package io.github.qwefgh90.handyfinder.springweb.websocket;

import java.nio.file.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * singleton class
 * Command pattern
 * @author choechangwon
 *
 */
@JsonIgnoreProperties({ "receiver", "command" })
public class ProgressCommand implements ICommand {
	private final static Logger LOG = LoggerFactory.getLogger(ProgressCommand.class);
	public enum STATE {
		START, PROGRESS, TERMINATE
	}

	private STATE state;	//START , PROGRESS , TERMINATE
	private int processIndex; // integer starting from ZERO
	private Path processPath; // path string
	private int totalProcessCount; // integer
	private ICommandReceiver receiver;

	private ProgressCommand() {
	}

	private static ProgressCommand command;

	public static ProgressCommand getInstance(ICommandReceiver receiver) {
		if (command == null)
			command = new ProgressCommand();
		command.receiver = receiver;
		return command;
	}

	@Override
	public void execute() {
		switch (this.state) {
		case START:
			receiver.startProgressChannel(this);
			break;
		case PROGRESS:
			receiver.sendToProgressChannel(this);
			break;
		case TERMINATE:
			receiver.terminateProgressChannel(this);
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

	public ICommandReceiver getReceiver() {
		return receiver;
	}

	public void setReceiver(ICommandReceiver receiver) {
		this.receiver = receiver;
	}

}
