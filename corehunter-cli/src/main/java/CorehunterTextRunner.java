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

package org.cimmyt.corehunter.textui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.cimmyt.corehunter.*;
import org.cimmyt.corehunter.measures.*;
import org.cimmyt.corehunter.search.CoreSubsetSearch;

/**
 * A simple text based driver for Corehunter.
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 */
public final class CorehunterTextRunner {
    private final String[] measureNames = {"MR","CE","SH", "HE", "NE", "PN", "CV"};
    private final int DEFAULT_REPLICAS = 10;
    private final int DEFAULT_MC_STEPS = 50;
    private final double DEFAULT_RUNTIME = 60.0;
    private final double DEFAULT_MIN_TEMPERATURE = 50.0;
    private final double DEFAULT_MAX_TEMPERATURE = 200.0;
    private final double DEFAULT_SAMPLING_INTENSITY = 0.2;

    private Options miscOpts;
    private Options measuresOpts;
    private Options searchOpts;
    private Options opts;
    private double sampleIntensity;
    private double runtime;
    private double minT;
    private double maxT;
    private int sampleMin;
    private int sampleMax;
    private int replicas;
    private int mcSteps;
    private boolean sampleSizesSpecified;
    private String collectionFile;
    private String coresubsetFile;
    private Map<String,Double> measureWeights;

    /**
     * 
     */
    public CorehunterTextRunner() {
	miscOpts = new Options();
	measuresOpts = new Options();
	searchOpts = new Options();
	opts = new Options();

	measureWeights = new HashMap<String,Double>();
	collectionFile = coresubsetFile = null;

	// set up default search parameters
	sampleIntensity = DEFAULT_SAMPLING_INTENSITY;
	runtime = DEFAULT_RUNTIME;
	replicas = DEFAULT_REPLICAS;
	mcSteps = DEFAULT_MC_STEPS;
	minT = DEFAULT_MIN_TEMPERATURE;
	maxT = DEFAULT_MAX_TEMPERATURE;
	sampleSizesSpecified = false;
    }

    public void run(String[] args) {
	setupOptions();
	if (!parseOptions(args)) {
	    showUsage();
	}

	// try to create dataset
	SSRDataset ds = SSRDataset.createFromFile(collectionFile);
	if(ds == null) {
	    System.err.println("\nProblem parsing dataset file.  Aborting.");
	    System.exit(0);
	}
	
	// create an accession collection
	AccessionCollection ac = new AccessionCollection();
	ac.addDataset(ds);

	int collectionSize = ac.size();
	
	if (!sampleSizesSpecified) {
	    sampleMin = sampleMax = (int)(sampleIntensity * collectionSize);
	}

	if (sampleMax > collectionSize) {
            sampleMax = collectionSize;
            System.err.println("\nSpecified core size is larger than collection size.  ");
            System.err.println("Assuming max size is collection size.");
	}
	
	// create a pseudo-index and add user specified measure to it, with respective weights
	PseudoMeasure pm = new PseudoMeasure();
	for(int i=0; i<measureNames.length; i++) {
	    String measure = measureNames[i];
	    if (measureWeights.containsKey(measure)) {
		Double weight = measureWeights.get(measure);
		try {
		    pm.addMeasure(MeasureFactory.createMeasure(measure), weight.doubleValue());
		} catch(DuplicateMeasureException dme) {
		    System.err.println("");
		    System.err.println(dme.getMessage());
		    System.exit(0);
		} catch(UnknownMeasureException ume) {
		    System.err.println("");
		    System.err.println(ume.getMessage());
		    System.exit(0);
		}
	    }
	}

	//System.out.println("Collection score: " + pm.calculate(ac.getAccessions()));

	// search for the core subset
	AccessionCollection core = null;
	core = CoreSubsetSearch.remcSearch(ac, pm, sampleMin, sampleMax, 
					   runtime, replicas,
					   minT, maxT, mcSteps);

	Map<String, Double> scores = pm.componentScores(core.getAccessions());
	
	System.out.println("--------");
	for(String comp : scores.keySet()) {
	    System.out.println(comp + ": " + scores.get(comp));
	}

	List<String> accessions = new ArrayList<String>(core.getAccessionNames());
	ds.writeToFile(coresubsetFile, accessions);
    }

    private void setupOptions() {
	// set up the misc option group
	miscOpts.addOption( new Option("help", "print this message") );
	miscOpts.addOption( new Option("version", "print the version information and exit") );
	miscOpts.addOption( new Option("quiet", "be extra quiet") );
	miscOpts.addOption( new Option("verbose", "be extra verbose") );

	// set up the measures option group
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Modified Rogers distance and specify weight")
			    .create("MR") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Cavalli-Sforza and Edwards distance and specify weight")
			    .create("CE") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Shannons Diversity Index and specify weight")
			    .create("SH") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Heterozygous Loci Diversity Index and specify weight")
			    .create("HE") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Number of Effective Alleles Index and specify weight")
			    .create("NE") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Proportion of Non-informativs alleles and specify weight")
			    .create("PN") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Coverage measure and specify weight")
			    .create("CV") );

	// set up the advanced search option group
	searchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("run search for t seconds, defaults to " + DEFAULT_RUNTIME)
			      .create("runtime") );
	searchOpts.addOption( OptionBuilder.withArgName("r")
			      .hasArg()
			      .withDescription("use r replicas for search, defaults to " + DEFAULT_REPLICAS)
			      .create("replicas") );
	searchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("use t local steps for each monte carlo search, defaults to " + DEFAULT_MC_STEPS)
			      .create("mc_steps") );
	searchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("select a fraction of size s of accessions in collection for coresubset, " +
					       "defaults to " + DEFAULT_SAMPLING_INTENSITY +
					       "\nNote: this overrides sample_size")
			      .create("sample_intensity") );
	searchOpts.addOption( OptionBuilder.withArgName("min max")
			      .hasArgs(2)
			      .withDescription("specify minimum and maximum size of core (number of accessions)")
			      .create("sample_size") );
	searchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("minimum temperature of any replica, " +
					       "defaults to " + DEFAULT_MIN_TEMPERATURE)
			      .create("min_t") );
	searchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("maximum temperature of any replica, " +
					       "defaults to " + DEFAULT_MAX_TEMPERATURE)
			      .create("max_t") );


	// add the option groups into one option collection
	Iterator i = miscOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}
	    
	i = measuresOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

	i = searchOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}
    }

    private boolean parseOptions(String[] args) {
	CommandLineParser parser = new GnuParser();
	    
	try {
	    CommandLine cl = parser.parse(opts, args);

	    // check for -help
	    if (cl.hasOption("help")) {
		showUsage();
		System.exit(0);
	    }

	    // check for -version
	    if (cl.hasOption("version")) {
		showVersion();
		System.exit(0);
	    } 

	    // make sure two required args are present
	    if (cl.getArgs().length != 2) {
		System.err.println("\n2 required arguments expected");
		return false;
	    } 

	    // grab the filenames
	    collectionFile = cl.getArgs()[0];
	    coresubsetFile = cl.getArgs()[1];

	    // parse the weights for different measures
	    boolean hasMeasures = false;
	    for(int i=0; i<measureNames.length; i++) {
		String m = measureNames[i];
		if(cl.hasOption(m)) {
		    try {
			double weight = Double.parseDouble(cl.getOptionValue(m));
			if (weight<=0.0) throw new NumberFormatException();
			measureWeights.put(m, weight);
		    } catch(NumberFormatException nfe) {
			System.err.println("\nweight for " + measureNames[i] +
					   " must be a positive numeric value");
			return false;
		    }
		    hasMeasures = true;
		}
	    }

	    // ensure at least one measure was set
	    if (!hasMeasures) {
		System.err.println("\nAt least one measure must be specified");
		return false;
	    }

	    // check if specific core size ranges were set
	    if (cl.hasOption("sample_size")) {
		try {
		    sampleMin = Integer.parseInt(cl.getOptionValues("sample_size")[0]);
		    sampleMax = Integer.parseInt(cl.getOptionValues("sample_size")[1]);
		    if (sampleMin > sampleMax || sampleMin < 2) throw new NumberFormatException();
		    sampleSizesSpecified = true;
		} catch(NumberFormatException nfe) {
		    System.err.println("\nsample_size must specify two integer values with max >= min and min >= 2");
		    return false;
		}
	    }

	    // make sure sampling intensity is between 0 and 1 inclusive
	    if (cl.hasOption("sample_intensity")) {
		try {
		    sampleIntensity = Double.parseDouble(cl.getOptionValue("sample_intensity"));
		    if (sampleIntensity<0.0 || sampleIntensity>1.0) {
			throw new NumberFormatException();
		    }
		} catch(NumberFormatException nfe) {
		    System.err.println("\nsample_intensity must a numeric value in the range [0..1]");
		    return false;
		}
	    }
	    
	    // check for runtime
	    if (cl.hasOption("runtime")) {
		try {
		    runtime = Double.parseDouble(cl.getOptionValue("runtime"));
		    if (runtime <= 0.0) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nruntime must be a positve numeric value");
		    return false;
		}
	    }
		
	    // check for replicas
	    if (cl.hasOption("replicas")) {
		try {
		    replicas = Integer.parseInt(cl.getOptionValue("replicas"));
		    if (replicas < 1 || replicas > 100) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nreplicas must be a positive integer in the range [1..100]");
		    return false;
		}
	    }

	    // check for mc_steps
	    if (cl.hasOption("mc_steps")) {
		try {
		    mcSteps = Integer.parseInt(cl.getOptionValue("mc_steps"));
		    if (mcSteps < 1 || mcSteps > 1000000) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nmc_steps must be a positive integer in the range [1..1000000]");
		    return false;
		}
	    }

	    // check for min_t
	    if (cl.hasOption("min_t")) {
		try {
		    minT = Double.parseDouble(cl.getOptionValue("min_t"));
		    if (minT <= 0.0) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nmin_t must be a positve numeric value");
		    return false;
		}
	    }

	    // check for max_t
	    if (cl.hasOption("max_t")) {
		try {
		    maxT = Double.parseDouble(cl.getOptionValue("max_t"));
		    if (maxT <= minT) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nmax_t must be a postive numeric value, larger than min_t");
		    return false;
		}
	    }
	     	    
	} catch (ParseException e) {
	    System.err.println("");
	    System.err.println( e.getMessage() );
	    return false;
	}
	    
	return true;
    }

    private void showUsage() {
	System.out.println("");
	System.out.println("usage: corehunter [options] [measures] <collection_file> <coresubset_file>");
	System.out.println("");
	System.out.println("\texample: The following command will store a coresubset in the file 'coresubset.dat'" +
			   "\n\tby selecting 20% (default value) of the accessions from the dataset in the file " +
			   "\n\t'collection.dat'.  The accesions will be chosen by attemping to optimize a " +
			   "\n\tpseudo-objective function where 70% of the weight is based on Modified Rogers " +
			   "\n\tdistance and 30% of the weight is based on Shannons diversity index.");
	System.out.println("");
	System.out.println("\tcorehunter -MR 0.7 -SH 0.3 collection.dat coresubset.dat");
	System.out.println("");
	    
	HelpFormatter f = new HelpFormatter();
	f.setSyntaxPrefix("");

	f.printHelp("measures (at least one must be specified):", measuresOpts);
	System.out.println("");
	f.printHelp("advanced search options:", searchOpts);
	System.out.println("");
	f.printHelp("misc options:", miscOpts);
	System.out.println("");
	System.exit(0);
    }	

    private void showVersion() {
	System.out.println("");
	System.out.println("Corehunter version: 1.0beta");
	System.out.println("");
    }

    public static void main(String[] args) {
	CorehunterTextRunner corehunter = new CorehunterTextRunner();
	corehunter.run(args);
	
	System.exit(0);
    }
}

