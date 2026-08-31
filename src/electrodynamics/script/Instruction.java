package electrodynamics.script;

import java.util.ArrayList;
import java.util.List;

public abstract class Instruction {
	public int pos;
	public boolean suppress_output = false;
	public Bounds bounds = null;
	abstract int execute(State state, Evaluator evaluator);
};

class If extends Instruction {
	List<Unit> condition;
	int end_pos = -2;
	int else_pos = -2;
	
	public If(List<Token> tokens, Evaluator evaluator, Highlighter highlighter) {
		List<Token> sub_tokens = new ArrayList<Token>();
		for (int i = 1; i < tokens.size(); i++) {
			if (tokens.get(i).chars.equals(";"))
				suppress_output = true;
			else
				sub_tokens.add(tokens.get(i));
		}
		
		condition = evaluator.parse(sub_tokens, highlighter);
	}
	
	@Override
	int execute(State state, Evaluator evaluator) {
		state.suppress_output = suppress_output;
		Object result = evaluator.eval(condition, state.variables);
		if (!(result instanceof Double))
			throw new CodeException("If: Number expected, got " + result.toString() + " instead.", bounds);
		
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

class While extends Instruction {
	List<Unit> condition;
	int end_pos = -2;

	public While(List<Token> tokens, Evaluator evaluator, Highlighter highlighter) {
		List<Token> sub_tokens = new ArrayList<Token>();
		for (int i = 1; i < tokens.size(); i++) {
			if (tokens.get(i).chars.equals(";"))
				suppress_output = true;
			else
				sub_tokens.add(tokens.get(i));
		}
		
		condition = evaluator.parse(sub_tokens, highlighter);
		Debugger.print(condition);
	}
	
	@Override
	int execute(State state, Evaluator evaluator) {
		state.suppress_output = suppress_output;
		Object result = evaluator.eval(condition, state.variables);
		if (!(result instanceof Double))
			throw new CodeException("While: Number expected, got " + result.toString() + " instead.", bounds);
		
		if ((double) result > 0.5)
			return pos+1;
		else
			return end_pos+1;
	}
}

class Else extends Instruction {
	int end_pos = -2;
	
	@Override
	int execute(State state, Evaluator evaluator) {
		return end_pos+1;
	}
}

class End extends Instruction {
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

class Assign extends Instruction {
	String var;
	List<Unit> expression;

	public Assign(List<Token> tokens, Evaluator evaluator, Highlighter highlighter) {
		var = tokens.get(0).chars;
		
		if (evaluator.isReserved(var)) {
			evaluator.throwError("Error: Reserved variable name: " + var, tokens.get(0).bounds, highlighter);
		}
		
		List<Token> sub_tokens = new ArrayList<Token>();
		for (int i = 2; i < tokens.size(); i++) {
			if (tokens.get(i).chars.equals(";"))
				suppress_output = true;
			else
				sub_tokens.add(tokens.get(i));
		}
		
		expression = evaluator.parse(sub_tokens, highlighter);
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

class ExecuteFunction extends Instruction {
	List<Unit> function;

	public ExecuteFunction(List<Token> tokens, Evaluator evaluator, Highlighter highlighter) {
		List<Token> sub_tokens = new ArrayList<Token>();
		for (int i = 0; i < tokens.size(); i++) {
			if (tokens.get(i).chars.equals(";"))
				suppress_output = true;
			else
				sub_tokens.add(tokens.get(i));
		}
		
		function = evaluator.parse(sub_tokens, highlighter);
	}
	
	@Override
	int execute(State state, Evaluator evaluator) {
		state.suppress_output = suppress_output;
		evaluator.eval(function, state.variables);
		return pos+1;
	}
}