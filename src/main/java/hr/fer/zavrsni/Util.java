package hr.fer.zavrsni;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Util {
	
	private final Map<String, Consumer<Util>> commands = fillCommands();
	private final Map<String, String> exceptionOutput = new HashMap<>();
	private String command;
	private String masterPass;
	private String account;
	private String password;
	
	public Util(String[] args) {
		if(exceptionOutput.isEmpty())
			fillExceptionOutput();
		
		testNumberOfElements(args);
		
		parseInput(args);
	}
	
	private void parseInput(String[] args) {
		switch(args.length) {
			case 4 : this.password = args[3];
			case 3 : this.account = args[2];
			case 2 : this.masterPass = args[1];
					 this.command = args[0];
		}
	}
	
	private void testNumberOfElements(String[] args) {
		if(args.length == 0)
			throw new IllegalArgumentException("Arguments needed, there are no given arguments!");
			
		else if(args[0].equals("init") && args.length != 2)
			throw new IllegalArgumentException("Arguments don't match expected input! \n" + this.exceptionOutput.get(args[0]));
		
		else if(args[0].equals("put") && args.length != 4)
			throw new IllegalArgumentException("Arguments don't match expected input! \n" + this.exceptionOutput.get(args[0]));
		
		else if(args[0].equals("get") && args.length != 3)
			throw new IllegalArgumentException("Arguments don't match expected input! \n" + this.exceptionOutput.get(args[0]));
		
		validCommand(args[0]);
	}
		
	private void validCommand(String action) {
		if(!commands.containsKey(action))
			throw new IllegalArgumentException("Given command is not implemented!");
	}
	
	private void fillExceptionOutput() {
		this.exceptionOutput.put("init", "Expected -> init \"masterPassword\"");
		this.exceptionOutput.put("put", "Expected -> put \"masterPassword\" \"accountName\" \"accountPassword\"");
		this.exceptionOutput.put("get", "Expected -> get \"masterPassword\" \"accountName\"");
	}
	
	private HashMap<String, Consumer<Util>> fillCommands() {
		HashMap<String, Consumer<Util>> commands = new HashMap<>(); 
		
		commands.put("init", (u)-> CryptoMain.initCommand(u) );
		commands.put("put", (u) -> CryptoMain.putCommand(u) );
		commands.put("get", (u) -> CryptoMain.getCommand(u) );
		
		return commands;
	}

	public Map<String, Consumer<Util>> getCommands() {
		return commands;
	}

	public String getCommand() {
		return command;
	}

	public String getMasterPass() {
		return masterPass;
	}

	public String getAccount() {
		return account;
	}

	public String getPassword() {
		return password;
	}
	
}
