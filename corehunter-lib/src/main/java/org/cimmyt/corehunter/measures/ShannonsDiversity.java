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
public final class ShannonsDiversity extends Measure {
    private Map<String,SHCachedResult> cachedResults;

    public ShannonsDiversity() {
	this("SH", "Shannons Diversity Index");
    }
    
    public ShannonsDiversity(String name, String description) {
	super(name, description);
	cachedResults = Collections.synchronizedMap(new HashMap<String,SHCachedResult>());
    }

    public double calculate(List<Accession> accessions) {
	return calculate(accessions, new SHCachedResult(accessions));
    }

    @Override
    public double calculate(List<Accession> accessions, String id) {
	SHCachedResult cache = cachedResults.get(id);

	if (cache == null) {
	    cache = this.new SHCachedResult(accessions);
	    cachedResults.put(id, cache);
	}
	
	return calculate(accessions, cache);
    }

    protected double calculate(List<Accession> accessions, SHCachedResult cache) {
	List<Accession> aAccessions = cache.getAddedAccessions(accessions);
	List<Accession> rAccessions = cache.getRemovedAccessions(accessions);

	double total = cache.getTotal();
	double alleleTotals[] = cache.getAlleleTotals();
	double addTotals[] = Accession.getAlleleTotals(aAccessions);
	double remTotals[] = Accession.getAlleleTotals(rAccessions);

	for(int i=0; i<alleleTotals.length; i++) {
	    double diff = 0.0;
	    if (addTotals != null) {
		diff += addTotals[i];
	    }
	    if (remTotals != null) {
		diff -= remTotals[i];
	    }
	    alleleTotals[i] += diff;
	    total += diff;
	}

	double sum = 0.0;
	for(int i=0; i<alleleTotals.length; i++) {
	    double fraction = alleleTotals[i] / total;
	    if (!Double.isNaN(fraction) && fraction != 0) {
		// for some reason, java's precision isn't as good as C++
		// so needed to add this check in the port
		double t = fraction * Math.log(fraction);
		if (!Double.isNaN(t)) {
		    sum += t;
		}
	    }
	}

	// recache our results under this id
	cache.setTotal(total);
	cache.setAccessions(accessions);

	return -sum;	
    }

    private class SHCachedResult extends CachedResult {
	private double pTotal;
	private double pAlleleTotals[];
	
	public SHCachedResult(List<Accession> accessions) {
	    super();
	    Accession a1 = accessions.get(0);
	    int alleleCnt = a1.numSSRAlleles();

	    pAlleleTotals = new double[alleleCnt];
	    for(int i=0; i<alleleCnt; i++) {
		pAlleleTotals[i] = 0.0;
	    }

	    pTotal = 0.0;
	}

	public double getTotal() {
	    return pTotal;
	}

	public double[] getAlleleTotals() {
	    return pAlleleTotals;
	}

	public void setTotal(double total) {
	    pTotal = total;
	}
    }
}

