package reika.dragonapi.exception;

import org.objectweb.asm.tree.*;
import reika.dragonapi.libraries.java.ReikaASMHelper;

import java.util.Arrays;

public abstract class ASMException extends RuntimeException {

    public static final boolean DEV_ENV = true;//todo IMPORTANT ON RELEASE !FMLForgePlugin.RUNTIME_DEOBF;

    protected final ClassNode node;

    protected ASMException(ClassNode cn) {
        node = cn;
    }

    @Override
    public String getMessage() {
        return "Error ASMing " + node.name + ":\n";
    }

    private abstract static class NoSuchInstructionASMException extends ASMException {

        private final ClassNode owner;
        protected final MethodNode method;
        protected final String memberOwner;

        private NoSuchInstructionASMException(ClassNode cn, MethodNode m, String own) {
            super(null);
            method = m;
            owner = cn;
            memberOwner = own;
        }

        @Override
        public String getMessage() {
            return "Error ASMing method " + method.name + " " + method.desc + " in " + owner.name + ":\n";
        }

    }

    public static class NoSuchASMMethodInstructionException extends NoSuchInstructionASMException {

        private final String callName;
        private final String callDesc;
        private final int callInt;

        public NoSuchASMMethodInstructionException(ClassNode cn, MethodNode m, String own, String name, String sig, int n) {
            super(cn, m, own);
            callName = name;
            callDesc = sig;
            callInt = n;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage());
            sb.append("Could not find an instruction for a method call to "+memberOwner+"'s "+callName+" "+callDesc+":\n");
            if (callInt > 0)
                sb.append("Was looking for call #"+callInt+" to that method call.\n");
            sb.append("Found method calls:\n");
            for (int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode ain = method.instructions.get(i);
                if (ain instanceof MethodInsnNode min) {
                    String s = ReikaASMHelper.clearString(min).replace("\n", "");
                    if (min.owner.equals(memberOwner)) {
                        s = s+" * Owner match";
                    }
                    if (min.name.equals(callName)) {
                        s = s+" * Name match";
                    }
                    if (min.desc.equals(callDesc)) {
                        s = s+" * Desc match";
                    }
                    sb.append(s+"\n");
                }
            }
            return sb.toString();
        }

    }

    public static class NoSuchASMFieldInstructionException extends NoSuchInstructionASMException {

        private final String callName;
        //private final String callDesc;
        private final int callInt;

        public NoSuchASMFieldInstructionException(ClassNode cn, MethodNode m, String own, String name, int n) {
            super(cn, m, own);
            callName = name;
            //callDesc = desc;
            callInt = n;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage());
            sb.append("Could not find an instruction for a field call to "+memberOwner+"'s "+callName+":\n");
            if (callInt > 0)
                sb.append("Was looking for call #"+callInt+" to that field call.\n");
            sb.append("Found field calls:\n");
            for (int i = 0; i < method.instructions.size(); i++) {
                AbstractInsnNode ain = method.instructions.get(i);
                if (ain instanceof FieldInsnNode min) {
                    String s = ReikaASMHelper.clearString(min);
                    if (min.owner.equals(memberOwner)) {
                        s = s+" * Owner match";
                    }
                    if (min.name.equals(callName)) {
                        s = s+" * Name match";
                    }
                    //if (min.desc.equals(callDesc)) {
                    //	s = s+" * Desc match";
                    //}
                    sb.append(s);
                }
            }
            return sb.toString();
        }

    }

    private abstract static class NoSuchMemberASMException extends ASMException {

        protected final String label;

        private NoSuchMemberASMException secondaryCheck;

        private NoSuchMemberASMException(ClassNode cn, String name) {
            super(cn);
            label = name;
        }

        @Override
        public final String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage());
            sb.append(this.getTitle());
            sb.append(" not found in class ");
            sb.append(node.name);
            sb.append(".\n");
            sb.append("This is a critical ASM error and the class transformer operation cannot proceed.");
            sb.append(" If you are the developer of this mod, check for proper use of SRG/deobf names and/or sideonly elements.");
            sb.append(" If not, report it to the developer.");
            if (secondaryCheck != null) {
                sb.append(" The backup lookup also failed: ");
                sb.append(Arrays.toString(secondaryCheck.getStackTrace()));
            }
            sb.append("\n\nAdditional information:\n");
            sb.append(this.getAdditionalInformation());
            return sb.toString();
        }

        protected abstract String getAdditionalInformation();
        protected abstract String getTitle();

        public final boolean isVanillaClass() {
            return ASMException.isVanillaClass(node); //need the direct class reference or compiler has a seizure <_<
        }

        public final void addSecondary(NoSuchMemberASMException e2) {
            secondaryCheck = e2;
        }

    }

    @Override
    public final String toString() {
        return super.toString();
    }

    private static final boolean isVanillaClass(ClassNode node) {
        return node.name.startsWith("net/minecraft");
    }

    public static final class NoSuchASMMethodException extends NoSuchMemberASMException {

        private final String signature;

        public NoSuchASMMethodException(ClassNode cn, String name, String sig) {
            super(cn, name);
            signature = sig;
        }

        @Override
        protected String getTitle() {
            return "Method "+label+" "+signature;
        }

        @Override
        protected String getAdditionalInformation() {
            StringBuilder sb = new StringBuilder();
            if (DEV_ENV && label.startsWith("func_")) {
                sb.append("Use of SRG name in dev environment. This may be an error.\n");
            }
            else if (!DEV_ENV && !label.startsWith("func_") && this.isVanillaClass()) {
                sb.append("Use of non-SRG name in compiled game on vanilla code. This is very likely an error.\n");
            }
            sb.append("Identified methods:\n");
            for (MethodNode m : node.methods) {
                String tag = m.name.equals(label) ? " * Name match" : "";
                sb.append("\t"+m.name+" "+m.desc+tag+"\n");
            }
            return sb.toString();
        }

    }

    public static final class NoSuchASMFieldException extends NoSuchMemberASMException {

        public NoSuchASMFieldException(ClassNode cn, String name) {
            super(cn, name);
        }

        @Override
        protected String getTitle() {
            return "Field "+label;
        }

        @Override
        protected String getAdditionalInformation() {
            StringBuilder sb = new StringBuilder();
            if (DEV_ENV && label.startsWith("field_")) {
                sb.append("Use of SRG name in dev environment. This may be an error.\n");
            }
            else if (!DEV_ENV && !label.startsWith("field_") && this.isVanillaClass()) {
                sb.append("Use of non-SRG name in compiled game on vanilla code. This is very likely an error.\n");
            }
            sb.append("Identified fields:\n");
            for (FieldNode f : node.fields) {
                String tag = f.name.equals(label) ? " * Name match" : "";
                sb.append("\t"+f.name+" "+f.desc+tag+"\n");
            }
            return sb.toString();
        }

    }

    private abstract static class DuplicateMemberASMException extends ASMException {

        protected final String label;

        private DuplicateMemberASMException(ClassNode cn, String name) {
            super(cn);
            label = name;
        }

        @Override
        public final String getMessage() {
            String sb = super.getMessage() +
                    this.getTitle() +
                    " already found in class " +
                    node.name +
                    " when trying to add another member with the same erasure.\n" +
                    "This is a critical ASM error and the class transformer operation cannot proceed." +
                    " If you are the developer of this mod, check for copy-paste errors or broken overloading." +
                    " If not, report it to the developer." +
                    "\n\nAdditional information:\n" +
                    this.getAdditionalInformation();
            return sb;
        }

        protected abstract String getAdditionalInformation();
        protected abstract String getTitle();

        public final boolean isVanillaClass() {
            return ASMException.isVanillaClass(node); //need the direct class reference or compiler has a seizure <_<
        }

    }

    public static final class DuplicateASMMethodException extends DuplicateMemberASMException {

        private final String signature;

        public DuplicateASMMethodException(ClassNode cn, String name, String sig) {
            super(cn, name);
            signature = sig;
        }

        @Override
        protected String getTitle() {
            return "Method "+label+" "+signature;
        }

        @Override
        protected String getAdditionalInformation() {
            StringBuilder sb = new StringBuilder();
            sb.append("Identified methods:\n");
            for (MethodNode m : node.methods) {
                String tag1 = m.name.equals(label) ? " * Name match" : "";
                String tag2 = m.desc.equals(label) ? " * Signature match" : "";
                sb.append("\t"+m.name+" "+m.desc+tag1+"|"+tag2+"\n");
            }
            return sb.toString();
        }

    }

    public static final class DuplicateASMFieldException extends DuplicateMemberASMException {

        public DuplicateASMFieldException(ClassNode cn, String name) {
            super(cn, name);
        }

        @Override
        protected String getTitle() {
            return "Field "+label;
        }

        @Override
        protected String getAdditionalInformation() {
            StringBuilder sb = new StringBuilder();
            sb.append("Identified fields:\n");
            for (FieldNode f : node.fields) {
                String tag = f.name.equals(label) ? " * Name match" : "";
                sb.append("\t"+f.name+" "+f.desc+tag+"\n");
            }
            return sb.toString();
        }

    }

    public static final class ASMConflictException extends ASMException {

        private final String asmMod;
        private final String patch;
        private final String message;

        private final MethodNode method;

        public ASMConflictException(String mod, ClassNode cn, MethodNode m, String p, String msg) {
            super(cn);

            asmMod = mod;
            patch = p;
            message = msg;

            method = m;
        }

        @Override
        public String getMessage() {
            StringBuilder sb = new StringBuilder();
            sb.append(super.getMessage());
            sb.append("An ASM conflict occurred when applying the patch ");
            sb.append(patch);
            sb.append(" to method '");
            sb.append(method.name);
            sb.append(" ");
            sb.append(method.desc);
            sb.append("'\n");
            sb.append(message);
            sb.append("\n");
            sb.append("One of the mods involved: ");
            sb.append(asmMod);
            sb.append("\n");
            sb.append("It is not possible to identify the other conflicting mod or mods; there may be multiple and the condition may be complex.");
            sb.append("\n");
            sb.append("However, try reproducing this error with ONLY a base Forge installation, without tweaks such as KCauldron, Optifine, or FastCraft.");
            sb.append("\n");
            sb.append("Due to their nature, such tweak mods are the most likely causes of the conflict.");
            sb.append("\n");
            sb.append("Once you identify the conflict, contact the developers of both mods so that a solution can be attempted.");
            sb.append("\n");
            sb.append("Note that in a worst-case scenario, no solution may be possible.");
            sb.append("\n");
            sb.append("Method body:");
            sb.append("\n");
            for (int i = 0; i < method.instructions.size(); i++) {
                sb.append(ReikaASMHelper.clearString(method.instructions.get(i)));
                sb.append("\n");
            }
            sb.append("\n");
            sb.append("A .class file was generated in your MC folder, under the \"ClassError\" subfolder. Find the one with the matching name.");
            return sb.toString();
        }

    }

    public static final class InvalidASMArgumentException extends ASMException {

        public final String message;

        public InvalidASMArgumentException(ClassNode cn, String msg) {
            super(cn);
            message = msg;
        }

        @Override
        public String getMessage() {
            String sb = super.getMessage() +
                    "Invalid argument to function:\n" +
                    message;
            return sb;
        }

    }
}
