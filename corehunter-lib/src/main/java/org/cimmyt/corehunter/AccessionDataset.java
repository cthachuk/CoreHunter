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
