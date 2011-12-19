/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.measures;

import org.cimmyt.corehunter.search.AccessionCluster;

/**
 *
 * @author hermandebeukelaer
 */
public class CentroidClusterDistance extends ClusterDistanceMeasure {

    public CentroidClusterDistance(DistanceMeasure dm){
        super(dm);
    }

    @Override
    public double calculate(AccessionCluster clust1, AccessionCluster clust2) {
        return dm.calculate(clust1.getCentroid(), clust2.getCentroid());
    }

    @Override
    public boolean needsCentroid(){
        return true;
    }

}
