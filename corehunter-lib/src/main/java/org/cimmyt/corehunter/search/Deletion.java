/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.search;

import java.util.List;
import org.cimmyt.corehunter.Accession;

/**
 *
 * @author hermandebeukelaer
 */
public class Deletion implements SinglePerturbation {

    // position of the removed item in the core, before it was removed
    private int coreIndex;
    // tabu list of indices in the core which are tabu
    private List<Integer> tabuList;

    public Deletion(int coreIndex){
        this(coreIndex, null);
    }

    public Deletion(int coreIndex, List<Integer> tabuList){
        this.coreIndex = coreIndex;
        this.tabuList = tabuList;
    }

    public void undo(List<Accession> core, List<Accession> unselected) {
        // To undo a pure deletion:
        //  - remove last element from unselected
        //  - restore this element to its old position in the core
        //  - update tabu list if present!! (shift indices after coreIndex)
        core.add(coreIndex, unselected.remove(unselected.size()-1));
        if (tabuList != null){
            for (int i=0; i<tabuList.size(); i++){
                if (tabuList.get(i) >= coreIndex){
                    tabuList.set(i, tabuList.get(i)+1);
                }
            }
        }
    }

}
