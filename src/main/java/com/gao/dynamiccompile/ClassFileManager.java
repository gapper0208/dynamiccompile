package com.gao.dynamiccompile;

import java.io.IOException;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

// 用于封装进行了动态编译之后所得到的字节码，也就是把字节码存入内存中！
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
