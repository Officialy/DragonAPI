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

import reika.dragonapi.DragonAPI;
import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.libraries.mathsci.ReikaMathLibrary;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.chunk.ChunkAccess;


import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public final class ReikaArrayHelper {

	public static int countTrue(boolean[] arr) {
		int c = 0;
		for (int i = 0; i < arr.length; i++) {
			if (arr[i])
				c++;
		}
		return c;
	}

	public static boolean containsTrue(boolean[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i])
				return true;
		}
		return false;
	}

	//TODO Condense functions into accept-all-primitive designs

	/**
	 * Returns the sum of all values in an array. Args: Array
	 */
	public static int sumArray(int[] arr) {
		int sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i];
		}
		return sum;
	}

	/**
	 * Returns the sum of all values in an array. Args: Array
	 */
	public static double sumArray(double[] arr) {
		double sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i];
		}
		return sum;
	}

	/**
	 * Returns the sum of all values in an array. Args: Array
	 */
	public static float sumArray(float[] arr) {
		float sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i];
		}
		return sum;
	}

	/**
	 * Returns the product of all values in an array. Args: Array
	 */
	public static int productArray(int[] arr) {
		int product = 1;
		for (int i = 0; i < arr.length; i++) {
			product *= arr[i];
		}
		return product;
	}

	/**
	 * Returns the product of all values in an array. Args: Array
	 */
	public static double productArray(double[] arr) {
		double product = 1;
		for (int i = 0; i < arr.length; i++) {
			product *= arr[i];
		}
		return product;
	}

	/**
	 * Returns the product of all values in an array. Args: Array
	 */
	public static float productArray(float[] arr) {
		float product = 1;
		for (int i = 0; i < arr.length; i++) {
			product *= arr[i];
		}
		return product;
	}

	/**
	 * Fills an array with the specified value and returns the array.
	 * Args: array, value
	 */
	public static double[] fillArray(double[] arr, double val) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = val;
		return arr;
	}

	/**
	 * Fills an array with the specified value and returns the array.
	 * Args: array, value
	 */
	public static int[] fillArray(int[] arr, int val) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = val;
		return arr;
	}

	/**
	 * Fills an array with the specified value and returns the array.
	 * Args: array, value
	 */
	public static boolean[] fillArray(boolean[] arr, boolean val) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = val;
		return arr;
	}

	/**
	 * Fills an array with the specified value and returns the array.
	 * Args: array, value
	 */
	public static String[] fillArray(String[] arr, String val) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = val;
		return arr;
	}

	/**
	 * Fills an array with the specified value and returns the array.
	 * Args: array, value
	 */
	public static ItemStack[] fillArray(ItemStack[] arr, ItemStack val) {
		for (int i = 0; i < arr.length; i++)
			arr[i] = val;
		return arr;
	}

	/**
	 * Fills a matrix with the specified value and returns the array.
	 * Args: Array, value
	 */
	public static int[][] fillMatrix(int[][] mat, int val) {
		for (int i = 0; i < mat.length; i++)
			mat[i] = ReikaArrayHelper.fillArray(mat[i], val);
		return mat;
	}

	/**
	 * Fills a matrix with the specified value and returns the array.
	 * Args: Array, value
	 */
	public static boolean[][] fillMatrix(boolean[][] mat, boolean val) {
		for (int i = 0; i < mat.length; i++)
			mat[i] = ReikaArrayHelper.fillArray(mat[i], val);
		return mat;
	}

	/**
	 * Fills a matrix with the specified value and returns the array.
	 * Args: Array, value
	 */
	public static double[][] fillMatrix(double[][] mat, double val) {
		for (int i = 0; i < mat.length; i++)
			mat[i] = ReikaArrayHelper.fillArray(mat[i], val);
		return mat;
	}

	/**
	 * Fills a matrix with the specified value and returns the array.
	 * Args: Array, value
	 */
	public static String[][] fillMatrix(String[][] mat, String val) {
		for (int i = 0; i < mat.length; i++)
			mat[i] = ReikaArrayHelper.fillArray(mat[i], val);
		return mat;
	}

	/**
	 * Fills a matrix with the specified value and returns the array.
	 * Args: Array, value
	 */
	public static ItemStack[][] fillMatrix(ItemStack[][] mat, ItemStack val) {
		for (int i = 0; i < mat.length; i++)
			mat[i] = ReikaArrayHelper.fillArray(mat[i], val);
		return mat;
	}

	/**
	 * Rotates a square matrix 90 degrees clockwise and returns it. Args: Matrix
	 */
	public static int[][] rotateMatrix(int[][] mat) {
		if (mat.length == 0)
			return new int[0][0];
		int[][] temp = new int[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				temp[j][mat.length-i-1] = mat[i][j];
		return temp;
	}

	/**
	 * Rotates a square matrix 90 degrees counterclockwise and returns it. Args: Matrix
	 */
	public static int[][] rotateMatrixM90(int[][] mat) {
		int[][] temp = transposeMatrix(mat);
		temp = reverseColumns(temp);
		return temp;
	}

	public static int[][] reverseColumns(int[][] mat) {
		int[][] temp = new int[mat.length][];
		for (int i = 0; i < mat.length; i++) {
			temp[i] = new int[mat[i].length];
			for (int j = 0; j < mat[i].length; j++)
				temp[i][mat[i].length-1-j] = mat[i][j];
		}
		return temp;
	}

	/**
	 * Rotates a square matrix 90 degrees clockwise and returns it. Args: Matrix
	 */
	public static boolean[][] rotateMatrix(boolean[][] mat) {
		if (mat.length == 0)
			return new boolean[0][0];
		boolean[][] temp = new boolean[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				temp[j][mat.length-i-1] = mat[i][j];
		return temp;
	}

	/**
	 * Transposes a 2D matrix and returns it. Args: Matrix
	 */
	public static int[][] transposeMatrix(int[][] mat) {
		if (mat.length == 0)
			return new int[0][0];
		int[][] arr = new int[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				arr[j][i] = mat[i][j];
		return arr;
	}

	/**
	 * Transposes a 2D matrix and returns it. Args: Matrix
	 */
	public static boolean[][] transposeMatrix(boolean[][] mat) {
		if (mat.length == 0)
			return new boolean[0][0];
		boolean[][] arr = new boolean[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				arr[j][i] = mat[i][j];
		return arr;
	}

	/**
	 * Rotates a square matrix 90 degrees clockwise and returns it. Args: Matrix
	 */
	public static double[][] rotateMatrix(double[][] mat) {
		if (mat.length == 0)
			return new double[0][0];
		double[][] temp = new double[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				temp[j][mat.length-i-1] = mat[i][j];
		return temp;
	}

	/**
	 * Transposes a 2D matrix and returns it. Args: Matrix
	 */
	public static double[][] transposeMatrix(double[][] mat) {
		if (mat.length == 0)
			return new double[0][0];
		double[][] arr = new double[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				arr[j][i] = mat[i][j];
		return arr;
	}

	/**
	 * Rotates a square matrix 90 degrees clockwise and returns it. Args: Matrix
	 */
	public static String[][] rotateMatrix(String[][] mat) {
		if (mat.length == 0)
			return new String[0][0];
		String[][] temp = new String[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				temp[j][mat.length-i-1] = mat[i][j];
		return temp;
	}

	/**
	 * Transposes a 2D matrix and returns it. Args: Matrix
	 */
	public static String[][] transposeMatrix(String[][] mat) {
		if (mat.length == 0)
			return new String[0][0];
		String[][] arr = new String[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				arr[j][i] = mat[i][j];
		return arr;
	}

	/**
	 * Rotates a square matrix 90 degrees clockwise and returns it. Args: Matrix
	 */
	public static ItemStack[][] rotateMatrix(ItemStack[][] mat) {
		if (mat.length == 0)
			return new ItemStack[0][0];
		ItemStack[][] temp = new ItemStack[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				temp[j][mat.length-i-1] = mat[i][j];
		return temp;
	}

	/**
	 * Transposes a 2D matrix and returns it. Args: Matrix
	 */
	public static ItemStack[][] transposeMatrix(ItemStack[][] mat) {
		if (mat.length == 0)
			return new ItemStack[0][0];
		ItemStack[][] arr = new ItemStack[mat[0].length][mat.length];
		for (int i = 0; i < mat.length; i++)
			for (int j = 0; j < mat[i].length; j++)
				arr[j][i] = mat[i][j];
		return arr;
	}

	/**
	 * Returns true if all nonzero values in the array are equal. Args: Array
	 */
	public static boolean allNonZerosEqual(long[] powers) {
		long expected = 0;
		for (long value : powers) {
			if (value == 0)
				continue;
			if (expected == 0)
				expected = value;
			else if (value != expected)
				return false;
		}
		return true;
	}

	/**
	 * Returns true if all values in the array are equal. Args: Array
	 */
	public static boolean allEqual(int[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[0] != arr[i])
				return false;
		}
		return true;
	}

	public static void shuffleArray(char[] a) {
		for (int i = a.length-1; i > 0; i--) {
			int lo = DragonAPI.rand.nextInt(i+1);
			char buffer = a[lo];
			a[lo] = a[i];
			a[i] = buffer;
		}
	}

	public static void shuffleArray(int[] a) {
		for (int i = a.length-1; i > 0; i--) {
			int lo = DragonAPI.rand.nextInt(i+1);
			int buffer = a[lo];
			a[lo] = a[i];
			a[i] = buffer;
		}
	}

	public static void shuffleArray(double[] a) {
		for (int i = a.length-1; i > 0; i--) {
			int lo = DragonAPI.rand.nextInt(i+1);
			double buffer = a[lo];
			a[lo] = a[i];
			a[i] = buffer;
		}
	}

	public static void shuffleArray(Object[] a) {
		shuffleArray(a, DragonAPI.rand);
	}

	public static void shuffleArray(Object[] a, Random r) {
		for (int i = a.length-1; i > 0; i--) {
			int lo = r.nextInt(i+1);
			Object buffer = a[lo];
			a[lo] = a[i];
			a[i] = buffer;
		}
	}

	public static long sumArray(long[] arr) {
		long sum = 0;
		for (int i = 0; i < arr.length; i++) {
			sum += arr[i];
		}
		return sum;
	}

	public static boolean contains(int[] arr, int val) {
		if (arr == null)
			return false;
		for (int i = 0; i < arr.length; i++) {
			if (val == arr[i])
				return true;
		}
		return false;
	}

	public static boolean contains(Object[] arr, Object val) {
		if (arr == null)
			return false;
		for (int i = 0; i < arr.length; i++) {
			if (java.util.Objects.equals(val, arr[i]))
				return true;
		}
		return false;
	}

	public static int[] getLinearArray(int size) {
		return getLinearArray(0, size - 1);
	}

	public static int[] getLinearArray(int from, int to) {
		int[] n = new int[to - from + 1];
		for (int i = from; i <= to; i++)
			n[i] = i;
		return n;
	}

	public static int[] getLinearArrayExceptFor(int size, int... vals) {
		if (size < 0)
			throw new IllegalArgumentException("Array size cannot be negative");
		boolean[] excluded = new boolean[size];
		int excludedCount = 0;
		if (vals != null) {
			for (int value : vals) {
				if (value < 0 || value >= size)
					throw new IllegalArgumentException("Excluded index out of bounds: " + value);
				if (!excluded[value]) {
					excluded[value] = true;
					excludedCount++;
				}
			}
		}
		int[] n = new int[size-excludedCount];
		int index = 0;
		for (int value = 0; value < size; value++) {
			if (!excluded[value])
				n[index++] = value;
		}
		return n;
	}

	public static int[] intListToArray(List<Integer> li) {
		int[] a = new int[li.size()];
		for (int i = 0; i < a.length; i++) {
			a[i] = li.get(i);
		}
		return a;
	}

	public static int[] getArrayOf(int val, int length) {
		int[] data = new int[length];
		for (int i = 0; i < length; i++) {
			data[i] = val;
		}
		return data;
	}

	public static boolean isAllTrue(boolean[] arr) {
		for (int i = 0; i < arr.length; i++) {
			if (!arr[i])
				return false;
		}
		return true;
	}

	public static boolean arrayContains(String[] arr, String sg, boolean ignoreCase) {
		for (int i = 0; i < arr.length; i++) {
			if (ignoreCase) {
				if (sg.equalsIgnoreCase(arr[i]))
					return true;
			} else {
				if (sg.equals(arr[i]))
					return true;
			}
		}
		return false;
	}

	public static int booleanToBitflags(boolean[] flags) {
		if (flags.length > 31)
			throw new IllegalArgumentException("You cannot store more than 31 bits on an int!");
		int n = 0;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i])
				n += (1 << i);
		}
		return n;
	}

	public static boolean[] booleanFromBitflags(int bitflags, int len) {
		if (len > 31)
			throw new IllegalArgumentException("You cannot store more than 31 bits on an int!");
		boolean[] arr = new boolean[len];
		for (int i = 0; i < len; i++) {
			int n = (1 << i);
			boolean flag = (bitflags & n) != 0;
			arr[i] = flag;
		}
		return arr;
	}

	public static byte booleanToByteBitflags(boolean[] flags) {
		if (flags.length > 8)
			throw new IllegalArgumentException("You cannot store more than 8 bits on a byte!");
		byte n = 0;
		for (int i = 0; i < flags.length; i++) {
			if (flags[i])
				n += (1 << i);
		}
		return n;
	}

	public static boolean[] booleanFromByteBitflags(byte bitflags, int len) {
		if (len > 8)
			throw new IllegalArgumentException("You cannot store more than 8 bits on a byte!");
		boolean[] arr = new boolean[len];
		for (int i = 0; i < len; i++) {
			int n = (1 << i);
			boolean flag = (bitflags & n) != 0;
			arr[i] = flag;
		}
		return arr;
	}

	public static int getIndexOfLargest(int[] arr) {
		int idx = 0;
		for (int i = 1; i < arr.length; i++) {
			int n = arr[i];
			if (n > arr[idx])
				idx = i;
		}
		return idx;
	}

	public static boolean[] getTrueArray(int n) {
		boolean[] arr = new boolean[n];
		for (int i = 0; i < arr.length; i++) {
			arr[i] = true;
		}
		return arr;
	}

	public static void cycleArray(int[] arr, int newVal) {
		for (int i = arr.length - 1; i > 0; i--) {
			arr[i] = arr[i - 1];
		}
		arr[0] = newVal;
	}

	public static void cycleArray(double[] arr, double newVal) {
		for (int i = arr.length - 1; i > 0; i--) {
			arr[i] = arr[i - 1];
		}
		arr[0] = newVal;
	}

	public static void cycleArray(float[] arr, float newVal) {
		for (int i = arr.length - 1; i > 0; i--) {
			arr[i] = arr[i - 1];
		}
		arr[0] = newVal;
	}

	public static void cycleArray(boolean[] arr, boolean newVal) {
		for (int i = arr.length - 1; i > 0; i--) {
			arr[i] = arr[i - 1];
		}
		arr[0] = newVal;
	}

	public static <A> void cycleArray(A[] arr, A newVal) {
		for (int i = arr.length - 1; i > 0; i--) {
			arr[i] = arr[i - 1];
		}
		arr[0] = newVal;
	}

	public static void cycleArrayReverse(boolean[] arr, boolean newVal) {
		for (int i = 0; i < arr.length - 1; i++) {
			arr[i] = arr[i + 1];
		}
		arr[arr.length - 1] = newVal;
	}

	public static <A> void cycleArrayReverse(A[] arr, A newVal) {
		for (int i = 0; i < arr.length - 1; i++) {
			arr[i] = arr[i + 1];
		}
		arr[arr.length - 1] = newVal;
	}

	public static <A> A[] getArrayOf(A val, int length) {
		A[] arr = (A[]) Array.newInstance(val.getClass(), length);
		for (int i = 0; i < length; i++) {
			arr[i] = val;
		}
		return arr;
	}

	public static double[] averageArrays(double[] a1, double[] a2) {
		if (a1.length != a2.length)
			throw new MisuseException("You cannot average arrays of different lengths!");
		double[] ret = new double[a1.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = (a1[i] + a2[i]) / 2D;
		}
		return ret;
	}

	public static double[] onesComplementArray(double[] arr) {
		double[] ret = new double[arr.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = 1 - arr[i];
		}
		return ret;
	}

	public static boolean isSquare(Object[][] arr) {
		int l = arr.length;
		for (int i = 0; i < l; i++) {
			if (arr[i].length != l)
				return false;
		}
		return true;
	}

	public static double[][] splitSquareArray(double[] arr) {
		if (!ReikaMathLibrary.isPerfectSquare(arr.length))
			throw new MisuseException("You can only split square arrays!");
		int d = (int) Math.sqrt(arr.length);
		double[][] ret = new double[d][d];
		for (int i = 0; i < d; i++) {
			for (int k = 0; k < d; k++) {
				int idx = i * d + k;
				double val = arr[idx];
				ret[i][k] = val;
			}
		}
		return ret;
	}

	public static double getMinValue(double[] arr) {
		if (arr == null || arr.length == 0)
			throw new IllegalArgumentException("Cannot find a minimum in an empty array");
		double val = arr[0];
		for (int i = 1; i < arr.length; i++) {
			if (arr[i] < val)
				val = arr[i];
		}
		return val;
	}

	public static double getMaxValue(double[] arr) {
		if (arr == null || arr.length == 0)
			throw new IllegalArgumentException("Cannot find a maximum in an empty array");
		double val = arr[0];
		for (int i = 1; i < arr.length; i++) {
			if (arr[i] > val)
				val = arr[i];
		}
		return val;
	}

	public static int getMaxValue(int[] arr) {
		if (arr == null || arr.length == 0)
			throw new IllegalArgumentException("Cannot find a maximum in an empty array");
		int val = arr[0];
		for (int i = 1; i < arr.length; i++) {
			if (arr[i] > val)
				val = arr[i];
		}
		return val;
	}

	public static BlockPos[] chunkCoordsToBlockCoords(ChunkAccess[] pos) {
		BlockPos[] ret = new BlockPos[pos.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = new BlockPos(pos[i].getPos().x() << 4, 0, pos[i].getPos().z() << 4);
		}
		return ret;
	}

	public static Object[] deepCopyArray(Object[] dat) {
		Object[] ret = new Object[dat.length];
		for (int i = 0; i < ret.length; i++) {
			ret[i] = ReikaJavaLibrary.copyObject(dat[i]);
		}
		return ret;
	}

	public static <E> E[] convertArray(Object[] array, Class<E> type) {
		E[] ret = (E[]) Array.newInstance(type, array.length);
		System.arraycopy(array, 0, ret, 0, ret.length);
		return ret;
	}

	public static int indexOf(int[] arr, int d) {
		return ReikaJavaLibrary.makeIntListFromArray(arr).indexOf(d);
	}

	public static <E> int indexOf(E[] arr, E d) {
		return ReikaJavaLibrary.makeListFromArray(arr).indexOf(d);
	}
}
