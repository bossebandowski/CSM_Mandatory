package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import javax.script.ScriptEngineManager;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

public class Memory {
	Map<String, Integer> sigma = new HashMap<String, Integer>();
	Set<String> ops = new HashSet<String>(Arrays.asList("true", "false","skip","+","-","^","*","!","(",")","!=","=","<","<=",">",">=","&","&&","|","||"));
	
	public String toString() {
		String output = "";
		
		for (String var : sigma.keySet()) {
			output = output + " " + var + " " + String.valueOf(sigma.get(var));
		}
		
		return output;
	}
	
	public boolean contains(String key) {
		return sigma.get(key) != null;
	}
	
	public int getVarVal(String key) {
		return sigma.get(key);
	}
	
	public void setVarVal(String key, int val) {
		sigma.put(key, val);
	}
	
	public boolean isInt (String i) {
		try {
			Integer.valueOf(i);
			return true;
		} catch (Exception e) {
			return false;
		}
	}
	
	public boolean isOp (String op) {
		return this.ops.contains(op);
	}
	
	public void defUnknownVars (Label label) {
		for (String c : label.getChars()) {
			if (!(isInt(c) | isOp(c)) | this.contains(c)) {
				System.out.println(this.contains(c));
				this.setVarVal(c, 0);
			}
		}
	}

	
	public boolean checkBool (Label label) {
		
		if (label.getChars().peek().equals("true") || (label.getChars().peek().equals("skip"))) {
			return true;
		} else if (label.getChars().peek().equals("false")) {
			return false;
		} else {
		
			LinkedList<String> charCopy = label.copy().getChars();
			
			this.defUnknownVars(label);
			int lhs = this.getVarVal(charCopy.pop());
			String op = charCopy.pop();
			int rhs = Integer.valueOf(charCopy.pop());
			
			switch (op) {
				case "=" :	return lhs == rhs;
				case "<" :	return lhs < rhs;
				case "<=":	return lhs <= rhs;
				case "!=": 	return lhs != rhs;
				case ">=":	return lhs >= rhs;
				case ">" :	return lhs > rhs;
				default  :	return true;
			}
		}
	}
		
	public void convertToInts (LinkedList<String> chars) {
		for (String c : chars) {
			if (!(isInt(c) || isOp(c))) {
				String newc = String.valueOf(this.getVarVal(c));
				int index = chars.indexOf(c);
				chars.set(index, newc);
			}
		}
	}
	
	public void updateMem (Label label) throws ScriptException {
		Label labelCopy = label.copy();
		String key = labelCopy.getChars().pop();
		labelCopy.getChars().pop();
		this.defUnknownVars(labelCopy);
		convertToInts(labelCopy.getChars());
		String rhs = labelCopy.toString();
		ScriptEngineManager mgr = new ScriptEngineManager();
	    ScriptEngine engine = mgr.getEngineByName("JavaScript");
		this.setVarVal(key, (int) engine.eval(rhs));
	}

}
