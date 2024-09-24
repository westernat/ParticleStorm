package org.mesdag.particlestorm.data.molang.compiler;

import org.mesdag.particlestorm.data.molang.compiler.value.Variable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class MolangQueries {
	private static final Map<String, Variable> QUERIES = new ConcurrentHashMap<>();

	static {
		setDefaultQueryValues();
	}

	public static boolean isExistingVariable(String name) {
		return QUERIES.containsKey(name);
	}

	static void registerVariable(String name, Variable variable) {
		QUERIES.put(name, variable);
	}

	static Variable getQueryFor(String name) {
		return QUERIES.computeIfAbsent(applyPrefixAliases(name, "query.", "q."), key -> new Variable(key, 0));
	}

	/**
	 * Parse a given string formatted with a prefix, swapping out any potential aliases for the defined proper name
	 *
	 * @param text The base text to parse
	 * @param properName The "correct" prefix to apply
	 * @param aliases The available prefixes to check and replace
	 * @return The unaliased string, or the original string if no aliases match
	 */
	public static String applyPrefixAliases(String text, String properName, String... aliases) {
		for (String alias : aliases) {
			if (text.startsWith(alias))
				return properName + text.substring(alias.length());
		}

		return text;
	}
	private static void setDefaultQueryValues() {
		getQueryFor("PI").set(Math.PI);
		getQueryFor("E").set(Math.E);
		getQueryFor("game_time").set(p -> p.level().getGameTime());
	}
}
