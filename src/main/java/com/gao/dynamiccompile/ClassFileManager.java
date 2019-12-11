package com.gao.dynamiccompile;

import java.io.IOException;

import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.JavaFileObject.Kind;

// ���ڷ�װ�����˶�̬����֮�����õ����ֽ��룬Ҳ���ǰ��ֽ�������ڴ��У�
class ClassFileManager extends ForwardingJavaFileManager {
	
	// ��װ����õ��ֽ��뵽jclassObject�ֶ���
	private JavaClassObject jclassObject;
	
	public ClassFileManager(JavaFileManager fileManager) {
		super(fileManager);
	}
	
	// ��¶������ӿڣ��Ա��ñ�������ͨ������ӿ�����ȡ��һ������
	// �����������þ���Ϊ�˷�װ������������õ��ֽ���
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
