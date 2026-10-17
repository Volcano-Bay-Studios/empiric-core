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


        int finalId;
        if (!isDefaultNetwork) {
            Set<Integer> existingIds = new HashSet<>();
            for (SpatialPos p : members) {
                int id = cache.get(p.x(), p.y(), p.z());
                if (id != -1) {
                    existingIds.add(findRoot(id));
                }
            }
            if (existingIds.isEmpty()) {
                finalId = idGenerator.getAndIncrement();
                dsu.put(finalId, finalId);
            } else {
                Iterator<Integer> it = existingIds.iterator();
                finalId = it.next();
                while (it.hasNext()) {
                    union(finalId, it.next());
                }
            }
        } else {
            finalId = -2;
            dsu.put(-2, -2);
        }

        networkPositions.putIfAbsent(finalId, new HashSet<>());
        for (SpatialPos p : members) {
            cache.put(p.x(), p.y(), p.z(), finalId);
            if (!isDefaultNetwork) {
                networkPositions.get(finalId).add(p); // If it is the default network, we don't want to waste memory filling up these maps.
            }
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
            cache.put(x, y, z, finalId);
            networkPositions.computeIfAbsent(finalId, k -> new HashSet<>()).add(new SpatialPos(x, y, z));
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
            cache.put(x, y, z, finalId);
            networkPositions.computeIfAbsent(finalId, k -> new HashSet<>()).add(new SpatialPos(x, y, z));
            update(finalId);
        }
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

        if (r1 != r2) {
            Set<SpatialPos> nodes1 = networkPositions.computeIfAbsent(r1, k -> new HashSet<>());
            Set<SpatialPos> nodes2 = networkPositions.remove(r2);

            if (nodes2 != null) {
                nodes1.addAll(nodes2);
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