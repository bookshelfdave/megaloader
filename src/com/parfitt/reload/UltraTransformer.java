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
import java.util.HashMap;
import java.util.Map;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

// Unused... 


public class UltraTransformer implements ClassFileTransformer {	
	private String pkgs[];
	private String[] classpath;
	
	Map<ClassLoader, ClassPool> loaderPools = new HashMap<ClassLoader,ClassPool>();
	static {
		System.out.println("MegaLoader: Transformer initialized.");
	}
	public UltraTransformer(String[] packages, String[] cp) {
		this.pkgs = packages;
		this.classpath = cp;
	}

	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {		
		String dotClass = className.replace('/', '.');
						
		for(String pkg:pkgs) {			
			if(dotClass.startsWith(pkg) && !dotClass.endsWith("_Stub")) {			
				
				try {
					ClassPool cp = null;
					if(!loaderPools.containsKey(loader)) {						
						ClassPool parent = ClassPool.getDefault();
						ClassPool child = new ClassPool(parent);
						child.appendSystemPath();
						child.childFirstLookup = true;
						loaderPools.put(loader, child);
					}
					cp = loaderPools.get(loader);
										
					CtClass klass = cp.makeClass(new ByteArrayInputStream(classfileBuffer));										
					for(String c: classpath) {
						cp.insertClassPath(c);
					}
					
	
					CtMethod[] methods = klass.getMethods();
					if(!klass.isInterface()) {
							System.out.println("MegaTrace: Class trace found -> " + className);							
							for(CtMethod method:methods) {
								if(!method.isEmpty()) {
									//String output = "MegaTrace: " + className + "." + method.getName() + ":" + method.getSignature();
									String methString = new String("[\"+java.lang.Thread.currentThread().getId()+\"]" + className + "." + method.getName());
									StringBuffer output = new StringBuffer();
									try {
										//System.out.println("Adding code to " + method.getName());
											
										CtClass[] paramTypes = method.getParameterTypes();								
										boolean first = true;
										int count = 1;
										for(CtClass param : paramTypes) {
											if(!first) {
												output.append(" + \",\" + ");
											} else {
												output.append(" + ");
											}
											if(param.isPrimitive()) {
												output.append("$" + count);
											} else {
												output.append("$" + count);
											}
											first = false;
											count++;
										}	
										String finalOutput = null;
										if(output.toString().equals("")) {
											finalOutput = "+\"()\"";
										} else {
											finalOutput = output.toString();
										}
										//System.out.println(output.toString());										
										
										String trace = "if(com.parfitt.reload.MegaLoader.traceEnabled) {com.parfitt.reload.MegaLoader.trace(\"->"+methString+":\" "+finalOutput+");}";
										//System.out.println(trace);
										method.insertBefore(trace);
										
										CtClass etype = ClassPool.getDefault().get("java.lang.Throwable");
										method.addCatch("{ com.parfitt.reload.MegaLoader.trace(\"!!" + methString + "\" + $e); throw $e; }", etype);
										//System.out.println("com.parfitt.reload.MegaLoader.trace(\"<-" + methString + ": \" + $_ + \");");
										method.insertAfter("com.parfitt.reload.MegaLoader.trace(\"<-" + methString + ": \" + $_);" ,false);
										
									//} catch (CannotCompileException cce) {
									} catch (Exception e) {										
										if(!e.getMessage().contains("no method body")) {
											System.out.println("Can't add trace code to :" + method.getName() + ":" + e.getMessage());
										}
										//e.printStackTrace();
									}
								}
								//method.insertAfter(src,)
							}					
							byte[] b =  klass.toBytecode();
							klass.detach();
							return b;
					}
				} catch (Exception e) {		
					e.printStackTrace();
				}
			}	
		}
		
		return classfileBuffer;
	}

}
