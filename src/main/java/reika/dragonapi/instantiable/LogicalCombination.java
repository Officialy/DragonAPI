package reika.dragonapi.instantiable;

import reika.dragonapi.libraries.java.ReikaStringParser;
import reika.dragonapi.libraries.logic.LogicalOperators;

import java.util.ArrayList;
import java.util.function.Function;

public class LogicalCombination<E> implements Function<E, Boolean> {

	public final LogicalOperators rule;
	private final ArrayList<Function<E, Boolean>> params = new ArrayList<>();

	public LogicalCombination(LogicalOperators lc) {
		rule = lc;
	}

	public void addArgument(Function<E, Boolean> arg) {
		params.add(arg);
	}

	public Boolean apply(E val) {
		boolean[] arr = new boolean[params.size()];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = params.get(i).apply(val);
		}
		return rule.evaluate(arr);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		ArrayList<String> li = this.writeToStrings();
		for (String s : li) {
			sb.append(s);
			sb.append("\n");
		}
		return sb.toString();
	}

	public ArrayList<String> writeToStrings() {
		return this.writeToStrings(1);
	}

	private ArrayList<String> writeToStrings(int indent) {
		ArrayList<String> li = new ArrayList<>();
		String pre = ReikaStringParser.getNOf("\t", indent);
		if (indent == 1)
			li.add("{");
		for (Function<E, Boolean> c : params) {
			if (c instanceof LogicalCombination lg) {
				//s = lg.rule+" = {";
				li.add(pre + lg.rule.toString() + " = {");
				li.addAll(lg.writeToStrings(indent + 1));
				li.add(pre + "}");
			} else {
				li.add(pre + c.toString());
			}
		}
		if (indent == 1)
			li.add("}");
		return li;
	}

	public interface EvaluatorConstructor<E> {

		Function<E, Boolean> create(String s);

	}

}
