package org.cimmyt.corehunter.test;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Before; 
import org.junit.Ignore;
import org.junit.Test; 
import static org.junit.Assert.*;

import org.cimmyt.corehunter.*;
import org.cimmyt.corehunter.measures.*;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk &lt;&gt;
 * @version $Rev$
 */
public final class TestAccessionCollection {
   	private static final double precision = 0.00001;
	private AccessionCollection ac;
	private SSRDataset ssrData;
	Collection<String> ssrAccessionNames;
	Map<String, List<String>> ssrMarkersToAlleles;
	private DArTDataset dartData;
	Collection<String> dartAccessionNames;
	Collection<String> dartMarkerNames;

	@Before
	public void setUpBefore() throws Exception {
		ac = new AccessionCollection();
		
		ssrAccessionNames = new HashSet<String>();
		ssrAccessionNames.add("A1");
		ssrAccessionNames.add("A2");
		ssrAccessionNames.add("A3");
		ssrAccessionNames.add("A4");
				
		ssrMarkersToAlleles = new HashMap<String, List<String>>();
		ssrMarkersToAlleles.put("M1", new ArrayList<String>());
		ssrMarkersToAlleles.get("M1").add("allele1");
		ssrMarkersToAlleles.get("M1").add("allele2");
		ssrMarkersToAlleles.get("M1").add("allele3");
		ssrMarkersToAlleles.put("M2", new ArrayList<String>());
		ssrMarkersToAlleles.get("M2").add("allele1");
		ssrMarkersToAlleles.get("M2").add("allele2");
		
		ssrData = new SSRDataset(ssrAccessionNames, ssrMarkersToAlleles);
		
		// A1
		ssrData.setValue("A1", "M1", "allele1", 0.3);
		ssrData.setValue("A1", "M1", "allele2", 0.2);
		ssrData.setValue("A1", "M1", "allele3", 0.5);
		
		ssrData.setValue("A1", "M2", "allele1", 0.8);
		ssrData.setValue("A1", "M2", "allele2", 0.2);
		
		// A2
		ssrData.setValue("A2", "M1", "allele1", 0.1);
		ssrData.setValue("A2", "M1", "allele2", 0.0);
		ssrData.setValue("A2", "M1", "allele3", 0.9);
		                   
		ssrData.setValue("A2", "M2", "allele1", 0.4);
		ssrData.setValue("A2", "M2", "allele2", 0.6);
		
		// A3
		ssrData.setValue("A3", "M1", "allele1", 0.3);
		ssrData.setValue("A3", "M1", "allele2", 0.3);
		ssrData.setValue("A3", "M1", "allele3", 0.4);
		
		ssrData.setValue("A3", "M2", "allele1", null);
		ssrData.setValue("A3", "M2", "allele2", null);
		
		// A4
		ssrData.setValue("A4", "M1", "allele1", 0.8);
		ssrData.setValue("A4", "M1", "allele2", 0.0);
		ssrData.setValue("A4", "M1", "allele3", 0.2);
		
		ssrData.setValue("A4", "M2", "allele1", 0.5);
		ssrData.setValue("A4", "M2", "allele2", 0.5);
		
		dartAccessionNames = new HashSet<String>();
		dartAccessionNames.add("A1");
		dartAccessionNames.add("A5");
		dartAccessionNames.add("A9");
		
		dartMarkerNames = new HashSet<String>();
		dartMarkerNames.add("M1");
		dartMarkerNames.add("M2");
		
		dartData = new DArTDataset(dartAccessionNames, dartMarkerNames);
		dartData.setValue("A1", "M2", DArTValue.PRESENT);
		dartData.setValue("A1", "M1", DArTValue.ABSENT);
		dartData.setValue("A5", "M1", DArTValue.ABSENT);
		dartData.setValue("A5", "M2", DArTValue.ABSENT);
		dartData.setValue("A9", "M1", DArTValue.PRESENT);
		dartData.setValue("A9", "M2", DArTValue.ABSENT);
	}
	
	@Test
	public void verifyAddDataset() throws Exception {
		ac.addDataset(ssrData);
		assertTrue(ssrAccessionNames.equals(ac.getAccessionNames()));
	}
	
	@Test
	public void verifyMultipleAddDataset() throws Exception {
		ac.addDataset(ssrData);
		ac.addDataset(dartData);
		
		Set<String> ssrAndDartNames = new HashSet<String>(ssrAccessionNames);
		ssrAndDartNames.addAll(dartAccessionNames);
		assertTrue(ssrAndDartNames.equals(ac.getAccessionNames()));
	}
	
	@Ignore @Test
	public void verifyCopyConstructor() throws Exception {

	}
	
	@Ignore @Test(expected=UnknownMeasureException.class)
	public void verifyGetMeasureValueForMissingMeasure() throws Exception {
		ac.addDataset(ssrData);
		//ac.getMeasureValue("MR");
	}
	
	@Ignore @Test
	public void verifyGetMeasureValue() throws Exception {
		ac.addDataset(ssrData);
		//ac.addMeasure(new ModifiedRogersDistance("MR", "Modified Rogers Distance"));
		//assertEquals(0.322580592628, ac.getMeasureValue("MR"), precision);
	}
}
