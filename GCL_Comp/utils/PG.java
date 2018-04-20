package utils;

import java.io.File;
import java.io.PrintWriter;
import java.util.LinkedList;

public class PG {

	public LinkedList<Integer> index = new LinkedList<Integer>();
	public LinkedList<Node> doNodeStash = new LinkedList<Node>();
	public LinkedList<Node> ifNodeStash = new LinkedList<Node>();
	public LinkedList<Node> nodeStash = new LinkedList<Node>();
	public LinkedList<Edge> edgeStash = new LinkedList <Edge>();
	public String output = "digraph program_graph {rankdir=LR;\n"
			+ "node [shape = circle]; q▷;\n"
			+ "node [shape = doublecircle]; q◀;\n"
			+ "node [shape = circle]\n";

	public int nodeCount = 0;
	
	public void createBody () {
		for (Edge e : this.edgeStash) {
			this.output = this.output + e.toString();
		}
		
		this.output = this.output + "}";
	}

	
	public void createFile () {

		File f = new File("PG.gv");
		this.createBody();
        
        try (PrintWriter out = new PrintWriter(f)) {
            out.println(this.output);
	        f.createNewFile();
        }
        catch (Exception e) {
        	System.out.println("Couldn't write to file");
        }
	}		
}
