package reika.dragonapi.libraries.java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReikaSortingHelperTest {
    private record Entry(int key, String label) {}

    @Test void algorithmsMatchNaturalAndReverseOrderAcrossExhaustedHalvesAndDuplicates() {
        Random random = new Random(0xD58F);
        for (int size : List.of(0, 1, 2, 3, 8, 31, 100)) {
            List<Integer> input = new ArrayList<>();
            for (int index = 0; index < size; index++) input.add(random.nextInt(21) - 10);
            for (Comparator<Integer> comparator : List.of(Comparator.<Integer>naturalOrder(), Comparator.<Integer>reverseOrder())) {
                List<Integer> expected = new ArrayList<>(input);
                expected.sort(comparator);
                ArrayList<Integer> bubble = new ArrayList<>(input);
                ArrayList<Integer> merge = new ArrayList<>(input);
                ArrayList<Integer> pairs = new ArrayList<>(input);
                ReikaSortingHelper.bubbleSort(bubble, comparator);
                ReikaSortingHelper.mergeSort(merge, comparator);
                ReikaSortingHelper.allPairSort(pairs, comparator);
                assertEquals(expected, bubble);
                assertEquals(expected, merge);
                assertEquals(expected, pairs);
            }
        }
        ArrayList<Integer> bubble = new ArrayList<>(List.of(3, 1, 2));
        ArrayList<Integer> merge = new ArrayList<>(bubble);
        ArrayList<Integer> pairs = new ArrayList<>(bubble);
        ReikaSortingHelper.bubbleSort(bubble);
        ReikaSortingHelper.mergeSort(merge);
        ReikaSortingHelper.allPairSort(pairs);
        assertEquals(List.of(1, 2, 3), bubble);
        assertEquals(bubble, merge);
        assertEquals(bubble, pairs);
    }

    @Test void comparatorOnlyElementsRetainTieIdentityAndSupportNullPoliciesAndListViews() {
        Entry a = new Entry(2, "a"), b = new Entry(1, "b"), c = new Entry(2, "c"), d = new Entry(1, "d");
        List<Entry> input = List.of(a, b, c, d, a);
        List<Entry> expected = List.of(b, d, a, c, a);
        Comparator<Entry> comparator = Comparator.comparingInt(Entry::key);
        var bubble = new ArrayList<>(input);
        var merge = new LinkedList<>(input);
        var pairs = new ArrayList<>(input);
        ReikaSortingHelper.bubbleSort(bubble, comparator);
        ReikaSortingHelper.mergeSort(merge, comparator);
        ReikaSortingHelper.allPairSort(pairs, comparator);
        for (List<Entry> result : List.of(bubble, merge, pairs)) {
            for (int index = 0; index < result.size(); index++) assertSame(expected.get(index), result.get(index));
        }
        var fixed = Arrays.asList(9, 3, 1, 2, 8);
        ReikaSortingHelper.mergeSort(fixed.subList(1, 4));
        assertEquals(List.of(9, 1, 2, 3, 8), fixed);
        var nullable = new ArrayList<>(Arrays.asList(2, null, 1, null));
        ReikaSortingHelper.allPairSort(nullable, Comparator.nullsFirst(Comparator.naturalOrder()));
        assertEquals(Arrays.asList(null, null, 1, 2), nullable);
        ReikaSortingHelper.mergeSort(nullable, Comparator.nullsLast(Comparator.naturalOrder()));
        assertEquals(Arrays.asList(1, 2, null, null), nullable);
    }

    @Test void allPairsUsesOccurrenceConstraintsRatherThanEqualityAndComparesEachPairOnce() {
        final class EqualEntry {
            final int order;
            EqualEntry(int order) { this.order = order; }
            @Override public boolean equals(Object other) { return other instanceof EqualEntry; }
            @Override public int hashCode() { throw new AssertionError("Sorting must not hash elements"); }
        }
        EqualEntry a = new EqualEntry(3), b = new EqualEntry(1), c = new EqualEntry(2);
        var values = new ArrayList<>(List.of(a, b, c));
        int[] calls = {0};
        ReikaSortingHelper.allPairSort(values, (left, right) -> {
            calls[0]++;
            return left.order < right.order ? Integer.MIN_VALUE : left.order == right.order ? 0 : Integer.MAX_VALUE;
        });
        assertEquals(3, calls[0]);
        assertSame(b, values.get(0));
        assertSame(c, values.get(1));
        assertSame(a, values.get(2));
    }

    @Test void partialOrdersResolveEveryConstraintAndCyclesFailWithoutMutation() {
        var values = new ArrayList<>(List.of(2, 0, 1, 3));
        ReikaSortingHelper.allPairSort(values, (a, b) -> a == 0 && b == 2 ? -1 : a == 2 && b == 0 ? 1 : 0);
        assertEquals(List.of(0, 2, 1, 3), values);
        var cycle = new ArrayList<>(List.of(0, 1, 2));
        var error = assertThrows(IllegalArgumentException.class, () -> ReikaSortingHelper.allPairSort(cycle,
                (a, b) -> a.equals(b) ? 0 : (a + 1) % 3 == b ? -1 : 1));
        assertTrue(error.getMessage().contains("cycle"));
        assertEquals(List.of(0, 1, 2), cycle);
    }

    @Test void comparatorFailuresPropagateAndWorkingSortsPreserveInputOnFailure() {
        RuntimeException failure = new IllegalStateException("expected comparator failure");
        Comparator<Integer> comparator = (a, b) -> { throw failure; };
        var values = new ArrayList<>(List.of(3, 2, 1));
        assertSame(failure, assertThrows(IllegalStateException.class, () -> ReikaSortingHelper.mergeSort(values, comparator)));
        assertEquals(List.of(3, 2, 1), values);
        assertSame(failure, assertThrows(IllegalStateException.class, () -> ReikaSortingHelper.allPairSort(values, comparator)));
        assertEquals(List.of(3, 2, 1), values);
        assertThrows(NullPointerException.class, () -> ReikaSortingHelper.mergeSort(null, comparator));
        assertThrows(NullPointerException.class, () -> ReikaSortingHelper.allPairSort(values, null));
    }
}
