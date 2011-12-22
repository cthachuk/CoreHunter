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

package org.cimmyt.corehunter.measures;

/**
 *
 * @author hermandebeukelaer
 */
public enum DistanceMeasureType {

    MEAN_DISTANCE("",""), // for optimzing minimum distance

    MIN_DISTANCE("min", " (minimum)"); // for optimizing mean distance


    private final String nameSuffix;
    private final String descriptionSuffix;

    DistanceMeasureType(String nameSuffix, String descriptionSuffix) {
        this.nameSuffix = nameSuffix;
        this.descriptionSuffix = descriptionSuffix;
    }

    public String getNameSuffix(){
        return nameSuffix;
    }
    
    public String getDescriptionSuffix(){
        return descriptionSuffix;
                
    }

}
