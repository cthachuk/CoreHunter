// Copyright 2008 Chris Thachuk (chris.thachuk@gmail.com)
//
// This file is part of Core Hunter.

// Core Hunter is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Core Hunter is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Core Hunter.  If not, see <http://www.gnu.org/licenses/>.

package org.cimmyt.corehunter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public class AccessionCollection {
    private List<Accession> accessions;
    private Map<String, Accession> accessionNameMap;
    
    /**
     * 
     */
    public AccessionCollection() {
	accessions = new ArrayList<Accession>();
	accessionNameMap = new HashMap<String, Accession>();
    }

    public void add(Accession a) {
	if (!accessionNameMap.containsKey(a.getName())) {
	    accessions.add(a);
	    accessionNameMap.put(a.getName(), a);
	}
    }

    public void add(List<Accession> as) {
	for(Accession a : as) {
	    add(a);
	}
    }
    
    public void addDataset(AccessionDataset<?> ds) {
	// first determine if we need to add any new accessions
	Set<String> newAccessionNames = new HashSet<String>(ds.getAccessionNames());
	newAccessionNames.removeAll(accessionNameMap.keySet());
	
	for (String accessionName : newAccessionNames) {
	    Accession newAccession = new Accession(accessionName);
	    add(newAccession);
	}
	
	// next we bind trait values from new data set
	// to any existing accessions in our collection
	for (Accession accession : accessions) {
	    accession.bindTraitValues(ds);
	}
    }
    
    /**
     * getAccessionNames
     *
     * @param  
     * @return 
     */
    public Set<String> getAccessionNames() {
	return accessionNameMap.keySet();
    }
    
    /**
     * getAccessions
     *
     * @param  
     * @return 
     */
    public List<Accession> getAccessions() {
	return accessions;
    }
    
    /**
     * size
     *
     * @param  
     * @return 
     */
    public int size() {
	return accessions.size();
    }

    /**
     * Get a subset of this accession collection, given an array of indices
     * in the range [1..col_size]
     *
     * @param indices
     * @return
     */
    public AccessionCollection subset(Integer[] indices){
        AccessionCollection subset = new AccessionCollection();
        for(int i : indices){
            subset.add(accessions.get(i-1));
        }
        return subset;
    }
}

