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

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.AccessionCollection;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 *
 * @author hermandebeukelaer
 */
public class LRReplica extends Replica {

    private int l;
    private int r;
    private boolean exhFirstPair;
    private double bestNewScore, dscore;
    private int bestAddIndex, bestRemIndex;
    Stack<SinglePerturbation> history;


    private boolean cont;
    private int totalSteps;
    private boolean skipadd;

    public LRReplica(AccessionCollection ac, PseudoMeasure pm, int nrOfSteps, int repTime,
                     int sampleMin, int sampleMax, int l, int r, boolean exhFirstPair){

        super("LR", ac, pm, null, nrOfSteps, repTime, sampleMin, sampleMax);
        this.l = l;
        this.r = r;
        this.exhFirstPair = exhFirstPair;
        cont = true;
        totalSteps=0;
    }

    @Override
    public void init(){
        history = new Stack<SinglePerturbation>();
        cacheId = PseudoMeasure.getUniqueId();
        skipadd = false;
        if (l>r) {
            // Start with minimal set, stepwise increase size
            if(exhFirstPair){
                // Because distance measures require at least two accessions to be
                // computable, exhaustively select the best core set of size 2
                core = CoreSubsetSearch.exhaustiveSearch(ac, pm, 2, 2, false).getAccessions();
            } else {
                // Random first pair, to save computational cost: this transforms the
                // deterministic lr search into a semi-random method
                core = CoreSubsetSearch.randomSearch(ac, 2, 2).getAccessions();
            }
            unselected = new ArrayList<Accession>(accessions);
            unselected.removeAll(core);
        } else {
            // Start with full set, stepwise decresase size
            core = new ArrayList<Accession>(accessions);
            unselected = new ArrayList<Accession>();
            skipadd = true;
        }
        score = pm.calculate(core, cacheId);
        bestNewScore = score;
    }

    /**
     * LR Replica always start with empty or full set, so if an initial core is
     * given, it is ignored and the default init method is called instead.
     *
     * @param core
     */
    @Override
    public void init(List<Accession> core){
        init();
    }

    public void stop(){
        cont = false;
    }

    public boolean isDone(){
        return !cont;
    }

    public int getCurSteps(){
        return totalSteps;
    }

    private int stucked = 0;
    private final int STUCK_STAY = 10;

    @Override
    public boolean stuck(){
        stucked++;
        return isDone() && stucked > STUCK_STAY;
    }

    @Override
    public void doSteps() {
        int steps=0;
        double etime = System.currentTimeMillis() + repTime;

        while(cont &&
               (  (nrOfSteps > 0 && steps < nrOfSteps)
                  || (repTime > 0 && System.currentTimeMillis() < etime)
                  || (nrOfSteps <= 0 && repTime <= 0) ) ){

            // Add l new accessions to core
            if(!skipadd){
                for(int i=0; i<l; i++){
                    // Search for best new accession
                    bestNewScore = -Double.MAX_VALUE;
                    for(int j=0; j<unselected.size(); j++){
                        Accession add = unselected.get(j);
                        core.add(add);
                        newScore = pm.calculate(core, cacheId);
                        if(newScore > bestNewScore){
                            bestNewScore = newScore;
                            bestAddIndex = j;
                        }
                        core.remove(core.size()-1);
                    }
                    // Add best new accession
                    core.add(unselected.remove(bestAddIndex));
                    history.add(new Addition(bestAddIndex));
                }
                skipadd=false;
            }
            // Remove r accessions from core
            for(int i=0; i<r; i++){
                // Search for worst accession
                bestNewScore = -Double.MAX_VALUE;
                for(int j=0; j<core.size(); j++){
                    Accession rem = core.remove(j);
                    newScore = pm.calculate(core, cacheId);
                    if(newScore > bestNewScore){
                        bestNewScore = newScore;
                        bestRemIndex = j;
                    }
                    core.add(j, rem);
                }
                // Remove worst accession
                unselected.add(core.remove(bestRemIndex));
                history.add(new Deletion(bestRemIndex));
            }

            dscore = bestNewScore - score;
            score = bestNewScore;

            // Determine whether to continue search
            if(l > r){
                // Increasing core size
                if (core.size() > sampleMin && dscore <= 0) {
                    cont = false; // Equal or worse score and size increased
                    // Restore previous core
                    for(int i=0; i<l+r; i++){
                        history.pop().undo(core, unselected);
                    }
                } else if(core.size()+l-r > sampleMax){
                    cont = false; // Max size reached
                }

            } else {
                // Decreasing core size
                if (core.size() < sampleMax && dscore < 0){
                    cont = false; // Worse score
                    // Restore previous core
                    for(int i=0; i<l+r; i++){
                        history.pop().undo(core, unselected);
                    }
                } else if (core.size()+l-r < sampleMin){
                    cont = false; // Min size reached
                }
            }
            steps++;
            totalSteps++;
        }
    }

}
