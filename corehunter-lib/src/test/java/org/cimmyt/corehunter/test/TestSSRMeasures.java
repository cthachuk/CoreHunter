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
import org.cimmyt.corehunter.measures.*;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 * @version $Rev$
 */
public final class TestSSRMeasures {
	private static final double precision = 0.00001;
	private SSRDataset ssrData;
	private List<Accession> accessions;
	private DistanceMeasure mr;

	@Before
	public void setUpBefore() throws Exception {
		mr = new ModifiedRogersDistance(4);
		
		Collection<String> accessionNames = new HashSet<String>();
		accessionNames.add("A1");
		accessionNames.add("A2");
		accessionNames.add("A3");
		accessionNames.add("A4");
		
		Map<String, List<String>> markersToAlleles = new HashMap<String, List<String>>();
		markersToAlleles.put("M1", new ArrayList<String>());
		markersToAlleles.get("M1").add("allele1");
		markersToAlleles.get("M1").add("allele2");
		markersToAlleles.get("M1").add("allele3");
		markersToAlleles.put("M2", new ArrayList<String>());
		markersToAlleles.get("M2").add("allele1");
		markersToAlleles.get("M2").add("allele2");
		
		ssrData = new SSRDataset(accessionNames, markersToAlleles);
		
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
		
		// bind accessions to data
		accessions = new ArrayList<Accession>(4);
		accessions.add( new Accession("A1") );
		accessions.add( new Accession("A2") );
		accessions.add( new Accession("A3") );
		accessions.add( new Accession("A4") );
		
		for (Accession accession : accessions) {
			accession.bindSSRValues(ssrData);
		}
	}
	
	@Test
	public void verifyMRBetweenAccessions() throws Exception {
		assertEquals(0.374165738677394, mr.calculate(accessions.get(0), accessions.get(1)), precision);
		assertEquals(0.070710678118655, mr.calculate(accessions.get(0), accessions.get(2)), precision);
		assertEquals(0.374165738677394, mr.calculate(accessions.get(0), accessions.get(3)), precision);
		assertEquals(0.308220700148449, mr.calculate(accessions.get(1), accessions.get(2)), precision);
		assertEquals(0.500000000000000, mr.calculate(accessions.get(1), accessions.get(3)), precision);
		assertEquals(0.308220700148449, mr.calculate(accessions.get(2), accessions.get(3)), precision);
	}
	
	@Test
	public void verifyAverageMRAllAccessions() throws Exception {
		assertEquals(0.322580592628, mr.calculate(accessions), precision);
	}
	
}
