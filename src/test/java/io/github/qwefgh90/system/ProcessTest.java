package io.github.qwefgh90.system;

import static org.junit.Assert.*;

import java.lang.management.ManagementFactory;

import org.junit.Test;
import org.jutils.jprocesses.JProcesses;

public class ProcessTest {

	@Test
	public void test() {

		String process = ManagementFactory.getRuntimeMXBean().getName();
		System.out.println(process);
		
		System.out.println(JProcesses.getProcess(Integer.parseInt(
		process.split("@")[0])).toString());
		
		JProcesses.getProcessList().forEach((proc) ->{
			System.out.println(proc.toString());
		});
	}

}
