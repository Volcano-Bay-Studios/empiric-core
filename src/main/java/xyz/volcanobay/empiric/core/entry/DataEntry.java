package xyz.volcanobay.empiric.core.entry;

import xyz.volcanobay.empiric.core.data.tree.DataNode;

public record DataEntry<T>(String id, T value, DataNode extras) {
}
