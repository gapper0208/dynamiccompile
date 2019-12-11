package com.gao.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;


class CharSequenceJavaFileObject extends SimpleJavaFileObject {

	// 封装字符串类型的源码到content字段中
	private String content;
	
	protected CharSequenceJavaFileObject(String className, String content) {
		super(URI.create(className + Kind.SOURCE.extension), Kind.SOURCE);
		this.content = content;
	}
	
	// 暴露出这个接口，以便让编译器能通过这个接口来获取到字符串类型的源码！
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return content;
	}
}


class JavaClassObject extends SimpleJavaFileObject {
	
	// 定义一个输出流，用于装载JavaCompiler编译后的class文件
	private ByteArrayOutputStream bos = new ByteArrayOutputStream();

	public JavaClassObject(String name, Kind kind) {
		super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
	}
	
	public byte[] getBytes() {
		return bos.toByteArray();
	}
	
	@Override
	public OutputStream openOutputStream() throws IOException {
		return bos;
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		bos.close();
	}
	
}


class ClassFileManager extends ForwardingJavaFileManager {
	
	// 封装编译好的字节码到jclassObject字段中
	private JavaClassObject jclassObject;
	
	public ClassFileManager(JavaFileManager fileManager) {
		super(fileManager);
	}
	
	// 暴露出这个接口，以便让编译器能通过这个接口来获取到一个对象，
	// 这个对象的作用就是为了封装编译器所编译好的字节码
	@Override
	public JavaFileObject getJavaFileForOutput(Location location, String className, Kind kind, FileObject sibling)
			throws IOException {
		if(jclassObject == null) {
			jclassObject = new JavaClassObject(className, kind);
		}
		return jclassObject;
	}
	
	public JavaClassObject getJavaClassObject() {
		return jclassObject;
	}
}


// 用于加载内存中的字节码的类加载器：动态类加载器
class DynamicClassLoader extends URLClassLoader {
    public DynamicClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }
 
    public Class loadClass(String fullName, JavaClassObject jco) {
        byte[] classData = jco.getBytes();
        return this.defineClass(fullName, classData, 0, classData.length);
    }
}


public class AppTest {
	public static void main(String[] args) throws Exception {
		
		// 字符串源码
		String src = "public class Foo {\r\n" + 
				"	public static void main(String[] args) throws Exception {\r\n" + 
				"		System.out.println(\"java dynamic compile!\");\r\n" + 
				"	}\r\n" + 
				"}\r\n" + 
				"";
		
		Pattern pattern = Pattern.compile("class\\s+(\\S+)\\s*\\{");
		Matcher matcher = pattern.matcher(src);
		matcher.find();
		String fullClassName = matcher.group(1);
		
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
		
		if(r) {
			// 编译成功！
			// 获取编译好的，存放在内存中的字节码！
			JavaClassObject jco = fileManager.getJavaClassObject();
			
			DynamicClassLoader dcl = new DynamicClassLoader(ucl);
			
			// 自定的类加载器DynamicClassLoader的loadClass方法先会回调回调传入的JavaObjectClass对象jco的getBytes方法，
			// 如此会得到已经在内存中保存好的字节码的2进制数据！ 进而转换为Class对象！
			Class clazz = dcl.loadClass(fullClassName, jco);
			System.out.println("Class is: " + clazz);
			
			// 截获目标类的输出
			Method m = clazz.getDeclaredMethod("main", String[].class);
			PrintStream oldOut = System.out;
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			System.setOut(new PrintStream(out));
			m.invoke(null, (Object) new String[] {});
			byte[] bb = out.toByteArray();
			System.out.close();
			System.setOut(oldOut);
			System.out.println("result is: " + new String(bb));
		} else {
			// 编译失败！ 既然编译失败了，也就不会再进一步使用反射机制来调用main方法了！
			// 而是打印编译失败的信息！
			System.out.println("编译失败！！");
			StringBuilder error = new StringBuilder();
			for (Diagnostic diagnostic : diagnostics.getDiagnostics()) {
				error.append(compilePrint(diagnostic));
            }
		}
		
		
		System.out.println("i am here!!");
	}
	
	
	private static String compilePrint(Diagnostic diagnostic) {
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
}