/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.measures;

import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.search.AccessionCluster;

/**
 * Calculate cluster distance as mean distance between all cross-cluster pairs.
 *
 * @author hermandebeukelaer
 */
public class GroupAverageClusterDistance extends ClusterDistanceMeasure {

    public GroupAverageClusterDistance(DistanceMeasure dm){
        super(dm);
    }

    @Override
    public double calculate(AccessionCluster clust1, AccessionCluster clust2) {
        double sum = 0.0;
        for(Accession a : clust1.getAccessions()){
            for(Accession b: clust2.getAccessions()){
                sum += dm.calculate(a, b);
            }
        }
        sum = sum / (clust1.size()*clust2.size());
        return sum;
    }

}
