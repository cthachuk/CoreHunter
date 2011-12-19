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

import java.util.List;
import java.util.ListIterator;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 */
public /*final*/ class Accession implements Comparable<Accession> {
    protected static final int UNKNOWN_ID = -1;
    protected static int nextAccessionId = 0;

    protected String name;
    protected int id;
    protected Integer dartIndex;
    protected Integer ssrIndex;
    
    protected List<DArTValue> dartValues;
    protected List<List<Double>> ssrValues;

    protected Double extDistance;
	
    /**
     * 
     */
    public Accession(String name) {
	setName(name);
	id = nextAccessionId++;	
    }

    public void setName(String name) {
	this.name = name;
    }
	
    public String getName() {
	return name;
    }

    public int getId() {
	return id;
    }

    public static void reset() {
	nextAccessionId = 0;
    }
	
    public void bindTraitValues(AccessionDataset ds) {
	if (ds instanceof DArTDataset) {
	    bindDArTValues((DArTDataset)ds);
	} else if (ds instanceof SSRDataset) {
	    bindSSRValues((SSRDataset)ds);
	}
    }
	
    public void bindDArTValues(DArTDataset ds) {
	dartIndex = ds.getAccessionIndex(name);
	dartValues = ds.getValues(name);
    }
	
    public void bindSSRValues(SSRDataset ds) {
	ssrIndex = ds.getAccessionIndex(name);
	ssrValues = ds.getValues(name);
        extDistance = ds.getExtDistance(name);
    }
	
    public List<List<Double>> getSSRValues() {
	return ssrValues;
    }

    public Double getExtDistance(){
        return extDistance;
    }

    public int numSSRAlleles() {
	ListIterator<List<Double>> mItr = getSSRValues().listIterator();
	ListIterator<Double> aItr = null;
	
	int alleleCnt = 0;
	while (mItr.hasNext()) {
	    aItr = mItr.next().listIterator();
	    while (aItr.hasNext()) {
		alleleCnt++;
		aItr.next();
	    }
	}
	return alleleCnt;
    }

    public int numSSRMarkers() {
	return getSSRValues().size();
    }

    public int compareTo(Accession a) {
        return name.compareTo(a.name);
    }

    public static double[][] getMarkerAlleleTotals(List<Accession> accessions) {
	if (accessions.size()==0) return null;

	Accession a1 = accessions.get(0);
	int markerCnt = a1.numSSRMarkers();
	double markerAlleleTotals[][] = new double[markerCnt][];

	ListIterator<List<Double>> mItr = a1.getSSRValues().listIterator();
	ListIterator<Double> aItr = null;

	int i = 0;
	while (mItr.hasNext()) {
	    List<Double> alleles = mItr.next();

	    markerAlleleTotals[i] = new double[alleles.size()];
	    for(int j=0; j<alleles.size(); j++) {
		markerAlleleTotals[i][j] = 0.0;
	    }
	    i++;
	}
	
	for(Accession a : accessions) {
	    mItr = a.getSSRValues().listIterator();
	    aItr = null;

	    i = 0;
	    while (mItr.hasNext()) {
		aItr = mItr.next().listIterator();
		int j = 0;
		while (aItr.hasNext()) {
		    Double val = aItr.next();
		    if(val != null) {
			double v = val.doubleValue();
			markerAlleleTotals[i][j] += v;
		    }
		    j++;
		}
		i++;
	    }
	}

	return markerAlleleTotals;
    }

    public static double[] getAlleleTotals(List<Accession> accessions) {
	if (accessions.size()==0) return null;

	Accession a1 = accessions.get(0);
	int alleleCnt = a1.numSSRAlleles();
	double alleleTotals[] = new double[alleleCnt];
	for(int i=0; i<alleleCnt; i++) {
	    alleleTotals[i] = 0.0;
	}
		
	for(Accession a : accessions) {
	    ListIterator<List<Double>> mItr = a.getSSRValues().listIterator();
	    ListIterator<Double> aItr = null;

	    int i = 0;
	    while (mItr.hasNext()) {
		aItr = mItr.next().listIterator();
		while (aItr.hasNext()) {
		    Double val = aItr.next();
		    if(val != null) {
			double v = val.doubleValue();
			alleleTotals[i] += v;
		    }
		    i++;
		}
	    }
	}
	
	return alleleTotals;
    }

    public static int[] getAlleleCounts(List<Accession> accessions) {
	if (accessions.size()==0) return null;

	Accession a1 = accessions.get(0);
	int alleleCnt = a1.numSSRAlleles();
	int alleleTotals[] = new int[alleleCnt];
	for(int i=0; i<alleleCnt; i++) {
	    alleleTotals[i] = 0;
	}
		
	for(Accession a : accessions) {
	    ListIterator<List<Double>> mItr = a.getSSRValues().listIterator();
	    ListIterator<Double> aItr = null;

	    int i = 0;
	    while (mItr.hasNext()) {
		aItr = mItr.next().listIterator();
		while (aItr.hasNext()) {
		    Double val = aItr.next();
		    if(val != null) {
			double v = val.doubleValue();
			if (v > 0) {
			    alleleTotals[i] += 1;
			}
		    }
		    i++;
		}
	    }
	}
	
	return alleleTotals;
    }

}

