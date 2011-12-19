// Copyright 2008 Chris Thachuk (chris.thachuk@gmail.com)
//
// This file is part of Core Hunter.

// Core Hunter is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// Core Hunter is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with Core Hunter.  If not, see <http://www.gnu.org/licenses/>.

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
public final class CavalliSforzaEdwardsDistance extends DistanceMeasure {

    public CavalliSforzaEdwardsDistance(int accessionCount) {
	this(accessionCount, DistanceMeasureType.MEAN_DISTANCE);
    }

    public CavalliSforzaEdwardsDistance(int accessionCount, DistanceMeasureType type) {
	this("CE" + type.getNameSuffix(), "Cavalli-Sforza and Edwards Distance" + type.getDescriptionSuffix(), accessionCount, type);
    }
    
    public CavalliSforzaEdwardsDistance(String name, String description, int accessionCount, DistanceMeasureType type) {
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
		    double sqrtDiff = Math.sqrt(Pxla) - Math.sqrt(Pyla);
		    markerSqDiff += (sqrtDiff) * (sqrtDiff);
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
