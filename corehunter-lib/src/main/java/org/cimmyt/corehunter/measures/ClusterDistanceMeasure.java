//  Copyright 2008,2011 Chris Thachuk, Herman De Beukelaer
//
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.

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
