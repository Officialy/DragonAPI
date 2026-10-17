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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.PriorityQueue;

public class ReikaSortingHelper {

    public static <T extends Comparable<? super T>> void bubbleSort(List<T> list) {
        bubbleSort(list, Comparator.naturalOrder());
    }

    public static <T> void bubbleSort(List<T> list, Comparator<? super T> comparator) {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(comparator, "comparator");
        for (int end = list.size(); end > 1; end--) {
            boolean changed = false;
            for (int index = 1; index < end; index++) {
                T before = list.get(index - 1);
                T after = list.get(index);
                if (comparator.compare(before, after) > 0) {
                    list.set(index - 1, after);
                    list.set(index, before);
                    changed = true;
                }
            }
            if (!changed) return;
        }
    }

    public static <T extends Comparable<? super T>> void mergeSort(List<T> list) {
        mergeSort(list, Comparator.naturalOrder());
    }

    /** Stable merge sort; independent working storage prevents writes from corrupting either half. */
    public static <T> void mergeSort(List<T> list, Comparator<? super T> comparator) {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(comparator, "comparator");
        if (list.size() < 2) return;
        ArrayList<T> values = new ArrayList<>(list);
        mergeSort(values, new ArrayList<>(values), 0, values.size(), comparator);
        replaceContents(list, values);
    }

    private static <T> void mergeSort(ArrayList<T> values, ArrayList<T> buffer, int start, int end,
            Comparator<? super T> comparator) {
        if (end - start < 2) return;
        int middle = start + (end - start) / 2;
        mergeSort(values, buffer, start, middle, comparator);
        mergeSort(values, buffer, middle, end, comparator);
        int left = start;
        int right = middle;
        for (int index = start; index < end; index++) {
            if (right == end || left < middle && comparator.compare(values.get(left), values.get(right)) <= 0)
                buffer.set(index, values.get(left++));
            else
                buffer.set(index, values.get(right++));
        }
        for (int index = start; index < end; index++) values.set(index, buffer.get(index));
    }

    public static <T extends Comparable<? super T>> void allPairSort(ArrayList<T> list) {
        allPairSort(list, Comparator.naturalOrder());
    }

    /**
     * Compares every distinct pair of occurrences once and satisfies all resulting ordering
     * constraints. Equal comparisons add no constraint; available occurrences retain original
     * index priority. Cyclic constraints are unsortable and fail before changing the input.
     * Uses quadratic time and storage for the complete pair graph, without unbounded swap cycles.
     */
    public static <T> void allPairSort(ArrayList<T> list, Comparator<? super T> comparator) {
        Objects.requireNonNull(list, "list");
        Objects.requireNonNull(comparator, "comparator");
        int size = list.size();
        if (size < 2) return;
        DragonAPI.LOGGER.debug("All-pair sorting {} element occurrences", size);
        List<List<Integer>> successors = new ArrayList<>(size);
        int[] predecessors = new int[size];
        for (int index = 0; index < size; index++) successors.add(new ArrayList<>());
        for (int first = 0; first < size; first++) {
            for (int second = first + 1; second < size; second++) {
                int relation = comparator.compare(list.get(first), list.get(second));
                if (relation == 0) continue;
                int before = relation < 0 ? first : second;
                int after = relation < 0 ? second : first;
                successors.get(before).add(after);
                predecessors[after]++;
            }
        }
        PriorityQueue<Integer> available = new PriorityQueue<>();
        for (int index = 0; index < size; index++) if (predecessors[index] == 0) available.add(index);
        List<T> sorted = new ArrayList<>(size);
        while (!available.isEmpty()) {
            int index = available.remove();
            sorted.add(list.get(index));
            for (int after : successors.get(index)) if (--predecessors[after] == 0) available.add(after);
        }
        if (sorted.size() != size)
            throw new IllegalArgumentException("Cannot all-pair sort " + size + " elements: comparator ordering contains a cycle");
        replaceContents(list, sorted);
    }

    private static <T> void replaceContents(List<T> target, List<T> values) {
        ListIterator<T> iterator = target.listIterator();
        for (T value : values) {
            iterator.next();
            iterator.set(value);
        }
    }

	/*
	public static void mapKeySort(ArrayList li) {
		mapKeySort(li, new DefaultComparator());
	}

	public static void mapKeySort(ArrayList li, Comparator c) {
		IdentityHashMap<Object, Integer> map = new IdentityHashMap();
		for (int i = 0; i < li.size(); i++) {
			Object o = li.get(i);
			map.put(o, i);
		}
	}
	 */
}
