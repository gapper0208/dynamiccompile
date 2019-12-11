package com.gao.dynamiccompile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

// ����Ķ�����ΪClassFileManager���ֶΣ�ClassFileManager��������װ��̬��������ֽ����
// ��ʵ�Ƿ�װ��ClassFileManager�����JavaClassObject���͵��ֶ���
class JavaClassObject extends SimpleJavaFileObject {
	// ����һ�������������װ��JavaCompiler������class�ļ�
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
