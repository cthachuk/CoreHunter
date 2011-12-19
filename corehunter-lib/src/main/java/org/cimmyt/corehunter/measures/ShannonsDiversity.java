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

