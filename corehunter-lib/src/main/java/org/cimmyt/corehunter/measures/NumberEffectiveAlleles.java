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
import java.util.ListIterator;
import java.util.Map;

import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public final class NumberEffectiveAlleles extends Measure {
    private Map<String,NECachedResult> cachedResults;

    public NumberEffectiveAlleles() {
	this("NE", "Number of effective alleles");
    }
    
    public NumberEffectiveAlleles(String name, String description) {
	super(name, description);
	cachedResults = Collections.synchronizedMap(new HashMap<String,NECachedResult>());
    }

    public double calculate(List<Accession> accessions) {
	return calculate(accessions, new NECachedResult(accessions));
    }

    public double calculate(List<Accession> accessions, String id) {
	NECachedResult cache = cachedResults.get(id);

	if (cache == null) {
	    cache = this.new NECachedResult(accessions);
	    cachedResults.put(id, cache);
	}
	
	return calculate(accessions, cache);
    }

    protected double calculate(List<Accession> accessions, NECachedResult cache) {
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
	    diversityTotal += Math.pow(lociTotal,2) / lociTerm;
	}
	
	double score = (1.0 / (double)markerAlleleTotals.length) * diversityTotal;
	// recache our results under this id
	cache.setAccessions(accessions);

	return score;
    }

    private class NECachedResult extends CachedResult {
	private double pMarkerAlleleTotals[][];
	
	public NECachedResult(List<Accession> accessions) {
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
