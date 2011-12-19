/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.search;

import java.util.Collections;
import java.util.List;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 *
 * @author hermandebeukelaer
 */
public class HeuristicSingleNeighborhood extends SingleNeighborhood {

    public HeuristicSingleNeighborhood(int minSize, int maxSize){
        this(minSize, maxSize,1);
    }

    public HeuristicSingleNeighborhood(int minSize, int maxSize, int historySize){
        super(minSize, maxSize, historySize);
    }

    @Override
    public HeuristicSingleNeighborhood clone(){
        return new HeuristicSingleNeighborhood(minSize, maxSize, historySize);
    }

    @Override
    public int genBestNeighbor(List<Accession> core, List<Accession> unselected, List<Integer> tabu,
                               double curBestScore, PseudoMeasure pm, String cacheID) {

        // search for a good neighbor by perturbing core using the following heuristic:
        //   - investigate all possible additions of one accession to the core and
        //     choose this one with the highest score for the new set of accessions
        //   - remove one old accesion from the core, which gives rise to the
        //     highest score of the new core
        // remark: only adding or deleting an item is also possible if core size limits not reached
        
        int bestRemIndex = -1;
        int bestAddIndex = -1;
        double bestScore = -Double.MAX_VALUE;
        double score;

        // first add best, then delete worst

        if(core.size() > minSize){
            // pure deletion is possible - include option without addition
            bestScore = pm.calculate(core, cacheID);
        }

        // try adding each accession from unselected
        for(int i=0; i<unselected.size(); i++){
            Accession a = unselected.get(i);
            core.add(a);
            score = pm.calculate(core, cacheID);
            if(score > bestScore){
                bestScore = score;
                bestAddIndex = i;
            }
            // restore core
            core.remove(core.size()-1);
        }

        // best addition has been determined, reset best score
        bestScore = -Double.MAX_VALUE;

        // now determine best removal
        if(bestAddIndex == -1){
            // no addition proved to be best option --> pure removal
            // search for worst accession and remove
            for(int i=core.size()-1; i>=0; i--){ //backward loop, reverses order
                Accession a = core.remove(i);
                score = pm.calculate(core, cacheID);
                if(score > bestScore && (tabu == null || !tabu.contains(i) || score - curBestScore > MIN_TABU_ASPIRATION_PROG)){
                    bestScore = score;
                    bestRemIndex = i;
                }
                core.add(a);
            }
            // restore original order of accessions in core
            Collections.reverse(core);
        } else {

            // new accession will be added, now two options remain:
            //  - only add this new accession (pure addition)
            //  - swap (remove one old accession)
            // --> choose best

            // try all possible non-tabu swaps
            Accession a = unselected.get(bestAddIndex);
            for(int i=0; i<core.size(); i++){
                Accession b = core.set(i, a);
                score = pm.calculate(core, cacheID);
                if(score > bestScore && (tabu == null || !tabu.contains(i) || score - curBestScore > MIN_TABU_ASPIRATION_PROG)){
                    bestScore = score;
                    bestRemIndex = i;
                }
                core.set(i, b);
            }

            // try pure addition if possible and not tabu
            if(core.size() < maxSize){
                core.add(a);
                score = pm.calculate(core, cacheID);
                if(score > bestScore && (tabu == null || !tabu.contains(-1) || score - curBestScore > MIN_TABU_ASPIRATION_PROG)){
                    bestScore = score;
                    bestRemIndex = -1;
                }
                core.remove(core.size()-1);
            }

        }

        // peturb core into 'best' neighor found by heuristic
        return performBestPerturbation(core, bestRemIndex, unselected, bestAddIndex, tabu);
    }

    @Override
    public int genRandomNeighbor(List<Accession> core, List<Accession> unselected) {
        throw new UnsupportedOperationException("The HeuristicSingleNeighborhood can not be use to generate random neighbors. "
                                                    + "It is especially designed as a heuristic to generate one of the 'best' "
                                                    + "neighbors, without investigating them all. To generate random neighbors "
                                                    + "obtained by a single perturbation, please use the RandomSingleNeighborhood.");
    }

}
