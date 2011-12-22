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

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
class CachedResult {
    private static final String baseId = "id:";
    private static int nextId = 0;
    protected List<Accession> pAccessions;


    public CachedResult() {
	pAccessions = new ArrayList<Accession>();
    }

    public void setAccessions(List<Accession> accessions) {
	pAccessions.clear();
	pAccessions.addAll(accessions);
    }

    public List<Accession> getAccessions() {
	return pAccessions;
    }

    public List<Accession> getAddedAccessions(List<Accession> accessions) {
	List<Accession> aAccessions = new ArrayList<Accession>(accessions);
	aAccessions.removeAll(pAccessions);
	return aAccessions;
    }

    public List<Accession> getRemovedAccessions(List<Accession> accessions) {
	List<Accession> rAccessions = new ArrayList<Accession>(pAccessions);
	rAccessions.removeAll(accessions);
	return rAccessions;
    }

    public List<Accession> getCommonAccessions(List<Accession> accessions) {
	List<Accession> cAccessions = new ArrayList<Accession>(pAccessions);
	ListIterator<Accession> itr = cAccessions.listIterator();

	while(itr.hasNext()) {
	    Accession a = itr.next();
	    if (!accessions.contains(a)) {
		itr.remove();
	    }
	}

	return cAccessions;
    }

    public static String getUniqueId() {
	String id = baseId + nextId;
	nextId++;
	return id;
    }
}

