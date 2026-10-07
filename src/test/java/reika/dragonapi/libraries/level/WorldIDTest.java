package reika.dragonapi.libraries.level;

import java.nio.file.Path;
import java.util.Set;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class WorldIDTest {
    @Test
    void savedModListAndLongIndexSurviveReadWrite(@TempDir Path directory) throws Exception {
        var tag = new CompoundTag();
        tag.putLong("creationTime", 12345L);
        tag.putLong("sourceSession", 54321L);
        tag.putLong("sessionIndex", 3000000000L);
        tag.putString("originalFolder", "original-save");
        tag.putString("creatingPlayer", "placer");
        var mods = new ListTag();
        mods.add(StringTag.valueOf("dragonapi"));
        mods.add(StringTag.valueOf("rotarycraft"));
        tag.put("mods", mods);
        Path original = directory.resolve("original.dat"), saved = directory.resolve("saved.dat");
        NbtIo.writeCompressed(tag, original);
        var reader = ReikaWorldHelper.WorldID.class.getDeclaredMethod("readFile", java.io.File.class);
        var writer = ReikaWorldHelper.WorldID.class.getDeclaredMethod("writeToFile", java.io.File.class);
        reader.setAccessible(true);
        writer.setAccessible(true);
        var id = (ReikaWorldHelper.WorldID)reader.invoke(null, original.toFile());
        assertTrue(id.isValid());
        assertEquals(Set.of("dragonapi", "rotarycraft"), id.getMods());
        writer.invoke(id, saved.toFile());
        var restored = (ReikaWorldHelper.WorldID)reader.invoke(null, saved.toFile());
        assertEquals(3000000000L, restored.sessionWorldIndex);
        assertEquals(id.getMods(), restored.getMods());
        assertEquals(id.originalFolder, restored.originalFolder);
    }
}
