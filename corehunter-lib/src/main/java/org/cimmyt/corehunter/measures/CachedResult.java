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

