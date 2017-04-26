package com.fuib.util;

import sun.misc.BASE64Encoder;
import sun.misc.BASE64Decoder;
import java.io.*;

/**
 * This is a class that can base64 encode or base64 decode
 * a given file or decode base64 string. 
 * File will do the encoding/decoding "in place", such
 * that the file will be overwritten.  
*/
public class Base64Java {
	/** 
	 * @param sBase64String - base64 encoded string
	 * @return - decoded byte array
	 * @throws IOException
	 */
	public static byte[] decodeString(String sBase64String) throws IOException {
		BASE64Decoder decoder = new BASE64Decoder();
		return decoder.decodeBuffer(sBase64String);
	}
	
	/** 
	 * @param sBase64String  - base64 encoded string
	 * @param sFileName - name of file to write decoded byte array. File will create in temp directory
	 * @return	full path to created file 
	 * @throws IOException
	 */
	public static String decodeStringToFile(String sBase64String, String sFileName) throws IOException {		
		String sFName = (sFileName.lastIndexOf(".")==-1)?sFileName:sFileName.substring(0, sFileName.lastIndexOf("."));
		String sFExt = (sFileName.lastIndexOf(".")==-1)? "" : sFileName.substring(sFileName.lastIndexOf("."), sFileName.length());		

		File tempFile = File.createTempFile(sFName, sFExt);
		FileOutputStream out = new FileOutputStream(tempFile);
		
		out.write(decodeString(sBase64String));
		out.close();
		
		return tempFile.getPath();
	}

	public static void decodeFile (String filePathAndName) throws IOException {
		BufferedInputStream in = new BufferedInputStream( 
				new FileInputStream(filePathAndName) );
		
		File tempFile = File.createTempFile("Base64Temp", ".b64");
		FileOutputStream out = new FileOutputStream(tempFile);
		
		BASE64Decoder decoder = new BASE64Decoder();
		decoder.decodeBuffer(in, out);
		in.close();
		out.close();
		
		File newFile = new File(filePathAndName);
		newFile.delete();
		tempFile.renameTo(newFile);
	}
	
	
	public static void encodeFile (String filePathAndName) throws IOException {
		BufferedInputStream in = new BufferedInputStream( 
				new FileInputStream(filePathAndName) );
		
		File tempFile = File.createTempFile("Base64Temp", ".b64");
		tempFile.deleteOnExit();
		FileOutputStream out = new FileOutputStream(tempFile);
		
		BASE64Encoder encoder = new BASE64Encoder();
		encoder.encodeBuffer(in, out);
		in.close();
		out.close();
		
		File newFile = new File(filePathAndName);
		newFile.delete();
		tempFile.renameTo(newFile);
	}	
}
