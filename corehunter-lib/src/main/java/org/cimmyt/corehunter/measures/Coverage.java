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

import org.cimmyt.corehunter.Accession;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public final class Coverage extends Measure {
    private ProportionNonInformativeAlleles pn;

    public Coverage() {
	this("CV", "Trait coverage relative to collection");
    }
    
    public Coverage(String name, String description) {
	super(name, description);
	pn = new ProportionNonInformativeAlleles();
    }

    public double calculate(List<Accession> accessions) {
	return 1.0 - pn.calculate(accessions);
    }

    public double calculate(List<Accession> accessions, String id) {
	return 1.0 - pn.calculate(accessions, id);
    }
}

