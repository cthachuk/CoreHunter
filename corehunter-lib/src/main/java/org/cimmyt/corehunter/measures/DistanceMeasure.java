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
import java.util.TreeMap;

import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public abstract class DistanceMeasure extends Measure {
    //private static final int DEFAULT_ACCESSION_COUNT = 512;
    //private static final int MAX_ACCESSION_COUNT = 8192;
    //private List<List<Double>> M;
    private double[][] M;
    private Map<String,DistanceCachedResult> cachedResults;

    protected static final double MISSING_VAL = -1.0;

    protected DistanceMeasureType type; // states whether mean or min distance should be computed

    public DistanceMeasure(int accessionCount) {
	this("UM", "Unknown Measure", accessionCount, DistanceMeasureType.MEAN_DISTANCE);
    }
    
    /*public DistanceMeasure(String name, String description) {
	this(name, description, DEFAULT_ACCESSION_COUNT);
    }*/

    public DistanceMeasure(String name, String description, int accessionCount, DistanceMeasureType type) {
	super(name, description);

	//M = new ArrayList<List<Double>>(accessionCount);
	/*for(int i=0; i<accessionCount; i++) {
	    M.add(new ArrayList<Double>(i+1));
	    for(int j=0; j<=i; j++) {
		M.get(i).add(new Double(MISSING_VAL));
	    }
	}*/
        M = new double[accessionCount][];
        for(int i=0; i<accessionCount; i++) {
	    M[i] = new double[i+1];
	    for(int j=0; j<=i; j++) {
		M[i][j] = MISSING_VAL;
	    }
	}

	cachedResults = Collections.synchronizedMap(new HashMap<String,DistanceCachedResult>());

        this.type = type;
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
        
        double dist;
        
        if (type == DistanceMeasureType.MEAN_DISTANCE){

            double total = cache.getTotal();
            double count = cache.getCount();

            for(Accession a : aAccessions) {
                for(Accession b : cAccessions) {
                    dist = calculate(a,b);
                    total += dist;
                    count++;
                }
            }

            int size = aAccessions.size();
            for(int i=0; i<size-1; i++) {
                for(int j=i+1; j<size; j++) {
                    dist = calculate(aAccessions.get(i), aAccessions.get(j));
                    total += dist;
                    count++;
                }
            }

            for(Accession a : rAccessions) {
                for(Accession b : cAccessions) {
                    dist = calculate(a,b);
                    total -= dist;
                    count--;
                }
            }           

            size = rAccessions.size();
            for(int i=0; i<size-1; i++) {
                for(int j=i+1; j<size; j++) {
                    dist = calculate(rAccessions.get(i), rAccessions.get(j));
                    total -= dist;
                    count--;
                }
            }

            // recache our results under this id
            cache.setTotal(total);
            cache.setCount(count);
            cache.setAccessions(accessions);

            return total/count;

        } else if (type == DistanceMeasureType.MIN_DISTANCE){

            TreeMap<Double, Integer> minFreqTable = cache.getMinFreqTable();

            // add new distances

            for(Accession a : aAccessions) {
                for(Accession b : cAccessions) {
                    dist = calculate(a,b);
                    Integer freq = minFreqTable.get(dist);
                    if(freq == null){
                        minFreqTable.put(dist, 1);
                    } else {
                        minFreqTable.put(dist, freq+1);
                    }
                }
            }

            int size = aAccessions.size();
            for(int i=0; i<size-1; i++) {
                for(int j=i+1; j<size; j++) {
                    dist = calculate(aAccessions.get(i), aAccessions.get(j));
                    Integer freq = minFreqTable.get(dist);
                    if(freq == null){
                        minFreqTable.put(dist, 1);
                    } else {
                        minFreqTable.put(dist, freq+1);
                    }
                }
            }

            // remove old distances

            for(Accession a : rAccessions) {
                for(Accession b : cAccessions) {
                    dist = calculate(a,b);
                    Integer freq = minFreqTable.get(dist);
                    freq--;
                    if(freq == 0){
                        minFreqTable.remove(dist);
                    } else if (freq > 0) {
                        minFreqTable.put(dist, freq);
                    } else {
                        System.err.println("Error in minimum distance cacheing scheme!"
                                           + "\nThis is a bug, please contact authors!");
                    }
                }
            }

            size = rAccessions.size();
            for(int i=0; i<size-1; i++) {
                for(int j=i+1; j<size; j++) {
                    dist = calculate(rAccessions.get(i), rAccessions.get(j));
                    Integer freq = minFreqTable.get(dist);
                    freq--;
                    if(freq == 0){
                        minFreqTable.remove(dist);
                    } else if (freq > 0) {
                        minFreqTable.put(dist, freq);
                    } else {
                        System.err.println("Error in minimum distance cacheing scheme!"
                                           + "\nThis is a bug, please contact authors!");
                    }
                }
            }

            // recache results
            cache.setAccessions(accessions);

            //System.out.println("Min cache size: " + minFreqTable.size());
            return minFreqTable.firstKey();

            /*
            //implementation without cache
            double minDist = Double.MAX_VALUE;
            int size = accessions.size();
            for(int i=0; i<size-1; i++) {
                for(int j=i+1; j<size; j++) {
                    dist = calculate(accessions.get(i), accessions.get(j));
                    if(dist<minDist){
                        minDist = dist;
                    }
                }
            }
            return minDist;*/

        } else {
            // THIS SHOULD NOT HAPPEN
            System.err.println("Unkown distance measure type -- this is a bug! Please contact authors.");
            System.exit(1);
            return -1;
        }
        
    }
	
    public abstract double calculate(Accession a1, Accession a2);

    protected double getMemoizedValue(int id1, int id2) {

	int a = Math.max(id1, id2);
	int b = Math.min(id1, id2);

        /*double ret;
	if (a >= M.size()) { 
	    ret = MISSING_VAL;
	} else {
	    ret = M.get(a).get(b).doubleValue();
	}

        return ret;*/

        if(a >= M.length){
            return MISSING_VAL;
        } else {
            return M[a][b];
        }
    }

    protected void setMemoizedValue(int id1, int id2, double v) {

	int a = Math.max(id1, id2);
	int b = Math.min(id1, id2);

        /*if (a >= M.size()) {
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
	
	M.get(a).set(b, new Double(v));*/
        if(a >= M.length){
            return;
        } else {
            M[a][b] = v;
        }
    }

    private class DistanceCachedResult extends CachedResult {
	private double pTotal;
	private double pCnt;

        private TreeMap<Double, Integer> minFreqTable;

	public DistanceCachedResult(List<Accession> accessions) {
	    super();
	    pTotal = 0.0;
	    pCnt = 0.0;

            minFreqTable = new TreeMap<Double, Integer>();
	}

	public double getTotal() {
	    return pTotal;
	}

	public double getCount() {
	    return pCnt;
	}

        public TreeMap<Double, Integer> getMinFreqTable(){
            return minFreqTable;
        }

	public void setTotal(double total) {
	    pTotal = total;
	}

	public void setCount(double count) {
	    pCnt = count;
	}

    }
    
}
