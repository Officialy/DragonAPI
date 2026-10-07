package reika.dragonapi.modinteract.lua;

import reika.dragonapi.modinteract.lua.LuaMethod.LuaMethodException;

final class LuaArguments {
    private LuaArguments() {}

    static int integer(Object value, String name, int minimum, int maximum) throws LuaMethodException {
        if (!(value instanceof Number number)) throw new LuaMethodException(name + " must be an integer");
        double numeric = number.doubleValue();
        if (!Double.isFinite(numeric) || numeric != Math.rint(numeric) || numeric < minimum || numeric > maximum)
            throw new LuaMethodException(name + " must be an integer between " + minimum + " and " + maximum);
        return (int)numeric;
    }
}
