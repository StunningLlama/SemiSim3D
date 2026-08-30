// Copyright (c) Brandon Li 2026
// This file is part of Brandon's Semiconductor Simulator which is released under GNU GPL v3.0.
// See LICENSE.txt for full license details.

package electrodynamics.script;

import java.util.ArrayList;
import java.util.EmptyStackException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Stack;

public class Evaluator {
	public HashMap<String, Operator> operators;
	public HashMap<String, Function> functions;
	public HashMap<String, Double> constants;
	public HashSet<String> reserved_names;
	boolean DEBUG = false;
	
	public Evaluator()
	{
		constants = new HashMap<String, Double>();
		operators = new HashMap<String, Operator>();
		functions = new HashMap<String, Function>();
		reserved_names = new HashSet<String>();
	}
	
	public List<Unit> parse(List<Token> tokens) {
		List<Unit> list = processTokens(tokens);
		Queue<Unit> input = new LinkedList<Unit>();
		for (Unit s: list) input.add(s);
		List<Unit> rpn = toPostfix(input);
		return rpn;
	}
	
	public boolean isReserved(String name) {
		return reserved_names.contains(name);
	}
	
	public void setConstant(String s, double c) {
		constants.put(s, c);
		reserved_names.add(s);
	}
	
	public Double getConstant(String s) {
		return constants.get(s.toLowerCase());
	}
	
	public void registerOperator(String str, int precedence, Association assoc, BinaryOperator func)
	{
		operators.put(str, new Operator(precedence, assoc, func));
		reserved_names.add(str);
	}

	public void registerOperator(String str, int precedence, Association assoc, UnaryOperator func)
	{
		operators.put(str, new Operator(precedence, assoc, func));
		reserved_names.add(str);
	}
	
	public void registerFunction(String str, BinaryOperator func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}

	public void registerFunction(String str, UnaryOperator func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}

	public void registerFunction(String str, NaryFunction func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}
	
	public List<Unit> processTokens(List<Token> tokens) {
		for (int i = tokens.size() - 1; i >= 0 ; i--) 
			if (tokens.get(i).chars.equals("-") && (i == 0 || (i > 0 && followingTokenUnary(tokens.get(i-1))))) {
				tokens.get(i).chars = "_unary_sub";
				tokens.get(i).type = TokenType.NAME;
			}
		
		List<Unit> units = new ArrayList<Unit>(tokens.size());
		
		for (int i = 0; i < tokens.size(); i++) {
			Token t = tokens.get(i);
			Unit u = new Unit();
			if (t.type == TokenType.NUMBER) {
				u.type = UnitType.VALUE;
				u.value = Double.valueOf(t.chars);
			} else if (t.type == TokenType.NAME) {
				if (operators.containsKey(t.chars)) {
					u.op = operators.get(t.chars);
					u.func = u.op.func;
					u.type = UnitType.OPERATOR;
				} else if (functions.containsKey(t.chars)) {
					u.func = functions.get(t.chars);
					u.type = UnitType.FUNCTION;
				} else if (constants.containsKey(t.chars)) {
					u.type = UnitType.VALUE;
					u.value = constants.get(t.chars);
				} else {
					u.type = UnitType.VALUE;
				}
			} else if (t.type == TokenType.PUNCTUATION) {
				if (t.chars.equals("("))
					u.type = UnitType.LEFT_PAREN;
				else if (t.chars.equals(")"))
					u.type = UnitType.RIGHT_PAREN;
				else if (t.chars.equals(","))
					u.type = UnitType.COMMA;
				else {
					if (operators.containsKey(t.chars)) {
						u.op = operators.get(t.chars);
						u.func = u.op.func;
						u.type = UnitType.OPERATOR;
					} else if (functions.containsKey(t.chars)) {
						u.func = functions.get(t.chars);
						u.type = UnitType.FUNCTION;
					} else {
						throw new EvalException("Missing symbol: " + t.chars);
					}
				}
			} else if (t.type == TokenType.STRING) {
				u.type = UnitType.VALUE;
				u.value = t.chars;
			} else {
				throw new EvalException("Missing token: " + t.chars);
			}
			
			u.chars = t.chars;
			units.add(u);
		}

		if (DEBUG) {
			Debugger.print(units);
		}
		
		return units;
	}
	
	private boolean followingTokenUnary(Token t) {
		return t.type == TokenType.PUNCTUATION && !t.chars.equals(")") || t.type == TokenType.NAME && functions.containsKey(t.chars);
	}
	
	public List<Unit> toPostfix(Queue<Unit> tokens)
	{
		List<Unit> output = new ArrayList<Unit>();
		Stack<Unit> operatorstack = new Stack<Unit>();

		try {
			while(!tokens.isEmpty())
			{
				Unit token = tokens.poll();

				if (token.type == UnitType.VALUE)
					output.add(token);
				else if (token.type == UnitType.LEFT_PAREN)
					operatorstack.push(token);
				else if (token.type == UnitType.RIGHT_PAREN) {
					while (operatorstack.peek().type != UnitType.LEFT_PAREN)
						output.add(operatorstack.pop());
					operatorstack.pop();
					if (!operatorstack.isEmpty() && operatorstack.peek().type == UnitType.FUNCTION)
						output.add(operatorstack.pop());
				}
				else if (token.type == UnitType.COMMA)
					while (operatorstack.peek().type != UnitType.LEFT_PAREN)
						output.add(operatorstack.pop());
				else if (token.type == UnitType.FUNCTION)
					operatorstack.push(token);
				else if (token.type == UnitType.OPERATOR) {
					while (!operatorstack.isEmpty() && operatorstack.peek().type == UnitType.OPERATOR &&
							((token.op.precedence < operatorstack.peek().op.precedence)
									|| (token.op.assoc == Association.LEFT && token.op.precedence == operatorstack.peek().op.precedence)))
						output.add(operatorstack.pop());
					operatorstack.push(token);
				}
			} 
		} catch (EmptyStackException ex) {
			throw new EvalException("Malformed expression!");
		}

		while (!operatorstack.isEmpty())
		{
			if (operatorstack.peek().type == UnitType.LEFT_PAREN || operatorstack.peek().type == UnitType.RIGHT_PAREN)
				throw new EvalException("Mismatched Parenthesis!");
			output.add(operatorstack.pop());
		}

		if (DEBUG) {
			Debugger.print(output);
		}

		return output;
	}
	
	public Object eval(List<Unit> units, HashMap<String, Object> variables)
	{
		Stack<Object> stack = new Stack<Object>();
		Object[] args = new Object[100];
		Unit currentunit = null;
		try {
			for (Unit unit: units) {
				currentunit = unit;
				if (unit.func != null) {
					if (unit.func instanceof UnaryOperator) {
						stack.push(((UnaryOperator) unit.func).operate((double) stack.pop()));
					} else if (unit.func instanceof BinaryOperator) {
						double b = (double)stack.pop();
						double a = (double)stack.pop();
						stack.push(((BinaryOperator) unit.func).operate(a, b));
					} else if (unit.func instanceof NaryFunction) {
						int n_args = ((NaryFunction) unit.func).get_n_args();
						for (int i = n_args-1; i >= 0; i--) {
							args[i] = stack.pop();
						}
						stack.push(((NaryFunction) unit.func).operate(args));
					}
				} else {
					if (unit.value != null) {
						stack.push(unit.value);
					} else {
						if (variables.containsKey(unit.chars)) {
							stack.push(variables.get(unit.chars));
						} else {
							throw new EvalException("Variable " + unit.chars + " not found!");
						}
					}
				}
			}
		} catch (EmptyStackException ex) {
			throw new EvalException("Function " + currentunit.toString() + " has not enough args.");
		} catch (ClassCastException ex) {
			throw new EvalException("Type mismatch during execution of " + currentunit.toString() + ": " + ex.getMessage());
		}
		
		if (!stack.isEmpty())
			return stack.peek();
		else
			return Double.NaN;
	}

	public void registerDefaultMathOperators() {
		registerOperator("+", 1, Association.LEFT, (a, b) -> a+b);
		registerOperator("-", 1, Association.LEFT, (a, b) -> a-b);
		registerOperator("*", 2, Association.LEFT, (a, b) -> a*b);
		registerOperator("/", 2, Association.LEFT, (a, b) -> a/b);
		registerOperator("^", 3, Association.LEFT, (a, b) -> Math.pow(a, b));
		registerOperator("%", 2, Association.LEFT, (a, b) -> a%b);
		registerOperator("<", 0, Association.LEFT, (a, b) -> (a<b)? 1: 0);
		registerOperator(">", 0, Association.LEFT, (a, b) -> (a>b)? 1: 0);
		registerOperator("==", 2, Association.LEFT, (a, b) -> (a==b)? 1: 0);
		registerOperator("!=", 2, Association.LEFT, (a, b) -> (a!=b)? 1: 0);
		registerOperator("_unary_sub", 4, Association.RIGHT, a -> -a);
		
		registerFunction("sqrt", a -> Math.sqrt(a));
		registerFunction("ln", a -> Math.log(a));
		registerFunction("exp", a -> Math.exp(a));
		registerFunction("sin", a -> Math.sin(a));
		registerFunction("cos", a -> Math.cos(a));
		registerFunction("tan", a -> Math.tan(a));
		registerFunction("asin", a -> Math.asin(a));
		registerFunction("acos", a -> Math.acos(a));
		registerFunction("atan", a -> Math.atan(a));
		registerFunction("sinh", a -> Math.sinh(a));
		registerFunction("cosh", a -> Math.cosh(a));
		registerFunction("tanh", a -> Math.tanh(a));
		registerFunction("abs", a -> Math.abs(a));
		registerFunction("min", (a, b) -> Math.min(a, b));
		registerFunction("max", (a, b) -> Math.max(a, b));
		registerFunction("ceil", a -> Math.ceil(a));
		registerFunction("floor", a -> Math.floor(a));
		registerFunction("round", a -> Math.round(a));

		registerFunction("stdout", a -> {System.out.println(a); return 0;});

		setConstant("pi", Math.PI);
		setConstant("e", Math.E);
		setConstant("true", 1);
		setConstant("false", 0);
	}

	public class EvalException extends RuntimeException {
		private static final long serialVersionUID = 2662454885274139289L;
		public EvalException(String msg)
		{
			super(msg);
		}
	}
}

class Token {
	String chars;
	TokenType type;
	
	public Token(String chars, TokenType type) {
		this.chars = chars;
		this.type = type;
	}
	
	@Override
	public String toString() {
		return chars + ":" + type.toString();
	}
	
	public boolean isComment() {
		return type == TokenType.PUNCTUATION && chars.equals("#");
	}
}

enum TokenType {
	NAME, NUMBER, PUNCTUATION, STRING, UNKNOWN;
	
	public static TokenType fromInt(int i) {
		if (i == 1)
			return NUMBER;
		else if (i == 2)
			return NAME;
		else if (i == 3 || i == 4)
			return PUNCTUATION;
		else
			return UNKNOWN;
	}
}

class Unit {
	UnitType type;
	Operator op = null;
	Function func = null;
	String chars = "";
	Object value = null;

	@Override
	public String toString() {
		if (op != null)
			return chars + " (Op, " + op.precedence + ", " + op.assoc.toString() + ");";
		else if (func != null) {
			return chars + " (Func);";
		} else {
			return chars + " (" + type.toString() + ");";
		}
	}
}

enum UnitType {
	LEFT_PAREN, RIGHT_PAREN, COMMA, OPERATOR, FUNCTION, VALUE
}

enum Association {
	LEFT, RIGHT
}

interface BinaryOperator extends Function {
	public double operate(double b, double a);
}

interface UnaryOperator extends Function {
	public double operate(double a);
}

interface NaryFunction extends Function {
	public int get_n_args();
	public Object operate(Object[] args);
}

class Operator {
	public int precedence;
	public Association assoc;
	public Function func;
	public Operator(int p, Association a, Function o)
	{
		precedence = p;
		assoc = a;
		func = o;
	}
}