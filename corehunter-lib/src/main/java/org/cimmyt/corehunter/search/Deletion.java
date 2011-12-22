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
