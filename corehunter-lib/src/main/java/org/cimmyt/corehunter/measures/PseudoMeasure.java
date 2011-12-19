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
