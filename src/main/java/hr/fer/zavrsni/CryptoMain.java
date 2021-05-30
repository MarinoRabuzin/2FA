package hr.fer.zavrsni;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Scanner;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;


public class CryptoMain {
	
	private static int numberOfIterations = 100000;
	//lenght must be 256 bytes because AES works with 128 or 256 bytes
	private static int lenghtOfDerivatedKey = 256;
	private static String passwordDerivationAlgorithm = "PBKDF2WithHmacSHA256";
	private static String HMACAlgorithm = "HmacSHA256";
	private static Path filePath = Paths.get("./Password_Manager.txt");
	//DODATI U SISTEMSKE VARIJABLE ACCOUNT_SID I AUTH_TOKEN te ih izvalciti s System.getenv
	private static String ACCOUNT_SID = "ACefea66c282918f0fd34e0a228f24b598";
	public static final String AUTH_TOKEN = "b2e62a3859ad2ae58f54279ee9d7ef38";
	
	public static void initCommand(Util u) {		
		try {
			if(Files.exists(filePath))
				Files.delete(filePath);
			
			byte[] IV = getSecuredRandomBytes(16);
			byte[] salt = getSecuredRandomBytes(1024);
			byte[] masterPassword = getKeyFromPassword(u.getMasterPass().toCharArray(), salt, numberOfIterations, CryptoMain.lenghtOfDerivatedKey);

			SecretKeySpec keySpec = new SecretKeySpec(masterPassword, "AES");
			//IV must be 16 bytes -> cipher.init expects 16 bytes IV
			AlgorithmParameterSpec paramSpec = new IvParameterSpec(IV);
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);

			CryptoMap cryptoMap = new CryptoMap();
			cryptoMap.accountInfoMap.put("test", "test");
			
			byte[] HMac = getHMac(masterPassword, cryptoMap.mapToString().getBytes());

			Scanner sc = new Scanner(System.in);

			String securedNumber = getNumbersFromString();
			
			System.out.print("Enter your phone number (with country calling code): ");
			String phoneNumber = sc.next();
			
			Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
			Message message = Message.creator(
					new com.twilio.type.PhoneNumber(phoneNumber),
					new com.twilio.type.PhoneNumber("+16783943499"),
					"8-digit code for authentication is: " + securedNumber)
					.create();
			
			System.out.println("Enter received 8-digit code to console");
			System.out.print("> ");
			
			Boolean authenticated = false; 
			for(int i = 0; i < 3; i++) {
				if(sc.next().equals(securedNumber)) {
					authenticated = true;
					break;
				}
				System.out.println("Typed 8-digit code is wrong, try again!");
				if(i != 2)
					System.out.print("> ");
			}
			
			if(authenticated) {
				OutputStream os = Files.newOutputStream(filePath);	
				CipherOutputStream cos = new CipherOutputStream(os, cipher);
				
				os.write(salt);
				os.write(IV);
				os.write(HMac);
				cos.write(cryptoMap.mapToString().getBytes());
				
				System.out.println("\nAuthentication passed!");
				System.out.println("Password manager initalized.");
				
				cos.close();
				os.close();			
			}else
				System.out.println("\nAuthentication failed!");
		} catch (Exception e) {
			System.out.println("Wrong master password is used or integrity of the file is violated!");
		}		
	}
	
	public static void putCommand(Util u) {		
		try {
			InputStream is = Files.newInputStream(filePath);
			
			byte[] salt = is.readNBytes(1024);
			byte[] IV = is.readNBytes(16);
			byte[] expectedHMAC = is.readNBytes(32);
			byte[] masterPassword = getKeyFromPassword(u.getMasterPass().toCharArray(), salt, numberOfIterations, CryptoMain.lenghtOfDerivatedKey);

			SecretKeySpec keySpec = new SecretKeySpec(masterPassword, "AES");
			//IV must be 16 bytes -> cipher.init expects 16 bytes IV
			AlgorithmParameterSpec paramSpec = new IvParameterSpec(IV);
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, keySpec, paramSpec);
			
			Cipher decode = Cipher.getInstance("AES/CBC/PKCS5Padding");
			decode.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
			
			CipherInputStream cis = new CipherInputStream(is, decode);
			
			CryptoMap cryptoMap = CryptoMap.stringToMap(new String(cis.readAllBytes()));
			
			if(cryptoMap == null)
				cryptoMap = new CryptoMap();
			
			byte[] actualHMAC = getHMac(masterPassword, cryptoMap.mapToString().getBytes());
			
			if(!Arrays.equals(actualHMAC, expectedHMAC)) {
					cis.close();
					is.close();
					throw new Exception();
			}

			cryptoMap.accountInfoMap.put(u.getAccount(), u.getPassword());			
			
			actualHMAC = getHMac(masterPassword, cryptoMap.mapToString().getBytes());
			
			OutputStream os = Files.newOutputStream(filePath);
			CipherOutputStream cos = new CipherOutputStream(os, cipher);
			
			os.write(salt);
			os.write(IV);
			os.write(actualHMAC);
			cos.write(cryptoMap.mapToString().getBytes());
			
			cis.close();
			is.close();
			cos.close();
			os.close();
			System.out.println("Stored password for " + u.getAccount() + ".");
		}catch (Exception  e) {
			System.out.println("Wrong master password is used or integrity of the file is violated!");
		}
	}
	
	public static void getCommand(Util u) {		
		try {	
			InputStream is = Files.newInputStream(filePath);
			
			byte[] salt = is.readNBytes(1024);
			byte[] IV = is.readNBytes(16);
			byte[] actualHMac = is.readNBytes(32);
			byte[] masterPassword = getKeyFromPassword(u.getMasterPass().toCharArray(), salt, numberOfIterations, CryptoMain.lenghtOfDerivatedKey);

			SecretKeySpec keySpec = new SecretKeySpec(masterPassword, "AES");
			//IV must be 16 bytes -> cipher.init expects 16 bytes IV
			AlgorithmParameterSpec paramSpec = new IvParameterSpec(IV);
			
			Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
			
			CipherInputStream cis = new CipherInputStream(is, cipher);
			
			CryptoMap cryptoMap = CryptoMap.stringToMap(new String(cis.readAllBytes()));						
			byte[] expectedHMac = getHMac(masterPassword, cryptoMap.mapToString().getBytes());
			
			if(!Arrays.equals(actualHMac, expectedHMac)) {
				cis.close();
				is.close();
				throw new Exception();
			}
			
			if(cryptoMap.accountInfoMap.containsKey(u.getAccount()))
				System.out.println("Password for " + u.getAccount() + " is: " + cryptoMap.accountInfoMap.get(u.getAccount()));
			else
				System.out.println("Account does not exist!");
			
			cis.close();
			is.close();
		} catch (Exception e) {
			System.out.println("Wrong master password is used or integrity of the file is violated!");
		}	
			
	}
	
	private static String getNumbersFromString() {
		Base64.Encoder base64Encoder = Base64.getUrlEncoder();
		String secureString = base64Encoder.encodeToString(getSecuredRandomBytes(1024));
		StringBuilder sb = new StringBuilder();
		
		int count = 0;
		for(char c : secureString.toCharArray()) {
			if(Character.isDigit(c)) {
				sb.append(c);
				count ++;
			}
			if(count == 8)
				break;
		}
		
		return sb.toString();
	}
	
	private static byte[] getHMac(byte[] key, byte[] mapToCalculatedHmac) {
		try {
			Mac mac = Mac.getInstance(CryptoMain.HMACAlgorithm);
			SecretKeySpec secretKeySpec = new SecretKeySpec(key, CryptoMain.HMACAlgorithm);
			mac.init(secretKeySpec);
			return mac.doFinal(mapToCalculatedHmac);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static byte[] getSecuredRandomBytes(int numOfBytes) {
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[numOfBytes];
		random.nextBytes(bytes);
		
		return bytes;
	}
	
	private static byte[] getKeyFromPassword(char[] password, byte[] salt ,int numberOfIterations ,int lengthOfDerivatedKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeySpec spec = new PBEKeySpec(password, salt, numberOfIterations, lengthOfDerivatedKey);
		SecretKeyFactory factory = SecretKeyFactory.getInstance(CryptoMain.passwordDerivationAlgorithm);
		
		//key from masterPassword
		return factory.generateSecret(spec).getEncoded();
	}
		
	public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException {
		try {
			Util util = new Util(args);
			util.getCommands().get(util.getCommand()).accept(util);
		}catch(IllegalArgumentException exc) {
			System.out.println("Error: " + exc.getMessage());
			return;
		}		
	}
}
