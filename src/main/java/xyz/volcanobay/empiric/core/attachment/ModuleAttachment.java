package xyz.volcanobay.empiric.core.attachment;

import xyz.volcanobay.empiric.core.EmpiricCore;
import xyz.volcanobay.empiric.core.module.Module;
import xyz.volcanobay.empiric.core.module.ModuleHolder;
import xyz.volcanobay.empiric.core.module.Side;

public final class ModuleAttachment implements Attachment {

    private final Module<?> parent;
    private final ModuleHolder modules = new ModuleHolder(this);

    public ModuleAttachment(Module<?> parent) {
        this.parent = parent;
    }

    public Module<?> parent() {
        return parent;
    }

    @Override
    public AttachmentType<?, ?> attachmentType() {
        return EmpiricCore.modules().moduleAttachments();
    }

    @Override
    public ModuleHolder modules() {
        return modules;
    }

    @Override
    public Side side() {
        return parent.side();
    }
}
