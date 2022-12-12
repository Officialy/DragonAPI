package reika.dragonapi.libraries.logic;

import reika.dragonapi.exception.MisuseException;
import reika.dragonapi.exception.UnreachableCodeException;
import reika.dragonapi.libraries.java.ReikaArrayHelper;

public enum LogicalOperators {

    AND(),
    OR(),
    NOT(),
    XOR(),
    NOR(),
    NAND(),
    XNOR();

    public boolean evaluate(boolean... args) {
        switch (this) {
            case NOT:
                if (args.length != 1)
                    throw new MisuseException("You can only use NOT on single operands!");
                return !args[0];
            case AND:
                return ReikaArrayHelper.isAllTrue(args);
            case NAND:
                return !AND.evaluate(args);
            case OR:
                return ReikaArrayHelper.containsTrue(args);
            case NOR:
                return !OR.evaluate(args);
            case XOR:
                int c = 0;
                for (boolean b : args) {
                    if (b)
                        c++;
                }
                return c == 1;
            case XNOR:
                return !XOR.evaluate(args);
        }
        throw new UnreachableCodeException();
    }

}
