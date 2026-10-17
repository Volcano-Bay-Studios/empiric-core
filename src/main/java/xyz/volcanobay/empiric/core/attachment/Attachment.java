package xyz.volcanobay.empiric.core.attachment;

import xyz.volcanobay.empiric.core.module.ModuleHolder;
import xyz.volcanobay.empiric.core.module.Side;

/**
 * Attachments connect modules to an {@link AttachmentType}. Other applications can implement their own attachment types.
 * An example of an attachment type would be attaching to an Entity, or attaching to a game Level.
 */
public interface Attachment {
    AttachmentType<?, ?> attachmentType();

    ModuleHolder modules();

    Side side();
}
