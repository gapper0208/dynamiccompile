package com.gao.dynamiccompile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

// 该类的对象作为ClassFileManager的字段，ClassFileManager是用来封装动态编译出的字节码的
// 其实是封装在ClassFileManager的这个JavaClassObject类型的字段中
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
