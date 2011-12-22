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
