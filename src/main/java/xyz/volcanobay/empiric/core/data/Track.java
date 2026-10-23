package xyz.volcanobay.empiric.core.data;

/**
 * Whether a viewer gets a synced field, as decided by the method named in {@link Data#track()}.
 * Going back to {@link #SYNC} from either of the others always sends the field in full again.
 */
public enum Track {

    /**
     * The viewer gets the field.
     */
    SYNC,

    /**
     * Updates stop, and the viewer's client keeps the last value it had.
     */
    PAUSE,

    /**
     * Updates stop, and the viewer's client resets the field to its default.
     */
    CLEAR
}
