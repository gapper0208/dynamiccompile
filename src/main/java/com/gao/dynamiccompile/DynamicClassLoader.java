package com.gao.dynamiccompile;

import java.net.URL;
import java.net.URLClassLoader;

//用于加载内存中的字节码的类加载器：动态类加载器
class DynamicClassLoader extends URLClassLoader {
	public DynamicClassLoader(ClassLoader parent) {
		super(new URL[0], parent);
	}

	public Class loadClass(String fullName, JavaClassObject jco) {
		byte[] classData = jco.getBytes();
		return this.defineClass(fullName, classData, 0, classData.length);
	}
}