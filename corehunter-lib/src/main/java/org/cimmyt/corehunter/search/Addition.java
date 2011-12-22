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
