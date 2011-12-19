/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.search;

import java.util.ArrayList;
import java.util.List;
import org.cimmyt.corehunter.Accession;

/**
 *
 * @author hermandebeukelaer
 */
public class ClusterCentroid extends Accession {

    public ClusterCentroid(long clusterID){
        super("Cluster Centroid " + clusterID);
        ssrValues = new ArrayList<List<Double>>();
    }

    /**
     * Update centroid after adding accession a to the cluster.
     */
    public void update(Accession a, int prevSize){
        List<List<Double>> newValues = a.getSSRValues();
        List<Double> alleles, newAlleles;
        if(prevSize == 0){
            // First accession is added, so this is also the centroid
            for(int i=0; i<newValues.size(); i++){
                newAlleles = newValues.get(i);
                alleles = new ArrayList<Double>(newAlleles.size());
                alleles.addAll(newAlleles);
                ssrValues.add(alleles);
            }
        } else {
            // Not the first accession, properly update centroid
            Double freq, newFreq, updFreq;
            for(int i=0; i<ssrValues.size(); i++){
                alleles = ssrValues.get(i);
                newAlleles = newValues.get(i);
                for(int j=0; j<alleles.size(); j++){
                    freq = alleles.get(j);
                    newFreq = newAlleles.get(j);
                    if(newFreq != null){
                        if(freq != null){
                            updFreq = ((freq * prevSize) + newFreq)/(prevSize+1);
                            alleles.set(j, updFreq);
                        } else {
                            alleles.set(j, newFreq);
                        }
                    }
                }
            }
        }
    }

    /**
     * Update centroid after adding multiple accessions
     */
    public void update(List<Accession> accessions, int prevSize){
        // To do: direct, faster implementation
        for(Accession a : accessions){
            update(a, prevSize);
            prevSize++;
        }
    }

    @Override
    public String toString(){
        StringBuilder str = new StringBuilder("(");
        for(List<Double> alleles : ssrValues){
            for(Double allele : alleles){
                str.append(allele + ",");
            }
        }
        str.append(")");
        return str.toString();
    }

}
