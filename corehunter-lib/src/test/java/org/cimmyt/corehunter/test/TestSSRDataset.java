package org.cimmyt.corehunter.test;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Before; 
import org.junit.Ignore;
import org.junit.Test; 
import static org.junit.Assert.*; 

import org.cimmyt.corehunter.*;

/**
 * Tests that the SSRDataset class returns correct values for mappings of
 * accession and marker names, for a simple SSR dataset
 */
public final class TestSSRDataset {
	private static final double precision = 0.00001;
	private Collection<String> accessionNames;
	private Map<String, List<String>> markersToAlleles;
	private SSRDataset ssrData;

	@Before
	public void setUpBefore() throws Exception {
		accessionNames = new HashSet<String>();
		accessionNames.add("A1");
		accessionNames.add("A2");
		
		markersToAlleles = new HashMap<String, List<String>>();
		markersToAlleles.put("M1", new ArrayList<String>());
		markersToAlleles.get("M1").add("allele1");
		markersToAlleles.get("M1").add("allele2");
		markersToAlleles.get("M1").add("allele3");
		markersToAlleles.put("M2", new ArrayList<String>());
		markersToAlleles.get("M2").add("allele1");
		markersToAlleles.get("M2").add("allele2");
		
		ssrData = new SSRDataset(accessionNames, markersToAlleles);
		
		ssrData.setValue("A1", "M1", "allele1", 0.3);
		ssrData.setValue("A1", "M1", "allele2", 0.6);
		ssrData.setValue("A1", "M1", "allele3", 0.0);
		ssrData.setValue("A1", "M2", "allele1", 0.3);
		ssrData.setValue("A2", "M1", "allele2", 0.4);
		ssrData.setValue("A2", "M2", "allele2", 1.0);
	}
	
	@Test 
	public void verifyInitialSSRValues() throws Exception { 
		assertEquals(0.3, ssrData.getValue("A1", "M1", "allele1"), precision);
		assertEquals(0.6, ssrData.getValue("A1", "M1", "allele2"), precision);
		assertEquals(0.0, ssrData.getValue("A1", "M1", "allele3"), precision);
		assertEquals(0.3, ssrData.getValue("A1", "M2", "allele1"), precision);
		assertEquals(0.4, ssrData.getValue("A2", "M1", "allele2"), precision);
		assertEquals(1.0, ssrData.getValue("A2", "M2", "allele2"), precision);
	}
	
	@Test 
	public void verifyInitialNullSSRValues() throws Exception { 
		assertNull(ssrData.getValue("A1", "M2", "allele2"));
		assertNull(ssrData.getValue("A2", "M1", "allele1"));
		assertNull(ssrData.getValue("A2", "M1", "allele3"));
		assertNull(ssrData.getValue("A2", "M2", "allele1"));
	}
	
	@Test 
	public void verifyInvalidAccessionMapsToNull() throws Exception { 
		assertNull(ssrData.getValue("A100", "M1", "A1"));
	}
	
	@Test 
	public void verifyInvalidMarkerMapsToNull() throws Exception { 
		assertNull(ssrData.getValue("A1", "M100", "allele1"));
	}
	
	@Test 
	public void verifyInvalidAlleleMapsToNull() throws Exception { 
		assertNull(ssrData.getValue("A1", "M1", "allele100"));
	}
	
	@Test
	public void verifyValueChange() throws Exception {
		ssrData.setValue("A1", "M1", "allele2", 0.5);
		assertEquals(0.5, ssrData.getValue("A1", "M1", "allele2"), precision);
	}
	
	@Test
	public void verifyValueListChange() throws Exception {
		List<Double> newAlleleValues = new ArrayList<Double>(3);
		newAlleleValues.add(0.7);
		newAlleleValues.add(0.1);
		newAlleleValues.add(0.2);
		ssrData.setValue("A1", "M1", newAlleleValues);
		assertArrayEquals(newAlleleValues.toArray(), ssrData.getValue("A1", "M1").toArray());
	}
	
	@Test
	public void verifyNullToValue() throws Exception {
		ssrData.setValue("A2", "M1", "allele1", 0.3);
		assertEquals(0.3, ssrData.getValue("A2", "M1", "allele1"), precision);
	}
	
	@Test
	public void verifyValueToNull() throws Exception {
		ssrData.setValue("A2", "M1", "allele2", null);
		assertNull(ssrData.getValue("A2", "M1", "allele2"));
	}
	
	@Test(expected=UnknownAccessionException.class) 
	public void verifyUnknownAccessionCantBeSet() throws Exception {
		ssrData.setValue("A100", "M1", "allele1", 1.0);
	}
	
	@Test(expected=UnknownTraitException.class) 
	public void verifyUnknownMarkerCantBeSet() throws Exception {
		ssrData.setValue("A1", "M100", "allele1", 1.0);
	}
	
	@Test(expected=UnknownTraitException.class) 
	public void verifyUnknownAlleleCantBeSet() throws Exception {
		ssrData.setValue("A1", "M1", "allele100", 1.0);
	}
	
	@Test
	public void verifyNormilizeScalesData() throws Exception {
		ssrData.normalize();
		assertEquals(0.3/0.9, ssrData.getValue("A1", "M1", "allele1"), precision);
		assertEquals(0.6/0.9, ssrData.getValue("A1", "M1", "allele2"), precision);
		assertEquals(0.0/0.9, ssrData.getValue("A1", "M1", "allele3"), precision);
	}
} 

