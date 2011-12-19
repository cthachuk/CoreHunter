/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
