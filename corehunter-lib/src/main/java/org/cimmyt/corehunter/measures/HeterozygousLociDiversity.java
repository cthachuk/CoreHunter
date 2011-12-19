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
import java.util.ListIterator;
import java.util.Map;

import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public final class HeterozygousLociDiversity extends Measure {
    private Map<String,HECachedResult> cachedResults;

    public HeterozygousLociDiversity() {
	this("HE", "Proportion of Heterozygous Loci");
    }
    
    public HeterozygousLociDiversity(String name, String description) {
	super(name, description);
	cachedResults = Collections.synchronizedMap(new HashMap<String,HECachedResult>());
    }

    public double calculate(List<Accession> accessions) {
	return calculate(accessions, new HECachedResult(accessions));
    }

    public double calculate(List<Accession> accessions, String id) {
	HECachedResult cache = cachedResults.get(id);

	if (cache == null) {
	    cache = this.new HECachedResult(accessions);
	    cachedResults.put(id, cache);
	}
	
	return calculate(accessions, cache);
    }

    protected double calculate(List<Accession> accessions, HECachedResult cache) {
	List<Accession> aAccessions = cache.getAddedAccessions(accessions);
	List<Accession> rAccessions = cache.getRemovedAccessions(accessions);

	double markerAlleleTotals[][] = cache.getMarkerAlleleTotals();
	double addTotals[][] = Accession.getMarkerAlleleTotals(aAccessions);
	double remTotals[][] = Accession.getMarkerAlleleTotals(rAccessions);

	for(int i=0; i<markerAlleleTotals.length; i++) {
	    for(int j=0; j<markerAlleleTotals[i].length; j++) {
		double diff = 0.0;
		if (addTotals != null) {
		    diff += addTotals[i][j];
		}
		if (remTotals != null) {
		    diff -= remTotals[i][j];
		}
		markerAlleleTotals[i][j] += diff;
	    }
	}
	
	double diversityTotal = 0.0;
	for(int i=0; i<markerAlleleTotals.length; i++) {
	    double lociTotal = 0.0;
	    double lociTerm = 0.0;
	    for(int j=0; j<markerAlleleTotals[i].length; j++) {
		lociTerm += Math.pow(markerAlleleTotals[i][j], 2);
		lociTotal += markerAlleleTotals[i][j];
	    }
	    diversityTotal += (1.0 - (lociTerm/Math.pow(lociTotal,2)));
	}
	
	double score = (1.0 / (double)markerAlleleTotals.length) * diversityTotal;
	// recache our results under this id
	cache.setAccessions(accessions);

	return score;
    }

    private class HECachedResult extends CachedResult {
	private double pMarkerAlleleTotals[][];
	
	public HECachedResult(List<Accession> accessions) {
	    super();

	    Accession a1 = accessions.get(0);
	    int markerCnt = a1.numSSRMarkers();
	    double markerAlleleTotals[][] = new double[markerCnt][];
	    
	    ListIterator<List<Double>> mItr = a1.getSSRValues().listIterator();
	    ListIterator<Double> aItr = null;
	    
	    int i = 0;
	    while (mItr.hasNext()) {
		List<Double> alleles = mItr.next();
		
		markerAlleleTotals[i] = new double[alleles.size()];
		for(int j=0; j<alleles.size(); j++) {
		    markerAlleleTotals[i][j] = 0.0;
		}
		i++;
	    }
	    pMarkerAlleleTotals = markerAlleleTotals;
	}

	public double[][] getMarkerAlleleTotals() {
	    return pMarkerAlleleTotals;
	}
    }
}
