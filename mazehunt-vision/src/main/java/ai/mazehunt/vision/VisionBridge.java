package ai.mazehunt.vision;

import ai.mazehunt.api.model.Message;
import ai.mazehunt.api.model.ModelClient;
import ai.mazehunt.api.model.ModelRequest;
import ai.mazehunt.core.router.ModelRouter;
import ai.mazehunt.core.util.Mimes;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Helper that routes image-conditioned requests to whichever vision-capable
 * model the router chose. The rest of the agent never needs to know which
 * backend answered.
 */
public final class VisionBridge {

    private final ModelRouter router;

    public VisionBridge(ModelRouter router) { this.router = router; }

    public String describe(Path imageFile, String instruction) {
        try {
            return describe(Files.readAllBytes(imageFile),
                    Mimes.imageMimeFromPath(imageFile), instruction);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public String describe(byte[] imageBytes, String mimeType, String instruction) {
        ModelClient vm = router.pick(ModelRouter.Role.VISION, ModelRouter.Task.light());
        Message msg = new Message(Message.Role.USER, List.of(
                new Message.TextPart(instruction == null
                        ? "Describe this image precisely; do not speculate beyond what is visible."
                        : instruction),
                new Message.ImagePart(imageBytes,
                        mimeType == null ? Mimes.DEFAULT_IMAGE : mimeType)));
        return vm.complete(ModelRequest.of(List.of(msg))).text();
    }
}
