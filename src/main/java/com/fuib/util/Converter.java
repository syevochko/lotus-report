package com.fuib.util;

public class Converter {
    static final String HEXES = "0123456789abcdef";
    
    public static String toHex( byte [] raw ) {
    	if ( raw == null )	return null;

    	StringBuilder hex = new StringBuilder( 2 * raw.length );

    	for (int i = 0; i < raw.length; i++) {
    		int b = raw[i] & 0xff; 
    		
    		hex.append(HEXES.charAt(b >> 4));
    		hex.append(HEXES.charAt(b & 0x0F));
    	}

    	return hex.toString();
    }
    
    
    public static byte[] fromHex(String sBuf) {     
    	int len = sBuf.length();     
    	byte[] data = new byte[len/2]; 
    	
    	for (int i = 0; i < len; i += 2) {
    		data[i/2] = (byte) ((Character.digit(sBuf.charAt(i), 16) << 4) + Character.digit(sBuf.charAt(i+1), 16));     
    	}     
    	
    	return data; 
    } 
}