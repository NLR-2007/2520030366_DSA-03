package algo;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * ==========================================================================
 * ALGORITHM 8 : RANDOMIZED QUICKSORT
 * ==========================================================================
 * WHERE FARMASSIST USES IT
 *   Ranking. Every search result carries a relevance score and every related
 *   article carries a similarity length. This sorts them so the best answer is
 *   printed first. We never call Collections.sort - the ranking is our own.
 *
 * IDEA
 *   Pick a RANDOM pivot, partition the list into "smaller" and "larger", then
 *   sort both sides recursively. Choosing the pivot at random is what removes
 *   the O(n^2) worst case that plain quicksort has on already sorted input -
 *   and search results are very often already partly sorted.
 *
 * TIME  : O(n log n) expected     SPACE : O(log n) recursion
 * ==========================================================================
 */
public class RandomizedQuickSort {

    private static final Random RANDOM = new Random();

    /** Sort the list in place using the given comparator. */
    public static <T> void sort(List<T> list, Comparator<? super T> comparator) {
        if (list != null && list.size() > 1) {
            quickSort(list, 0, list.size() - 1, comparator);
        }
    }

    private static <T> void quickSort(List<T> list, int low, int high, Comparator<? super T> cmp) {
        if (low >= high) return;
        int p = partition(list, low, high, cmp);
        quickSort(list, low, p - 1, cmp);
        quickSort(list, p + 1, high, cmp);
    }

    /** Lomuto partition with a randomly chosen pivot. */
    private static <T> int partition(List<T> list, int low, int high, Comparator<? super T> cmp) {
        int randomIndex = low + RANDOM.nextInt(high - low + 1);
        swap(list, randomIndex, high);                 // move pivot to the end

        T pivot = list.get(high);
        int i = low - 1;
        for (int j = low; j < high; j++) {
            if (cmp.compare(list.get(j), pivot) <= 0) {
                i++;
                swap(list, i, j);
            }
        }
        swap(list, i + 1, high);
        return i + 1;
    }

    private static <T> void swap(List<T> list, int a, int b) {
        T t = list.get(a);
        list.set(a, list.get(b));
        list.set(b, t);
    }
}
