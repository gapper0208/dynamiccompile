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

// ��̬�������� ���Ƕ��Ⱪ¶���࣬���ظ��ӵ�ʵ��ϸ�ڣ���¶�򵥵�ʹ�÷���
public class DynamicCompiler {
	
	private static DynamicCompiler dc = new DynamicCompiler();
	
	private DynamicCompiler() {
	}
	
	public static DynamicCompiler getDeDynamicCompiler() {
		return dc;
	}

	// У��Դ�룬��ֹRuntime�ɻ��£�
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

		// ����DiagnosticCollector���󣬸ö������ڴ�Ŷ�̬����������Ϣ��
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

		// ע���ڽ��ж�̬����ʱ������ʹ��diagnostics��������¼��̬�������Ϣ��
		CompilationTask task = javac.getTask(null, fileManager, diagnostics, options, null, srcList);

		boolean r = task.call();

		if (r) {
			// ����ɹ���
			// ��ȡ����õģ�������ڴ��е��ֽ��룡
			JavaClassObject jco = fileManager.getJavaClassObject();

			DynamicClassLoader dcl = new DynamicClassLoader(ucl);

			// �Զ����������DynamicClassLoader��loadClass�����Ȼ�ص��ص������JavaObjectClass����jco��getBytes������
			// ��˻�õ��Ѿ����ڴ��б���õ��ֽ����2�������ݣ� ����ת��ΪClass����
			Class clazz = dcl.loadClass(fullClassName, jco);
			return clazz;
			
		} else {
			// ����ʧ�ܣ� ��Ȼ����ʧ���ˣ�Ҳ�Ͳ����ٽ�һ��ʹ�÷������������main�����ˣ�
			// ���Ǵ�ӡ����ʧ�ܵ���Ϣ��
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
		// �ػ�Ŀ��������
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
			throw new RuntimeException("���÷�����Ƶ��õ�main�����׳��쳣��",e);
		} finally {
			// �������÷�����Ƶ��õ�main�����׳��쳣�ˣ�
			// Ҳ�ܱ�֤�ѡ���׼������ָ���
			System.setOut(oldOut);
			// System.out.println("finally");
		}
		return new String(bb);
	}
	
	// ����ֱ�ӵ��õķ���
	public  String justRun(String src) {
		try {
			// У��Դ��
			boolean bb = validateSrc(src);
			if(!bb) {
				// �����ߵ����˵��У��ʧ�ܣ�
				throw new RuntimeException("Դ���а�������api��������ִ�У���");
			}
			String fullClassName = getFullClassName(src);
			Class clazz = dynamicCompile(fullClassName, src);
			String result = "";
			if(clazz != null) {
				// �ܽ������˵������ɹ�����
				result = run(clazz);
			}
			return result;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
}
