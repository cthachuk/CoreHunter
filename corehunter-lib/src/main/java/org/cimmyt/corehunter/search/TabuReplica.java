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
import java.util.LinkedList;
import java.util.List;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.AccessionCollection;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 *
 * @author hermandebeukelaer
 */
public class TabuReplica extends Replica {

    private LinkedList<Integer> tabuList;
    private int tabuListSize;

    private List<Accession> bestCore;
    private double bestScore;
    private double lastImpr;

    private static final double MIN_PROG = 10e-9;


    /**
     * Create a TabuReplica which starts with the given core. If given core
     * is null, a random core is selected.
     *
     * @param ac
     * @param pm
     * @param nh
     * @param nrOfSteps
     * @param repTime
     * @param sampleMin
     * @param sampleMax
     * @param tabuListSize
     */
    public TabuReplica(AccessionCollection ac, PseudoMeasure pm, Neighborhood nh,
                       int nrOfSteps, int repTime, int sampleMin, int sampleMax, int tabuListSize){

        super("Tabu", ac, pm, nh, nrOfSteps, repTime, sampleMin, sampleMax);

        this.tabuListSize = tabuListSize;
        this.lastImpr = Double.MAX_VALUE;
    }

    @Override
    public String type(){
        return type + " (list size = " + tabuListSize + ")";
    }

    @Override
    public void init(){
        super.init();
        tabuList = new LinkedList<Integer>();
        bestCore = new ArrayList<Accession>();
        bestCore.addAll(core);
        bestScore = score;
    }

    @Override
    public void init(List<Accession> core){
        super.init(core);
        tabuList = new LinkedList<Integer>();
        bestCore = new ArrayList<Accession>();
        bestCore.addAll(core);
        bestScore = score;
    }

    @Override
    public void doSteps(){
        if(lastImpr >= MIN_PROG){
            stuck = true;
            int addIndex;
            double etime = System.currentTimeMillis() + repTime;
            int i=0;

            while((  (nrOfSteps > 0 && i < nrOfSteps)
                      || (repTime > 0 && System.currentTimeMillis() < etime) )){

                // run Tabu Search step

                // ALWAYS accept new core, even it is not an improvement
                addIndex = nh.genBestNeighbor(core, unselected, tabuList, bestScore, pm, cacheId);
                score = pm.calculate(core, cacheId);
                size = core.size();


                // check for improvement
                if(score > bestScore || (score == bestScore && size < bestCore.size())){
                    //System.out.println(coreHash);
                    stuck = false;
                    bestCore.clear();
                    bestCore.addAll(core);
                    lastImpr =  score - bestScore;
                    bestScore = score;
                }

                // finally, update tabu list
                if(tabuList.size() == tabuListSize){
                    // capacity reached, remove oldest tabu index
                    tabuList.poll();
                }
                // add new tabu index
                //tabuList.offer(addIndex);
                tabuList.offer(addIndex);

                i++;
            }
        }
    }

    @Override
    public List<Accession> getBestCore(){
        return bestCore;
    }

    @Override
    public double getBestScore(){
        return bestScore;
    }

    @Override
    public boolean stuck(){
        return stuck || lastImpr < MIN_PROG;
    }

}
