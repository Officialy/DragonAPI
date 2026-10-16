package reika.dragonapi.libraries.java;

import java.lang.reflect.Modifier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReikaReflectionHelperTest {
    private static class Parent {
        private int number = 42;
        private boolean flag = true;
        private static int shared = 19;
        private static final Object CONSTANT = new Object();
        Parent(String unused) {}
        private String inherited(String text) { return text; }
    }
    private static class Child extends Parent {
        Child() { super("parent"); }
    }
    private static class Reconstructed {
        private final int number;
        Reconstructed(int number) { this.number = number; }
    }
    private record ImmutableRecord(int value) {}
    private static class Methods {
        private static String choose(Integer value) { return "integer:" + value; }
        private static String choose(String value) { return "string:" + value; }
        private String primitive(int value) { return "primitive:" + value; }
        private String nullable(CharSequence value) { return value == null ? "null" : value.toString(); }
        private static String fails() { throw new IllegalStateException("expected invocation failure"); }
    }

    @Test void privatePrimitiveReadersHandleInheritanceStaticFieldsAndHistoricalSentinels() {
        var value = new Child();
        assertEquals(42, ReikaReflectionHelper.getPrivateInteger(value, "number"));
        assertTrue(ReikaReflectionHelper.getPrivateBoolean(value, "flag"));
        assertEquals(19, ReikaReflectionHelper.getPrivateInteger(value, "shared"));
        assertEquals(Integer.MIN_VALUE, ReikaReflectionHelper.getPrivateInteger(null, "number"));
        assertFalse(ReikaReflectionHelper.getPrivateBoolean(value, null));
        assertEquals(Integer.MIN_VALUE, ReikaReflectionHelper.getPrivateInteger(value, "flag"));
        assertFalse(ReikaReflectionHelper.getPrivateBoolean(value, "missing"));
    }

    @Test void inheritedMethodsRemainDiscoverableButParentConstructorsAreNotInherited() {
        assertNotNull(ReikaReflectionHelper.getProtectedInheritedMethod(Child.class, "inherited", String.class));
        assertNotNull(ReikaReflectionHelper.getProtectedInheritedMethod(Parent.class, "<init>", String.class));
        assertNull(ReikaReflectionHelper.getProtectedInheritedMethod(Child.class, "<init>", String.class));
    }

    @SuppressWarnings("deprecation")
    @Test void reconstructionUsesSupportedInstanceWritesAndRestoresAccessOverrideOnFailure() throws Exception {
        var value = new Reconstructed(1);
        var field = Reconstructed.class.getDeclaredField("number");
        assertFalse(field.isAccessible());
        ReikaReflectionHelper.setFinalField(field, value, 9);
        assertEquals(9, ReikaReflectionHelper.getPrivateInteger(value, "number"));
        assertTrue(Modifier.isFinal(field.getModifiers()));
        assertFalse(field.isAccessible());
        assertThrows(IllegalArgumentException.class, () -> ReikaReflectionHelper.setFinalField(field, value, "wrong type"));
        assertFalse(field.isAccessible());
        field.setAccessible(true);
        ReikaReflectionHelper.setFinalField(field, value, 10);
        assertTrue(field.isAccessible());
        assertThrows(NoSuchFieldException.class, () -> ReikaReflectionHelper.setFinalField(Reconstructed.class, "missing", value, 2));
    }

    @Test void unsupportedStaticAndRecordFinalWritesFailWithoutModifierHacks() throws Exception {
        assertThrows(IllegalAccessException.class, () -> ReikaReflectionHelper.setFinalField(
                Parent.class.getDeclaredField("CONSTANT"), null, new Object()));
        assertThrows(IllegalAccessException.class, () -> ReikaReflectionHelper.setFinalField(
                ImmutableRecord.class.getDeclaredField("value"), new ImmutableRecord(1), 2));
    }

    @Test void signatureCacheSeparatesOverloadsAndSupportsConcurrentCallers() {
        String owner = Methods.class.getName();
        IntStream.range(0, 200).parallel().forEach(i -> {
            assertEquals("integer:" + i, ReikaReflectionHelper.cacheAndInvokeMethod(owner, "choose", null, i));
            assertEquals("string:" + i, ReikaReflectionHelper.cacheAndInvokeMethod(owner, "choose", null, Integer.toString(i)));
        });
    }

    @Test void explicitSignaturesSupportPrimitiveInterfaceAndNullArgumentsWithFailureCompatibility() {
        var value = new Methods();
        assertEquals("primitive:7", ReikaReflectionHelper.cacheAndInvokeMethod(Methods.class, "primitive", new Class<?>[]{int.class}, value, 7));
        assertEquals("null", ReikaReflectionHelper.cacheAndInvokeMethod(Methods.class, "nullable", new Class<?>[]{CharSequence.class}, value, (Object)null));
        assertEquals("text", ReikaReflectionHelper.cacheAndInvokeMethod(Methods.class, "nullable", new Class<?>[]{CharSequence.class}, value, "text"));
        assertNull(ReikaReflectionHelper.cacheAndInvokeMethod(Methods.class, "fails", new Class<?>[0], null));
        assertNull(ReikaReflectionHelper.cacheAndInvokeMethod(Methods.class, "missing", new Class<?>[0], null));
    }
}
