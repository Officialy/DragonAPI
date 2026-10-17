package reika.dragonapi.libraries.java;

import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ReikaStringReferenceTest {
    public static class Parent {
        public static int INHERITED = 7;
        public static long inheritedMethod() { return 11L; }
    }
    public static class Values extends Parent {
        public static int INT = -42;
        public static long LONG = Long.MIN_VALUE;
        public static float FLOAT = 2.5F;
        public static double DOUBLE = Math.PI;
        public static byte BYTE = 1;
        public static Integer BOXED = 9;
        public int instanceField;
        public int instanceMethod() { return 1; }
        public static String text() { return "text"; }
        public static Object nullResult() { return null; }
        public static int checkedFailure() throws IOException { throw new IOException("expected method failure"); }
        public static int fatalFailure() { throw FATAL; }
        private static int hidden() { return 1; }
    }
    public enum Choice {
        FIRST, SECOND;
        public String text() { return "enum-" + name(); }
        public int number() { return ordinal(); }
        public String checkedFailure() throws IOException { throw new IOException("expected enum failure"); }
    }
    private static final AssertionError FATAL = new AssertionError("expected fatal invocation");

    private static String field(String name) { return "PARSE_NUMBER_VARIABLE(" + Values.class.getName() + "$" + name + ")"; }
    private static String method(String name) { return "PARSE_NUMBER_METHOD(" + Values.class.getName() + "$" + name + ")"; }
    private static String enumeration(String constant, String name) { return "PARSE_ENUM(" + Choice.class.getName() + "$" + constant + "$" + name + ")"; }
    private static String resolve(String expression) { return ReikaStringParser.getStringWithEmbeddedReferences(expression); }

    @Test void numericReferencesRetainSupportedTypesInheritanceAndUnsupportedTypeResults() {
        assertEquals("-42", resolve(field("INT")));
        assertEquals(Long.toString(Long.MIN_VALUE), resolve(field("LONG")));
        assertEquals("2.5", resolve(field("FLOAT")));
        assertEquals(Double.toString(Math.PI), resolve(field("DOUBLE")));
        assertEquals("7", resolve(field("INHERITED")));
        assertEquals("11", resolve(method("inheritedMethod")));
        assertEquals("text", resolve(method("text")));
        assertEquals("NOT FOUND", resolve(field("BYTE")));
        assertEquals("NOT FOUND", resolve(field("BOXED")));
        assertEquals("before-42after", resolve("before+" + field("INT") + "+after"));
        assertEquals(Integer.toString(Integer.MAX_VALUE), resolve("PARSE_NUMBER_VARIABLE(java.lang.Integer$MAX_VALUE)"));
    }

    @Test void enumReferencesSupportNestedBinaryNamesAndPreserveMethodResultText() {
        assertEquals("enum-FIRST", resolve(enumeration("FIRST", "text")));
        assertEquals("1", resolve(enumeration("SECOND", "number")));
        var missing = assertThrows(IllegalArgumentException.class, () -> resolve(enumeration("MISSING", "text")));
        assertTrue(missing.getMessage().contains(enumeration("MISSING", "text")));
        assertInstanceOf(IllegalArgumentException.class, missing.getCause());
        assertTrue(missing.getCause().getMessage().contains("MISSING"));
    }

    @Test void lookupAndReceiverFailuresRetainTheExactReferenceAndCause() {
        for (var entry : List.of(
                java.util.Map.entry(field("absent"), NoSuchFieldException.class),
                java.util.Map.entry(method("absent"), NoSuchMethodException.class),
                java.util.Map.entry(method("hidden"), NoSuchMethodException.class),
                java.util.Map.entry(field("instanceField"), IllegalArgumentException.class),
                java.util.Map.entry(method("instanceMethod"), IllegalArgumentException.class),
                java.util.Map.entry("PARSE_NUMBER_VARIABLE(dragonapi.test.Missing$FIELD)", ClassNotFoundException.class),
                java.util.Map.entry("PARSE_ENUM(java.lang.String$FIRST$text)", IllegalArgumentException.class))) {
            var error = assertThrows(IllegalArgumentException.class, () -> resolve(entry.getKey()));
            assertTrue(error.getMessage().contains(entry.getKey()));
            assertInstanceOf(entry.getValue(), error.getCause());
        }
    }

    @Test void invokedFailuresExposeTheirRealCauseAndDoNotHideErrors() {
        for (String expression : List.of(method("checkedFailure"), enumeration("FIRST", "checkedFailure"))) {
            var failure = assertThrows(IllegalArgumentException.class, () -> resolve(expression));
            assertInstanceOf(IOException.class, failure.getCause());
            assertTrue(failure.getMessage().contains(expression));
            assertTrue(failure.getCause().getMessage().startsWith("expected"));
        }
        var nullResult = assertThrows(IllegalArgumentException.class, () -> resolve(method("nullResult")));
        assertInstanceOf(NullPointerException.class, nullResult.getCause());
        assertSame(FATAL, assertThrows(AssertionError.class, () -> resolve(method("fatalFailure"))));
    }

    @Test void malformedReferencesAreContextualAndDirectStringFormatBehaviorIsRetained() {
        for (String expression : List.of("PARSE_ENUM()", "PARSE_ENUM($FIRST$text)", "PARSE_ENUM(java.lang.String$$text)",
                "PARSE_NUMBER_METHOD(java.lang.Math$)", "PARSE_NUMBER_VARIABLE(java.lang.Integer$MAX_VALUE",
                "PARSE_NUMBER_VARIABLEjava.lang.Integer$MAX_VALUE)")) {
            var failure = assertThrows(IllegalArgumentException.class, () -> resolve(expression));
            assertTrue(failure.getMessage().contains(expression));
        }
        assertEquals("value:-42", ReikaStringParser.parseStringFormat("value:%s," + field("INT")));
        var invalid = assertThrows(IllegalArgumentException.class, () -> ReikaStringParser.parseStringFormat("%d," + field("INT")));
        assertInstanceOf(java.util.IllegalFormatException.class, invalid.getCause());
    }

    @Test void wrappedFormatsResolveNestedArgumentsAndKeepInnerSeparators() {
        String inner = "PARSE_STRING_FORMAT(%s+%s," + field("INT") + "," + method("text") + ")";
        String outer = "PARSE_STRING_FORMAT([%s]:%s," + inner + "," + enumeration("FIRST", "text") + ")";
        assertEquals("-42+text", resolve(inner));
        assertEquals("[-42+text]:enum-FIRST", ReikaStringParser.parseStringFormat(outer));
        assertEquals("before[-42+text]:enum-FIRSTafter", resolve("before+" + outer + "+after"));
        assertEquals("(-42+text)", ReikaStringParser.parseStringFormat("(%s)," + inner));
        assertEquals("text%", resolve("PARSE_STRING_FORMAT(text%%)"));
        assertEquals("text", resolve("PARSE_STRING_FORMAT( %s , " + method("text") + " )"));
    }

    @Test void malformedFormatArgumentsAndWrappersFailWithExpressionContext() {
        for (String expression : List.of("PARSE_STRING_FORMAT", "PARSE_STRING_FORMAT(%s," + field("INT"),
                "PARSE_STRING_FORMAT(text))", "PARSE_STRING_FORMAT(text)(more)",
                "PARSE_STRING_FORMAT(%s,)", "PARSE_STRING_FORMAT(%s,unknown)",
                "PARSE_STRING_FORMAT(%s," + field("INT") + ",)")) {
            var error = assertThrows(IllegalArgumentException.class, () -> ReikaStringParser.parseStringFormat(expression));
            assertTrue(error.getMessage().contains(expression));
        }
        assertThrows(IllegalArgumentException.class, () -> ReikaStringParser.parseStringFormat(null));
        assertThrows(IllegalArgumentException.class, () -> ReikaStringParser.parseStringFormat(" "));
        assertThrows(IllegalArgumentException.class, () -> resolve("prefix+PARSE_STRING_FORMAT(%s," + field("INT")));
    }

    @Test void nestedFormatsPreserveInvocationFailuresAndLegacyConcatenation() {
        String expression = "PARSE_STRING_FORMAT(%s,PARSE_STRING_FORMAT(%s," + method("checkedFailure") + "))";
        var error = assertThrows(IllegalArgumentException.class, () -> resolve(expression));
        assertInstanceOf(IOException.class, error.getCause());
        assertTrue(error.getMessage().contains(method("checkedFailure")));
        assertSame(FATAL, assertThrows(AssertionError.class,
                () -> resolve("PARSE_STRING_FORMAT(%s," + method("fatalFailure") + ")")));
        assertEquals("ordinary(parenthesestext", resolve("ordinary(parentheses+text"));
        assertEquals(" PARSE_STRING_FORMAT(unclosedtext", resolve(" PARSE_STRING_FORMAT(unclosed+text"));
        assertEquals("abc", resolve("+a++b+c+"));
        assertEquals("line\n\ttab", resolve("line\\n+\\ttab"));
        assertEquals("", resolve(""));
    }
}
