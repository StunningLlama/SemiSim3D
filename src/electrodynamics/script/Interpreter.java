// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

public class Interpreter {
	
	/*public static void main(String[] args) {
		Interpreter i = new Interpreter();
		String testcode = "n = 7200\r\n"
				+ "m = 2\r\n"
				+ "while m < sqrt(n) + 1\r\n"
				+ "	if n % m eq 0\r\n"
				+ "		print(m)\r\n"
				+ "	end\r\n"
				+ "#print(m)\r\n"
				+ "	m = m+1\r\n"
				+ "end";
		
		//String testcode = "Potat%  \"pota2 .to\"    23+5";
		List<Instruction> program = i.process(testcode);
		State state = new State();
		i.execute(program, state);
	}*/
	
	public Evaluator evaluator;
	
	public Interpreter() {
		evaluator = new Evaluator();
		evaluator.registerDefaultMathOperators();
	}
	
	public List<Instruction> process(String code) {
		return process(code.split("\\r?\\n"));
	}
	
	public List<Instruction> process(String[] lines) {
		if (lines.length > Limits.LINES_LIMIT)
			throw new CodeException("Too many lines! Limit is " + Limits.LINES_LIMIT);
		
		List<Instruction> instructions = new ArrayList<Instruction>();
		
		for (int i = 0; i < lines.length; i++) {
			if (lines[i].length() > Limits.CHAR_LIMIT)
				throw new CodeException("Too many characters! Limit is " + Limits.CHAR_LIMIT);
			
			Instruction ins = parseLine(lines[i]);
			if (ins != null) {
				instructions.add(ins);
			}
		}
		
		Stack<Instruction> stack = new Stack<Instruction>();
		
		for (int i = 0; i < instructions.size(); i++) {
			Instruction ins = instructions.get(i);
			ins.pos = i;
			
			if (ins instanceof If) {
				stack.push(ins);
			} else if (ins instanceof While) {
				stack.push(ins);
			} else if (ins instanceof End) {
				if (!stack.isEmpty()) {
					Instruction ins2 = stack.pop();
					if (ins2 instanceof If) {
						((If) ins2).end_pos = ins.pos;
					} else if (ins2 instanceof While) {
						((While) ins2).end_pos = ins.pos;
						((End) ins).start_pos = ins2.pos;
						((End) ins).end_while = true;
					} else if (ins2 instanceof Else) {
						((Else) ins2).end_pos = ins.pos;
						if (!stack.isEmpty()) {
							Instruction ins3 = stack.pop();
							if (ins3 instanceof If) {
								((If) ins3).end_pos = ins.pos;
							}
						} else {
							throw new CodeException("Too many ends!");
						}
					}
				} else {
					throw new CodeException("Too many ends!");
				}
			} else if (ins instanceof Else) {
				if (!stack.isEmpty()) {
					Instruction ins2 = stack.peek();
					if (ins2 instanceof If) {
						((If) ins2).else_pos = ins.pos;
					} else {
						throw new CodeException("Else must match if.");
					}
					stack.push(ins);
				} else {
					throw new CodeException("Too many elses!");
				}
			}
		}
		
		if (!stack.isEmpty()) {
			throw new CodeException("Not enough ends!");
		}
		
		return instructions;
	}
	
	public void execute(List<Instruction> instructions, State state) {
		int i = 0;
		int cycles = 0;
		while (i >= 0 && i < instructions.size() && cycles < Limits.INSTRUCTION_LIMIT) {
			Instruction ins = instructions.get(i);
			i = ins.execute(state, evaluator);
			cycles++;
		}
		
		if (cycles >= Limits.INSTRUCTION_LIMIT) {
			throw new CodeException("Instruction limit reached (limit = " + Limits.INSTRUCTION_LIMIT + ")");
		}
	}
	
	public Instruction parseLine(String line) {
		List<Token> tokens = removeComments(readTokens(line));
		if (tokens.size() > 0) {
			Token first = tokens.get(0);
			switch(first.chars) {
			case "if":
				return new If(tokens, evaluator);
			case "while":
				return new While(tokens, evaluator);
			case "else":
				return new Else();
			case "end":
				return new End();
			default:
				if (tokens.size() > 1) {
					if (tokens.get(1).chars.equals("=")) {
						return new Assign(tokens, evaluator);
					} else {
						return new ExecuteFunction(tokens, evaluator);
					}
				} else {
					return new ExecuteFunction(tokens, evaluator);
				}
			}
		} else {
			return null;
		}
	}
	
	public List<Token> readTokens(String s) {
		int DISCARD = 0;
		int NEW = 1;
		int CONTINUE = 2;
		
		int[][] a = 
			{{DISCARD, NEW, NEW, NEW, NEW},
			{DISCARD, CONTINUE, CONTINUE, NEW, NEW},
			{DISCARD, CONTINUE, CONTINUE, NEW, NEW},
			{DISCARD, NEW, NEW, NEW, NEW},
			{DISCARD, NEW, NEW, NEW, CONTINUE}};
		
		List<Token> tokens = new ArrayList<Token>();
		StringBuilder b = new StringBuilder();
		TokenType type = TokenType.UNKNOWN;
		int state = 0;
		boolean insidestring = false;
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			
			int new_state = -1;
			if (isNum(c)) new_state = 1;
			else if (isLetter(c)) new_state = 2;
			else if (isWhitespace(c)) new_state = 0;
			else if (isSpecialPunctuation(c)) new_state = 4;
			else new_state = 3;
			
			int action = a[state][new_state];
			if (isQuote(c)) {
				if (insidestring) {
					tokens.add(new Token(b.toString(), TokenType.STRING));
					b.setLength(0);
					insidestring = false;
				} else {
					if (b.length() > 0)
						tokens.add(new Token(b.toString(), type));
					b.setLength(0);
					insidestring = true;
				}
			} else if (insidestring) {
				b.append(c);
			} else if (action == NEW) {
				if (b.length() > 0)
					tokens.add(new Token(b.toString(), type));
				b.setLength(0);
				b.append(Character.toLowerCase(c));
				type = TokenType.fromInt(new_state);
			} else if (action == CONTINUE) {
				b.append(Character.toLowerCase(c));
			} else if (action == DISCARD) {
				if (b.length() > 0)
					tokens.add(new Token(b.toString(), type));
				b.setLength(0);
			}

			state = new_state;
		}

		if (b.length() > 0)
			tokens.add(new Token(b.toString(), type));
		
		/*for (Token t : tokens) {
			System.out.println(t.toString());
		}*/

		return tokens;
	}
	
	public List<Token> removeComments(List<Token> input) {
		List<Token> output = new ArrayList<Token>();
		for (Token t : input) {
			if (!t.isComment())
				output.add(t);
			else
				break;
		}
		
		return output;
	}
	
	public boolean isNum(char c) {
		return Character.isDigit(c) || c == '.';
	}
	
	public boolean isLetter(char c) {
		return Character.isLetter(c) || c == '_';
	}

	public boolean isWhitespace(char c) {
		return Character.isWhitespace(c);
	}
	
	public boolean isSpecialPunctuation(char c) {
		return c == '!' || c == '=';
	}
	
	public boolean isQuote(char c) {
		return c == '\"';
	}
	
	public abstract class Instruction {
		int pos;
		boolean suppress_output = false;
		abstract int execute(State state, Evaluator evaluator);
	};

	public class If extends Instruction {
		List<Unit> condition;
		int end_pos = -2;
		int else_pos = -2;
		
		public If(List<Token> tokens, Evaluator evaluator) {
			List<Token> sub_tokens = new ArrayList<Token>();
			for (int i = 1; i < tokens.size(); i++) {
				if (tokens.get(i).chars.equals(";"))
					suppress_output = true;
				else
					sub_tokens.add(tokens.get(i));
			}
			
			condition = evaluator.parse(sub_tokens);
		}
		
		@Override
		int execute(State state, Evaluator evaluator) {
			state.suppress_output = suppress_output;
			Object result = evaluator.eval(condition, state.variables);
			if (!(result instanceof Double))
				throw new CodeException("If: Number expected, got " + result.toString() + " instead.");
			
			if ((double) result > 0.5) {
				return pos+1;
			} else {
				if (else_pos >= 0)
					return else_pos+1;
				else
					return end_pos+1;
			}
		}
	}

	public class While extends Instruction {
		List<Unit> condition;
		int end_pos = -2;

		public While(List<Token> tokens, Evaluator evaluator) {
			List<Token> sub_tokens = new ArrayList<Token>();
			for (int i = 1; i < tokens.size(); i++) {
				if (tokens.get(i).chars.equals(";"))
					suppress_output = true;
				else
					sub_tokens.add(tokens.get(i));
			}
			
			condition = evaluator.parse(sub_tokens);
			Debugger.print(condition);
		}
		
		@Override
		int execute(State state, Evaluator evaluator) {
			state.suppress_output = suppress_output;
			Object result = evaluator.eval(condition, state.variables);
			if (!(result instanceof Double))
				throw new CodeException("While: Number expected, got " + result.toString() + " instead.");
			
			if ((double) result > 0.5)
				return pos+1;
			else
				return end_pos+1;
		}
	}
	
	public class Else extends Instruction {
		int end_pos = -2;
		
		@Override
		int execute(State state, Evaluator evaluator) {
			return end_pos+1;
		}
	}
	
	public class End extends Instruction {
		int start_pos = -2;
		boolean end_while = false;

		@Override
		int execute(State state, Evaluator evaluator) {
			if (end_while) {
				return start_pos;
			} else {
				return pos+1;
			}
		}
	}

	public class Assign extends Instruction {
		String var;
		List<Unit> expression;

		public Assign(List<Token> tokens, Evaluator evaluator) {
			var = tokens.get(0).chars;
			
			if (evaluator.isReserved(var)) {
				throw new CodeException("Error: Reserved variable name: " + var);
			}
			
			List<Token> sub_tokens = new ArrayList<Token>();
			for (int i = 2; i < tokens.size(); i++) {
				if (tokens.get(i).chars.equals(";"))
					suppress_output = true;
				else
					sub_tokens.add(tokens.get(i));
			}
			
			expression = evaluator.parse(sub_tokens);
			//Debugger.print(expression);
		}
		
		@Override
		int execute(State state, Evaluator evaluator) {
			state.suppress_output = suppress_output;
			state.variables.put(var, evaluator.eval(expression, state.variables));
			if (state.variables.size() > Limits.MEM_LIMIT)
				throw new RuntimeException("Memory limit reached");
			return pos+1;
		}
	}

	public class ExecuteFunction extends Instruction {
		List<Unit> function;

		public ExecuteFunction(List<Token> tokens, Evaluator evaluator) {
			List<Token> sub_tokens = new ArrayList<Token>();
			for (int i = 0; i < tokens.size(); i++) {
				if (tokens.get(i).chars.equals(";"))
					suppress_output = true;
				else
					sub_tokens.add(tokens.get(i));
			}
			
			function = evaluator.parse(sub_tokens);
		}
		
		@Override
		int execute(State state, Evaluator evaluator) {
			state.suppress_output = suppress_output;
			evaluator.eval(function, state.variables);
			return pos+1;
		}
	}


	public class CodeException extends RuntimeException {
		private static final long serialVersionUID = 2662454885274139289L;
		public CodeException(String msg)
		{
			super(msg);
		}
	}
}

class Limits {
	public static int MEM_LIMIT = 1000;
	public static int INSTRUCTION_LIMIT = 1000000;
	public static int LINES_LIMIT = 10000;
	public static int CHAR_LIMIT = 1000;
}

