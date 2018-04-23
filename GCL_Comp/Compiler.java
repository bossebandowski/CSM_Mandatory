/* Authors:
 * 	Sergiu Ojog, s164587
 * 	Nicholas Rose, s164580
 * 	Bosse Bandowski, s164582
 * 
 * This program is based on the provided Calculator example as it is very similar in functionality.
 * It reads the input from the console and tries to parse it. To do so, it recursively iterates over the tree which is defined by the input and the accompanying .g file.
 * In order to implement the error handler as desired by the task description, it is needed to override the standard inbuilt error handler of ANTLR. This requires a few imports:
 */

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.misc.ParseCancellationException;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.InputMismatchException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.DefaultErrorStrategy;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.AbstractParseTreeVisitor;

import utils.Edge;
import utils.Node;
import utils.PG;
import utils.Status;
import utils.Label;
import utils.Memory;

import java.io.File;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import javax.script.ScriptException;

public class Compiler {
	
	public PG aPG = new PG();
	public Memory aMemory = new Memory();
	public Status aStatus = new Status();

	public static void main(String args[]) throws Exception  {

		try {
			// Instantiate a new class member
			Compiler c = new Compiler();
			c.run();
			System.out.println("ok");
		}
		catch (Exception  e) {
			System.out.println("ko");
			e.printStackTrace ();
		}
	}

	public void run() throws Exception  {

		Parse parse = new Parse();
		ProgramGraph graph = new ProgramGraph();
		
		// Allow the user to pick a functionality
		
		Set<String> allowedInput = new HashSet<>(Arrays.asList("p", "c", "i", "a", "e"));
		String choice;
		
		while (true) {
			System.out.print("Choose a functionality: Parser [p] Compiler [c] Interpreter [i] Analyzer [a] Exit [e]: ");
			choice = System.console().readLine();
			
			if (!allowedInput.contains(choice)) {
				System.out.println("invalid input");
			} else {
				if (choice.equals("e")) {System.exit(0); }
				break;
			}
		}
		
		// Read an expression from the console
		System.out.print("Enter your code: ");
		String input = System.console().readLine();

		// Build the parser for the content of the input in several steps

		// Translate the input string into stream of characters
		CharStream inputStream = CharStreams.fromString(input);

		// Create a lexer for the CharStream
		CompilerLexer lex = new CompilerLexer(inputStream);

		// Use the lexer to generate the token stream
		CommonTokenStream tokens = new CommonTokenStream(lex);

		// Create a parser for the given token stream
		CompilerParser parser = new CompilerParser(tokens);
		
		// Remove the inbuilt ANTLR error handler
		lex.removeErrorListeners();
		parser.removeErrorListeners();
		parser.setErrorHandler(new BailErrorStrategy());

		switch (choice) {
			case "p": {	System.out.println("Parsing...");
						parse.visit(parser.start());
						break; }
			case "c": {	System.out.println("Creating program graph...");
						graph.visit(parser.start());
						aPG.createFile();
						System.out.println("\n----------------------\n");
						System.out.println(aPG.output);
						System.out.println("\n----------------------\n");
						break; }
			case "i": {	System.out.println("Interpreting...");
						graph.visit(parser.start());
						interpret(aMemory, aPG.edgeStash);
						break; }
			case "a": { System.out.println("not yet implemented");
						break; }
			default : { System.out.println("input not recognized");
						break; }
		}
	}
	
	public class ProgramGraph extends CompilerBaseVisitor<String> {

		public String visitStart(CompilerParser.StartContext ctx) {
			aPG.nodeStash.add(new Node(-1));
			aPG.nodeStash.add(new Node(-2));
			
			return visitChildren(ctx);}
		public String visitVarDef(CompilerParser.VarDefContext ctx) {
			Label varDefLabel = new Label("vardef");
			aPG.edgeStash.add(new Edge(aPG.nodeStash.pop(), aPG.nodeStash.peek(), new Label("vardef")));
			aPG.edgeStash.getLast().label.addChar(String.valueOf(ctx.lhs.getText()));
			aPG.edgeStash.getLast().label.addChar(":=");
			visit(ctx.rhs);
			
			return ""; }
		public String visitAppend(CompilerParser.AppendContext ctx) {
			aPG.nodeStash.add(aPG.nodeStash.size() - 1, new Node(aPG.nodeCount));
			aPG.nodeCount++;
			
			return visitChildren(ctx); }
		public String visitDoLoop(CompilerParser.DoLoopContext ctx) {
			aPG.ifNodeStash.push(aPG.nodeStash.pop());
			aPG.doNodeStash.push(aPG.ifNodeStash.peek());
			aPG.index.push(aPG.edgeStash.size());
			visitChildren(ctx);
			aPG.edgeStash.getLast().to = aPG.doNodeStash.peek();
			
			aPG.edgeStash.add(new Edge(aPG.doNodeStash.pop(), aPG.nodeStash.peek(), new Label("bool")));
			aPG.edgeStash.getLast().label.addChar("!");
			aPG.edgeStash.getLast().label.addChar("(");
			
			for (String c : aPG.edgeStash.get(aPG.index.pop()).label.getChars()) {
				aPG.edgeStash.getLast().label.addChar(c);
			}
			
			aPG.edgeStash.getLast().label.addChar(")");
			
			return ""; }
		public String visitSkip(CompilerParser.SkipContext ctx) {
			aPG.edgeStash.add(new Edge(aPG.nodeStash.pop(), aPG.nodeStash.peek(), new Label("bool")));
			aPG.edgeStash.getLast().label.addChar("skip");
			
			return visitChildren(ctx); }
		public String visitIf(CompilerParser.IfContext ctx) {
			aPG.ifNodeStash.push(aPG.nodeStash.pop());
			
			return visitChildren(ctx); }
		public String visitIfElif(CompilerParser.IfElifContext ctx) {
			aPG.ifNodeStash.push(aPG.ifNodeStash.peek());
			
			return visitChildren(ctx); }
		public String visitIfThen(CompilerParser.IfThenContext ctx) {
			aPG.nodeStash.push(new Node(aPG.nodeCount));
			aPG.nodeCount++;
			aPG.edgeStash.add(new Edge(aPG.ifNodeStash.pop(), aPG.nodeStash.peek(), new Label("bool")));
			
			return visitChildren(ctx); }
		public String visitPlusExpr(CompilerParser.PlusExprContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("+");
			visit(ctx.rhs);
			return ""; }
		public String visitVar(CompilerParser.VarContext ctx) {
			aPG.edgeStash.getLast().label.addChar(String.valueOf(ctx.exp.getText()));
			return ""; }
		public String visitNum(CompilerParser.NumContext ctx) {
			aPG.edgeStash.getLast().label.addChar(String.valueOf(ctx.exp.getText()));
			return ""; }
		public String visitPowExpr(CompilerParser.PowExprContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("^");
			visit(ctx.rhs);
			return ""; }
		public String visitNestedExpr(CompilerParser.NestedExprContext ctx) {
			aPG.edgeStash.getLast().label.addChar("(");
			visit(ctx.exp);
			aPG.edgeStash.getLast().label.addChar(")");
			return ""; }
		public String visitProdExpr(CompilerParser.ProdExprContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("*");
			visit(ctx.rhs);
			return ""; }
		public String visitUMinusExpr(CompilerParser.UMinusExprContext ctx) {
			aPG.edgeStash.getLast().label.addChar("-");
			visit(ctx.exp);
			return ""; }
		public String visitMinusExpr(CompilerParser.MinusExprContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("-");
			visit(ctx.rhs);
			return ""; }
		public String visitOr(CompilerParser.OrContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("|");
			visit(ctx.rhs);
			return ""; }
		public String visitTrue(CompilerParser.TrueContext ctx) {
			aPG.edgeStash.getLast().label.addChar("true");
			return ""; }
		public String visitSmallerEqual(CompilerParser.SmallerEqualContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("<=");
			visit(ctx.rhs);
			return ""; }
		public String visitFalse(CompilerParser.FalseContext ctx) {
			aPG.edgeStash.getLast().label.addChar("true");
			return ""; }
		public String visitUnequal(CompilerParser.UnequalContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("!=");
			visit(ctx.rhs);
			return ""; }
		public String visitNeg(CompilerParser.NegContext ctx) {
			aPG.edgeStash.getLast().label.addChar("!");
			visit(ctx.exp);
			return ""; }
		public String visitGreaterEqual(CompilerParser.GreaterEqualContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar(">=");
			visit(ctx.rhs);
			return ""; }
		public String visitEqual(CompilerParser.EqualContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("=");
			visit(ctx.rhs);
			return ""; }
		public String visitNestedBool(CompilerParser.NestedBoolContext ctx) {
			aPG.edgeStash.getLast().label.addChar("(");
			visit(ctx.exp);
			aPG.edgeStash.getLast().label.addChar(")");
			return ""; }
		public String visitSCOr(CompilerParser.SCOrContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("||");
			visit(ctx.rhs);
			return ""; }
		public String visitAnd(CompilerParser.AndContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("&");
			visit(ctx.rhs);
			return ""; }
		public String visitSCAnd(CompilerParser.SCAndContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("&&");
			visit(ctx.rhs);
			return ""; }
		public String visitGreater(CompilerParser.GreaterContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar(">");
			visit(ctx.rhs);
			return ""; }
		public String visitSmaller(CompilerParser.SmallerContext ctx) {
			visit(ctx.lhs);
			aPG.edgeStash.getLast().label.addChar("<");
			visit(ctx.rhs);
			return ""; }
	}
	
	public class Parse extends CompilerBaseVisitor<String> {

		public String visitStart(CompilerParser.StartContext ctx) {
			return visitChildren(ctx);}
		public String visitVarDef(CompilerParser.VarDefContext ctx) {
			return visitChildren(ctx); }
		public String visitAppend(CompilerParser.AppendContext ctx) {
			return visitChildren(ctx); }
		public String visitDoLoop(CompilerParser.DoLoopContext ctx) {
			return visitChildren(ctx); }
		public String visitSkip(CompilerParser.SkipContext ctx) {
			return visitChildren(ctx); }
		public String visitIf(CompilerParser.IfContext ctx) {
			return visitChildren(ctx); }
		public String visitIfElif(CompilerParser.IfElifContext ctx) {
			return visitChildren(ctx); }
		public String visitIfThen(CompilerParser.IfThenContext ctx) {
			return visitChildren(ctx); }
		public String visitPlusExpr(CompilerParser.PlusExprContext ctx) {
			return visitChildren(ctx); }
		public String visitVar(CompilerParser.VarContext ctx) { 
			return visitChildren(ctx); }
		public String visitNum(CompilerParser.NumContext ctx) {
			return visitChildren(ctx); }
		public String visitPowExpr(CompilerParser.PowExprContext ctx) {
			return visitChildren(ctx); }
		public String visitNestedExpr(CompilerParser.NestedExprContext ctx) {
			return visitChildren(ctx); }
		public String visitProdExpr(CompilerParser.ProdExprContext ctx) {
			return visitChildren(ctx); }
		public String visitUMinusExpr(CompilerParser.UMinusExprContext ctx) {
			return visitChildren(ctx); }
		public String visitMinusExpr(CompilerParser.MinusExprContext ctx) {
			return visitChildren(ctx); }
		public String visitOr(CompilerParser.OrContext ctx) {
			return visitChildren(ctx); }
		public String visitTrue(CompilerParser.TrueContext ctx) {
			return visitChildren(ctx); }
		public String visitSmallerEqual(CompilerParser.SmallerEqualContext ctx) {
			return visitChildren(ctx); }
		public String visitFalse(CompilerParser.FalseContext ctx) {
			return visitChildren(ctx); }
		public String visitUnequal(CompilerParser.UnequalContext ctx) {
			return visitChildren(ctx); }
		public String visitNeg(CompilerParser.NegContext ctx) {
			return visitChildren(ctx); }
		public String visitGreaterEqual(CompilerParser.GreaterEqualContext ctx) {
			return visitChildren(ctx); }
		public String visitEqual(CompilerParser.EqualContext ctx) {
			return visitChildren(ctx); }
		public String visitNestedBool(CompilerParser.NestedBoolContext ctx) {
			return visitChildren(ctx); }
		public String visitSCOr(CompilerParser.SCOrContext ctx) {
			return visitChildren(ctx); }
		public String visitAnd(CompilerParser.AndContext ctx) {
			return visitChildren(ctx); }
		public String visitSCAnd(CompilerParser.SCAndContext ctx) {
			return visitChildren(ctx); }
		public String visitGreater(CompilerParser.GreaterContext ctx) {
			return visitChildren(ctx); }
		public String visitSmaller(CompilerParser.SmallerContext ctx) {
			return visitChildren(ctx); }
	}

	public void interpret(Memory mem, LinkedList<Edge> edgeList) throws ScriptException {
		evalEdge(mem, edgeList, edgeList.peek());
	}
	
	public void evalEdge(Memory mem, LinkedList<Edge> edgeList, Edge currentEdge) throws ScriptException{
		LinkedList<Edge> nexts = new LinkedList<Edge>();
		
		if (currentEdge.label.getType().equals("bool")) {
			if (mem.checkBool(currentEdge.label)) {
				
				if (currentEdge.to.number == -2) {
					finishInterpret(1);
				}
				
				for (Edge e : edgeList) {
					if (e.from == currentEdge.to) {
						evalEdge(mem, edgeList, e);
					}
				}
			} else {
				for (Edge e : edgeList) {
					if (e != currentEdge && e.from == currentEdge.from) {
						evalEdge(mem, edgeList, e);
					}
				}
				
				finishInterpret(0);
				
			}
		} else if (currentEdge.label.getType().equals("vardef")) {
			System.out.println(this.aMemory.toString());
			mem.updateMem(currentEdge.label);
			if (currentEdge.to.number == -2) {
				finishInterpret(1);
			}
			for (Edge e : edgeList) {
				if (e.from == currentEdge.to) {
					evalEdge(mem, edgeList, e);
				}
			}
		}
	}
	
	public void finishInterpret(int status) {
		switch (status) {
			case 0 : this.aStatus.setStatus("stuck"); break;
			case 1 : this.aStatus.setStatus("terminated"); break;
		}
		
		System.out.println("\n----------------------\n");
		System.out.println(aStatus.toString() + aMemory.toString());
		System.out.println("\n----------------------\n");
		System.exit(0);
	}

	// This class is used to throw the correct exception
	public class BailErrorStrategy extends DefaultErrorStrategy {
		
	    
	    public void recover(Parser recognizer, RecognitionException e) {
			for (ParserRuleContext context = recognizer.getContext(); context != null; context = context.getParent()) {
				context.exception = e;
			}

	        throw new ParseCancellationException(e);
	    }

	    
	    public Token recoverInline(Parser recognizer)
	        throws RecognitionException
	    {
			InputMismatchException e = new InputMismatchException(recognizer);
			for (ParserRuleContext context = recognizer.getContext(); context != null; context = context.getParent()) {
				context.exception = e;
			}

	        throw new ParseCancellationException(e);
	    }

	    
	    public void sync(Parser recognizer) { }
	}
}