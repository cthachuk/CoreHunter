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

import java.util.List;
import java.util.ListIterator;
import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public final class ModifiedRogersDistance extends DistanceMeasure {

    public ModifiedRogersDistance(int accessionCount) {
	this(accessionCount, DistanceMeasureType.MEAN_DISTANCE);
    }

    public ModifiedRogersDistance(int accessionCount, DistanceMeasureType type) {
	this("MR" + type.getNameSuffix(), "Modified Rogers Distance" + type.getDescriptionSuffix(), accessionCount, type);
    }
    
    public ModifiedRogersDistance(String name, String description, int accessionCount, DistanceMeasureType type) {
	super(name, description, accessionCount, type);
    }
    
    public double calculate(Accession a1, Accession a2) {
	double value = getMemoizedValue(a1.getId(), a2.getId());
	if (value != MISSING_VAL) {
	    return value;
	}

	ListIterator<List<Double>> m1Itr = a1.getSSRValues().listIterator();
	ListIterator<List<Double>> m2Itr = a2.getSSRValues().listIterator();
	
	double markerCnt = 0;
	double sumMarkerSqDiff = 0;
	while (m1Itr.hasNext() && m2Itr.hasNext()) {
	    ListIterator<Double> a1Itr = m1Itr.next().listIterator();
	    ListIterator<Double> a2Itr = m2Itr.next().listIterator();
	    
	    double markerSqDiff = 0;
	    while (a1Itr.hasNext() && a2Itr.hasNext()) {
		Double Pxla = a1Itr.next(); 
		Double Pyla = a2Itr.next();
		
		if( Pxla != null && Pyla != null ) {
		    markerSqDiff += (Pxla - Pyla) * (Pxla - Pyla);
		}
	    }
	    
	    sumMarkerSqDiff += markerSqDiff;
	    markerCnt++;
	}
	
	value = 1.0/(Math.sqrt(2.0 * markerCnt))*Math.sqrt(sumMarkerSqDiff);
	setMemoizedValue(a1.getId(), a2.getId(), value);
	return value;
    }
	
}
