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

import java.util.List;
import org.cimmyt.corehunter.Accession;

/**
 *
 * @author hermandebeukelaer
 */
public class ExternalDistanceMeasure extends Measure {

    //private Map<String,EXCachedResult> cachedResults;

    public ExternalDistanceMeasure() {
	this("EX", "External Distance Measure");
    }

    public ExternalDistanceMeasure(String name, String description) {
	super(name, description);
 	//cachedResults = new HashMap<String,EXCachedResult>();
    }

    public double calculate(List<Accession> accessions) {
	//return calculate(accessions, new EXCachedResult(accessions));
        try{
            double sum = 0.0;
            for(Accession a : accessions){
                sum += a.getExtDistance();
            }
            sum = sum/accessions.size();
            return sum;
        } catch (NullPointerException ne){
            System.err.println("No external distances present in dataset! Cannot use EX measure.");
            System.exit(1);
        }
        return -1;
    }

    /*
    @Override
    public double calculate(List<Accession> accessions, String id) {
	EXCachedResult cache = cachedResults.get(id);

	if (cache == null) {
	    cache = this.new EXCachedResult(accessions);
	    cachedResults.put(id, cache);
	}

	return calculate(accessions, cache);
    }

    protected double calculate(List<Accession> accessions, EXCachedResult cache) {
	List<Accession> aAccessions = cache.getAddedAccessions(accessions);
	List<Accession> rAccessions = cache.getRemovedAccessions(accessions);

        double sum = cache.getSum();

        // remove part of sum contributed by removed accessions
        for(Accession a : rAccessions){
            sum -= a.getExtDistance();
        }

        // add sum of newly added accessions
        for(Accession a : aAccessions){
            sum += a.getExtDistance();
        }

        // compute  mean
        double mean = sum/accessions.size();

        // update cache
        cache.setAccessions(accessions);
        cache.setMean(mean);

        return mean;
    }

    protected class EXCachedResult extends CachedResult {
        
        private double pMean;

	public EXCachedResult(List<Accession> accessions) {
	    super();
            pMean = 0.0;
	}

        public double getMean(){
            return pMean;
        }

        public void setMean(double mean){
            pMean = mean;
        }

        public double getSum(){
            if(pAccessions.size() > 0)
                return pMean * pAccessions.size();
            else
                return 0.0;
        }

    }
    */

}
