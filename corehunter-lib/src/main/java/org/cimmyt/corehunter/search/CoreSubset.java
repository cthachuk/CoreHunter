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

package org.cimmyt.corehunter.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public final class CoreSubset {
    private List<Accession> selected;
    private List<Accession> unselected;
    private Random rand;
    private List<Accession> lastSelections;
    private List<Accession> lastUnselections;

    public CoreSubset(List<Accession> accessions) {
	this(accessions, new Random());
    }
    
    public CoreSubset(List<Accession> accessions, Random r) {
	rand = r;
	selected = new ArrayList<Accession>();
	unselected = new ArrayList<Accession>(accessions);
	lastSelections = new ArrayList<Accession>();
	lastUnselections = new ArrayList<Accession>();
    }
    
    /**
     * getSelected
     *
     * @param  
     * @return 
     */
    public List<Accession> getSelected() {
	return selected;
    }
    
    /**
     * getUnselected
     *
     * @param  
     * @return 
     */
    public List<Accession> getUnselected() {
	return unselected;
    }	


    public void selectAll() {
	selected.addAll(unselected);
	unselected.clear();
    }

    public void unselectAll() {
	unselected.addAll(selected);
	selected.clear();
    }

    public void perturb(int na, int nr) {
	lastSelections.clear();
	lastUnselections.clear();
	
	int add = Math.min(na, unselected.size());
	int remove = Math.min(nr, selected.size());

	for(int i=0; i<add; i++) {
	    Accession a = swapOneAtRandom(unselected, selected);
	    lastSelections.add(a);
	}

	for(int i=0; i<remove; i++) {
	    Accession a = swapOneAtRandom(selected, unselected);
	    lastUnselections.add(a);
	}
    }

    public void unperturb() {
	// TODO: this is real bottleneck, optimize when there is time
	for (Accession a : lastSelections) {
	    selected.remove(a);
	    unselected.add(a);
	}

	for (Accession a : lastUnselections) {
	    unselected.remove(a);
	    selected.add(a);
	}
    }

    private Accession removeOneAtRandom(List<Accession> accessions) {
	int index = rand.nextInt(accessions.size());
	Accession a = accessions.remove(index);
	return a;
    }

    private Accession swapOneAtRandom(List<Accession> from, List <Accession> to) {
	Accession a = removeOneAtRandom(from);
	to.add(a);
	return a;
    }
}
