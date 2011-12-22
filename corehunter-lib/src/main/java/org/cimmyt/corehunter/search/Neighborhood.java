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

import java.util.List;
import java.util.Random;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 * Implements an abstract neighborhood which defines the neighbors of a given
 * core subset. Depending on the choosen algorithm that uses the neighborhood,
 * one can generate a random neighbor or the entire neighborhood.
 *
 * @author hermandebeukelaer
 */
public abstract class Neighborhood {

    protected static final Random rg = new Random();
    protected static final double MIN_TABU_ASPIRATION_PROG = 10e-9;


    // min. & max. core subset size
    protected int minSize, maxSize;

    // nr of previous states that can be recovered using undo
    protected int historySize;

    public Neighborhood(int minSize, int maxSize, int historySize){
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.historySize = historySize;
    }

    @Override
    public abstract Neighborhood clone();

    /**
     * Undo the last perturbation to restore both the given core and list of unselected
     * accessions to their previous state. Implementations of the Neighborhood class
     * should keep track of last changes to make such undo possible. Remark that this
     * method only guarantees to give the correct result if both 'core' and 'unselected'
     * have not been changed externally after the last perturbation!
     *
     * The parameter 'historySize', which is set when creating the Neigborhood, defines
     * how much previous states are remembered by the neighborhood. If the standard
     * constructor is used, this is set to 1 and only the very last perturbation
     * can be undone.
     *
     * @param core
     * @param unselected
     * @return True if undo was successful, false if not successful because history was depleted
     */
    public abstract boolean undoLastPerturbation(List<Accession> core, List<Accession> unselected);

    /**
     * Perturb the given core set into its best neighbor. If neighborhood contains
     * multiple cores with exactly the same score, one of these is randomly selected.
     * This method also accepts a tabu list of indices in the core set which are currently tabu.
     *
     * @param core The current core subset
     * @param unselected List of all currently unselected accessions, i.e. all accession not
     *                   contained in the current core set (first parameter). The implementation
     *                   should ensure that this list is kept consistent after perturbing the core.
     * @param tabu List of indices which are tabu, meaning that the elements in the core set at
     *             these indices cannot be removed for constructing the 'best' neighbor, these
     *             neighbors themselves are tabu and must be avoided! If tabu list contains
     *             value(s) of "-1", only adding an element is tabu. Implementation should
     *             ensure that tabu list is kept consistent in case of reordering of elements
     *             in the core, e.g. in case of the deletion of an element.
     * @param curBestScore current best score over all visited solutions, used in an aspiration
     *                     criterion which overrides tabu: solutions which are better than currently
     *                     best observed solution are always accepted!
     * @param pm The pseudomeasure used to compute scores of core sets
     * @param cacheID The cacheID to be used for computing the (cached) pseudomeasure, null if no caching
     * @return The index where a new accession has been added to the core:
     *                     -1 In case of only deleting an accession
     *   0 <= i <= coreSize-1 In case of a swap
     *          newCoreSize-1 In case of only adding a new element, at the end of the list
     */
    public abstract int genBestNeighbor(List<Accession> core,List<Accession> unselected,
                                        List<Integer> tabu, double curBestScore, PseudoMeasure pm, String cacheID);

    /**
     * Perturb the given core set into its best neighbor. If neighborhood contains
     * multiple cores with exactly the same score, one of these is randomly selected.
     *
     * @param core The current core subset
     * @param unselected List of all currently unselected accessions, i.e. all accession not
     *                   contained in the current core set (first parameter). The implementation
     *                   should ensure that this list is kept consistent after perturbing the core.
     * @param pm The pseudomeasure used to compute scores of core sets
     * @param cacheID The cacheID to be used for computing the (cached) pseudomeasure, null if no caching
     * @return The index where a new accession has been added to the core:
     *                     -1 In case of only deleting an accession
     *   0 <= i <= coreSize-1 In case of a swap
     *          newCoreSize-1 In case of only adding a new element, at the end of the list
     */
    public int genBestNeighbor(List<Accession> core, List<Accession> unselected,
                                                            PseudoMeasure pm, String cacheID){
        return genBestNeighbor(core, unselected, null, -1, pm, cacheID);
    }

    /**
     * Randomly perturb the given core set into one of its neighbors.
     *
     * @param core The current core subset
     * @param unselected List of all currently unselected accessions, i.e. all accession not
     *                   contained in the current core set (first parameter). The implementation
     *                   should ensure that this list is kept consistent after perturbing the core.
     * @return The index where a new accession has been added to the core:
     *                     -1 In case of only deleting an accession
     *   0 <= i <= coreSize-1 In case of a swap
     *          newCoreSize-1 In case of only adding a new element, at the end of the list
     */
    public abstract int genRandomNeighbor(List<Accession> core, List<Accession> unselected);

}
