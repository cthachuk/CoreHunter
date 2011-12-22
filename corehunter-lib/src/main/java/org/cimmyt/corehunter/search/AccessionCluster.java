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

package org.cimmyt.corehunter.search;

import java.util.ArrayList;
import java.util.List;
import org.cimmyt.corehunter.Accession;

/**
 *
 * @author hermandebeukelaer
 */
public class AccessionCluster {

    private static long nextid = 0;

    private List<Accession> accessions;
    private long id;

    private boolean computeCentroid;
    private ClusterCentroid centroid;
    
    public AccessionCluster(boolean computeCentroid){
        accessions = new ArrayList<Accession>();
        id = nextid;
        this.computeCentroid = computeCentroid;
        if(computeCentroid){
            centroid = new ClusterCentroid(id);
        } else {
            centroid = null;
        }

        nextid++;
    }
    
    public AccessionCluster(Accession a, boolean computeCentroid){
        this(computeCentroid);

        if(computeCentroid){
            // Set centroid
            centroid.update(a, 0);
        }
        // Add first accession
        accessions.add(a);
    }

    public void merge(AccessionCluster clust){
        if(computeCentroid){
            // Update centroid
            centroid.update(clust.getAccessions(), accessions.size());
        }
        // Add accessions to list
        accessions.addAll(clust.getAccessions());
    }

    public long id(){
        return id;
    }

    public ClusterCentroid getCentroid(){
        return centroid;
    }

    public List<Accession> getAccessions(){
        return accessions;
    }

    public List<String> getAccessionNames(){
        List<String> names = new ArrayList<String>(accessions.size());
        for(Accession a : accessions){
            names.add(a.getName());
        }
        return names;
    }

    public int size(){
        return accessions.size();
    }

}
