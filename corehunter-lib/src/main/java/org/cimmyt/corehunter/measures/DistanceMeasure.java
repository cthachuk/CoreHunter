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

package org.cimmyt.corehunter.measures;

import java.util.ArrayList;
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
public abstract class DistanceMeasure extends Measure {
    private static final int DEFAULT_ACCESSION_COUNT = 512;
    private static final int MAX_ACCESSION_COUNT = 8192;
    private List<List<Double>> M;
    private Map<String,DistanceCachedResult> cachedResults;

    protected static final double MISSING_VAL = -1.0;

    public DistanceMeasure() {
	this("UM", "Unknown Measure");
    }
    
    public DistanceMeasure(String name, String description) {
	this(name, description, DEFAULT_ACCESSION_COUNT);
    }

    public DistanceMeasure(String name, String description, int accessionCount) {
	super(name, description);

	M = new ArrayList<List<Double>>(accessionCount);
	for(int i=0; i<accessionCount; i++) {
	    M.add(new ArrayList<Double>(i+1));
	    for(int j=0; j<=i; j++) {
		M.get(i).add(new Double(MISSING_VAL));
	    }
	}

	cachedResults = new HashMap<String,DistanceCachedResult>();
    }

    public double calculate(List<Accession> accessions, String id) {
	DistanceCachedResult cache = cachedResults.get(id);

	if (cache == null) {
	    cache = this.new DistanceCachedResult(accessions);
	    cachedResults.put(id, cache);
	}
	
	return calculate(accessions, cache);
    }

    public double calculate(List<Accession> accessions) {
	return calculate(accessions, new DistanceCachedResult(accessions));
    }

    public double calculate(List<Accession> accessions, DistanceCachedResult cache) {
	List<Accession> aAccessions = cache.getAddedAccessions(accessions);
	List<Accession> rAccessions = cache.getRemovedAccessions(accessions);
	List<Accession> cAccessions = cache.getCommonAccessions(accessions);

	double total = cache.getTotal();
	double count = cache.getCount();
	
	for(Accession a : aAccessions) {
	    for(Accession b : cAccessions) {
		    total += calculate(a,b);
		    count++;
	    }
	}

	for(Accession a : rAccessions) {
	    for(Accession b : cAccessions) {
		total -= calculate(a,b);
		count--;
	    }
	}

	int size = aAccessions.size();
	for(int i=0; i<size-1; i++) {
	    for(int j=i+1; j<size; j++) {
		total += calculate(aAccessions.get(i), aAccessions.get(j));
		count++;
	    }
	}

	size = rAccessions.size();
	for(int i=0; i<size-1; i++) {
	    for(int j=i+1; j<size; j++) {
		total -= calculate(rAccessions.get(i), rAccessions.get(j));
		count--;
	    }
	}

	// recache our results under this id
	cache.setTotal(total);
	cache.setCount(count);
	cache.setAccessions(accessions);

	return total/count;
    }
	
    public abstract double calculate(Accession a1, Accession a2);

    protected double getMemoizedValue(int id1, int id2) {
	int a = Math.max(id1, id2);
	int b = Math.min(id1, id2);
	if (a >= M.size()) { 
	    return MISSING_VAL;
	} else {
	    return M.get(a).get(b).doubleValue();
	}
    }

    protected void setMemoizedValue(int id1, int id2, double v) {
	int a = Math.max(id1, id2);
	int b = Math.min(id1, id2);
	if (a >= M.size()) { 
	    if (a >= MAX_ACCESSION_COUNT) {
		return;
	    }

	    for(int i=M.size(); i<=a; i++) {
		M.add( new ArrayList<Double>(i+1) );
		for(int j=0; j<=i; j++) {
		    M.get(i).add(new Double(MISSING_VAL));
		}
	    }
	} 
	
	M.get(a).set(b, new Double(v));
    }

    private class DistanceCachedResult extends CachedResult {
	private double pTotal;
	private double pCnt;
	
	public DistanceCachedResult(List<Accession> accessions) {
	    super();
	    pTotal = 0.0;
	    pCnt = 0.0;
	}

	public double getTotal() {
	    return pTotal;
	}

	public double getCount() {
	    return pCnt;
	}

	public void setTotal(double total) {
	    pTotal = total;
	}

	public void setCount(double count) {
	    pCnt = count;
	}
    }
}
