package utils;

import java.util.LinkedList;

public class Label {
	String type = "";
	LinkedList<String> chars = new LinkedList<String>();
	
	public Label (String type) {
		this.type = type;
	}
	
	public void addChar (String c) {
		this.chars.add(c);
	}
	
	public LinkedList<String> getChars() {
		return this.chars;
	}
	
	public String getType() {
		return this.type;
	}
	
	public Label copy() {
		Label output = new Label(this.getType());
		
		for (String c : this.getChars()) {
			output.addChar(c);
		}
		
		return output;
	}
	
	public String toString() {
		String output = "";
		for (String c : chars) {
			output = output + " " + c;
		}
		
		return output;
	}
}
