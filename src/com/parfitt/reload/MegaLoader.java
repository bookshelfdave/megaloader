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

import java.io.File;
import java.io.InputStream;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

// Hey there - this isn't really production quality code... I wrote it just to see if I could 
// get it working. And I did. So, curiosity satisfied. There are no unit tests! 
// The quality isn't typical of what I normally write in Java!
// That being said... it does work nicely :-)
// 
public class MegaLoader   {
	public static final String version = "0.9 \"Post-Zilla\"";
	public static boolean running = true; 
	public static Instrumentation sys;	
	public static String[] rootDirNames = null;	
	public static String[] packages = null;
	private static BlockingQueue<File> reloadQueue = new LinkedBlockingQueue<File>(); 
	
	public static Set<ClassLoader> cls = new HashSet<ClassLoader>();
	private static Thread reloader = null;			
	private static List<PathCache> pathCaches = new ArrayList<PathCache>();
	
	static class Reloader implements Runnable {	
		public void run() {
			try {
				System.out.println("MegaLoader: reloading thread starting");
				while(running) {
					File f = MegaLoader.reloadQueue.take();
					String cname = MegaLoader.pathToClassname(f);
					reloadClass(cname,f);
					//cachedClasses.put(cname, new Long(0));					
				}				
				System.out.println("MegaLoader: reloading thread stopping");
			} catch (Exception e) {
				e.printStackTrace();
			}			
		}		
	}
	
	public static void addClassLoader(ClassLoader c) {
		if(!cls.contains(c)) {
			cls.add(c);			
		}
	}
	
	private static String pathToClassname(File f) {		
		String rootDirName = null;
		for(String root: rootDirNames) {
			if(f.getAbsolutePath().startsWith(root)) {
				rootDirName = root;
			}
		}		
		
		//String rootchunks[] = rootDirName.split("\\\\");	
		//String chunks[] = f.getAbsolutePath().split("\\\\");
		String rootchunks[] = rootDirName.split("\\" + File.separator);	
		String chunks[] = f.getAbsolutePath().split("\\" + File.separator);

		int takelast = chunks.length - rootchunks.length-1;		
		StringBuffer fqcn = new StringBuffer();
		boolean dot = false;
		for(int i = (chunks.length - takelast)-1; i < chunks.length; i++) {			
			if(dot) {
				fqcn.append(".");
			}
			dot = true;
			if(chunks[i].endsWith(".class")) {
				fqcn.append(chunks[i].substring(0,chunks[i].length()-6));
			} else {
				fqcn.append(chunks[i]);	
			}			
		}		
		return fqcn.toString();
	}
	
	// find the class in any classloader
	@SuppressWarnings("unchecked")
	public static List<Class> clClass(String cname) {
		List<Class> classes = new ArrayList<Class>();
		try {		
			Class klass = Class.forName(cname);
			//System.out.println("Found class in default");
			classes.add(klass);
		} catch (Exception e) {
			
		}		
		for(ClassLoader loader: cls) {
			try {				
				Class klass = Class.forName(cname, true, loader);
				//System.out.println("Found class in "+ loader.getClass().getName() + " out of " + cls.size() + " classloader");
				classes.add(klass);
			} catch (ClassNotFoundException e) {				
				
			}
		}
		
        return classes;
	}
		
	
	@SuppressWarnings("unchecked")
	public static void reloadClass(String cname, File c) throws Exception {		
		if(cname.contains("$")) {
			System.out.println("Skip reloading of class:" + cname + ":" + c.getAbsolutePath());
			return;
		} else {
			System.out.println("Reloading class:" + cname + " @ " + c.getAbsolutePath());
		}
		byte bytes[] = Utils.getFileBytes(c);												
		try {					    	        
			
			List<Class> klasses = clClass(cname);
			for(Class klass:klasses) {
				ClassDefinition[] d = {new ClassDefinition(klass,bytes)};																	        
				sys.redefineClasses(d);				
			}
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("MegaLoader can't load " + cname);
		}								
	}

	
	static PathCacheListener pcl = new PathCacheListener() {
		public void updates(List<String> updates) {
			MegaLoader.updates(updates);
		}
	};

	public static void checkRequiredProps(Properties props) {
		// this is probably bad form too...
		String[] requiredProps = {"reload.packages","reload.dir","class.loader.suffix"};
		for(String s: requiredProps) {
			if(props.getProperty(s) == null || props.getProperty(s).equals("")) {
				System.err.println("MegaLoader error! Property not specified in reload.properties:" + s);
				// bam, take that!
				// ALSO, probably VERY bad form :-)
				System.exit(-1);
			}
		}
	}
	
	public static void premain(String agentArgs, Instrumentation inst) {
		System.out.println("--------------------------------------------------------------");
		System.out.println("MegaLoader V" + version);
		System.out.println("* Copyright (C) 2011- Dave Parfitt. All Rights Reserved.");
		System.out.println("* Version: MPL 1.1");
		System.out.println("* See License.html for details");
		System.out.println("*  MegaLoader uses the Javassist toolkit:");
		System.out.println("* Javassist, a Java-bytecode translator toolkit.");
		System.out.println("* Copyright (C) 1999- Shigeru Chiba. All Rights Reserved.");
		System.out.println("* Version: MPL 1.1");
		System.out.println("* See License.html for details");			
		System.out.println("");
		System.out.println("MegaLoader starting @ " + new java.util.Date());				
		System.out.println("--------------------------------------------------------------");
				
		sys = inst;
		try {
				
			InputStream fis = MegaLoader.class.getResourceAsStream("/reload.properties");						
			Properties props = new Properties();
			props.load(fis);
			checkRequiredProps(props);
			
			packages = ((String)props.get("reload.packages")).split(",");
			for(int i = 0; i < packages.length; i++) {
				packages[i] = packages[i].trim();
				System.out.println("MegaLoader: Watching package " + packages[i]);				
			}
			
			rootDirNames = ((String)props.get("reload.dir")).split(",");
			if(rootDirNames == null || rootDirNames.length < 1) {
				System.out.println("MegaLoader: No directories to watch");
				return;
			}
					
			
			for(int i = 0; i < rootDirNames.length; i++) {
				rootDirNames[i] = rootDirNames[i].trim();					
				System.out.println("MegaLoader: Watching directory [" + rootDirNames[i] + "]");				
				pathCaches.add(new PathCache(pcl,rootDirNames[i]));				
				
			}
			
			reloader = new Thread(new Reloader());
			reloader.start();			
			String clName = props.getProperty("class.loader.suffix"); 
			if(clName == null || clName.equals("")) {				
				clName = "ClassLoader";
			}
			inst.addTransformer(new MegaTransformer(clName));																				
			for(PathCache pc: pathCaches) {
				pc.setRunning(true);
				pc.start();
			}

		} catch (Exception e1) {		
			e1.printStackTrace();
		}
		
		Runtime.getRuntime().addShutdownHook(new Thread() {
	          public void run() {
	        	System.out.println("Shutting down MegaLoader");
	        	try {
	    			for(PathCache pc: pathCaches) {
	    				pc.setRunning(false);
	    			}
					running = false;
				} catch (Exception e) {
					e.printStackTrace();
				}        	
	          }
	        });	    
	}
		
		public static void updates(List<String> updates) {
			for(String filename:updates) {
				File f = new File(filename);
				String cname = pathToClassname(f);			
				
				if(!f.getName().endsWith(".class")) {
					return;
				}		
				boolean validPackage = false;
				for(String pkg: packages) {
					if(cname.startsWith(pkg)){ 
						validPackage = true;
					} 
				}
				if(!validPackage) {
					return;
				}
				
				try {			
					reloadQueue.put(f);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
						
		
	
}