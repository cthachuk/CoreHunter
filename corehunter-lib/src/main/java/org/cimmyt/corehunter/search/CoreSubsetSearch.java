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

package org.cimmyt.corehunter.search;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.AccessionCollection;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk &lt;&gt;
 * @version $Rev$
 */
public final class CoreSubsetSearch {
    final static double K_b2 = 1.360572e-9;

    // this class should not be instantiable from outside class
    private CoreSubsetSearch() {

    }
    
    public static AccessionCollection remcSearch(AccessionCollection ac, 
						 PseudoMeasure pm, int sampleMin, int sampleMax,
						 double runtime, int numReplicas, double minT, 
						 double maxT, int mcSteps) {
	
	CoreSubsetSearch search = new CoreSubsetSearch();  // grab a private instance, so we can create replicas

	SearchReplica replicas[] = new SearchReplica[numReplicas];
	Random r = new Random();
	
	for(int i=0; i<numReplicas; i++) {
	    double T = minT + i*(maxT - minT)/(numReplicas - 1);
	    replicas[i] = search.new SearchReplica(ac.getAccessions(), pm, sampleMin, sampleMax, T, r);
	}
	
	double bestScore = -Double.MAX_VALUE;
	List<Accession> bestCore = new ArrayList<Accession>();

      	ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();
	double eTime = sTime + runtime * 1000000000;
	
	int swapBase = 0;
	while( tb.getCurrentThreadCpuTime() < eTime ) {

	    // run MC search for each replica
	    for(int i=0; i<numReplicas; i++) {
		double bestRepScore = replicas[i].mcSearch(mcSteps);
		
		if (bestRepScore > bestScore || 
		    (bestRepScore == bestScore && replicas[i].getBestCore().size() < bestCore.size())) {
		    bestScore = bestRepScore;
		    bestCore.clear();
		    bestCore.addAll(replicas[i].getBestCore());
		    System.out.println("best score: " + bestRepScore + "\tsize: " + bestCore.size() +
				       "\ttime: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);
		}				
	    }

	    // consider swapping temperatures of adjacent replicas
 	    for(int i=swapBase; i<numReplicas-1; i+=2) {
		SearchReplica m = replicas[i];
		SearchReplica n = replicas[i+1];
	
		double B_m = 1.0 / (K_b2 * m.getTemperature());
		double B_n = 1.0 / (K_b2 * n.getTemperature());
		double B_diff = B_n - B_m;
		double E_delta = m.getScore() - n.getScore();
		
		boolean swap = false;

		if( E_delta <= 0 ) {
		    swap = true;
		} else {
		    double p = r.nextDouble();
		    
		    if( Math.exp(B_diff * E_delta) > p ) {
			swap = true;
		    }
		}
		
		if (swap) {
		    m.swapTemperature(n);
		    SearchReplica temp = replicas[i];
		    replicas[i] = replicas[i+1];
		    replicas[i+1] = temp;
		}
	    }
	    swapBase = 1 - swapBase;
	}

	AccessionCollection core = new AccessionCollection();
	core.add(bestCore);
	
	// temp
	// for(int i=0; i<replicas.length; i++) {
	//    replicas[i].printStats();
	// }
	
	return core;
    }

    public static AccessionCollection randomSearch(AccessionCollection ac, double sampleIntensity) {
	List<Accession> accessions = new ArrayList<Accession>(ac.getAccessions());

	AccessionCollection core = new AccessionCollection();
	int coreSize = (int)(ac.size() * sampleIntensity);

	Random r = new Random();
	
	for(int i=0; i<coreSize; i++) {
	    int ai = r.nextInt(accessions.size());
	    Accession a = accessions.remove(ai);
	    core.add(a);
	}
	
	return core;
    }


    private class SearchReplica {
	private final static double K_b  = 7.213475e-7;
	
	private Random r;
	private List<Accession> selected;
	private List<Accession> unselected;
	private List<Accession> lastSelections;
	private List<Accession> lastUnselections;
	private List<Accession> bestCore;
	private PseudoMeasure pm;
	private String cacheId;
	private double T;
	private double score;
	private double lastScore;
	private double bestScore;

	private int minSize;
	private int maxSize;
	private int accepts;
	private int rejects;
	private int improvements;
	private int totSteps;

	public SearchReplica(List<Accession> accessions, PseudoMeasure pm, int minSize, int maxSize, double T) {
	    this(accessions, pm, minSize, maxSize, T, new Random());
	}
	
	public SearchReplica(List<Accession> accessions, PseudoMeasure pm, int minSize, int maxSize, double T, Random r) {
	    this.r = r;
	    this.pm = pm;
	    this.T = T;
	    this.minSize = minSize;
	    this.maxSize = maxSize;
	    bestScore = 0.0;
	    selected = new ArrayList<Accession>();
	    unselected = new ArrayList<Accession>(accessions);
	    lastSelections = new ArrayList<Accession>();
	    lastUnselections = new ArrayList<Accession>();
	    bestCore = new ArrayList<Accession>();

	    accepts = rejects = improvements = totSteps = 0;
	    
	    cacheId = PseudoMeasure.getUniqueId();
	    
	    // select an initial core
	    lastScore = -Double.MAX_VALUE;
	    bestScore = -Double.MAX_VALUE;
	    score = perturb(maxSize, 0);
	}

	public double mcSearch(int mcSteps) {
	    for(int j=0; j < mcSteps; j++) {
		if (minSize == maxSize) {
		    perturb(1, 1);
		} else {
		    double p = r.nextDouble();
		    if (p>=0.66 && selected.size() < maxSize) {
			perturb(1,0);
		    } else if (p>=0.33 && selected.size() > minSize) {
			perturb(0,1);
		    } else {
			perturb(1,1);
		    }
		}
		
		double deltaScore = score - lastScore;

		if (deltaScore > 0) {
		    improvements++;
		} else {
		    double deltaSize = lastSelections.size() - lastUnselections.size();
		    
		    if (deltaSize > 0) {
			rejects++;
			unperturb();
		    } else {
			
			double P = Math.exp(deltaScore/(T*K_b));
			double Q = r.nextDouble();
			
			if ( Q > P ) {
			    unperturb();
			    rejects++;
			} else {
			    accepts++;
			}
		    }
		}
		
		totSteps++;
	    }
	    return bestScore;
	}

	public void printStats() {
	    System.out.println("steps: " + totSteps + "\timprovements: " + improvements + "\taccepts: " + 
			       accepts + "\trejects: " + rejects);
	}

	public double getScore() {
	    return score;
	}

	public double getBestScore() {
	    return bestScore;
	}

	public double getTemperature() {
	    return T;
	}

	public void setTemperature(double temp) {
	    // temperature cannot be negative
	    T = Math.max(0,temp);
	}

	public List<Accession> getBestCore() {
	    return bestCore;
	}

	public void swapTemperature(SearchReplica other) {
	    double temp = T;
	    T = other.T;
	    other.T = temp;
	}
	
	/**
	 * getSelected
	 *
	 * @param  
	 * @return 
	 */
	public List<Accession> getSelected() {
	    return selected;
	}
    
	/**
	 * getUnselected
	 *
	 * @param  
	 * @return 
	 */
	public List<Accession> getUnselected() {
	    return unselected;
	}	


	public void selectAll() {
	    selected.addAll(unselected);
	    unselected.clear();
	}

	public void unselectAll() {
	    unselected.addAll(selected);
	    selected.clear();
	}

	public double perturb(int na, int nr) {
	    lastSelections.clear();
	    lastUnselections.clear();
	    lastScore = score;
	    
	    int add = Math.min(na, unselected.size());
	    int remove = Math.min(nr, selected.size());

	    for(int i=0; i<add; i++) {
		Accession a = swapOneAtRandom(unselected, selected);
		lastSelections.add(a);
	    }

	    for(int i=0; i<remove; i++) {
		Accession a = swapOneAtRandom(selected, unselected);
		lastUnselections.add(a);
	    }

	    score = pm.calculate(getSelected(), cacheId);
// 	    score = pm.calculate(getSelected());

	    if (score > bestScore 
		|| score == bestScore && getSelected().size() < bestCore.size()) {
		bestScore = score;
		bestCore.clear();
		bestCore.addAll(getSelected());
	    }
	    return score;
	}

	public double unperturb() {
	    List<Accession> temp = new ArrayList<Accession>();
	    
	    // TODO: this is real bottleneck, optimize when there is time
	    for (Accession a : lastSelections) {
		selected.remove(a);
		unselected.add(a);
		temp.add(a);
	    }
	    lastSelections.clear();

	    for (Accession a : lastUnselections) {
		unselected.remove(a);
		selected.add(a);
		lastSelections.add(a);
	    }
	    lastUnselections.clear();

	    for (Accession a : temp) {
		lastUnselections.add(a);
	    }
	    temp.clear();

	    double tmp = score;
	    score = lastScore;
	    lastScore = tmp;
	    return score;
	}

	private Accession removeOneAtRandom(List<Accession> accessions) {
	    int index = r.nextInt(accessions.size());
	    Accession a = accessions.remove(index);
	    return a;
	}

	private Accession swapOneAtRandom(List<Accession> from, List <Accession> to) {
	    Accession a = removeOneAtRandom(from);
	    to.add(a);
	    return a;
	}

    }
}
