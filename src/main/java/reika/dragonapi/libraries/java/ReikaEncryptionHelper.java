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

import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;


public class ReikaEncryptionHelper {

	private static String[] validateParts(String[] parts) {
		if (parts == null || parts.length == 0)
			return new String[0];
		for (String part : parts) {
			if (part == null)
				throw new IllegalArgumentException("Encryption inputs cannot contain null strings");
		}
		return parts;
	}

	public static <O> boolean isEncryptedMatch(O s, Encrypter<O> e, O... parts) {
		return s.equals(e.encryptInto(parts));
	}

	public interface Encrypter<O> {

		O encryptInto(O... parts);

	}

	public static final class AdditiveEncrypter implements Encrypter<String> {

		@Override
		public String encryptInto(String... parts) {
			parts = validateParts(parts);
			if (parts.length == 0)
				return "";
			char[] chars = new char[ReikaStringParser.getLongestString(parts).length()];
			for (int i = 0; i < parts.length; i++) {
				String s = parts[i];
				for (int k = 0; k < s.length(); k++) {
					int c = s.charAt(k);
					chars[k] = (char) ReikaMathLibrary.addAndRollover(chars[k], c, 0x0020, 0x007E);
				}
			}
			return new String(chars);
		}

	}

	public static final class SpliceEncrypter implements Encrypter<String> {

		@Override
		public String encryptInto(String... parts) {
			parts = validateParts(parts);
			StringBuilder sb = new StringBuilder();
			boolean action;
			int idx = 0;
			do {
				action = false;

				for (int i = 0; i < parts.length; i++) {
					if (idx < parts[i].length()) {
						sb.append(parts[i].charAt(idx));
						action = true;
					}
				}

				idx++;

			} while (action);

			return sb.toString();
		}

	}

	public static final class MorphEncrypter implements Encrypter<String> {

		@Override
		public String encryptInto(String... parts) {
			parts = validateParts(parts);
			if (parts.length == 0)
				return "";
			char[] chars = new char[ReikaStringParser.getLongestString(parts).length()];
			java.util.Arrays.fill(chars, ' ');

			System.arraycopy(parts[0].toCharArray(), 0, chars, 0, parts[0].length());
			for (int i = 1; i < parts.length; i++) {
				String s = parts[i];
				for (int k = 0; k < s.length(); k++) {
					int c = s.charAt(k);
					int d = c - chars[k];
					if (d != 0) {
						chars[k] = (char) ReikaMathLibrary.addAndRollover(chars[k], d / 2, 0x0020, 0x007E);
					}
				}
			}

			return new String(chars);
		}

	}

	public static final class ShiftEncrypter implements Encrypter<String> {

		@Override
		public String encryptInto(String... parts) {
			parts = validateParts(parts);
			if (parts.length == 0)
				return "";
			char[] chars = new char[ReikaStringParser.getLongestString(parts).length()];
			for (int i = 0; i < parts.length; i++) {
				String s = parts[i];
				for (int k = 0; k < s.length(); k++) {
					int c = s.charAt(k);
					chars[k] = (char) ReikaMathLibrary.addAndRollover(chars[k], c, 0x0020, 0x007E);
					chars[k] = (char) ReikaMathLibrary.addAndRollover(chars[k], s.length(), 0x0020, 0x007E);
				}
			}
			return new String(chars);
		}

	}

}
