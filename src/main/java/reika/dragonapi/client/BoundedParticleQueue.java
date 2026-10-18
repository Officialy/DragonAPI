package reika.dragonapi.client;

import java.util.AbstractQueue;
import java.util.ArrayDeque;
import java.util.ConcurrentModificationException;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/** Global FIFO ordering with an O(1) pending budget per native identity-keyed particle group. */
public final class BoundedParticleQueue<T,K> extends AbstractQueue<T> {
    private static final class Node<T,K> {
        final T value; final K key; final boolean counted; Node<T,K> previous,next;
        Node(T value,K key,boolean counted) { this.value=value; this.key=key; this.counted=counted; }
    }
    private final int limit;
    private final Function<T,K> key;
    private final Consumer<T> retired;
    private final Predicate<T> counted;
    private final Map<K,ArrayDeque<Node<T,K>>> groups=new IdentityHashMap<>();
    private Node<T,K> first,last;
    private int size,version;
    public BoundedParticleQueue(int limit,Function<T,K> key,Consumer<T> retired) {
        this(limit,key,value->true,retired);
    }
    /** Exempt entries retain FIFO order but neither consume the budget nor evict counted entries.
     * The predicate is sampled once at admission, so later mutation cannot corrupt the budget. */
    public BoundedParticleQueue(int limit,Function<T,K> key,Predicate<T> counted,Consumer<T> retired) {
        if(limit<1) throw new IllegalArgumentException("Positive pending particle limit required");
        this.limit=limit; this.key=Objects.requireNonNull(key); this.retired=Objects.requireNonNull(retired);
        this.counted=Objects.requireNonNull(counted);
    }
    @Override public boolean offer(T value) {
        Objects.requireNonNull(value,"value");
        K group=Objects.requireNonNull(key.apply(value),"particle group");
        boolean consumesBudget=counted.test(value);
        var bucket=groups.get(group);
        T evicted=null;
        if(consumesBudget && bucket!=null && bucket.size()>=limit) { var node=bucket.getFirst(); evicted=node.value; unlink(node); }
        var node=new Node<T,K>(value,group,consumesBudget);
        if(consumesBudget) groups.computeIfAbsent(group,ignored->new ArrayDeque<>()).addLast(node);
        node.previous=last;
        if(last==null) first=node; else last.next=node;
        last=node; size++; version++;
        if(evicted!=null) retired.accept(evicted);
        return true;
    }
    private void unlink(Node<T,K> node) {
        if(node.previous==null) first=node.next; else node.previous.next=node.next;
        if(node.next==null) last=node.previous; else node.next.previous=node.previous;
        if(node.counted) {
            var bucket=groups.get(node.key); bucket.remove(node);
            if(bucket.isEmpty()) groups.remove(node.key);
        }
        size--; version++;
    }
    @Override public T poll() { if(first==null) return null; var node=first; unlink(node); return node.value; }
    @Override public T peek() { return first==null?null:first.value; }
    @Override public int size() { return size; }
    @Override public void clear() {
        var values=new java.util.ArrayList<>(this);
        first=null; last=null; groups.clear(); size=0; version++;
        for(var value:values) retired.accept(value);
    }
    @Override public Iterator<T> iterator() {
        return new Iterator<>() {
            Node<T,K> next=first,returned;
            int expected=version;
            private void check() { if(expected!=version) throw new ConcurrentModificationException(); }
            @Override public boolean hasNext() { check(); return next!=null; }
            @Override public T next() { check(); if(next==null) throw new NoSuchElementException(); returned=next; next=next.next; return returned.value; }
            @Override public void remove() { check(); if(returned==null) throw new IllegalStateException(); unlink(returned); returned=null; expected=version; }
        };
    }
}
