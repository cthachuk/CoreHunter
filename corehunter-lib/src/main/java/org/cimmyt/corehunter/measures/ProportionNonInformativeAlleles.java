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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public final class ProportionNonInformativeAlleles extends Measure {
    private Map<String,PNCachedResult> cachedResults;

    public ProportionNonInformativeAlleles() {
	this("PN", "Proportion of non-informative alleles");
    }
    
    public ProportionNonInformativeAlleles(String name, String description) {
	super(name, description);
	this.minimizing = true;
	cachedResults = Collections.synchronizedMap(new HashMap<String,PNCachedResult>());
    }

    public double calculate(List<Accession> accessions) {
	return calculate(accessions, new PNCachedResult(accessions));
    }


    // TODO: not currently working, so use the slow method for now
    //
    public double calculate(List<Accession> accessions, String id) {
	PNCachedResult cache = cachedResults.get(id);

	if (cache == null) {
	    cache = this.new PNCachedResult(accessions);
	    cachedResults.put(id, cache);
	}
	
	return calculate(accessions, cache);
    }

    protected double calculate(List<Accession> accessions, PNCachedResult cache) {
	List<Accession> aAccessions = cache.getAddedAccessions(accessions);
	List<Accession> rAccessions = cache.getRemovedAccessions(accessions);

	int alleleCounts[] = cache.getAlleleCounts();
	int addTotals[] = Accession.getAlleleCounts(aAccessions);
	int remTotals[] = Accession.getAlleleCounts(rAccessions);

	int alleleCnt = 0;
	for(int i=0; i<alleleCounts.length; i++) {
	    int diff = 0;
	    if (addTotals != null) {
		diff += addTotals[i];
	    }
	    if (remTotals != null) {
		diff -= remTotals[i];
	    }
	    alleleCounts[i] += diff;
	    if (alleleCounts[i]<=0) {
		alleleCnt += 1;
	    }
	}

	cache.setAccessions(accessions);

	return (double)alleleCnt / (double)alleleCounts.length;	
    }

    private class PNCachedResult extends CachedResult {
	private int pAlleleCounts[];
	
	public PNCachedResult(List<Accession> accessions) {
	    super();
	    Accession a1 = accessions.get(0);
	    int alleleCnt = a1.numSSRAlleles();

	    pAlleleCounts = new int[alleleCnt];
	    for(int i=0; i<alleleCnt; i++) {
		pAlleleCounts[i] = 0;
	    }
	}

	public int[] getAlleleCounts() {
	    return pAlleleCounts;
	}
    }
}

