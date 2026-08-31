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
import java.util.Random;
import java.util.Stack;

import electrodynamics.script.Function.BinaryOperator;
import electrodynamics.script.Function.NaryFunction;
import electrodynamics.script.Function.NullaryOperator;
import electrodynamics.script.Function.TernaryOperator;
import electrodynamics.script.Function.UnaryOperator;
import electrodynamics.script.Operator.Association;
import electrodynamics.script.Token.TokenType;
import electrodynamics.script.Unit.UnitType;

public class Evaluator {
	public HashMap<String, Operator> operators;
	public HashMap<String, Function> functions;
	public HashMap<String, Double> constants;
	public HashSet<String> reserved_names;
	boolean DEBUG = false;
	public static Random rand = new Random(System.nanoTime());
	
	public Evaluator()
	{
		constants = new HashMap<String, Double>();
		operators = new HashMap<String, Operator>();
		functions = new HashMap<String, Function>();
		reserved_names = new HashSet<String>();
	}
	
	public List<Unit> parse(List<Token> tokens, Highlighter highlighter) {
		List<Unit> list = processTokens(tokens, highlighter);
		if (highlighter != null) highlight(list, highlighter);
		Queue<Unit> input = new LinkedList<Unit>();
		for (Unit s: list) input.add(s);
		List<Unit> rpn = toPostfix(input, highlighter);
		return rpn;
	}

	public void highlight(List<Unit> list, Highlighter highlighter) {
		if (highlighter != null) {
			for (Unit s: list) {
				if (s.type == UnitType.FUNCTION)
					highlighter.markText(s.bounds, highlighter.getFunctionColor());
				else if (s.type == UnitType.VARIABLE)
					highlighter.markText(s.bounds, highlighter.getVariableColor());
				else if (s.type == UnitType.NUMBER)
					highlighter.markText(s.bounds, highlighter.getNumberColor());
				else if (s.type == UnitType.STRING)
					highlighter.markText(s.bounds, highlighter.getStringColor());
				else if (s.type == UnitType.CONSTANT)
					highlighter.markText(s.bounds, highlighter.getKeywordColor());
				else
					highlighter.markText(s.bounds, highlighter.getOperatorColor());
			}
		}
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

	public void registerFunction(String str, NullaryOperator func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}
	
	public void registerFunction(String str, UnaryOperator func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}
	
	public void registerFunction(String str, BinaryOperator func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}
	
	public void registerFunction(String str, TernaryOperator func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}

	public void registerFunction(String str, NaryFunction func)
	{
		functions.put(str, func);
		reserved_names.add(str);
	}

	public List<Unit> processTokens(List<Token> tokens, Highlighter highlighter) {
		for (int i = tokens.size() - 1; i >= 0 ; i--) 
			if (i == 0 || (i > 0 && followingTokenUnary(tokens.get(i-1)))) {
				if (tokens.get(i).chars.equals("-")) {
					tokens.get(i).chars = "_unary_sub";
					tokens.get(i).type = TokenType.NAME;
				} else if (tokens.get(i).chars.equals("+")) {
					tokens.get(i).chars = "_unary_add";
					tokens.get(i).type = TokenType.NAME;
				}
			}
		
		List<Unit> units = new ArrayList<Unit>(tokens.size());
		
		for (int i = 0; i < tokens.size(); i++) {
			Token t = tokens.get(i);
			Unit u = new Unit();
			u.bounds = t.bounds;
			if (t.type == TokenType.NUMBER) {
				u.type = UnitType.NUMBER;
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
					u.type = UnitType.CONSTANT;
					u.value = constants.get(t.chars);
				} else {
					u.type = UnitType.VARIABLE;
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
						throwError("Missing symbol: " + t.chars, t.bounds, highlighter);
					}
				}
			} else if (t.type == TokenType.STRING) {
				u.type = UnitType.STRING;
				u.value = t.chars;
			} else {
				throwError("Missing token: " + t.chars, t.bounds, highlighter);
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
	
	public List<Unit> toPostfix(Queue<Unit> tokens, Highlighter highlighter)
	{
		List<Unit> output = new ArrayList<Unit>();
		Stack<Unit> operatorstack = new Stack<Unit>();

		try {
			while(!tokens.isEmpty())
			{
				Unit token = tokens.poll();

				if (token.type == UnitType.LEFT_PAREN) {
					operatorstack.push(token);
				} else if (token.type == UnitType.RIGHT_PAREN) {
					while (operatorstack.peek().type != UnitType.LEFT_PAREN)
						output.add(operatorstack.pop());
					operatorstack.pop();
					if (!operatorstack.isEmpty() && operatorstack.peek().type == UnitType.FUNCTION)
						output.add(operatorstack.pop());
				} else if (token.type == UnitType.COMMA) {
					while (operatorstack.peek().type != UnitType.LEFT_PAREN)
						output.add(operatorstack.pop());
				} else if (token.type == UnitType.FUNCTION) {
					operatorstack.push(token);
				} else if (token.type == UnitType.OPERATOR) {
					while (!operatorstack.isEmpty() && operatorstack.peek().type == UnitType.OPERATOR &&
							((token.op.precedence < operatorstack.peek().op.precedence)
									|| (token.op.assoc == Association.LEFT && token.op.precedence == operatorstack.peek().op.precedence)))
						output.add(operatorstack.pop());
					operatorstack.push(token);
				} else {
					output.add(token);
				}
			} 
		} catch (EmptyStackException ex) {
			throwError("Malformed expression!", null, highlighter);
		}

		while (!operatorstack.isEmpty())
		{
			if (operatorstack.peek().type == UnitType.LEFT_PAREN || operatorstack.peek().type == UnitType.RIGHT_PAREN)
				throwError("Mismatched Parenthesis!", operatorstack.peek().bounds, highlighter);
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
					} else if (unit.func instanceof TernaryOperator) {
						double c = (double)stack.pop();
						double b = (double)stack.pop();
						double a = (double)stack.pop();
						stack.push(((TernaryOperator) unit.func).operate(a, b, c));
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
							throw new CodeException("Variable " + unit.chars + " not found!", unit.bounds);
						}
					}
				}
			}
		} catch (EmptyStackException ex) {
			throw new CodeException("Function " + currentunit.toString() + " has not enough args.", currentunit.bounds);
		} catch (ClassCastException ex) {
			throw new CodeException("Type mismatch during execution of " + currentunit.toString() + ": " + ex.getMessage(), currentunit.bounds);
		}
		
		if (!stack.isEmpty())
			return stack.peek();
		else
			return Double.NaN;
	}

	public void registerDefaultMathOperators() {
		registerOperator("+", 301, Association.LEFT, (a, b) -> a+b);
		registerOperator("-", 301, Association.LEFT, (a, b) -> a-b);
		registerOperator("*", 302, Association.LEFT, (a, b) -> a*b);
		registerOperator("/", 302, Association.LEFT, (a, b) -> a/b);
		registerOperator("^", 303, Association.LEFT, (a, b) -> Math.pow(a, b));
		registerOperator("%", 302, Association.LEFT, (a, b) -> a%b);
		registerOperator("_unary_sub", 402, Association.RIGHT, a -> -a);
		registerOperator("_unary_add", 402, Association.RIGHT, a -> a);
		
		registerOperator("<", 202, Association.LEFT, (a, b) -> (a<b)? 1: 0);
		registerOperator(">", 202, Association.LEFT, (a, b) -> (a>b)? 1: 0);
		registerOperator("<=", 202, Association.LEFT, (a, b) -> (a<=b)? 1: 0);
		registerOperator(">=", 202, Association.LEFT, (a, b) -> (a>=b)? 1: 0);
		registerOperator("==", 201, Association.LEFT, (a, b) -> (a==b)? 1: 0);
		registerOperator("!=", 201, Association.LEFT, (a, b) -> (a!=b)? 1: 0);
		
		registerOperator("not", 401, Association.RIGHT, a -> (!(a>0.5))? 1: 0);
		registerOperator("and", 103, Association.LEFT, (a, b) -> (a>0.5 && b > 0.5)? 1: 0);
		registerOperator("xor", 102, Association.LEFT, (a, b) -> (a>0.5 ^ b > 0.5)? 1: 0);
		registerOperator("or", 101, Association.LEFT, (a, b) -> (a>0.5 || b > 0.5)? 1: 0);
		
		registerFunction("sqrt", a -> Math.sqrt(a));
		registerFunction("ln", a -> Math.log(a));
		registerFunction("exp", a -> Math.exp(a));
		registerFunction("sin", a -> Math.sin(a));
		registerFunction("cos", a -> Math.cos(a));
		registerFunction("tan", a -> Math.tan(a));
		registerFunction("csc", a -> 1/Math.sin(a));
		registerFunction("sec", a -> 1/Math.cos(a));
		registerFunction("cot", a -> 1/Math.tan(a));
		registerFunction("asin", a -> Math.asin(a));
		registerFunction("acos", a -> Math.acos(a));
		registerFunction("atan", a -> Math.atan(a));
		registerFunction("sinh", a -> Math.sinh(a));
		registerFunction("cosh", a -> Math.cosh(a));
		registerFunction("tanh", a -> Math.tanh(a));
		registerFunction("csch", a -> 1/Math.sinh(a));
		registerFunction("sech", a -> 1/Math.cosh(a));
		registerFunction("coth", a -> 1/Math.tanh(a));
		registerFunction("abs", a -> Math.abs(a));
		registerFunction("sign", a -> Math.signum(a));
		registerFunction("min", (a, b) -> Math.min(a, b));
		registerFunction("max", (a, b) -> Math.max(a, b));
		registerFunction("ceil", a -> Math.ceil(a));
		registerFunction("floor", a -> Math.floor(a));
		registerFunction("round", a -> Math.round(a));
		registerFunction("clamp", (a, b, c) -> ((a > c)? c : ((a < b)? b : a)));
		registerFunction("tern", (a, b, c) -> (a > 0.5)? b : c);
		registerFunction("factorial", a -> Math.sqrt(2*Math.PI*a)*Math.pow(a/Math.E, a)*(1+1/(12*a)+1/(288*a*a)));

		registerFunction("random", () -> rand.nextDouble());
		registerFunction("randomgaussian", () -> rand.nextGaussian());
		registerFunction("randomint", () -> rand.nextInt());

		registerFunction("stdout", a -> {System.out.println(a); return 0;});

		setConstant("pi", Math.PI);
		setConstant("e", Math.E);
		setConstant("true", 1);
		setConstant("false", 0);
		setConstant("inf", Double.POSITIVE_INFINITY);
		setConstant("nan", Double.NaN);
		setConstant("eulergamma", 0.57721566490153286060651209008240243104215933593992);
	}
	

	public void throwError(String message, Bounds bounds, Highlighter highlighter) {
		if (bounds != null && highlighter != null)
			highlighter.markError(bounds, highlighter.getErrorColor(), message);
		
		throw new CodeException(message, bounds);
	}
}