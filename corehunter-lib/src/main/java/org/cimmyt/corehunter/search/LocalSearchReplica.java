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

package org.cimmyt.corehunter.search;

import org.cimmyt.corehunter.AccessionCollection;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 *
 * @author hermandebeukelaer
 */
public class LocalSearchReplica extends Replica {


    /**
     * Create a new LocalSearchReplica.
     *
     * @param ac
     * @param pm
     * @param nh
     * @param nrOfSteps
     * @param repTime
     * @param sampleMin
     * @param sampleMax
     */
    public LocalSearchReplica(AccessionCollection ac, PseudoMeasure pm, Neighborhood nh,
                              int nrOfSteps, int repTime, int sampleMin, int sampleMax){
        super("Local", ac, pm, nh, nrOfSteps, repTime, sampleMin, sampleMax);
    }

    @Override
    public void doSteps(){
        stuck = true;
        double etime = System.currentTimeMillis() + repTime;
        int i = 0;

        while((  (nrOfSteps > 0 && i < nrOfSteps)
                  || (repTime > 0 && System.currentTimeMillis() < etime) )){

            // run Local Search step
            nh.genRandomNeighbor(core, unselected);
            newScore = pm.calculate(core, cacheId);
            newSize = core.size();

            if (newScore > score || (newScore == score && newSize < size)) {
                // Accept new (better) core!
                score = newScore;
                size = newSize;
                // Improvement!
                stuck = false;
            } else {
                // Reject new core
                nh.undoLastPerturbation(core, unselected);
            }

            i++;
        }
    }

}
