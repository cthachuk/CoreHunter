//  Copyright 2008,2011 Chris Thachuk
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
}

