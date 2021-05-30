package hr.fer.zavrsni;

import java.security.SecureRandom;
import java.util.Base64;

public class Tmp {

	public static void main(String[] args) {
		
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[1024];
		random.nextBytes(bytes);

		Base64.Encoder base64Encoder = Base64.getUrlEncoder();
		String output = getNumbersFromString(base64Encoder.encodeToString(bytes));
		
		System.out.println(output);
	}
	
	private static String getNumbersFromString(String secureString) {
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
	
}
