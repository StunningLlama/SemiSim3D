// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.script;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import electrodynamics.script.Token.TokenType;

public class Interpreter {
	
	public Evaluator evaluator;
	
	public Interpreter() {
		evaluator = new Evaluator();
		evaluator.registerDefaultMathOperators();
	}

	public List<Instruction> process(String code, Highlighter highlighter) {
		if (highlighter != null)
			highlighter.resetHighlight();
		
		int DISCARD = 0;
		int NEW = 1;
		int CONTINUE = 2;

		int[][] a = 
			{{DISCARD, NEW},
			{DISCARD, CONTINUE}};

		List<Line> lines = new ArrayList<Line>();
		StringBuilder b = new StringBuilder();
		int i_start = 0;
		int state = 0;
		for (int i = 0; i < code.length(); i++) {
			char c = code.charAt(i);

			int new_state;
			if (isNewline(c)) new_state = 0;
			else new_state = 1;

			int action = a[state][new_state];

			if (action == NEW) {
				if (b.length() > 0)
					lines.add(new Line(b.toString(), i_start, i-1));
				b.setLength(0); i_start = i;
				b.append(c);
			} else if (action == CONTINUE) {
				b.append(c);
			} else if (action == DISCARD) {
				if (b.length() > 0)
					lines.add(new Line(b.toString(), i_start, i-1));
				b.setLength(0); i_start = i;
			}

			state = new_state;
		}

		if (b.length() > 0)
			lines.add(new Line(b.toString(), i_start, code.length()-1));
		
		return process(lines, highlighter);
	}
	
	public List<Token> readTokens(Line line, Highlighter highlighter) {
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
		String s = line.string;
		StringBuilder b = new StringBuilder();
		TokenType type = TokenType.UNKNOWN;
		int i_offset = line.bounds.start;
		int i_start = 0;
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
					tokens.add(new Token(b.toString(), TokenType.STRING, i_start+i_offset, i+i_offset));
					b.setLength(0); i_start = i;
					insidestring = false;
				} else {
					if (b.length() > 0)
						tokens.add(new Token(b.toString(), type, i_start+i_offset, i-1+i_offset));
					b.setLength(0); i_start = i;
					insidestring = true;
				}
			} else if (insidestring) {
				b.append(c);
			} else if (action == NEW) {
				if (b.length() > 0)
					tokens.add(new Token(b.toString(), type, i_start+i_offset, i-1+i_offset));
				b.setLength(0); i_start = i;
				b.append(Character.toLowerCase(c));
				type = TokenType.fromInt(new_state);
			} else if (action == CONTINUE) {
				b.append(Character.toLowerCase(c));
			} else if (action == DISCARD) {
				if (b.length() > 0)
					tokens.add(new Token(b.toString(), type, i_start+i_offset, i-1+i_offset));
				b.setLength(0); i_start = i;
			}

			state = new_state;
		}

		if (b.length() > 0)
			tokens.add(new Token(b.toString(), type, i_start+i_offset, s.length()-1+i_offset));

		/*if (highlighter != null) {
			for (Token t : tokens) {
				if (t.type == TokenType.NUMBER)
					highlighter.setColor(t.bounds, highlighter.getNumberColor());
				else if (t.type == TokenType.STRING)
					highlighter.setColor(t.bounds, highlighter.getStringColor());
			}
		}*/

		return tokens;
	}
	
	public List<Token> removeComments(List<Token> input, Highlighter highlighter) {
		List<Token> output = new ArrayList<Token>();
		boolean comment = false;
		for (Token t : input) {
			comment |= t.isComment();
			if (!comment)
				output.add(t);
			else if (highlighter != null)
				highlighter.markText(t.bounds, highlighter.getCommentColor());
		}
		
		return output;
	}
	
	public Instruction parseLine(Line line, Highlighter highlighter) {
		List<Token> tokens = removeComments(readTokens(line, highlighter), highlighter);
		if (tokens.size() > 0) {
			Token first = tokens.get(0);
			switch(first.chars) {
			case "if":
				if (highlighter != null) highlighter.markText(first.bounds, highlighter.getKeywordColor());
				return new If(tokens, evaluator, highlighter);
			case "while":
				if (highlighter != null) highlighter.markText(first.bounds, highlighter.getKeywordColor());
				return new While(tokens, evaluator, highlighter);
			case "else":
				if (highlighter != null) highlighter.markText(first.bounds, highlighter.getKeywordColor());
				return new Else();
			case "end":
				if (highlighter != null) highlighter.markText(first.bounds, highlighter.getKeywordColor());
				return new End();
			default:
				if (tokens.size() > 1) {
					if (tokens.get(1).chars.equals("=")) {
						if (highlighter != null) highlighter.markText(first.bounds, highlighter.getVariableColor());
						return new Assign(tokens, evaluator, highlighter);
					} else {
						return new ExecuteFunction(tokens, evaluator, highlighter);
					}
				} else {
					return new ExecuteFunction(tokens, evaluator, highlighter);
				}
			}
		} else {
			return null;
		}
	}
	
	public List<Instruction> process(List<Line> lines, Highlighter highlighter) {
		if (lines.size() > Limits.LINES_LIMIT)
			throwError("Too many lines! Limit is " + Limits.LINES_LIMIT, new Bounds(0, 1), highlighter);
		
		List<Instruction> instructions = new ArrayList<Instruction>();
		
		for (int i = 0; i < lines.size(); i++) {
			if (lines.get(i).string.length() > Limits.CHAR_LIMIT)
				throwError("Too many characters! Limit is " + Limits.CHAR_LIMIT, new Bounds(0, 1), highlighter);
			
			Instruction ins = parseLine(lines.get(i), highlighter);
			if (ins != null) {
				ins.bounds = lines.get(i).bounds;
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
							throwError("Too many ends!", ins.bounds, highlighter);
						}
					}
				} else {
					throwError("Too many ends!", ins.bounds, highlighter);
				}
			} else if (ins instanceof Else) {
				if (!stack.isEmpty()) {
					Instruction ins2 = stack.peek();
					if (ins2 instanceof If) {
						((If) ins2).else_pos = ins.pos;
					} else {
						throwError("Else must match if.", ins.bounds, highlighter);
					}
					stack.push(ins);
				} else {
					throwError("Too many elses!", ins.bounds, highlighter);
				}
			}
		}
		
		if (!stack.isEmpty()) {
			throwError("Not enough ends!", stack.peek().bounds, highlighter);
		}
		
		return instructions;
	}
	
	public void execute(List<Instruction> instructions, State state) {
		state.instruction_pointer = 0;
		int cycles = 0;
		while (state.instruction_pointer >= 0 && state.instruction_pointer < instructions.size() && cycles < Limits.INSTRUCTION_LIMIT) {
			Instruction ins = instructions.get(state.instruction_pointer);
			state.instruction_pointer = ins.execute(state, evaluator);
			cycles++;
		}
		
		if (cycles >= Limits.INSTRUCTION_LIMIT) {
			throw new CodeException("Instruction limit reached (limit = " + Limits.INSTRUCTION_LIMIT + ")", null);
		}
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
		return c == '!' || c == '=' || c == '<' || c == '>';
	}
	
	public boolean isQuote(char c) {
		return c == '\"';
	}
	
	public boolean isNewline(char c) {
		return c == '\r' || c == '\n';
	}
	
	public void throwError(String message, Bounds bounds, Highlighter highlighter) {
		if (bounds != null && highlighter != null)
			highlighter.markError(bounds, highlighter.getErrorColor(), message);
		
		throw new CodeException(message, bounds);
	}
	
	
}

class Line {
	String string;
	Bounds bounds;
	
	public Line(String string, int start, int end) {
		this.string = string;
		this.bounds = new Bounds(start, end);
	}
}

class Limits {
	public static int MEM_LIMIT = 1000;
	public static int INSTRUCTION_LIMIT = 1000000;
	public static int LINES_LIMIT = 10000;
	public static int CHAR_LIMIT = 1000;
}

