package com.gao.dynamiccompile;

import java.io.IOException;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

// ���ڷ�װҪ���ж�̬������ַ���Դ��
class CharSequenceJavaFileObject extends SimpleJavaFileObject {

	// ��װ�ַ������͵�Դ�뵽content�ֶ���
	private String content;
	
	protected CharSequenceJavaFileObject(String className, String content) {
		super(URI.create(className + Kind.SOURCE.extension), Kind.SOURCE);
		this.content = content;
	}
	// ��¶������ӿڣ��Ա��ñ�������ͨ������ӿ�����ȡ���ַ������͵�Դ�룡
	@Override
	public CharSequence getCharContent(boolean ignoreEncodingErrors) throws IOException {
		return content;
	}
}