package com.gao.app;

import com.gao.dynamiccompile.DynamicCompiler;

public class App {
	public static void main(String[] args) throws NoSuchMethodException, SecurityException  {
		String src = "import java.util.concurrent.TimeUnit;\r\n" + 
				"\r\n" + 
				"public class Foo {\r\n" + 
				"	public static void main(String[] args)  {\r\n" + 
				"		for (int i = 1; i<=10 ; i++) {\r\n" + 
				"			System.out.println(\"疾风\");\r\n" + 
				"		}\r\n" + 
				"	}\r\n" + 
				"}";
		
		DynamicCompiler dc = DynamicCompiler.getDeDynamicCompiler();
		String result = dc.justRun(src);
		System.out.println(result);
		
//		以下代码是用来防止出现死循环的！
//		Thread th = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				DynamicCompiler dc = DynamicCompiler.getDeDynamicCompiler();
//				String result = dc.justRun(src);
//				System.out.println(result);
//			}
//		});
//		th.start();
//		
//		Thread terminator = new Thread(new Runnable() {
//			@Override
//			public void run() {
//				try {
//					Thread.sleep(3000);
//				} catch (InterruptedException e) {
//					e.printStackTrace();
//				}
//				th.stop();
//			}
//		});
//		
//		terminator.start();
//		System.out.println("i am here!");
		
	}
}
