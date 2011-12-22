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

import java.util.LinkedList;
import java.util.List;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 *
 * @author hermandebeukelaer
 */
public abstract class SingleNeighborhood extends Neighborhood {

    protected LinkedList<SinglePerturbation> history;

    public SingleNeighborhood(int minSize, int maxSize, int historySize){
        super(minSize, maxSize, historySize);
        history = new LinkedList<SinglePerturbation>();
    }
    
    protected int performBestPerturbation(List<Accession> core, int bestRemIndex,
                                          List<Accession> unselected, int bestAddIndex,
                                          List<Integer> tabu){
        SinglePerturbation pert;
        if (bestAddIndex != -1){
            if (bestRemIndex == -1){
                // only add new element (at the end)
                core.add(unselected.remove(bestAddIndex));
                // create history item
                pert = new Addition(bestAddIndex);
            } else {
                // swap element
                Accession rem = core.set(bestRemIndex, unselected.get(bestAddIndex));
                unselected.set(bestAddIndex, rem);
                // create history item
                pert = new Swap(bestRemIndex, bestAddIndex);
            }
        } else {
            // only remove element
            unselected.add(core.remove(bestRemIndex));
            // update tabu indices!
            if (tabu != null){
                for (int i=0; i<tabu.size(); i++){
                    if (tabu.get(i) > bestRemIndex){
                        tabu.set(i, tabu.get(i)-1);
                    }
                }
            }
            // create history item
            pert = new Deletion(bestRemIndex, tabu);
        }
        //update history
        addHistoryItem(pert);

        if (bestAddIndex != -1){
            if (bestRemIndex == -1){
                return core.size()-1; // added at the end
            } else {
                return bestRemIndex; // swapped
            }
        } else {
            return -1; // only removed one
        }
    }

    protected void addHistoryItem(SinglePerturbation pert){
        if (history.size() == historySize){
            // history is full, delete oldest item
            history.poll();
        }
        // add new item
        history.offer(pert);
    }

    @Override
    public boolean undoLastPerturbation(List<Accession> core, List<Accession> unselected){
        if (!history.isEmpty()){
            SinglePerturbation pert = history.removeLast();
            pert.undo(core, unselected);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public abstract int genBestNeighbor(List<Accession> core, List<Accession> unselected,
                                        List<Integer> tabu, double curBestScore, PseudoMeasure pm, String cacheID);

    @Override
    public abstract int genRandomNeighbor(List<Accession> core, List<Accession> unselected);

}
