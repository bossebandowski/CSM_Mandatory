package utils;

public class Node {
	public int number;
	
	public Node (int number) {
		this.number = number;
	}
	
	public String toString() {
		if (this.number == -1) {
			return "q▷";
		}
		else if (this.number == -2) {
			return "q◀";
		} else {
			return "q" + String.valueOf(this.number);
		}
	}
}