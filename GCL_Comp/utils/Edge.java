package utils;

public class Edge {
	public Node from;
	public Node to;
	public Label label;
	
	public Edge (Node from, Node to, Label label) {
		this.from = from;
		this.to = to;
		this.label = label;
	}
	
	public String toString() {
		return from.toString() + " -> " + to.toString() + " [label = \"" + this.label.label + "\"];\n";
	}
}
