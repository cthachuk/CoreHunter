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

import org.cimmyt.corehunter.UnknownMeasureException;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @author Guy Davenport <g.davenport@cgiar.org>
 * @version $Rev$
 */
public final class MeasureFactory {
  
  private static final String[] measureNames = {"MR", "MRmin", "CE", "CEmin","SH", "HE", "NE", "PN", "CV", "EX"};
  
    public static Measure createMeasure(String measureName, int accessionCount) throws UnknownMeasureException {
    if (measureName.equals("MR")) {
        return new ModifiedRogersDistance(accessionCount);
    } else if (measureName.equals("MRmin")) {
        return new ModifiedRogersDistance(accessionCount, DistanceMeasureType.MIN_DISTANCE);
    } else if (measureName.equals("CE")) {
        return new CavalliSforzaEdwardsDistance(accessionCount);
    } else if (measureName.equals("CEmin")) {
        return new CavalliSforzaEdwardsDistance(accessionCount, DistanceMeasureType.MIN_DISTANCE);
    } else if (measureName.equals("SH")) {
        return new ShannonsDiversity();
    } else if (measureName.equals("HE")) {
        return new HeterozygousLociDiversity();
    } else if (measureName.equals("NE")) {
        return new NumberEffectiveAlleles();
    } else if (measureName.equals("PN")) {
        return new ProportionNonInformativeAlleles();
    } else if (measureName.equals("CV")) {
        return new Coverage();
    } else if (measureName.equals("EX")) {
        return new ExternalDistanceMeasure();
    } else {
        throw new UnknownMeasureException("Unknown measure '" + measureName + "'");
    }
    }
    
    public static String[] getMeasureNames()
    {
      return measureNames ;
    }
}
