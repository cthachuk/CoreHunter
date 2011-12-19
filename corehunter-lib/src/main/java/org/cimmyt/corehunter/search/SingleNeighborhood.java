/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

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
