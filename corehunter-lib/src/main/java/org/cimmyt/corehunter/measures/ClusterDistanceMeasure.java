/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.measures;

import org.cimmyt.corehunter.search.AccessionCluster;

/**
 * Computes distances between clusters of accessions.
 *
 * @author hermandebeukelaer
 */
public abstract class ClusterDistanceMeasure {

    // Normal distance measure, between pairs of accessions, which will be used
    // to compute distances between clusters
    protected DistanceMeasure dm;

    public ClusterDistanceMeasure(DistanceMeasure dm){
        this.dm = dm;
    }
    
    /**
     * Calculate distance between two clusters of accessions.
     *
     * @param clust1
     * @param clust2
     * @return
     */
    public abstract double calculate(AccessionCluster clust1, AccessionCluster clust2);

    public boolean needsCentroid(){
        return false;
    }

}
