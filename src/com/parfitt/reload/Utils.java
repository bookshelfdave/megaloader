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
import java.io.FileInputStream;


public class Utils {
	public static byte[] getFileBytes(File f) throws Exception {
	    FileInputStream fis = new FileInputStream(f);
	    // unsafe cast to int :-(
	    byte[] bytes = new byte[(int)f.length()];

	    int offset = 0;
	    int bytesRead = 0;
	    while (offset < bytes.length && 
	    		(bytesRead = fis.read(bytes, offset, bytes.length-offset)) >= 0) {
	        offset += bytesRead;
	    }

	    if (offset < bytes.length) {
	        throw new Exception("Error reading file "+f.getName());
	    }
	    
	    fis.close();
	    return bytes;
	}	
}
