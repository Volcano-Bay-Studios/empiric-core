package xyz.volcanobay.empiric.core.spatial;


import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public abstract class ManagedSpatialNetwork {
    protected final SpatialMap cache;
    protected final AtomicInteger idGenerator = new AtomicInteger(1);
    protected final Map<Integer, Integer> dsu = new HashMap<>();
    protected final Map<Integer, Set<SpatialPos>> networkPositions = new HashMap<>();

    public ManagedSpatialNetwork(int initialCacheSize) {
        this.cache = new SpatialMap(initialCacheSize);
    }

    /**
     * Abstract method to fetch all connected points from the infinite world source.
     */
    protected abstract Discovery discoverNetworkMembers(long x, long y, long z);

    /**
     * Determines if a connection can exist between the point at (x1, y1, z1)
     * and the point at (x2, y2, z2).
     */
    protected abstract boolean canConnect(long x1, long y1, long z1, long x2, long y2, long z2);

    /**
     * Returns if a block is allowed to be part of this network.
     */
    protected abstract boolean isMember(long x, long y, long z);

    protected boolean allowDefaultNetwork() {
        return false;
    }

    public void merge(int first, int second) {
    }

    public void split(int first, int second) {
    }

    public void update(int id) {
    }

    /**
     * Returns a set of all positions in a given network.
     */
    public Set<SpatialPos> getNetworkMembers(int networkId) {
        int rootId = findRoot(networkId);
        return Collections.unmodifiableSet(networkPositions.getOrDefault(rootId, Collections.emptySet()));
    }

    /**
     * This will try to find a network, otherwise it will return the identity of the default network.
     */
    public int get(long x, long y, long z) {
        int rawId = cache.get(x, y, z);
        if (rawId != -1) {
            return findRoot(rawId);
        }
        return -2;
    }

    /**
     * This will find or create a network in the cache.
     */
    public int getOrDiscover(long x, long y, long z) {
        int rawId = cache.get(x, y, z);
        if (rawId != -1) {
            return findRoot(rawId);
        }

        if (!isMember(x, y, z)) {
            return -1;
        }

        Discovery discovery = discoverNetworkMembers(x, y, z);
        Collection<SpatialPos> members = discovery.members();
        boolean isDefaultNetwork = discovery.overflow();
        if (members.isEmpty()) {
            return -1;
        }
        if (!allowDefaultNetwork() && isDefaultNetwork) {
            return -1;
        }


        Set<Integer> existingIds = new HashSet<>();
        boolean grew = false;
        for (SpatialPos p : members) {
            int id = cache.get(p.x(), p.y(), p.z());
            if (id != -1) {
                existingIds.add(findRoot(id));
            } else {
                grew = true;
            }
        }
        int finalId;
        if (isDefaultNetwork) {
            finalId = -2;
            dsu.put(-2, -2);
        } else if (existingIds.isEmpty()) {
            finalId = idGenerator.getAndIncrement();
            dsu.put(finalId, finalId);
        } else {
            finalId = existingIds.iterator().next();
        }
        // An overflowing flood fill absorbs every network it ran into.
        for (int id : existingIds) {
            union(finalId, id);
        }
        finalId = findRoot(finalId);

        for (SpatialPos p : members) {
            cache.put(p.x(), p.y(), p.z(), finalId);
            if (finalId != -2) {
                networkPositions.computeIfAbsent(finalId, k -> new HashSet<>()).add(p); // If it is the default network, we don't want to waste memory filling up these maps.
            }
        }
        // if (grew && !existingIds.isEmpty())
        if (grew && !existingIds.isEmpty() && finalId != -2) {
            update(finalId);
        }
        return finalId;
    }

    public int addPoint(long x, long y, long z) {
        if (cache.get(x, y, z) != -1) {
            return findRoot(cache.get(x, y, z));
        }

        Set<Integer> neighborRoots = new HashSet<>();
        for (SpatialPos n : getPhysicalNeighbors(x, y, z)) {
            int nid = cache.get(n.x(), n.y(), n.z());
            if (nid != -1 && canConnect(x, y, z, n.x(), n.y(), n.z())) {
                neighborRoots.add(findRoot(nid));
            }
        }

        if (neighborRoots.isEmpty()) {
            return getOrDiscover(x, y, z);
        } else {
            int finalId = neighborRoots.iterator().next();
            for (int r : neighborRoots) {
                union(finalId, r);
            }
            finalId = findRoot(finalId);
            cache.put(x, y, z, finalId);
            if (finalId != -2) {
                networkPositions.computeIfAbsent(finalId, k -> new HashSet<>()).add(new SpatialPos(x, y, z));
            }
            update(finalId);
            return finalId;
        }
    }

    public void removePoint(long x, long y, long z) {
        int netId = cache.get(x, y, z);
        if (netId == -1) {
            return;
        }
        int rootId = findRoot(netId);

        cache.remove(x, y, z);

        Set<SpatialPos> oldSet = networkPositions.get(rootId);
        if (oldSet != null) {
            oldSet.remove(new SpatialPos(x, y, z));
        }

        SpatialPos[] neighbors = getPhysicalNeighbors(x, y, z);
        List<SpatialPos> oldNetworkNeighbors = new ArrayList<>();

        for (SpatialPos n : neighbors) {
            int nRaw = cache.get(n.x(), n.y(), n.z());
            if (nRaw != -1 && findRoot(nRaw) == rootId) {
                oldNetworkNeighbors.add(n);
            }
        }

        if (oldNetworkNeighbors.size() > 1) {
            handlePotentialSplit(oldNetworkNeighbors, rootId);
        }
        update(rootId);
    }

    public void updatePoint(long x, long y, long z) {
        int currentRawId = cache.get(x, y, z);
        if (currentRawId == -1) {
            return;
        }
        int oldRootId = findRoot(currentRawId);

        cache.remove(x, y, z);

        Set<SpatialPos> oldSet = networkPositions.get(oldRootId);
        if (oldSet != null) {
            oldSet.remove(new SpatialPos(x, y, z));
        }

        SpatialPos[] physicalNeighbors = getPhysicalNeighbors(x, y, z);
        List<SpatialPos> oldNetworkNeighbors = new ArrayList<>();

        for (SpatialPos n : physicalNeighbors) {
            int nRaw = cache.get(n.x(), n.y(), n.z());
            if (nRaw != -1 && findRoot(nRaw) == oldRootId) {
                oldNetworkNeighbors.add(n);
            }
        }

        if (oldNetworkNeighbors.size() > 1) {
            handlePotentialSplit(oldNetworkNeighbors, oldRootId);
        }

        Set<Integer> currentNeighborRoots = new HashSet<>();
        for (SpatialPos n : physicalNeighbors) {
            int nRaw = cache.get(n.x(), n.y(), n.z());
            if (nRaw != -1 && canConnect(x, y, z, n.x(), n.y(), n.z())) {
                currentNeighborRoots.add(findRoot(nRaw));
            }
        }

        if (currentNeighborRoots.isEmpty()) {
            int newId = idGenerator.getAndIncrement();
            dsu.put(newId, newId);
            cache.put(x, y, z, newId);
            networkPositions.computeIfAbsent(newId, k -> new HashSet<>()).add(new SpatialPos(x, y, z));
        } else {
            Iterator<Integer> it = currentNeighborRoots.iterator();
            int finalId = it.next();
            while (it.hasNext()) {
                union(finalId, it.next());
            }
            finalId = findRoot(finalId);
            cache.put(x, y, z, finalId);
            if (finalId != -2) {
                networkPositions.computeIfAbsent(finalId, k -> new HashSet<>()).add(new SpatialPos(x, y, z));
            }
            update(finalId);
        }
    }

    /**
     * Brings the cache in line with the world after the given points changed together.
     * Points that stopped being members leave their network, which may split it, and new members join any cached network next to them.
     * New members with no cached neighbor are left for discovery, so changes in untouched space cost nothing.
     */
    public void refreshPoints(Collection<SpatialPos> points) {
        Map<Integer, Set<SpatialPos>> removed = new HashMap<>();
        for (SpatialPos p : points) {
            int raw = cache.get(p.x(), p.y(), p.z());
            if (raw == -1 || isMember(p.x(), p.y(), p.z())) {
                continue;
            }
            int rootId = findRoot(raw);
            cache.remove(p.x(), p.y(), p.z());
            Set<SpatialPos> oldSet = networkPositions.get(rootId);
            if (oldSet != null) {
                oldSet.remove(p);
            }
            removed.computeIfAbsent(rootId, k -> new LinkedHashSet<>()).add(p);
        }
        for (Map.Entry<Integer, Set<SpatialPos>> entry : removed.entrySet()) {
            int rootId = entry.getKey();
            Set<SpatialPos> neighbors = new LinkedHashSet<>();
            for (SpatialPos p : entry.getValue()) {
                for (SpatialPos n : getPhysicalNeighbors(p)) {
                    int nRaw = cache.get(n.x(), n.y(), n.z());
                    if (nRaw != -1 && findRoot(nRaw) == rootId && isMember(n.x(), n.y(), n.z())) {
                        neighbors.add(n);
                    }
                }
            }
            if (rootId == -2) {
                splitDefault(new ArrayList<>(neighbors));
            } else {
                handlePotentialSplit(new ArrayList<>(neighbors), rootId);
            }
            update(rootId);
        }

        List<SpatialPos> joined = new ArrayList<>();
        List<SpatialPos> waiting = new ArrayList<>();
        for (SpatialPos p : points) {
            if (cache.get(p.x(), p.y(), p.z()) == -1 && isMember(p.x(), p.y(), p.z())) {
                waiting.add(p);
            }
        }
        boolean progress = true;
        while (progress && !waiting.isEmpty()) {
            progress = false;
            Iterator<SpatialPos> it = waiting.iterator();
            while (it.hasNext()) {
                SpatialPos p = it.next();
                if (hasCachedNeighbor(p)) {
                    addPoint(p.x(), p.y(), p.z());
                    joined.add(p);
                    it.remove();
                    progress = true;
                }
            }
        }
        // A finite network that now touches undiscovered members may have opened up, so flood from there.
        for (SpatialPos p : joined) {
            int raw = cache.get(p.x(), p.y(), p.z());
            if (raw == -1 || findRoot(raw) == -2) {
                continue;
            }
            for (SpatialPos n : getPhysicalNeighbors(p)) {
                if (cache.get(n.x(), n.y(), n.z()) == -1 && canConnect(p.x(), p.y(), p.z(), n.x(), n.y(), n.z())) {
                    getOrDiscover(n.x(), n.y(), n.z());
                }
            }
        }
    }

    private boolean hasCachedNeighbor(SpatialPos p) {
        for (SpatialPos n : getPhysicalNeighbors(p)) {
            if (cache.get(n.x(), n.y(), n.z()) != -1 && canConnect(p.x(), p.y(), p.z(), n.x(), n.y(), n.z())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Splits enclosed pockets off the default network.
     * The default network isn't tracked position by position, so this floods from every neighbor at once, one step each in turn, and stops as soon as
     * only one flood is left running: that one is the open space, and every flood that ran dry first was a pocket.
     * Floods also cross undiscovered members, since an overflowing discovery only caches part of what it saw, and a flood that grows past
     * {@link #maxSize()} is open space.
     */
    private void splitDefault(List<SpatialPos> neighbors) {
        int count = neighbors.size();
        if (count <= 1) {
            return;
        }
        int[] parent = new int[count];
        List<Deque<SpatialPos>> queues = new ArrayList<>();
        int[] sizes = new int[count];
        boolean[] open = new boolean[count];
        Map<SpatialPos, Integer> owner = new HashMap<>();
        for (int i = 0; i < count; i++) {
            parent[i] = i;
            queues.add(new ArrayDeque<>(List.of(neighbors.get(i))));
            sizes[i] = 1;
            owner.put(neighbors.get(i), i);
        }

        while (true) {
            boolean anyOpen = false;
            int running = 0;
            for (int i = 0; i < count; i++) {
                if (parent[i] != i) {
                    continue;
                }
                if (open[i]) {
                    anyOpen = true;
                } else if (!queues.get(i).isEmpty()) {
                    running++;
                }
            }
            if (running == 0 || (!anyOpen && running == 1)) {
                break;
            }
            for (int i = 0; i < count; i++) {
                if (parent[i] != i || open[i] || queues.get(i).isEmpty()) {
                    continue;
                }
                SpatialPos curr = queues.get(i).poll();
                for (SpatialPos n : getPhysicalNeighbors(curr)) {
                    if (!canConnect(curr.x(), curr.y(), curr.z(), n.x(), n.y(), n.z())) {
                        continue;
                    }
                    int raw = cache.get(n.x(), n.y(), n.z());
                    if (raw == -1 ? !isMember(n.x(), n.y(), n.z()) : findRoot(raw) != -2) {
                        continue;
                    }
                    Integer other = owner.get(n);
                    if (other == null) {
                        owner.put(n, i);
                        queues.get(i).add(n);
                        if (++sizes[i] > maxSize()) {
                            open[i] = true;
                        }
                        continue;
                    }
                    int otherRoot = other;
                    while (parent[otherRoot] != otherRoot) {
                        otherRoot = parent[otherRoot];
                    }
                    if (otherRoot != i) {
                        parent[otherRoot] = i;
                        queues.get(i).addAll(queues.get(otherRoot));
                        queues.get(otherRoot).clear();
                        sizes[i] += sizes[otherRoot];
                        open[i] |= open[otherRoot];
                    }
                }
            }
        }

        // If every flood ran dry, the biggest keeps the default network.
        int keep = -1;
        boolean anyOpen = false;
        for (int i = 0; i < count; i++) {
            if (parent[i] == i && (open[i] || !queues.get(i).isEmpty())) {
                anyOpen = true;
            }
        }
        if (!anyOpen) {
            for (int i = 0; i < count; i++) {
                if (parent[i] == i && (keep == -1 || sizes[i] > sizes[keep])) {
                    keep = i;
                }
            }
        }
        Map<Integer, List<SpatialPos>> pockets = new HashMap<>();
        for (Map.Entry<SpatialPos, Integer> entry : owner.entrySet()) {
            int root = entry.getValue();
            while (parent[root] != root) {
                root = parent[root];
            }
            if (root != keep && !open[root] && queues.get(root).isEmpty()) {
                pockets.computeIfAbsent(root, k -> new ArrayList<>()).add(entry.getKey());
            }
        }
        for (List<SpatialPos> pocket : pockets.values()) {
            int newId = idGenerator.getAndIncrement();
            dsu.put(newId, newId);
            Set<SpatialPos> positions = new HashSet<>(pocket);
            networkPositions.put(newId, positions);
            for (SpatialPos p : positions) {
                cache.put(p.x(), p.y(), p.z(), newId);
            }
            split(-2, newId);
        }
    }

    /**
     * The most members a flood may reach before it counts as open space.
     */
    protected int maxSize() {
        return 100_000;
    }

    private Set<SpatialPos> performBFS(SpatialPos start, int rootId) {
        Set<SpatialPos> visited = new HashSet<>();
        Queue<SpatialPos> queue = new LinkedList<>();
        queue.add(start);

        while (!queue.isEmpty()) {
            SpatialPos curr = queue.poll();
            if (visited.contains(curr)) {
                continue;
            }
            visited.add(curr);

            for (SpatialPos n : getPhysicalNeighbors(curr)) {
                int neighborId = cache.get(n.x(), n.y(), n.z());
                if (neighborId != -1 && findRoot(neighborId) == rootId && canConnect(curr.x(), curr.y(), curr.z(), n.x(), n.y(), n.z())) {
                    queue.add(n);
                }
            }
        }
        return visited;
    }

    private void handlePotentialSplit(List<SpatialPos> neighbors, int oldRootId) {
        if (neighbors.size() <= 1) {
            return;
        }

        Set<SpatialPos> primaryComponent = performBFS(neighbors.getFirst(), oldRootId);
        Set<SpatialPos> processedNodes = new HashSet<>(primaryComponent);

        for (int i = 1; i < neighbors.size(); i++) {
            SpatialPos n = neighbors.get(i);

            if (!processedNodes.contains(n)) {
                Set<SpatialPos> newComponent = reindexComponent(n, oldRootId);
                processedNodes.addAll(newComponent);
            }
        }
    }

    private Set<SpatialPos> reindexComponent(SpatialPos startNode, int oldRootId) {
        int newId = idGenerator.getAndIncrement();
        dsu.put(newId, newId);

        networkPositions.put(newId, new HashSet<>());

        Set<SpatialPos> componentNodes = new HashSet<>();
        Queue<SpatialPos> queue = new LinkedList<>();
        queue.add(startNode);

        Set<SpatialPos> oldNetworkNodes = networkPositions.get(oldRootId);

        while (!queue.isEmpty()) {
            SpatialPos curr = queue.poll();

            if (componentNodes.contains(curr)) {
                continue;
            }
            componentNodes.add(curr);

            cache.put(curr.x(), curr.y(), curr.z(), newId);

            networkPositions.get(newId).add(curr);
            if (oldNetworkNodes != null) {
                oldNetworkNodes.remove(curr);
            }

            for (SpatialPos n : getPhysicalNeighbors(curr)) {
                int currentRawId = cache.get(n.x(), n.y(), n.z());
                if (currentRawId != -1) {
                    int currentRoot = findRoot(currentRawId);
                    if (currentRoot == oldRootId && canConnect(curr.x(), curr.y(), curr.z(), n.x(), n.y(), n.z())) {
                        queue.add(n);
                    }
                }
            }
        }
        split(oldRootId, newId);
        return componentNodes;
    }

    private SpatialPos[] getPhysicalNeighbors(long x, long y, long z) {
        return new SpatialPos[]{new SpatialPos(x + 1, y, z), new SpatialPos(x - 1, y, z), new SpatialPos(x, y + 1, z), new SpatialPos(x, y - 1, z), new SpatialPos(x, y, z + 1), new SpatialPos(x, y, z - 1)};
    }

    private SpatialPos[] getPhysicalNeighbors(SpatialPos pos) {
        return getPhysicalNeighbors(pos.x(), pos.y(), pos.z());
    }

    private int findRoot(int id) {
        if (id == -1) {
            return -1;
        }
        if (!dsu.containsKey(id)) {
            dsu.put(id, id);
        }
        int parent = dsu.get(id);
        if (parent == id) {
            return id;
        }
        int root = findRoot(parent);
        dsu.put(id, root);
        return root;
    }

    private void union(int id1, int id2) {
        int r1 = findRoot(id1);
        int r2 = findRoot(id2);
        // The default network always survives a merge.
        if (r2 == -2) {
            r2 = r1;
            r1 = -2;
        }

        if (r1 != r2) {
            Set<SpatialPos> nodes2 = networkPositions.remove(r2);
            if (r1 != -2 && nodes2 != null) {
                networkPositions.computeIfAbsent(r1, k -> new HashSet<>()).addAll(nodes2);
            }

            dsu.put(r2, r1);
            merge(r1, r2);
        }
    }

    public record SpatialPos(long x, long y, long z) {
    }

    public record Discovery(boolean overflow, Collection<SpatialPos> members) {
    }
}