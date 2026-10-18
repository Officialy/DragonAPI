package reika.dragonapi.client;

import java.io.InputStream;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import reika.dragonapi.auxiliary.FileInputThread;

/** Resource lookup/snapshot and result delivery belong to client main; worker decoding sees only detached bytes. */
public final class ClientFileInputs {
    private ClientFileInputs() {}

    public static CompletionStage<Object> loadResource(Identifier resource, FileInputThread.FileType type, Executor worker) {
        Objects.requireNonNull(resource, "resource"); Objects.requireNonNull(type, "file type"); Objects.requireNonNull(worker, "worker");
        var minecraft = Minecraft.getInstance();
        var result = new CompletableFuture<Object>();
        minecraft.execute(() -> {
            byte[] snapshot;
            try (InputStream stream = minecraft.getResourceManager().open(resource)) {
                snapshot = stream.readAllBytes();
            } catch (Exception failure) { result.completeExceptionally(failure); return; }
            new FileInputThread().setFiletype(type).setData(snapshot).submit(worker).whenComplete((value, failure) ->
                    minecraft.execute(() -> {
                        if (failure == null) result.complete(value);
                        else result.completeExceptionally(failure);
                    }));
        });
        return result.minimalCompletionStage();
    }
}
