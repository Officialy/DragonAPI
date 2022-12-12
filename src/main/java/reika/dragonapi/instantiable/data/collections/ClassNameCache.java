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
        while (s.charAt(s.length() - 1) == '*')
            s = s.substring(0, s.length() - 2);
        cache.add(s);
    }

    public boolean contains(Class c) {
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
