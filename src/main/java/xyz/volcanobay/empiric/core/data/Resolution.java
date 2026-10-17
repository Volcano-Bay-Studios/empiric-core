package xyz.volcanobay.empiric.core.data;

public record Resolution(Mode mode, int depth, int level, ModeResolver resolver, ListUpdate listUpdate) {

    public static final ModeResolver NO_RESOLVER = new ModeResolver.None();
    public static final Resolution DEFAULT = new Resolution(Mode.INHERIT, -1, 0, NO_RESOLVER, ListUpdate.SET);

    public Resolution child(Mode resolved) {
        if (resolved == Mode.UPDATE && depth != 0) {
            return new Resolution(Mode.UPDATE, depth < 0 ? depth : depth - 1, level + 1, resolver, ListUpdate.SET);
        }
        return new Resolution(Mode.INHERIT, -1, level + 1, resolver, ListUpdate.SET);
    }

    public Resolution withMode(Mode mode, int depth) {
        return new Resolution(mode, depth, level, resolver, listUpdate);
    }
}
