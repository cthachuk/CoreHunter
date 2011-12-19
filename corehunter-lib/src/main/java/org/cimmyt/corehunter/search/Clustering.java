/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.search;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.measures.ClusterDistanceMeasure;

/**
 *
 * @author hermandebeukelaer
 */
public class Clustering {

    private Map<Long, AccessionCluster> clusters;
    private PriorityQueue<ClusterPair> mergeQueue;

    private int desiredClusters;
    private ClusterDistanceMeasure cm;

    public Clustering(int desiredClusters, ClusterDistanceMeasure cm){
        this.desiredClusters = desiredClusters;
        this.cm = cm;
        clusters = new HashMap<Long, AccessionCluster>();
        mergeQueue = new PriorityQueue<ClusterPair>(desiredClusters, new ClusterPairComparator());
    }

    public void reset(){
        clusters.clear();
        mergeQueue.clear();
    }

    public void setDesiredClusters(int nr){
        desiredClusters = nr;
    }

    /**
     * Add one accession. First, a new cluster is created for this accession.
     * Then, if number of desired clusters is exceeded, merge two clusters to
     * retain previous number of clusters.
     *
     * @param a
     */
    public void addAccession(Accession a){
        // Create new cluster, containing a
        AccessionCluster newCluster = new AccessionCluster(a, cm.needsCentroid());
        
        // Calculate distances to all existing clusters and update merge queue
        updateMergeQueue(newCluster);

        // Add new cluster to clusters
        clusters.put(newCluster.id(), newCluster);
        
        // Merge two clusters if desired number of clusters exceeded
        if(clusters.size() > desiredClusters){
            merge();
        }
    }

    public Collection<AccessionCluster> getClusters(){
        return clusters.values();
    }

    private void updateMergeQueue(AccessionCluster newCluster){
        double dist;
        ClusterPair clustPair;
        for(long id : clusters.keySet()){
            dist = cm.calculate(newCluster, clusters.get(id));
            clustPair = new ClusterPair(id, newCluster.id(), dist);
            mergeQueue.add(clustPair);
        }
    }

    /**
     * Merge these two clusters with smallest distance between them
     */
    private void merge(){
        // Get pair to merge
        ClusterPair toMerge = mergeQueue.poll();
        // Remove all other pairs containing one of the two ids which will now be merged
        Iterator<ClusterPair> it = mergeQueue.iterator();
        ClusterPair pair;
        while(it.hasNext()){
            pair = it.next();
            if(pair.getID1() == toMerge.getID1()
                    || pair.getID1() == toMerge.getID2()
                    || pair.getID2() == toMerge.getID1()
                    || pair.getID2() == toMerge.getID2()){
                it.remove();
            }
        }
        // Merge clusters
        AccessionCluster merged = clusters.remove(toMerge.getID1());
        AccessionCluster clust2 = clusters.remove(toMerge.getID2());
        merged.merge(clust2);
        // Update merge queue
        updateMergeQueue(merged);
        // Add merged cluster to clusters
        clusters.put(merged.id(), merged);
    }

    private class ClusterPair{

        private long id1;
        private long id2;

        private double dist;

        public ClusterPair(long id1, long id2, double dist){
            this.id1 = id1;
            this.id2 = id2;
            this.dist = dist;
        }

        public long getID1(){
            return id1;
        }

        public long getID2(){
            return id2;
        }

        public double getDist(){
            return dist;
        }

    }

    private class ClusterPairComparator implements Comparator<ClusterPair>{

        public int compare(ClusterPair o1, ClusterPair o2) {
            return Double.compare(o1.getDist(), o2.getDist());
        }

    }
}
