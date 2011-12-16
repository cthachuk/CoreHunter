package org.cimmyt.corehunter.test;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import org.junit.Before; 
import org.junit.Test; 
import static org.junit.Assert.*; 

import org.cimmyt.corehunter.*;

/**
 * Tests that the DArTDataset class returns correct values for mappings of
 * accession and marker names, for a simple DArT dataset
 */
public final class TestDArTDataset {
	private Collection<String> markerNames;
	private Collection<String> accessionNames;
	private DArTDataset dartData;

	@Before
	public void setUpBefore() throws Exception { 
		markerNames = new HashSet<String>();
		markerNames.add("M1");
		markerNames.add("M2");
		
		accessionNames = new HashSet<String>();
		accessionNames.add("A1");
		accessionNames.add("A2");
		
		dartData = new DArTDataset(accessionNames, markerNames);
		dartData.setValue("A1", "M2", DArTValue.PRESENT);
		dartData.setValue("A1", "M1", DArTValue.ABSENT);
		dartData.setValue("A2", "M1", DArTValue.ABSENT);
	}
	
	@Test 
	public void verifyInitialPresentDArTValues() throws Exception { 
		assertEquals(dartData.getValue("A1", "M2"), DArTValue.PRESENT);
	}
	
	@Test 
	public void verifyUpdatedPresentDArTValues() throws Exception { 
		dartData.setValue("A1", "M1", DArTValue.PRESENT);
		assertEquals(dartData.getValue("A1", "M1"), DArTValue.PRESENT);
	}
	
	@Test 
	public void verifyInitialAbsentDArTValues() throws Exception { 
		assertEquals(dartData.getValue("A1", "M1"), DArTValue.ABSENT);
	}
	
	@Test 
	public void verifyUpdatedAbsentDArTValues() throws Exception { 
		dartData.setValue("A1", "M2", DArTValue.ABSENT);
		assertEquals(dartData.getValue("A1", "M2"), DArTValue.ABSENT);
	}
	
	@Test 
	public void verifyInitialNullDArTValues() throws Exception { 
		assertNull(dartData.getValue("A2", "M2"));
	}
	
	@Test 
	public void verifyUpdatedNullDArTValues() throws Exception { 
		dartData.setValue("A1", "M1", null);
		assertNull(dartData.getValue("A1", "M1"));
	}
	
	@Test 
	public void verifyInvalidAccessionMapsToNull() throws Exception { 
		assertNull(dartData.getValue("A100", "M1"));
	}
	
	@Test 
	public void verifyInvalidMarkerMapsToNull() throws Exception { 
		assertNull(dartData.getValue("A1", "M100"));
	}
	
	@Test(expected=UnknownAccessionException.class) 
	public void verifyUnknownAccessionCantBeSet() throws Exception {
		dartData.setValue("A100", "M1", DArTValue.PRESENT);
	}
	
	@Test(expected=UnknownTraitException.class) 
	public void verifyUnknownTraitCantBeSet() throws Exception {
		dartData.setValue("A1", "M100", DArTValue.PRESENT);
	}
	
} 

