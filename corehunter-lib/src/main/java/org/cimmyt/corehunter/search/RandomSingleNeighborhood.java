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

import java.util.Collections;
import java.util.List;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 * Implements a standard neighborhood which contains all sets that differ in
 * at most one accession from the current core set, meaning one accesion will
 * be deleted, added or swapped at random.
 *
 * @author hermandebeukelaer
 */
public class RandomSingleNeighborhood extends SingleNeighborhood {

    public RandomSingleNeighborhood(int minSize, int maxSize){
        this(minSize, maxSize,1);
    }

    public RandomSingleNeighborhood(int minSize, int maxSize, int historySize){
        super(minSize, maxSize, historySize);
    }

    @Override
    public RandomSingleNeighborhood clone(){
        return new RandomSingleNeighborhood(minSize, maxSize, historySize);
    }

    @Override
    public int genBestNeighbor(List<Accession> core, List<Accession> unselected, List<Integer> tabu,
                                                double curBestScore, PseudoMeasure pm, String cacheID) {

        // search for (one of the) best neighbor(s) by perturbing core
        // in all possible ways (remove 1, add 1, swap 1)
        int bestAddIndex = -1;
        int bestRemIndex = -1;
        double bestScore = -Double.MAX_VALUE;
        double score;

        // try removing one, if min size not reached
        if (core.size() > minSize){
            // try deleting all elements from the core (backward loop)
            for (int i=core.size()-1; i>=0; i--){
                Accession a = core.remove(i);
                score = pm.calculate(core, cacheID);
                // ensure index is not tabu
                if (score > bestScore && (tabu == null || !tabu.contains(i) || score - curBestScore > MIN_TABU_ASPIRATION_PROG)){
                    bestScore = score;
                    bestAddIndex = -1; // do not add anything
                    bestRemIndex = i; // remove element i from core
                }
                // re-add element at the end of the list to keep indices intact
                // of elements that still have to be considered
                core.add(a);
            }
            // because all elements are re-added at the end of the list, the order
            // of elements in the core is flipped, so we flip it again to keep
            // indices consistent
            Collections.reverse(core);
        }
        // try all possible swaps: remove 1 AND add 1 to replace it
        for (int i=0; i<unselected.size(); i++){
            // accession to add
            Accession add = unselected.get(i);
            // loop over all possible core elements and try replacing them with new element
            for (int j=0; j<core.size(); j++){
                // replace accession with new accession
                Accession rem = core.set(j, add);
                score = pm.calculate(core, cacheID);
                // ensure index is not tabu
                if (score > bestScore && (tabu == null || !tabu.contains(j) || score - curBestScore > MIN_TABU_ASPIRATION_PROG)){
                    bestScore = score;
                    bestAddIndex = i; // add element i from remaining accession collection
                    bestRemIndex = j; // remove element j from core
                }
                // undo swap
                core.set(j, rem);
            }
        }
        // try adding one, if max size not reached and not restricted by tabu list
        // (if a pure deletion occured in the scope of the tabu list, an pure a addition
        //  is not allowed to prevent going back to the previous solution!)
        if (core.size() < maxSize){
            // try adding all elements from unselected collection
            for (int i=0; i<unselected.size(); i++){
                Accession a = unselected.get(i);
                core.add(a);
                score = pm.calculate(core, cacheID);
                if (score > bestScore && (tabu == null || !tabu.contains(-1) || score - curBestScore > MIN_TABU_ASPIRATION_PROG)){
                    bestScore = score;
                    bestAddIndex = i; // add element i from accession collection
                    bestRemIndex = -1; // do not remove anything
                }
                // remove newly added accession
                core.remove(core.size()-1);
            }
        }

        // perform best perturbation on core and return this as the new core
        // also update unselected list and/or tabu list
        return performBestPerturbation(core, bestRemIndex, unselected, bestAddIndex, tabu);
        
    }

    @Override
    public int genRandomNeighbor(List<Accession> core, List<Accession> unselected) {
        // randomly perturb core elements
        if (unselected.size() == 0) {
            // core currently contains ALL accessions, only remove possible
            return removeRandom(core, unselected);
        } else {
            double p = rg.nextDouble();
            if (p>=0.66 && core.size() < maxSize) {
                return addRandom(core, unselected);
            } else if (p>=0.33 && core.size() > minSize) {
                return removeRandom(core, unselected);
            } else {
                return swapRandom(core, unselected);
            }
        }
    }

    private int swapRandom(List<Accession> core, List<Accession> unselected){

        // randomly swap one item
        int remIndex = rg.nextInt(core.size());
        int addIndex = rg.nextInt(unselected.size());

        // get new item from unselected and replace old item in core
        Accession removed = core.set(remIndex, unselected.get(addIndex));
        // put old item in unselected to complete swap
        unselected.set(addIndex, removed);

        // update history
        addHistoryItem(new Swap(remIndex, addIndex));

        return remIndex;
    }

    private int addRandom(List<Accession> core, List<Accession> unselected){
        // randomly add one item
        int addIndex = rg.nextInt(unselected.size());

        // remove new item from unselected and put it in the core (at the end)
        core.add(unselected.remove(addIndex));

        // update history
        addHistoryItem(new Addition(addIndex));

        return core.size()-1;
    }

    private int removeRandom(List<Accession> core, List<Accession> unselected){
        // randomly remove one item
        int remIndex = rg.nextInt(core.size());

        // remove item from core and add it to unselected (at the end)
        unselected.add(core.remove(remIndex));

        // update history
        addHistoryItem(new Deletion(remIndex));

        return -1;
    }

}
