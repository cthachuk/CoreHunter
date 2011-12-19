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
public class Swap implements SinglePerturbation {

    // position of swap in the core
    private int coreIndex;
    // position of swap in the unselected list
    private int unselIndex;

    public Swap(int coreIndex, int unselIndex){
        this.coreIndex = coreIndex;
        this.unselIndex = unselIndex;
    }

    public void undo(List<Accession> core, List<Accession> unselected) {
        // To undo a swap: swap again!
        Accession a = core.set(coreIndex, unselected.get(unselIndex));
        unselected.set(unselIndex, a);
    }

}
