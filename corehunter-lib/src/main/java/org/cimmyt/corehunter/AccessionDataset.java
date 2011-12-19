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

package org.cimmyt.corehunter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>;
 */
public abstract class AccessionDataset<T> {
	protected Integer traitCount;
	protected Integer accessionCount;
	protected Map<String, Integer> traitIndex;
	protected List<String> traitNames;
	protected Map<String, Integer> accessionIndex;
	protected List<String> accessionNames;	
	protected List<List<T>> dataMatrix;
    	
	public AccessionDataset(Collection<String> accessionNames, Collection<String> traitNames) {
		this.traitCount = traitNames.size();
		this.traitIndex = new HashMap<String, Integer>(traitCount);
		this.traitNames = new ArrayList<String>(traitCount);
		this.accessionCount = accessionNames.size() + 1;
		this.accessionIndex = new HashMap<String, Integer>(accessionCount);
		this.accessionNames = new ArrayList<String>(accessionCount);
		this.dataMatrix = new ArrayList<List<T>>(accessionCount);
		
		// initialize the dataMatrix to have all MISSING (null) values
		initDataMatrix();
		
		// initialize the traitNames <-> traitIndex mapping
		int traitID = 0;
		for (String trait : traitNames) {
			this.traitNames.add(trait);
			traitIndex.put(trait, traitID);
			traitID += 1;
		}
				
		// initialize the accessionNames <-> accessionIndex mapping
		int accessionID = 1;
		for (String accession : accessionNames) {
			this.accessionNames.add(accession);
			accessionIndex.put(accession, accessionID);
			accessionID += 1;
		}
	}
	
	/**
	 * setValue
	 *
	 * @param  
	 * @return 
	 */
	public void setValue(String accession, String trait, T v) 
		throws UnknownAccessionException, UnknownTraitException {
		
		Integer aIndex = getAccessionIndex(accession);
		Integer tIndex = getTraitIndex(trait);	
		
		if (aIndex == null) throw new UnknownAccessionException("No accession found with id: " + accession);
		if (tIndex == null) throw new UnknownTraitException("No trait found with id: " + trait);
		
		List<T> traitValues = dataMatrix.get(aIndex);
		traitValues.set(tIndex, v);
	}
	
	/**
	 * getValue
	 *
	 * @param  
	 * @return 
	 */
	public T getValue(String accession, String trait) {
		Integer aIndex = getAccessionIndex(accession);
		Integer tIndex = getTraitIndex(trait);
		if (aIndex == null || tIndex == null) return null;

		List<T> traitValues = dataMatrix.get(aIndex);
		return traitValues.get(tIndex);
	}
	
	/**
	 * getAccessionID
	 *
	 * @param  
	 * @return 
	 */
	public Integer getAccessionIndex(String accession) {
		return accessionIndex.get(accession);
	}
	
	/**
	 * getTraitID
	 *
	 * @param  
	 * @return 
	 */
	public Integer getTraitIndex(String trait) {
		return traitIndex.get(trait);
	}		
	
	/**
	 * getValues
	 *
	 * @param  
	 * @return 
	 */
	public List<T> getValues(String accession) {
		Integer aIndex = getAccessionIndex(accession);
		if (aIndex == null) return null;
		
		return dataMatrix.get(aIndex);
	}
	
	/**
	 * getAccessionNames
	 *
	 * @param  
	 * @return 
	 */
	public List<String> getAccessionNames() {
		return accessionNames;
	}

	
	
	/**
	 * normalize
	 *
	 * @param  
	 * @return 
	 */
	public void normalize() {
		
	}
	
	/**
	 * initData
	 *
	 * @param  
	 * @return 
	 */
	protected void initDataMatrix() {
		dataMatrix.clear();
		for (int i=0; i<accessionCount; i++) {
			List<T> accessionRow = new ArrayList<T>(traitCount);
			for (int j=0; j<traitCount; j++) {
				accessionRow.add(null);
			}
			dataMatrix.add(accessionRow);
		}
	}
	
}
