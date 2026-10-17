/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.libraries.java;

import org.apache.commons.lang3.text.WordUtils;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;

public class ReikaStringParser {

	private static final String NUMBER_VARIABLE_CODE = "PARSE_NUMBER_VARIABLE";
	private static final String NUMBER_METHOD_CODE = "PARSE_NUMBER_METHOD";
	private static final String STRING_FORMAT_CODE = "PARSE_STRING_FORMAT";
	private static final String ENUM_FUNCTION_CODE = "PARSE_ENUM";

	private static final TreeMap<Integer, String> romanNumerals = new TreeMap<>();

	private static final HashSet<Character> wordChars = new HashSet<>();

	static {
		romanNumerals.put(1000, "M");
		romanNumerals.put(900, "CM");
		romanNumerals.put(500, "D");
		romanNumerals.put(400, "CD");
		romanNumerals.put(100, "C");
		romanNumerals.put(90, "XC");
		romanNumerals.put(50, "L");
		romanNumerals.put(40, "XL");
		romanNumerals.put(10, "X");
		romanNumerals.put(9, "IX");
		romanNumerals.put(5, "V");
		romanNumerals.put(4, "IV");
		romanNumerals.put(1, "I");

		for (char c = 'a'; c <= 'z'; c++) {
			wordChars.add(c);
		}
		for (char c = 'A'; c <= 'Z'; c++) {
			wordChars.add(c);
		}
		for (char c = '0'; c <= '9'; c++) {
			wordChars.add(c);
		}
		wordChars.add('\'');
	}

	public static String getStringWithEmbeddedReferences(String sg) {
		List<String> parts = splitReferenceExpressions(sg, '+');
		StringBuilder sb = new StringBuilder();
		for (String part : parts) {
			if (isReference(part)) {
				sb.append(parseReference(part));
			} else
				sb.append(part);
		}
		return sb.toString().replaceAll("\\\\n", "\n").replaceAll("\\\\t", "\t");
	}

	private static String parseReference(String sg) {
		if (sg.startsWith(NUMBER_VARIABLE_CODE))
			return parseStaticNumberVariable(sg);
		if (sg.startsWith(NUMBER_METHOD_CODE))
			return parseNumberMethod(sg);
		if (sg.startsWith(STRING_FORMAT_CODE))
			return parseStringFormat(sg);
		if (sg.startsWith(ENUM_FUNCTION_CODE))
			return parseEnum(sg);
		return null;
	}

    private static String parseEnum(String expression) {
        String[] parts = referenceParts(expression, ENUM_FUNCTION_CODE, 2);
        try {
            Class<?> type = Class.forName(parts[0]);
            Object[] constants = type.getEnumConstants();
            if (constants == null) throw new IllegalArgumentException("Class is not an enum: " + type.getName());
            Enum<?> selected = null;
            for (Object constant : constants) {
                Enum<?> value = (Enum<?>)constant;
                if (value.name().equals(parts[1])) { selected = value; break; }
            }
            if (selected == null) throw new IllegalArgumentException("No enum constant " + type.getName() + "." + parts[1]);
            Method method = type.getDeclaredMethod(parts[2]);
            return Objects.requireNonNull(method.invoke(selected), "Enum method returned null").toString();
        } catch (ReflectiveOperationException | RuntimeException error) {
            throw referenceFailure("enum method", expression, error);
        }
    }

	/**
	 * Formats {@code template,reference,...}, optionally enclosed in
	 * {@code PARSE_STRING_FORMAT(...)}. References may contain nested formats;
	 * wrapper parentheses must balance and the first comma ends the template.
	 * Retains the legacy whitespace removal, default locale and string-valued arguments.
	 */
	public static String parseStringFormat(String sg) {
		if (sg == null || sg.isBlank())
			throw new IllegalArgumentException("Format expression cannot be empty");
		String body = sg;
		if (sg.startsWith(STRING_FORMAT_CODE)) {
			String opening = STRING_FORMAT_CODE + "(";
			if (!sg.startsWith(opening) || !sg.endsWith(")"))
				throw new IllegalArgumentException("Invalid format expression: " + sg);
			int depth = 1;
			for (int i = opening.length(); i < sg.length(); i++) {
				if (sg.charAt(i) == '(') depth++;
				else if (sg.charAt(i) == ')') depth--;
				if (depth == 0 && i != sg.length() - 1)
					throw new IllegalArgumentException("Trailing text in format expression: " + sg);
			}
			if (depth != 0) throw new IllegalArgumentException("Unbalanced format expression: " + sg);
			body = sg.substring(opening.length(), sg.length() - 1);
		}
		// The first comma separates the template; only argument expressions can nest.
		int separator = body.indexOf(',');
		String template = separator < 0 ? body : body.substring(0, separator);
		List<String> parts = separator < 0 ? List.of() : splitReferenceExpressions(body.substring(separator + 1), ',');
		String str = template.replaceAll("\\s", "");
		Object[] args = new Object[parts.size()];
		for (int i = 0; i < parts.size(); i++) {
			String reference = parts.get(i).replaceAll("\\s", "");
			if (reference.isEmpty())
				throw new IllegalArgumentException("Empty reference at argument " + (i + 1) + " in " + sg);
			if (!isReference(reference))
				throw new IllegalArgumentException("Invalid reference at argument " + (i + 1) + " in " + sg);
			args[i] = parseReference(reference);
		}
		try {
			return String.format(str, args);
		} catch (IllegalFormatException e) {
			throw new IllegalArgumentException("Invalid format expression: " + sg, e);
		}
	}

    /** Separators inside reference parentheses belong to that reference, not its caller. */
    private static List<String> splitReferenceExpressions(String expression, char separator) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        int depth = 0;
        boolean reference = isReference(separator == ',' ? expression.stripLeading() : expression);
        for (int i = 0; i < expression.length(); i++) {
            char character = expression.charAt(i);
            if (reference && character == '(') depth++;
            else if (reference && character == ')') {
                if (--depth < 0) throw new IllegalArgumentException("Unbalanced reference expression: " + expression);
            } else if (character == separator && depth == 0) {
                parts.add(expression.substring(start, i));
                start = i + 1;
                String remaining = expression.substring(start);
                reference = isReference(separator == ',' ? remaining.stripLeading() : remaining);
            }
        }
        if (depth != 0) throw new IllegalArgumentException("Unbalanced reference expression: " + expression);
        parts.add(expression.substring(start));
        return parts;
    }

    private static String parseNumberMethod(String expression) {
        String[] parts = referenceParts(expression, NUMBER_METHOD_CODE, 1);
        try {
            Class<?> type = Class.forName(parts[0]);
            Method method = type.getMethod(parts[1]);
            if (!Modifier.isStatic(method.getModifiers())) throw new IllegalArgumentException("Referenced method must be static");
            return Objects.requireNonNull(method.invoke(null), "Static method returned null").toString();
        } catch (ReflectiveOperationException | RuntimeException error) {
            throw referenceFailure("static method", expression, error);
        }
    }

    private static String parseStaticNumberVariable(String expression) {
        String[] parts = referenceParts(expression, NUMBER_VARIABLE_CODE, 1);
        try {
            Class<?> type = Class.forName(parts[0]);
            Field field = type.getField(parts[1]);
            Class<?> fieldType = field.getType();
            // Keep the original supported numeric types and unsupported-type result.
            if (fieldType != int.class && fieldType != float.class && fieldType != double.class && fieldType != long.class)
                return "NOT FOUND";
            if (!Modifier.isStatic(field.getModifiers())) throw new IllegalArgumentException("Referenced numeric field must be static");
            return field.get(null).toString();
        } catch (ReflectiveOperationException | RuntimeException error) {
            throw referenceFailure("static numeric field", expression, error);
        }
    }

    /** Split member separators from the right so binary nested-class names remain intact. */
    private static String[] referenceParts(String expression, String marker, int members) {
        String opening = marker + "(";
        if (!expression.startsWith(opening) || !expression.endsWith(")"))
            throw new IllegalArgumentException("Invalid " + marker + " reference: " + expression);
        String body = expression.substring(opening.length(), expression.length() - 1);
        String[] parts = new String[members + 1];
        int end = body.length();
        for (int i = members; i > 0; i--) {
            int separator = body.lastIndexOf('$', end - 1);
            if (separator < 0 || separator + 1 == end)
                throw new IllegalArgumentException("Missing member in " + marker + " reference: " + expression);
            parts[i] = body.substring(separator + 1, end);
            end = separator;
        }
        if (end == 0) throw new IllegalArgumentException("Missing class in " + marker + " reference: " + expression);
        parts[0] = body.substring(0, end);
        return parts;
    }

    /** Attach context once; callers receive the real invoked failure rather than a discarded wrapper. */
    private static IllegalArgumentException referenceFailure(String kind, String expression, Exception error) {
        Throwable cause = error instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause() : error;
        if (cause instanceof Error fatal) throw fatal;
        return new IllegalArgumentException("Could not resolve " + kind + " reference: " + expression, cause);
    }

	private static boolean isReference(String sg) {
		if (sg.startsWith(NUMBER_VARIABLE_CODE))
			return true;
		if (sg.startsWith(NUMBER_METHOD_CODE))
			return true;
		if (sg.startsWith(STRING_FORMAT_CODE))
			return true;
		return sg.startsWith(ENUM_FUNCTION_CODE);
	}

	public static String splitCamelCase(String s) {
		s = s.replaceAll(" ", "");
		String cap = String.format("%s|%s|%s", "(?<=[A-Z])(?=[A-Z][a-z])", "(?<=[^A-Z])(?=[A-Z])", "(?<=[A-Za-z])(?=[^A-Za-z])");
		String spl = s.replaceAll(cap, " ");
		return spl;
	}

	public static String stripSpaces(String s) {
		return s.replaceAll("\\s", "");
	}

	public static String capFirstChar(String s) {
		if (s == null || s.isEmpty())
			return s;
		return s.toUpperCase().charAt(0) + s.toLowerCase().substring(1);
	}

	public static String subtractFrom(String src, String p) {
		int len = p.length();
		return src.substring(len);
	}

	public static List<String> splitStringByLength(String str, int len) {
		return ReikaStringWrapper.listFormattedStringToWidth(str, len);
		/*
		List<String> li = new ArrayList<>();
		if (str == null || str.isEmpty())
			return li;
		if (str.length() <= len) {
			li.add(str);
			return li;
		}
		//char[] data = str.toCharArray();
		StringTokenizer tok = new StringTokenizer(str, "\\. | , | | -", true);

		String[] parts = new String[tok.countTokens()];

		for (int i = 0; i < parts.length; i++) {
			parts[i] = tok.nextToken();
		}

		//String[] parts = str.split("\\. | , | | -");
		//ReikaJavaLibrary.pConsole(Arrays.toString(parts));

		int space = len;
		StringBuilder sb = new StringBuilder();

		for (int i = 0; i < parts.length; i++) {
			String s = parts[i];
			String ms = s.replaceAll("\u00a7", "");//.replaceAll(" ", "");
			int reduct = ms.length()-(s.length()-ms.length());
			//ReikaJavaLibrary.pConsole(s+" : "+(len-space)+"+"+ms.length()+"/"+len);
			if (s.length() <= space) {
				space -= reduct;
				sb.append(s);
			}
			else {
				String line = sb.toString();
				if (line.length() > len) {
					String s1 = line.substring(0, len);
					li.add(s1);
					String s2 = line.substring(len);
					li.add(s2);
				}
				else
					li.add(line);
				sb.delete(0, sb.length());
				if (!" ".equals(s)) {
					sb.append(s);
					space = len-reduct;
				}
				else
					space = len;
			}
		}
		li.add(sb.toString());

		/*
		//ReikaJavaLibrary.pConsole(Arrays.toString(data));
		StringBuilder line = new StringBuilder();
		StringBuilder word = new StringBuilder();
		String formatter = "";
		for (int i = 0; i < data.length; i++) {
			char c = data[i];
			if (c == 0) {

			}
			if ("\u00a7".charAt(0) == c) {
				formatter = String.valueOf(c)+String.valueOf(data[i+1]);
				if ("\u00a7".charAt(0) == data[i+1])
					data[i+1] = 0;
			}
			if (c == ' ') {
				if (data[i+1] == ' ')
					data[i+1 ] = 0;
			}
			if (isSplittingChar(c)) {
				word.append(c);
				int a = Character.isWhitespace(c) ? 1 : 0;
				if ((line.length() + word.length()-a-formatter.length()) > len) {
					li.add(line.toString());
					line.delete(0, line.length());
				}
				line.append(formatter);
				line.append(word);
				word.delete(0, word.length());
			}
			else {
				// add it to the word and move on
				word.append(c);
			}
		}

		if (word.length() > 0) {
			if ((line.length() + word.length()) > len) {
				li.add(line.toString());
				line.delete(0, line.length());
			}

			line.append(word);
		}

		// handle extra line
		if (line.length() > 0) {
			li.add(line.toString());
		}*//*

		return li;*/
	}

	public static boolean isWordSplittingChar(char c) {
		return !wordChars.contains(c);
	}

	public static String getFirstWord(String s) {
		if (s == null || s.isEmpty())
			return s;
		return s.split(" ")[0];
	}

	public static boolean containsWord(String s, String word) {
		if (s == null || s.length() < word.length())
			return false;
		//return s.matches(".*\\b" + Pattern.quote(word) + "\\b.*"); //does not treat '
		int len = word.length();
		if (s.regionMatches(0, word, 0, len)) {
			if (s.length() == word.length() || isWordSplittingChar(s.charAt(len)))
				return true;
		}
		for (int i = 0; i < s.length() - len; i++) {
			if (isWordSplittingChar(s.charAt(i))) {
				if (s.regionMatches(i + 1, word, 0, len)) {
					if (word.length() + i + 1 == s.length() || isWordSplittingChar(s.charAt(i + 1 + len)))
						return true;
				}
			}
		}
		return false;
	}

	public static String getLongestString(String[] sgs) {
		if (sgs == null || sgs.length == 0)
			throw new IllegalArgumentException("Cannot select the longest string from an empty collection");
		int idx = 0;
		for (int i = 0; i < sgs.length; i++) {
			String sg = sgs[i];
			if (sg == null)
				throw new IllegalArgumentException("String collection cannot contain null entries");
			if (sg.length() > sgs[idx].length())
				idx = i;
		}
		return sgs[idx];
	}

	public static String getDelimited(String delimiter, ArrayList<Object> args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.size(); i++) {
			sb.append(args.get(i).toString());
			if (i < args.size() - 1)
				sb.append(delimiter);
		}
		return sb.toString();
	}

	public static String getDelimited(String delimiter, Object... args) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < args.length; i++) {
			sb.append(args[i].toString());
			if (i < args.length - 1)
				sb.append(delimiter);
		}
		return sb.toString();
	}

	public static String getNOf(String s, int n) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < n; i++) {
			sb.append(s);
		}
		return sb.toString();
	}

	public static String getAutoDecimal(double n) {
		String s = Double.toString(n);
		/*
		String[] parts = s.split("\\.");
		while (parts[1].charAt(0) == '0') {
			parts[1] = parts[1].substring(1);
		}
		while (parts[1].charAt(parts[1].length()-1) == '0') {
			parts[1] = parts[1].substring(0, parts[1].length()-1);
		}
		return parts[0]+"."+parts[1];
		 */
		while (s.indexOf('.') != -1 && (s.charAt(s.length() - 1) == '0' || s.charAt(s.length() - 1) == '.'))
			s = s.substring(0, s.length() - 1);
		return s.isEmpty() ? "0" : s;
	}

	public static ArrayList<String> splitStringByNewlines(String s) {
		ArrayList<String> li = new ArrayList<>();
		String[] parts = s.split("\\n");
		Collections.addAll(li, parts);
		return li;
	}

	public static char intToAlphaChar(int n) {
		return (char) ('a' + n);
	}

	public static String clipStringBefore(String s, String clip) {
		int idx = s.indexOf(clip);
		if (idx == -1)
			idx = s.length();
		return s.substring(0, idx);
	}

	public static boolean isWholeWord(String s, String lv) {
		if (s == null || lv == null || s.isEmpty())
			return false;
		int idx1 = lv.indexOf(s);
		if (idx1 < 0)
			return false;
		int idx2 = idx1 + s.length();
		return (idx1 == 0 || isWordSeparator(lv.charAt(idx1 - 1))) && (idx2 == lv.length() || isWordSeparator(lv.charAt(idx2)));
	}

	public static boolean isWordSeparator(char c) {
		return c == ' ' || c == '.' || c == '!' || c == '?' || c == ',' || c == ';';
	}

	public static String padToLength(String s, int len, String pad) {
		while (s.length() < len)
			s = s + pad;
		return s;
	}

	public static String capitalizeWords(String s) {
		s = capFirstChar(s);
		return WordUtils.capitalize(s);
	}

	public static String parseRomanRumeral(int val) {
		int digit = romanNumerals.floorKey(val);
		if (val == digit)
			return romanNumerals.get(val);
		return romanNumerals.get(digit) + parseRomanRumeral(val - digit);
	}

	public static String stripToAlphabet(String s, char repl) {
		char[] arr = s.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			if (!Character.isLetter(c)) {
				arr[i] = repl;
			}
		}
		return new String(arr);
	}

	public static String stripToAlphaNumeric(String s, char repl) {
		char[] arr = s.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			if (!Character.isLetterOrDigit(c)) {
				arr[i] = repl;
			}
		}
		return new String(arr);
	}

	public static String clearDoubledSpaces(String s) {
		while (s.contains("  ")) {
			s = s.replace("  ", " ");
		}
		return s;
	}

	public static String shuffle(String s) {
		char[] arr = s.toCharArray();
		ReikaArrayHelper.shuffleArray(arr);
		return new String(arr);
	}

	public static boolean isValidVariableName(String s) {
		if (s == null || s.isEmpty())
			return false;
		if (!Character.isLetter(s.charAt(0)))
			return false;
		char[] arr = s.toCharArray();
		for (int i = 0; i < arr.length; i++) {
			char c = arr[i];
			if (c != '_' && !Character.isLetterOrDigit(c)) {
				return false;
			}
		}
		return true;
	}
}
