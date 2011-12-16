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

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Iterator;
import java.util.Map;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>;
 */
public final class SSRDataset extends AccessionDataset<List<Double>> {
    protected Integer markerCount;
    protected Map<String, Integer> markerIndex;
    protected List<String> markerNames;
    protected List<Map<String, Integer>> alleleIndex;
    protected List<List<String>> alleleName;
    protected Map<String, List<String>> markersToAlleles;

    public SSRDataset(Collection<String> accessions, Map<String, List<String>> markersToAlleles) {
	super(accessions, markersToAlleles.keySet());
		
	this.markersToAlleles = markersToAlleles;

	// create an alias for traitName/traitIndex/traitCount for consistent naming within this class
	markerCount = traitCount;
	markerIndex = traitIndex;
	markerNames = traitNames;
		
	// initialize the allele name <-> allele index mapping data structures
	alleleIndex = new ArrayList<Map<String, Integer>>(markerCount);
	alleleName = new ArrayList<List<String>>(markerCount);
	int mIndex = 0;
	for (String marker : markerNames) {
	    List<String> alleles = markersToAlleles.get(marker);
	    alleleIndex.add( new HashMap<String, Integer>(alleles.size()) );
	    alleleName.add( new ArrayList<String>(alleles.size()) );
			
	    int alIndex = 0;
	    for (String allele : alleles) {
		alleleName.get(mIndex).add(allele);
		alleleIndex.get(mIndex).put(allele, alIndex++);
	    }
	    mIndex++;
	}
		
	// initialize the allele list to missing (null) values
	initAlleles();
    }

    public static SSRDataset createFromFile(String filename) {
	// TODO: add some error checking in here
	SSRDataset ds = null;
	List<String> accessions = new ArrayList<String>();
	Map<String, List<String>> markersToAlleles = new HashMap<String, List<String>>();
	String nextLine[];
		
	try {
	    CSVReader reader = new CSVReader(new FileReader(filename));
	    
	    List lines = reader.readAll();
	    reader.close();
	    
	    Iterator itr = lines.iterator();
	    
	    if (itr.hasNext()) {
		nextLine = (String [])itr.next();
		for(int i=2; i<nextLine.length; i++) {
		    accessions.add(nextLine[i]);
		}
	    }

            int lineNumber = 1;
	    while (itr.hasNext()) {
		nextLine = (String [])itr.next();

                if (nextLine.length < 2) {
                  lineNumber++;
                  System.err.println("Dataset is not properly formatted on line " + lineNumber);
                  System.err.println("Please refer to the CoreHunter manual.  " +
                                     "There should be a marker name and allele name separated by a comma " +
                                     "followed by values for each accession also separated by a comma.");
                  System.err.print("'" + nextLine[0] + "'");
                  return null;
                }

		String marker = nextLine[0];
		String allele = nextLine[1];

		if (!markersToAlleles.containsKey(marker)) {
		    markersToAlleles.put(marker, new ArrayList<String>());
		}
		markersToAlleles.get(marker).add(allele);
	    }

	    if (accessions.size()<2) {
		System.err.println("Dataset must contain at least 2 accessions");
		return null;
	    }

	    if (markersToAlleles.size()<1) {
		System.err.println("Dataset must contain at least 1 marker/allele");
		return null;
	    }

	    // create the SSRDataset object
	    ds = new SSRDataset(accessions, markersToAlleles);

	    itr = lines.iterator();
	    itr.next();

	    // add the allele values for each genotype
	    while (itr.hasNext()) {
                nextLine = (String [])itr.next();
                String marker = nextLine[0];
                String allele = nextLine[1];

		for(int i=2; i<nextLine.length; i++) {
		    if (nextLine[i].equals("")) continue;
		    String accession = accessions.get(i-2);
		    Double alleleValue = null;
		    
		    try {
			alleleValue = new Double(nextLine[i]);
			ds.setValue(accession, marker, allele, alleleValue);
		    } catch(NumberFormatException nfe) {
			System.err.println("");
			System.err.println( nfe.getMessage() );
			System.err.println("");
			System.err.println("Invalid allele value for accession '" + accession +
					   "' marker '" + marker + "' allele '" + allele + "'");
			return null;
		    } catch(UnknownAccessionException uae) {
			System.err.println("");
			System.err.println("bug found.  please contact authors");
			return null;
		    } catch(UnknownTraitException ute) {
			System.err.println("");
			System.err.println("bug found.  please contact authors");
			return null;
		    }
		}
	    }

 	} catch(IOException ioe) {
	    System.err.println("");
	    System.err.println( ioe.getMessage() );
	    return null;
	}

	return ds;
    }

    public void writeToFile(String filename) {
	writeToFile(filename, accessionNames);
    }

    // only write out accession names, contained in filter
    public void writeToFile(String filename, List<String> accessions) {
	try {
	    CSVWriter writer = new CSVWriter(new FileWriter(filename), ',', CSVWriter.NO_QUOTE_CHARACTER);

	    String[] entries = new String[accessions.size() + 2];
	    entries[0] = "Marker";
	    entries[1] = "Allele";
	    
	    for(int i=0; i<accessions.size(); i++) {
		entries[i+2] = accessions.get(i);
	    }
	    
	    writer.writeNext(entries);

	    // now write out data
	    for (String marker : markersToAlleles.keySet()) {
		for (String allele : markersToAlleles.get(marker)) {
		    entries[0] = marker;
		    entries[1] = allele;
		    for (int i=0; i<accessions.size(); i++) {
			Double val = getValue(accessions.get(i), marker, allele);
			if (val == null) {
			    entries[i+2] = "";
			} else {
			    entries[i+2] = val.toString();
			}
		    }
		    writer.writeNext(entries);
		}
	    }
	    
	    writer.close();
	} catch(Exception e) {
	    System.err.println("");
	    System.err.println(e.getMessage());
	}
    }
	
    /**
     * Setter for SSR dataMatrix.  Ensures that either each allele value is missing
     * within a marker (for a particular accession) or, the sum of all values is 1.0.
     * @param accession the accession index
     * @param marker the marker index
     * @param alleleValues new allele values for marker at accession
     * @throws UnknownAccessionException when accession is not found in the data set
     * @throws UnknownTraitException when marker / allele is not found in the data set
     */
    public void setValue(String accession, String marker, String allele, Double alleleValue) 
	throws UnknownAccessionException, UnknownTraitException
	{
	    Integer acIndex = accessionIndex.get(accession);
	    if (acIndex == null) throw new UnknownAccessionException("No accession found with id: " + accession);
	    Integer mIndex = markerIndex.get(marker);
	    if (mIndex == null) throw new UnknownTraitException("No marker found with id: " + marker);
	    Integer alIndex = alleleIndex.get(mIndex).get(allele);
	    if (alIndex == null) throw new UnknownTraitException("No allele found with id: " + allele);
		
	    setValue(acIndex, mIndex, alIndex, alleleValue);
	}
	

    /**
     * Setter for SSR dataMatrix.  Ensures that either each allele value is missing
     * within a marker (for a particular accession) or, the sum of all values is 1.0.
     * @param accession the accession index
     * @param marker the marker index
     * @param alleleValues new allele values for marker at accession
     * @throws UnknownAccessionException when accession is not found in the accession list
     * @throws UnknownTraitException when marker is not found in the marker list
     */
    public void setValue(String accession, String marker, List<Double> alleleValues)
	throws UnknownAccessionException, UnknownTraitException
	{
	    Integer acIndex = accessionIndex.get(accession);
	    if (acIndex == null) throw new UnknownAccessionException("No accession found with id: " + accession);
	    Integer mIndex = markerIndex.get(marker);
	    if (mIndex == null) throw new UnknownTraitException("No marker found with id: " + marker);
		
	    if (alleleName.get(mIndex).size() != alleleValues.size()) {
		throw new UnknownTraitException(
						"Marker " + marker + " requires " + alleleName.get(mIndex).size() 
						+ " allele values.  " + alleleValues.size() + " were provided."
						);
	    }
		
	    int alIndex = 0;
	    for (Double alleleValue : alleleValues) {
		setValue(acIndex, mIndex, alIndex++, alleleValue);
	    }
	}
	
    /**
     * getValue
     *
     * @param  
     * @return 
     */
    public Double getValue(String accession, String marker, String allele) {
	Integer acIndex = accessionIndex.get(accession);
	if (acIndex == null) return null;
	Integer mIndex = markerIndex.get(marker);
	if (mIndex == null) return null;
	Integer alIndex = alleleIndex.get(mIndex).get(allele);
	if (alIndex == null) return null;
		
	return dataMatrix.get(acIndex).get(mIndex).get(alIndex);
    }
	
    /**
     * normalize
     *
     * @param  
     * @return 
     */
    public void normalize() {
	ListIterator<List<List<Double>>> acItr = dataMatrix.listIterator();
	while (acItr.hasNext()) {
	    ListIterator<List<Double>> mItr = acItr.next().listIterator();
			
	    while (mItr.hasNext()) {
                double sum = 0.0;
                boolean allMissing = true;

		List<Double> alleles = mItr.next();
		ListIterator<Double> alItr = alleles.listIterator();
		while (alItr.hasNext()) {
		    Double allele = alItr.next();
		    if (allele != null) {
			sum += allele;
			allMissing = false;
		    }
		}
				
		if (!allMissing) {
		    alItr = alleles.listIterator();
		    while (alItr.hasNext()) {
			Double allele = alItr.next();
			Double v = (allele == null) ? 0.0 : allele;
			alItr.set((sum == 0.0) ? 0.0 : (v / sum));
		    }
		}
	    }
			
	}
    }
	
    /**
     * initAlleles
     *
     * @param  
     * @return 
     */
    private void initAlleles() {
	for (int acIndex = 0; acIndex < accessionCount; acIndex++) {
	    for (int mIndex = 0; mIndex < markerCount; mIndex++) {
		int alCnt = alleleName.get(mIndex).size();
		dataMatrix.get(acIndex).set(mIndex, new ArrayList<Double>(alCnt));
		for (int alIndex = 0; alIndex < alCnt; alIndex++) {
		    dataMatrix.get(acIndex).get(mIndex).add(null);
		}
	    }
	}
    }
	
    private void setValue(int accession, int marker, int allele, Double alleleValue) {
	//List<List<Double>> markerValues = dataMatrix.get(accession);
	//List<Double> alleleValues = markerValues.get(marker);	
	dataMatrix.get(accession).get(marker).set(allele, alleleValue);
	//alleleValues.set(allele, alleleValue);
    }
}
