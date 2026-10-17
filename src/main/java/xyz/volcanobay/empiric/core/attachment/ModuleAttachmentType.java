package xyz.volcanobay.empiric.core.attachment;

import xyz.volcanobay.empiric.core.data.DataBuffer;
import xyz.volcanobay.empiric.core.data.DataContext;
import xyz.volcanobay.empiric.core.module.Module;
import xyz.volcanobay.empiric.core.module.ModuleSystem;
import xyz.volcanobay.empiric.core.module.Side;

public final class ModuleAttachmentType extends AttachmentType<ModuleAttachment, Module<?>> {

    private final ModuleSystem system;

    public ModuleAttachmentType(ModuleSystem system) {
        super("empiric:module");
        this.system = system;
    }

    @Override
    public ModuleAttachment get(Module<?> parent) {
        return parent.children();
    }

    @Override
    public void writeKey(DataBuffer buffer, ModuleAttachment attachment, DataContext context) {
        system.writeLocator(buffer, attachment.parent(), context);
    }

    @Override
    public ModuleAttachment readKey(DataBuffer buffer, Side side, DataContext context) {
        Module<?> parent = system.readLocator(buffer, side, context);
        return parent == null ? null : parent.children();
    }
}
