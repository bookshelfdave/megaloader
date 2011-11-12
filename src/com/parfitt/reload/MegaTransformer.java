/* ***** BEGIN LICENSE BLOCK *****
 * Version: MPL 1.1
 * Copyright (C) 2011- Dave Parfitt. All Rights Reserved.
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * ***** END LICENSE BLOCK ***** */
package com.parfitt.reload;

import java.io.ByteArrayInputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;

public class MegaTransformer implements ClassFileTransformer {
	private String classLoaderName;
	static {
		System.out.println("MegaLoader: Class loader transformer initialized.");
	}
	public MegaTransformer(String classLoaderName) {
		this.classLoaderName = classLoaderName;
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {

		// yes, this should definately be a regex...
		if(className.endsWith(classLoaderName)) {			
			try {
				ClassPool cp = ClassPool.getDefault();				
				CtClass klass = cp.makeClass(new ByteArrayInputStream(classfileBuffer));
				CtConstructor cts[] = klass.getConstructors();
				for(CtConstructor c:cts) {
					c.insertBeforeBody("com.parfitt.reload.MegaLoader.addClassLoader(this);");
				}
				
				byte[] b =  klass.toBytecode();
				klass.detach();
				return b;
			} catch (Exception e) {		
				e.printStackTrace();
			}
		}
		return classfileBuffer;
	}
	
}
