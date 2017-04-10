package io.github.qwefgh90.future;

import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

public class FutureTest {

	@Test
	public void test() throws InterruptedException {
		CompletableFuture<String> a1 = CompletableFuture.supplyAsync(() -> {return "Hello";});
		CompletableFuture<String> a2 = CompletableFuture.supplyAsync(() -> {throw new RuntimeException("hell1"); });
		CompletableFuture<String> a3 = CompletableFuture.supplyAsync(() -> {try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} return "Hello";});
		CompletableFuture<String> a4 = CompletableFuture.supplyAsync(() -> {throw new RuntimeException("hell2");});
		List<CompletableFuture<String>> a = new ArrayList<>();
		a.add(a1);a.add(a2);a.add(a3);a.add(a4);
		CompletableFuture<Void> futureOfAll = CompletableFuture.allOf(a.toArray(new CompletableFuture[a.size()]));
		futureOfAll.whenComplete((result, exception)->{
			System.out.println(result);
			System.out.println(exception.toString());
		});
		
		Thread.sleep(10000);
	}

}
