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
public interface SinglePerturbation {

    public abstract void undo(List<Accession> core, List<Accession> unselected);

}
