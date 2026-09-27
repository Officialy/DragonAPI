/*******************************************************************************
 * @author Reika Kalseki
 *
 * Copyright 2017
 *
 * All rights reserved.
 * Distribution of the software in any form is only allowed with
 * explicit, prior permission from the owner.
 ******************************************************************************/
package reika.dragonapi.instantiable.data.collections;


import java.util.HashSet;


public class ClassNameCache {

    private final HashSet<String> cache = new HashSet();

    public void add(String s) {
        if (s == null || s.isBlank())
            throw new IllegalArgumentException("Class name cannot be empty");
        s = s.trim();
        if (s.endsWith(".*"))
            s = s.substring(0, s.length() - 2);
        else if (s.endsWith("*"))
            s = s.substring(0, s.length() - 1);
        while (s.endsWith("."))
            s = s.substring(0, s.length() - 1);
        if (s.isEmpty())
            throw new IllegalArgumentException("Wildcard must include a class or package prefix");
        cache.add(s);
    }

    public boolean contains(Class c) {
        if (c == null)
            return false;
        String s = c.getName();
        while (!s.isEmpty()) {
            if (cache.contains(s))
                return true;
            int idx = s.lastIndexOf('.');
            s = idx >= 0 ? s.substring(0, idx) : "";
        }
        return false;
    }

}
