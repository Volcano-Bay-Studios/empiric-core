package xyz.volcanobay.empiric.core.ui.graph;

import xyz.volcanobay.empiric.core.data.Data;
import xyz.volcanobay.empiric.core.data.SelfSpec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A node graph as plain data, held in a module's synced {@code @Data} field. {@link NodeGraphView} builds its widgets
 * from it by diffing, so whatever changes it, an edit or a sync, the view follows. Edits from the view replace the whole
 * field, so the last writer wins.
 */
@SelfSpec
public class NodeGraph {

    @Data
    public List<GraphNode> nodes = new ArrayList<>();
    @Data
    public List<GraphLink> links = new ArrayList<>();

    public GraphNode node(int id) {
        for (GraphNode node : nodes) {
            if (node.id == id) {
                return node;
            }
        }
        return null;
    }

    public int nextId() {
        int max = 0;
        for (GraphNode node : nodes) {
            max = Math.max(max, node.id);
        }
        return max + 1;
    }

    public GraphNode add(String type, float x, float y) {
        GraphNode node = new GraphNode(nextId(), type, x, y);
        nodes.add(node);
        return node;
    }

    /**
     * Removes a node and every link touching it.
     */
    public boolean remove(int id) {
        links.removeIf(link -> link.from == id || link.to == id);
        return nodes.removeIf(node -> node.id == id);
    }

    /**
     * Links an output to an input. An input takes one link, so an older link into it is replaced. Returns false if the
     * link would join a node to itself.
     */
    public boolean connect(int from, String output, int to, String input) {
        if (from == to) {
            return false;
        }
        links.removeIf(link -> link.to == to && link.input.equals(input));
        links.add(new GraphLink(from, output, to, input));
        return true;
    }

    public void disconnect(GraphLink link) {
        links.remove(link);
    }

    /**
     * Removes the links on one port of a node.
     */
    public void disconnect(int node, String port, boolean input) {
        links.removeIf(link -> input ? link.to == node && link.input.equals(port) : link.from == node && link.output.equals(port));
    }

    public GraphLink linkInto(int node, String input) {
        for (GraphLink link : links) {
            if (link.to == node && link.input.equals(input)) {
                return link;
            }
        }
        return null;
    }

    public List<GraphLink> linksFrom(int node, String output) {
        List<GraphLink> found = new ArrayList<>();
        for (GraphLink link : links) {
            if (link.from == node && link.output.equals(output)) {
                found.add(link);
            }
        }
        return found;
    }

    /**
     * Whether following links from {@code from} reaches {@code to}, so linking {@code to} into {@code from} would loop.
     */
    public boolean reaches(int from, int to) {
        Set<Integer> seen = new HashSet<>();
        List<Integer> queue = new ArrayList<>(List.of(from));
        while (!queue.isEmpty()) {
            int current = queue.removeLast();
            if (current == to) {
                return true;
            }
            if (!seen.add(current)) {
                continue;
            }
            for (GraphLink link : links) {
                if (link.from == current) {
                    queue.add(link.to);
                }
            }
        }
        return false;
    }

    /**
     * Drops what a palette doesn't allow: nodes of unknown types or repeated ids, links to missing nodes or ports,
     * links between ports of different kinds, second links into one input and, if the palette forbids them, cycles.
     * Servers run this on graphs clients send. Returns whether anything changed.
     */
    public boolean sanitize(GraphPalette palette) {
        int before = nodes.size() + links.size();
        Set<Integer> ids = new HashSet<>();
        nodes.removeIf(node -> palette.type(node.type) == null || !ids.add(node.id) || !Float.isFinite(node.x) || !Float.isFinite(node.y));
        Set<String> inputs = new HashSet<>();
        List<GraphLink> kept = new ArrayList<>();
        for (GraphLink link : links) {
            GraphNode from = node(link.from);
            GraphNode to = node(link.to);
            if (from == null || to == null || from == to) {
                continue;
            }
            GraphPort output = palette.type(from.type).output(link.output);
            GraphPort input = palette.type(to.type).input(link.input);
            if (output == null || input == null || !input.accepts(output) || !inputs.add(link.to + ":" + link.input)) {
                continue;
            }
            kept.add(link);
        }
        links = kept;
        if (!palette.allowsCycles()) {
            List<GraphLink> acyclic = new ArrayList<>();
            List<GraphLink> all = links;
            links = acyclic;
            for (GraphLink link : all) {
                if (!reaches(link.to, link.from)) {
                    acyclic.add(link);
                }
            }
        }
        return nodes.size() + links.size() != before;
    }
}
