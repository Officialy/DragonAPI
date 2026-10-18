package reika.dragonapi.client;

import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BoundedParticleQueueTest {
    @Test void clearDetachesTheOldGenerationBeforeRetirementCallbacksRun() {
        var group=new Object(); var reference=new AtomicReference<BoundedParticleQueue<Entry,Object>>(); var retired=new ArrayList<Integer>();
        var queue=new BoundedParticleQueue<Entry,Object>(2,Entry::group,value->{
            retired.add(value.id()); if(value.id()<10) reference.get().add(new Entry(group,value.id()+10));
        }); reference.set(queue);
        queue.add(new Entry(group,1)); queue.add(new Entry(group,2)); queue.clear();
        assertEquals(List.of(1,2),retired);
        assertEquals(List.of(new Entry(group,11),new Entry(group,12)),new ArrayList<>(queue));
    }
    @Test void exemptEntriesNeitherConsumeBudgetNorEvictCountedEntries() {
        var group=new Object(); var retired=new ArrayList<Entry>();
        var queue=new BoundedParticleQueue<Entry,Object>(2,Entry::group,value->value.id()>0,retired::add);
        var first=new Entry(group,1); var second=new Entry(group,2); var exempt=new Entry(group,-1);
        queue.add(first); queue.add(second); queue.add(exempt);
        assertTrue(retired.isEmpty());
        var third=new Entry(group,3); queue.add(third);
        assertEquals(List.of(first),retired);
        assertEquals(List.of(second,exempt,third),new ArrayList<>(queue));
        var iterator=queue.iterator(); iterator.next(); iterator.next(); iterator.remove();
        queue.add(new Entry(group,-2)); queue.add(new Entry(group,4));
        assertEquals(List.of(first,second),retired);
        queue.clear(); assertTrue(queue.isEmpty());
    }
    @Test void mutableExemptionIsFrozenAtAdmissionAndPredicateFailureIsAtomic() {
        var counted=new java.util.concurrent.atomic.AtomicBoolean(true);
        var reject=new java.util.concurrent.atomic.AtomicBoolean(false);
        var group=new Object(); var retired=new ArrayList<Entry>();
        var queue=new BoundedParticleQueue<Entry,Object>(1,Entry::group,value->{
            if(reject.get()) throw new IllegalArgumentException("rejected");
            return counted.get();
        },retired::add);
        var first=new Entry(group,1); queue.add(first);
        counted.set(false); var exempt=new Entry(group,2); queue.add(exempt);
        counted.set(true); reject.set(true);
        assertThrows(IllegalArgumentException.class,()->queue.add(new Entry(group,3)));
        assertEquals(List.of(first,exempt),new ArrayList<>(queue)); assertTrue(retired.isEmpty());
        reject.set(false); queue.add(new Entry(group,4));
        assertEquals(List.of(first),retired);
        assertSame(exempt,queue.poll());
        queue.add(new Entry(group,5));
        assertEquals(List.of(first,new Entry(group,4)),retired);
    }
    private record Entry(Object group,int id) {}
    @Test void oldestPendingOfOnlyTheFullGroupIsRetiredWithoutChangingGlobalOrder() {
        var a=new Object(); var b=new Object(); var retired=new ArrayList<Entry>();
        var queue=new BoundedParticleQueue<Entry,Object>(2,Entry::group,retired::add);
        var a1=new Entry(a,1); var b1=new Entry(b,2); var a2=new Entry(a,3); var a3=new Entry(a,4);
        queue.add(a1); queue.add(b1); queue.add(a2); queue.add(a3);
        assertEquals(List.of(a1),retired); assertEquals(List.of(b1,a2,a3),new ArrayList<>(queue));
        assertSame(b1,queue.poll()); assertSame(a2,queue.poll()); assertSame(a3,queue.poll()); assertNull(queue.poll());
    }
    @Test void identityKeysIteratorRemovalAndClearKeepBudgetsConsistent() {
        var a=new String("same"); var b=new String("same"); var retired=new ArrayList<Entry>();
        var queue=new BoundedParticleQueue<Entry,Object>(1,Entry::group,retired::add);
        var first=new Entry(a,1); var second=new Entry(b,2); queue.add(first); queue.add(second);
        assertEquals(2,queue.size());
        var iterator=queue.iterator(); assertSame(first,iterator.next()); iterator.remove();
        assertThrows(IllegalStateException.class,iterator::remove);
        queue.add(new Entry(a,3));
        assertThrows(ConcurrentModificationException.class,iterator::next);
        queue.clear(); assertTrue(queue.isEmpty()); assertEquals(List.of(second,new Entry(a,3)),retired);
        queue.add(first); assertSame(first,queue.poll());
    }
    @Test void retirementCallbacksMayEnqueueWithoutCorruptingTheTwoFifoOrders() {
        var group=new Object(); var reference=new AtomicReference<BoundedParticleQueue<Entry,Object>>(); var retired=new ArrayList<Integer>();
        var queue=new BoundedParticleQueue<Entry,Object>(2,Entry::group,value->{
            retired.add(value.id()); if(value.id()==1) reference.get().add(new Entry(group,4));
        }); reference.set(queue);
        queue.add(new Entry(group,1)); queue.add(new Entry(group,2)); queue.add(new Entry(group,3));
        assertEquals(List.of(1,2),retired);
        assertEquals(List.of(new Entry(group,3),new Entry(group,4)),new ArrayList<>(queue));
    }
    @Test void invalidInputsDoNotEvictAnExistingEntry() {
        assertThrows(IllegalArgumentException.class,()->new BoundedParticleQueue<>(0,value->value,value->{}));
        var group=new Object(); var queue=new BoundedParticleQueue<Entry,Object>(1,Entry::group,value->fail("must not retire"));
        var first=new Entry(group,1); queue.add(first);
        assertThrows(NullPointerException.class,()->queue.add(null));
        assertThrows(NullPointerException.class,()->queue.add(new Entry(null,2)));
        assertSame(first,queue.peek()); assertEquals(1,queue.size());
    }
}
