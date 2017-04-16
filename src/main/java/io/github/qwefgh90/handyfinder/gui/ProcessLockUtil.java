package io.github.qwefgh90.handyfinder.gui;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.jutils.jprocesses.JProcesses;
import org.jutils.jprocesses.model.ProcessInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ProcessLockUtil {
	private final static Logger LOG = LoggerFactory
			.getLogger(ProcessLockUtil.class);

	private final static String PID_KEY = "pid";
	private final static String HOME_URL_KEY= "homeUrl";

	/**
	 * Get a pid from lock file
	 * @return optional of a pid 
	 */
	private static Optional<Properties> getPropertiesFromLock(Path processLockPath) {
		if(Files.exists(processLockPath)){
			int pid;
			String homeUrl;
			Properties prop = new Properties();
			try(InputStream is = new FileInputStream(processLockPath.toFile())) {
				prop.load(is);
				//pid = (Integer)prop.get(PID_KEY);
				//homeUrl = prop.get(HOME_URL_KEY).toString();
				//				pid = Integer.parseInt(new String(Files.readAllBytes(AppStartupConfig.processLockPath)));
			} catch (NumberFormatException | IOException e) {
				LOG.warn(ExceptionUtils.getStackFrames(e).toString());
				return Optional.empty();
			}
			return Optional.of(prop);
		}
		return Optional.empty();
	}

	static Optional<Integer> getPidFromLock(Path processLockPath) {
		Optional<Properties> prop = getPropertiesFromLock(processLockPath);
		if(!prop.isPresent())
			return Optional.empty();
		else{
			return Optional.of(Integer.parseInt(prop.get().get(PID_KEY).toString()));
		}
	}

	static Optional<String> getHomeUrlFromLock(Path processLockPath) {
		Optional<Properties> prop = getPropertiesFromLock(processLockPath);
		if(!prop.isPresent())
			return Optional.empty();
		else{
			return Optional.of(prop.get().get(HOME_URL_KEY).toString());
		}
	}

	/**
	 * Get current process id
	 * @return current pid
	 */
	static int getCurrentPid(){
		final String process = ManagementFactory.getRuntimeMXBean().getName();
		return Integer.parseInt(
				process.split("@")[0]);
	}

	static void deleteAndWriteLockFile(Path processLockPath, int pid, String homeUrl){
		try {
			Files.deleteIfExists(processLockPath);
			Properties prop = new Properties();
			prop.put(PID_KEY, String.valueOf(pid));
			prop.put(HOME_URL_KEY, homeUrl);

			try(OutputStream out = new FileOutputStream(processLockPath.toFile())){
				prop.store(out, "lock file");
			}
		} catch (IOException e) {
			throw new RuntimeException(ExceptionUtils.getStackTrace(e));
		}
	}

	/**
	 * Get a process information
	 * @param pid
	 * @return return a object of ProcessInfo
	 */
	static Optional<ProcessInfo> getProcessInfo(int pid){
		String pidCompared = String.valueOf(pid);
		boolean matched = JProcesses.getProcessList().stream().anyMatch((processInfo) -> {
			return processInfo.getPid().equals(pidCompared);
		});
		return matched ? Optional.of(JProcesses.getProcess(pid)) : Optional.empty();
	}
}
