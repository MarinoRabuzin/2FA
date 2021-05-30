package hr.fer.zavrsni;

import java.util.HashMap;
import java.util.Map;

public class CryptoMap {
	
	public Map<String, String> accountInfoMap = new HashMap<>();
	
	public String mapToString() {
		StringBuilder sb = new StringBuilder();
		for(String key : this.accountInfoMap.keySet())
			sb.append(key + " " + this.accountInfoMap.get(key) + "\n");
		
		return sb.toString();
	}
	
	public static CryptoMap stringToMap(String str){
		if(str.isEmpty())
			return null;
		
		CryptoMap cmap = new CryptoMap();
		String[] tmp = str.split("\n");
		for(String element : tmp) {
			int index = element.indexOf(" ");
			cmap.accountInfoMap.put(element.substring(0, index), element.substring(index + 1));
		}
		
		return cmap;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((accountInfoMap == null) ? 0 : accountInfoMap.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CryptoMap other = (CryptoMap) obj;
		if (accountInfoMap == null) {
			if (other.accountInfoMap != null)
				return false;
		} else if (!accountInfoMap.equals(other.accountInfoMap))
			return false;
		return true;
	}
	
}
