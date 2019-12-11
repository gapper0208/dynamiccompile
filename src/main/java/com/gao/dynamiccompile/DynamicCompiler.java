package com.gao.dynamiccompile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;

// 动态编译器， 这是对外暴露的类，隐藏复杂的实现细节，暴露简单的使用方法
public class DynamicCompiler {
	
	private static DynamicCompiler dc = new DynamicCompiler();
	
	private DynamicCompiler() {
	}
	
	public static DynamicCompiler getDeDynamicCompiler() {
		return dc;
	}

	// 校验源码，防止Runtime干坏事！
	private boolean validateSrc(String src) {
		Pattern pattern = Pattern.compile("Runtime.getRuntime()");
		Matcher matcher = pattern.matcher(src);
		return !matcher.find();
	}
	
	private  String getFullClassName(String src) {
		Pattern pattern = Pattern.compile("class\\s+(\\S+)\\s*\\{");
		Matcher matcher = pattern.matcher(src);
		matcher.find();
		String fullClassName = matcher.group(1);
		return fullClassName;
	}

	private  Class dynamicCompile(String fullClassName, String src) throws NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

		// 建立DiagnosticCollector对象，该对象用于存放动态编译的输出信息！
		DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<JavaFileObject>();

		List<JavaFileObject> srcList = new ArrayList<>();
		srcList.add(new CharSequenceJavaFileObject(fullClassName, src));

		ClassFileManager fileManager = new ClassFileManager(javac.getStandardFileManager(null, null, null));

		List<String> options = new ArrayList<String>();
		options.add("-encoding");
		options.add("UTF-8");
		options.add("-classpath");

		URLClassLoader ucl = (URLClassLoader) ClassLoader.getSystemClassLoader();
		StringBuilder sb = new StringBuilder();
		for (URL url : ucl.getURLs()) {
			String p = url.getFile();
			sb.append(p).append(File.pathSeparator);
		}
		options.add(sb.toString());

		// 注意在进行动态编译时，这里使用diagnostics对象来记录动态编译的信息！
		CompilationTask task = javac.getTask(null, fileManager, diagnostics, options, null, srcList);

		boolean r = task.call();

		if (r) {
			// 编译成功！
			// 获取编译好的，存放在内存中的字节码！
			JavaClassObject jco = fileManager.getJavaClassObject();

			DynamicClassLoader dcl = new DynamicClassLoader(ucl);

			// 自定的类加载器DynamicClassLoader的loadClass方法先会回调回调传入的JavaObjectClass对象jco的getBytes方法，
			// 如此会得到已经在内存中保存好的字节码的2进制数据！ 进而转换为Class对象！
			Class clazz = dcl.loadClass(fullClassName, jco);
			return clazz;
			
		} else {
			// 编译失败！ 既然编译失败了，也就不会再进一步使用反射机制来调用main方法了！
			// 而是打印编译失败的信息！
			StringBuilder error = new StringBuilder();
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				error.append(compilePrint(diagnostic));
			}
			return null;
		}
	}
	
	
	private synchronized  String compilePrint(Diagnostic diagnostic) {
        System.out.println("Code:" + diagnostic.getCode());
        System.out.println("Kind:" + diagnostic.getKind());
        System.out.println("Position:" + diagnostic.getPosition());
        System.out.println("Start Position:" + diagnostic.getStartPosition());
        System.out.println("End Position:" + diagnostic.getEndPosition());
        System.out.println("Source:" + diagnostic.getSource());
        System.out.println("Message:" + diagnostic.getMessage(null));
        System.out.println("LineNumber:" + diagnostic.getLineNumber());
        System.out.println("ColumnNumber:" + diagnostic.getColumnNumber());
        StringBuffer res = new StringBuffer();
        res.append("Code:[" + diagnostic.getCode() + "]\n");
        res.append("Kind:[" + diagnostic.getKind() + "]\n");
        res.append("Position:[" + diagnostic.getPosition() + "]\n");
        res.append("Start Position:[" + diagnostic.getStartPosition() + "]\n");
        res.append("End Position:[" + diagnostic.getEndPosition() + "]\n");
        res.append("Source:[" + diagnostic.getSource() + "]\n");
        res.append("Message:[" + diagnostic.getMessage(null) + "]\n");
        res.append("LineNumber:[" + diagnostic.getLineNumber() + "]\n");
        res.append("ColumnNumber:[" + diagnostic.getColumnNumber() + "]\n");
        return res.toString();
    }
	
	private synchronized  String run(Class clazz) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		// 截获目标类的输出
		Method m = clazz.getDeclaredMethod("main", String[].class);
		PrintStream oldOut = System.out;
		byte[] bb = null;
		try {
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new PrintStream(out));
			m.invoke(null, (Object) new String[] {});
			bb = out.toByteArray();
			System.out.close();
		} catch(Exception e) {
			throw new RuntimeException("利用反射机制调用的main方法抛出异常了",e);
		} finally {
			// 就算利用反射机制调用的main方法抛出异常了，
			// 也能保证把“标准输出”恢复！
			System.setOut(oldOut);
			// System.out.println("finally");
		}
		return new String(bb);
	}
	
	// 外界可直接调用的方法
	public  String justRun(String src) {
		try {
			// 校验源码
			boolean bb = validateSrc(src);
			if(!bb) {
				// 流程走到这里，说明校验失败！
				throw new RuntimeException("源码中包含敏感api，不允许执行！！");
			}
			String fullClassName = getFullClassName(src);
			Class clazz = dynamicCompile(fullClassName, src);
			String result = "";
			if(clazz != null) {
				// 能进入这里，说明编译成功！！
				result = run(clazz);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
