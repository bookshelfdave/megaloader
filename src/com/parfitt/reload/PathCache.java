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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PathCache extends Thread {

	private File dir;
	private boolean running = false;
	private long sleepTimeMillis = 500;
	private Map<String,Long> cache =  new HashMap<String,Long>();
	private PathCacheListener listener;
	
	public PathCache(PathCacheListener l, String path) {
		this.dir = new File(path);
		this.listener = l;
	}
		
	public void run() {
		initialize();
		running = true;
		System.out.println("PathCache: " + dir.getAbsolutePath());
		while(running) {
			try {
				Thread.sleep(sleepTimeMillis);				
				List<String> updates = update();
				listener.updates(updates);			
			} catch (InterruptedException e) {
				e.printStackTrace();
			}			
		}
		System.out.println("Path cache complete.");
	}
	
	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running;
	}

	public void initialize() {
		List<String> allFiles = new ArrayList<String>();
		walk(dir,allFiles);
//		for(String f: allFiles) {
//			System.out.println("+ " + f);
//		}
	}
	public List<String> update() {
		List<String> allFiles = new ArrayList<String>();
		walk(dir,allFiles);
//		for(String f: allFiles) {
//			System.out.println(">" + f);
//		}
		return allFiles;
	}
		
	public void walk(File root, List<String> changes) {
        String suffix = ".class"; 
        
        File files[] = root.listFiles();
        if(files == null) 
        	return;
        
        for(int i=0; i<files.length; i++) {
            if(files[i].isDirectory()) {
            	walk(files[i],changes);
            } else {
            	
            	String name = files[i].getAbsolutePath(); 
            	if(name.endsWith(suffix)) {
                	if(cache.containsKey(name)) {                		
                		long lastModified = files[i].lastModified();
                		if(cache.get(name) < lastModified) {
                			changes.add(name);
                			cache.put(name, lastModified);
                		}
                	} else {
                		long lastModified = files[i].lastModified();
                		cache.put(name, lastModified);
                		changes.add(name);
                	}
                }
            }
        }        
    }
	 
	
}
