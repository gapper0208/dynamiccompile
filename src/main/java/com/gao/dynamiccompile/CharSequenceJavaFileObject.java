package com.gao.dynamiccompile;

import java.io.IOException;
import java.net.URI;

import javax.tools.SimpleJavaFileObject;

// 用于封装要进行动态编译的字符串源码
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