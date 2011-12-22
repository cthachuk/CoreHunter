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

