package com.fuib.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import com.fuib.util.Converter;

/**
 * @author gorobets
 * @param  
 */
public class SecureData {
	public static String DEFAULT_ALGHORITHM = "DES";
	public static int DEFAULT_KEY_LENGTH = 56;
	
	protected SecretKeySpec m_oKeySpec = null;
	protected Cipher m_cipher = null;
	
	public SecureData(byte[] arrKey) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException {
		byte[] aKey = arrKey;
 
		if ( aKey == null ) {
			// Get the KeyGenerator
			KeyGenerator kgen = KeyGenerator.getInstance(DEFAULT_ALGHORITHM);
	        kgen.init(DEFAULT_KEY_LENGTH); 

	        // Generate the secret key specs.
	        SecretKey skey = kgen.generateKey();
	        aKey = skey.getEncoded();
		}
		
		m_oKeySpec = new SecretKeySpec(aKey, DEFAULT_ALGHORITHM);
		m_cipher = Cipher.getInstance(DEFAULT_ALGHORITHM);
	}
	
	
	public static SecureData getInstance(String sKey) throws NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
		byte[] aKey = null;
		
		if ( sKey != null )
			aKey = sKey.getBytes();
		
		return new SecureData(aKey);
	}
	
	
	public byte[] encrypt(String sData) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		m_cipher.init(Cipher.ENCRYPT_MODE, m_oKeySpec);
		
		return m_cipher.doFinal(sData.getBytes());		
	}
	
	
	public String encryptAsHex(String sData) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		return Converter.toHex(encrypt(sData));		
	}
		
	
	public String decrypt(byte[] data) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		m_cipher.init(Cipher.DECRYPT_MODE, m_oKeySpec);
		
		return new String(m_cipher.doFinal(data));		
	}
	
	
	public String decryptFromHex(String sHexData) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
		return decrypt(Converter.fromHex(sHexData));		
	}
	
	
	public static String encryptAsHex(String sData, String sKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {	
		return SecureData.getInstance(sKey).encryptAsHex(sData);		
	}
	
	
	public static String decryptFromHex(String sHexData, String sKey) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchPaddingException {	
		return SecureData.getInstance(sKey).decryptFromHex(sHexData);		
	}
	
}
