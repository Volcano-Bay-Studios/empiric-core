package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.data.Data;
import xyz.volcanobay.empiric.core.data.SelfSpec;

import java.util.Objects;

/**
 * A connection from one node's output port to another node's input port.
 */
@SelfSpec
public class GraphLink {

    @Data
    public int from;
    @Data
    public String output = "";
    @Data
    public int to;
    @Data
    public String input = "";

    public GraphLink() {
    }

    public GraphLink(int from, String output, int to, String input) {
        this.from = from;
        this.output = output;
        this.to = to;
        this.input = input;
    }

    /**
     * A stable key for diffing links between syncs.
     */
    public String key() {
        return from + ":" + output + ">" + to + ":" + input;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof GraphLink link && link.from == from && link.to == to && link.output.equals(output) && link.input.equals(input);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, output, to, input);
    }
}
