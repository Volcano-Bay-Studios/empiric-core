package xyz.volcanobay.empiric.core.module;

import java.util.BitSet;

public interface DirtyConsumer {

    void accept(Module<?> module, BitSet dirty);
}
