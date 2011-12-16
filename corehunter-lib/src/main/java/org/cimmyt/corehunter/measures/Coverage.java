//  Copyright 2008,2011 Chris Thachuk
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

import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

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

