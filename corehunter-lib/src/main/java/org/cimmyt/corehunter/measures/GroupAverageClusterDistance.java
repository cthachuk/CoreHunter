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
