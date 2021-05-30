package hr.fer.zavrsni;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;

public class SmsSender {
	
	public static final String ACCOUNT_SID = "ACefea66c282918f0fd34e0a228f24b598";
	public static final String AUTH_TOKEN = "b2e62a3859ad2ae58f54279ee9d7ef38";

	public static void main(String[] args) {
		Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
		Message message = Message.creator(
				new com.twilio.type.PhoneNumber("+385991919193"),
				new com.twilio.type.PhoneNumber("+16783943499"),
				"This is the ship that made the Kessel Run in fourteen parsecs?")
				.create();

		System.out.println(message.getSid());
	}

}
