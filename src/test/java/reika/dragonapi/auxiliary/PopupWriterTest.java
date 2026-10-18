package reika.dragonapi.auxiliary;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PopupWriterTest {
    private final PopupWriter writer = PopupWriter.instance();

    @AfterEach void cleanup() { writer.clearMessages(); }

    @Test void snapshotsAreImmutableAndDoNotTrackLaterQueueChanges() {
        writer.clearMessages();
        var first = new PopupWriter.Warning("first", 192);
        writer.enqueue(first);
        var snapshot = writer.pendingMessages();
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        writer.enqueue(new PopupWriter.Warning("second"));
        assertEquals(java.util.List.of(first), snapshot);
        assertEquals(2, writer.pendingMessages().size());
        writer.clearMessages();
        assertEquals(java.util.List.of(first), snapshot);
        assertNull(writer.currentMessage());
    }

    @Test void staleClicksCannotDismissTheNextOccurrenceEvenWithTheSameWarningObject() {
        writer.clearMessages();
        var warning = new PopupWriter.Warning("duplicate");
        writer.enqueue(warning); writer.enqueue(warning);
        var old = writer.currentMessage();
        assertTrue(writer.dismiss(old));
        assertNotSame(old, writer.currentMessage());
        assertFalse(writer.dismiss(old));
        assertTrue(writer.hasMessages());
        assertTrue(writer.dismissFirst());
        assertFalse(writer.dismissFirst());
        assertFalse(writer.dismiss(null));
    }

    @Test void clearAndReplaceInvalidateEveryOldPresentation() {
        writer.clearMessages();
        var warning = new PopupWriter.Warning("same text", 200);
        writer.enqueue(warning);
        var previous = writer.currentMessage();
        writer.clearMessages(); writer.enqueue(warning);
        assertFalse(writer.dismiss(previous));
        assertSame(warning, writer.currentMessage().warning());
    }

    @Test void legacyWarningIdentityUsesTextAndKeepsWidthBounds() {
        var first = new PopupWriter.Warning("same text", 192);
        var second = new PopupWriter.Warning("same text", 250);
        assertEquals(first, second); assertEquals(first.hashCode(), second.hashCode());
        assertNotEquals(first, new PopupWriter.Warning("different text", 192));
        assertEquals(96, new PopupWriter.Warning("small", -1).width());
        assertEquals(300, new PopupWriter.Warning("large", 10000).width());
        assertEquals(192, new PopupWriter.Warning("short").width());
        assertEquals(300, new PopupWriter.Warning("long ".repeat(300)).width());
    }
}
