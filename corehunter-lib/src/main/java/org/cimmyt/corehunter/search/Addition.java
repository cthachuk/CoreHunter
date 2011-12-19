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
public class Addition implements SinglePerturbation {

    // position in the unselected list of the newly added core item,
    // before it was deleted from the unselected list
    private int unselIndex;

    public Addition(int unselIndex){
        this.unselIndex = unselIndex;
    }

    public void undo(List<Accession> core, List<Accession> unselected) {
        // To undo a pure addition at the end of the core:
        //  - remove last item from core
        //  - add this item to the unselected list at its old position
        unselected.add(unselIndex, core.remove(core.size()-1));
    }

}
