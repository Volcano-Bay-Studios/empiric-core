package xyz.volcanobay.empiric.core.ui.blueprint;

import xyz.volcanobay.empiric.core.ui.Widget;

/**
 * A widget whose whole content comes from a layout file, looked up by id when it joins a root. It fills its parent and
 * holds the file's root widget, so the root's anchor places it. This is why screens can be pure data.
 */
public class BlueprintView extends Widget {

    private final String blueprint;

    public BlueprintView(String blueprint) {
        this.blueprint = blueprint;
        fill();
    }

    public String blueprint() {
        return blueprint;
    }
}
