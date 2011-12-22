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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.DuplicateMeasureException;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public class PseudoMeasure {
    private String name;
    private String description;
    private List<Measure> measures;
    private Map<String, Integer> measureIndex;
    private List<Double> measureWeights;
    
    public PseudoMeasure() {
	this("PM", "A pseudo measure of many well defined Measure(s)");
    }
    
    public PseudoMeasure(String name, String description) {
	this.name = name;
	this.description = description;
	measures = new ArrayList<Measure>();
	measureIndex = new HashMap<String, Integer>();
	measureWeights = new ArrayList<Double>();
    }
    
    public String getName() {
	return this.name;
    }
    
    public String getDescription() {
	return this.description;
    }

    public double calculate(List<Accession> accessions) {
	return calculate(accessions, null);
    }
    
    public double calculate(List<Accession> accessions, String cacheId) {
	double score = 0.0;
	
	for(int i=0; i<measures.size(); i++) {
	    Measure m = measures.get(i);
	    double s;
	    if (cacheId != null) {
		s = m.calculate(accessions, cacheId);
	    } else {
		s = m.calculate(accessions);
	    }

	    if (m.isMinimizing()) {
		s = -s;
	    }

	    double weight = measureWeights.get(i).doubleValue();
	    score += s * weight;
	}

	return score;
    }

    public Map<String, Double> componentScores(List<Accession> accessions) {
	return componentScores(accessions, null);
    }

    public Map<String, Double> componentScores(List<Accession> accessions, String cacheId) {
	Map<String, Double> scores = new HashMap<String, Double>();

	for(int i=0; i<measures.size(); i++) {
	    Measure m = measures.get(i);
	    double s;
	    if (cacheId != null) {
		s = m.calculate(accessions, cacheId);
	    } else {
		s = m.calculate(accessions);
	    }
	    scores.put(m.getName(), new Double(s));
	}
	return scores;
    }

    public void addMeasure(Measure m, double weight) throws DuplicateMeasureException {
	if (measureIndex.get(m.getName()) != null) {
	    throw new DuplicateMeasureException("Measure having the named identifier " 
						+ m.getName() + " already exists.");
	}

	measures.add(m);
	measureIndex.put(m.getName(), measures.size());
	measureWeights.add(new Double(weight));
    }

    public static String getUniqueId() {
	return CachedResult.getUniqueId();
    }
    
}
