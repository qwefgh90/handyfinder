package io.github.qwefgh90.handyfinder.memory.monitor;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.Test;

public class HeapMemoryLimit {
	@Test
	public void logHeapMemory(){
		printHeap();
		//-Xmx2G
		StringBuffer sb = new StringBuffer();
		for(int j=0; j<100; j++)
			sb.append(j % 10);
		
		char[] ch = sb.toString().toCharArray();
		final List<String> list = new ArrayList<>();
		for(int i=0; i<1000000; i++){// * 1MB
			list.add(new String(ch));
		}
		
		System.out.println("\nadd 1000MB\n");
		
		printHeap();
		
	}
	
	public void printHeap(){
		long max = Runtime.getRuntime().maxMemory();
		long free = Runtime.getRuntime().freeMemory();
		long proc = Runtime.getRuntime().availableProcessors();
		long total = Runtime.getRuntime().totalMemory();
		System.out.println("max heap : " + String.format("%,d", max) + " bytes");
		System.out.println("free heap : " + String.format("%,d", free) + " bytes");
		System.out.println("processors : " + proc + " ");
		System.out.println("current heap : " + String.format("%,d", total) + " bytes");
		System.out.println("heap : " + String.format("%,d", total - free) + " bytes");
	}
	
	@Test
	public void logDiskSize(){
		printDisk();
		
	}
	
	public void printDisk(){
		File f = new File(".");
		System.out.println(String.format("total : %,d", f.getTotalSpace()));
		System.out.println(String.format("free : %,d", f.getFreeSpace()));
		System.out.println(String.format("usable : %,d", f.getUsableSpace()));
		System.out.println(String.format("percent : %f", ((double)f.getUsableSpace() / (double)f.getTotalSpace()) * 100));
		
		
		
	}
}
