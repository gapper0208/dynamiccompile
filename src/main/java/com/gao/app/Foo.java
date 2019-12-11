package com.gao.app;

import java.util.concurrent.TimeUnit;

public class Foo {
	public static void main(String[] args)  {
		try {
			TimeUnit.SECONDS.sleep(5);
		} catch (InterruptedException e) {
			System.out.println("´ò¶Ïsleep");
		}
		
		for (int i = 1; i <= 10; i++) {
			System.out.println("¼²·ç");
		}
	}
}