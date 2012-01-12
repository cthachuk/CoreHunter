//  Copyright 2008,2011 Chris Thachuk, Herman De Beukelaer
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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import org.cimmyt.corehunter.*;
import org.cimmyt.corehunter.measures.*;
import org.cimmyt.corehunter.search.CoreSubsetSearch;
import org.cimmyt.corehunter.search.HeuristicSingleNeighborhood;
import org.cimmyt.corehunter.search.Neighborhood;
import org.cimmyt.corehunter.search.RandomSingleNeighborhood;

/**
 * A simple text based driver for Corehunter.
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 */
public final class CorehunterTextRunner {

    private final String[] measureNames = {"MR", "MRmin", "CE", "CEmin","SH", "HE", "NE", "PN", "CV", "EX"};
    
    private final int DEFAULT_REPLICAS = 10;
    private final int DEFAULT_MC_STEPS = 50;

    private final double DEFAULT_RUNTIME = 60.0;
    private final double DEFAULT_MINPROG = 0.0;
    private final double DEFAULT_MIN_TEMPERATURE = 50.0;
    private final double DEFAULT_MAX_TEMPERATURE = 200.0;
    private final double DEFAULT_SAMPLING_INTENSITY = 0.2;

    private final int DEFAULT_GEN_POP_SIZE = 10;
    private final int DEFAULT_GEN_NR_OF_CHILDREN = 2;
    private final int DEFAULT_GEN_TOURNAMENT_SIZE = 4;
    private final double DEFAULT_GEN_MUTATION_RATE = 0.4;

    private final int DEFAULT_MERGEREP_NR_OF_REPLICAS = 6;
    private final int DEFAULT_MERGEREP_NR_OF_CHILDREN = 1;
    private final int DEFAULT_MERGEREP_TOURNAMENT_SIZE = 2;
    private final int DEFAULT_MERGEREP_NR_OF_STEPS = 50;

    private final int DEFAULT_MIXREP_NR_OF_TABU_REPLICAS = 2;
    private final int DEFAULT_MIXREP_NR_OF_NON_TABU_REPLICAS = 3;
    private final int DEFAULT_MIXREP_ROUNDS_WITHOUT_TABU = 10;
    private final int DEFAULT_MIXREP_TOURNAMENT_SIZE = 2;
    private final int DEFAULT_MIXREP_NR_OF_TABU_STEPS = 5;
    private final int DEFAULT_MIXREP_BOOST_NR = 2;
    private final double DEFAULT_MIXREP_BOOST_MIN_PROG = 10e-9;
    private final int DEFAULT_MIXREP_BOOST_TIME_FACTOR = 15;
    private final double DEFAULT_MIXREP_MIN_BOOST_TIME = 0.25;
    private final double DEFAULT_MIXREP_MIN_SIMAN_TEMP = 50.0;
    private final double DEFAULT_MIXREP_MAX_SIMAN_TEMP = 100.0;

    private final int DEFAULT_LR_L = 2;
    private final int DEFAULT_LR_R = 1;

    private Options miscOpts;
    private Options measuresOpts;
    private Options searchTypeOpts;
    private Options commonSearchOpts;
    private Options remcSearchOpts;
    private Options tabuSearchOpts;
    private Options genSearchOpts;
    private Options mergerepSearchOpts;
    private Options mixrepSearchOpts;
    private Options lrSearchOpts;
    private Options opts;

    private double sampleIntensity;
    private double runtime;
    private double minProg;
    private double stuckTime;
    private boolean stuckTimeSpecified = false;

    private double minT;
    private double maxT;
    private int sampleMin;
    private int sampleMax;
    private int replicas;
    private int mcSteps;
    private boolean sampleSizesSpecified = false;

    private int tabuListSize;
    private boolean tabuListSizeSpecified = false;

    private int genPopSize;
    private int genNrOfChildren;
    private int genTournamentSize;
    private double genMutationRate;

    private int mergerepNrOfReplicas;
    private int mergerepNrOfSteps;
    private int mergerepNrOfChildren;
    private int mergerepTournamentSize;

    private int mixrepNrOfTabuReplicas;
    private int mixrepNrOfNonTabuReplicas;
    private int mixrepRoundsWithoutTabu;
    private int mixrepNrOfTabuSteps;
    private int mixrepTournamentSize;
    private int mixrepBoostNr;
    private double mixrepBoostMinProg;
    private int mixrepBoostTimeFactor;
    private double mixrepMinBoostTime;
    private double mixrepMinSimAnTemp;
    private double mixrepMaxSimAnTemp;

    private int lr_l;
    private int lr_r;

    private String collectionFile;
    private String coresubsetFile;
    private Map<String,Double> measureWeights;

    private boolean remcSearch = false;
    private boolean parRemcSearch = false;
    private boolean exhSearch = false;
    private boolean randSearch = false;
    private boolean tabuSearch = false;
    private boolean localSearch = false;
    private boolean steepestDescentSearch = false;
    private boolean mstratSearch = false;
    private boolean geneticSearch = false;
    private boolean mergeReplicaSearch = false;
    private boolean parMergeReplicaSearch = false;
    private boolean mixedReplicaSearch = false;
    private boolean lrSearch = false;
    private boolean semiLrSearch = false;
    private boolean forwardSelection = false;
    private boolean semiForwardSelection = false;
    private boolean backwardSelection = false;

    /**
     * 
     */
    public CorehunterTextRunner() {
	miscOpts = new Options();
	measuresOpts = new Options();
        searchTypeOpts = new Options();
	commonSearchOpts = new Options();
        remcSearchOpts = new Options();
        tabuSearchOpts = new Options();
        genSearchOpts = new Options();
        mergerepSearchOpts = new Options();
        mixrepSearchOpts = new Options();
        lrSearchOpts = new Options();
	opts = new Options();

	measureWeights = new HashMap<String,Double>();
	collectionFile = coresubsetFile = null;

	// set up default search parameters
	sampleIntensity = DEFAULT_SAMPLING_INTENSITY;
	runtime = DEFAULT_RUNTIME;
        minProg = DEFAULT_MINPROG;
	replicas = DEFAULT_REPLICAS;
	mcSteps = DEFAULT_MC_STEPS;
	minT = DEFAULT_MIN_TEMPERATURE;
	maxT = DEFAULT_MAX_TEMPERATURE;

        genPopSize = DEFAULT_GEN_POP_SIZE;
        genNrOfChildren = DEFAULT_GEN_NR_OF_CHILDREN;
        genMutationRate = DEFAULT_GEN_MUTATION_RATE;
        genTournamentSize = DEFAULT_GEN_TOURNAMENT_SIZE;

        mergerepNrOfReplicas = DEFAULT_MERGEREP_NR_OF_REPLICAS;
        mergerepNrOfChildren = DEFAULT_MERGEREP_NR_OF_CHILDREN;
        mergerepTournamentSize = DEFAULT_MERGEREP_TOURNAMENT_SIZE;
        mergerepNrOfSteps = DEFAULT_MERGEREP_NR_OF_STEPS;

        mixrepNrOfTabuReplicas = DEFAULT_MIXREP_NR_OF_TABU_REPLICAS;
        mixrepNrOfNonTabuReplicas = DEFAULT_MIXREP_NR_OF_NON_TABU_REPLICAS;
        mixrepRoundsWithoutTabu = DEFAULT_MIXREP_ROUNDS_WITHOUT_TABU;
        mixrepTournamentSize = DEFAULT_MIXREP_TOURNAMENT_SIZE;
        mixrepNrOfTabuSteps = DEFAULT_MIXREP_NR_OF_TABU_STEPS;
        mixrepBoostNr = DEFAULT_MIXREP_BOOST_NR;
        mixrepBoostMinProg = DEFAULT_MIXREP_BOOST_MIN_PROG;
        mixrepBoostTimeFactor = DEFAULT_MIXREP_BOOST_TIME_FACTOR;
        mixrepMinBoostTime = DEFAULT_MIXREP_MIN_BOOST_TIME;
        mixrepMinSimAnTemp = DEFAULT_MIXREP_MIN_SIMAN_TEMP;
        mixrepMaxSimAnTemp = DEFAULT_MIXREP_MAX_SIMAN_TEMP;

        lr_l = DEFAULT_LR_L;
        lr_r = DEFAULT_LR_R;
    }

    public void run(String[] args) {
	setupOptions();
	if (!parseOptions(args)) {
	    showUsage();
	}

	// try to create dataset
        System.out.println("Reading dataset...");
	SSRDataset ds = SSRDataset.createFromFile(collectionFile);
	if(ds == null) {
	    System.err.println("\nProblem parsing dataset file.  Aborting.");
	    System.exit(0);
	}
	
	// create an accession collection
	AccessionCollection ac = new AccessionCollection();
	ac.addDataset(ds);

        int collectionSize = ac.size();
        
        /*** TMP: compute distance distribution of loaded dataset
        System.out.println("Computing distance distribution...");
        Map<Double, Integer> distanceFreq = new HashMap<Double, Integer>();
        DistanceMeasure mr = new ModifiedRogersDistance(collectionSize);
        List<Accession> acs = ac.getAccessions();
        for(int i=0; i<collectionSize; i++){
            System.out.print(".");
            for(int j=i+1; j<collectionSize; j++){
                double dist = mr.calculate(acs.get(i), acs.get(j));
                Integer freq = distanceFreq.get(dist);
                if(freq == null){
                    distanceFreq.put(dist, 1);
                } else {
                    distanceFreq.put(dist, freq+1);
                }
            }
        }
        System.out.println("");
        // write distance distribution to file with name 'distancefreq'
        File distfreqoutput = new File("distancefreq");
        try {
            FileWriter wr = new FileWriter(distfreqoutput);
            for(Double dist : distanceFreq.keySet()){
                wr.write(dist + "\t" + distanceFreq.get(dist) + "\n");
            }
            wr.flush();
            wr.close();
        } catch (IOException ex) {
            System.err.println("Error writing distance distribution file: " + ex);
            System.exit(1);
        }
        System.exit(0);
         END TMP ***/


        if (!stuckTimeSpecified){
            stuckTime = runtime;
        }

	if (!sampleSizesSpecified) {
	    sampleMin = sampleMax = (int)(sampleIntensity * collectionSize);
	}

	if (sampleMax > collectionSize) {
            sampleMax = collectionSize;
            System.err.println("\nSpecified core size is larger than collection size.  ");
            System.err.println("Assuming max size is collection size.");
	}

        if (tabuListSizeSpecified && tabuListSize >= sampleMax){
            tabuListSize = sampleMax-1;
            System.err.println("\nSpecified tabu list size is larger than or equal to max core size.");
            System.err.println("List size was changed to 'max core size - 1' = " + (sampleMax-1) + ", to ensure at least one non-tabu neighbor.");
        }

        if (!tabuListSizeSpecified){
            // Default tabu list size = 30% of minimum sample size
            tabuListSize = Math.max((int) (0.3 * sampleMin), 1);
        }

	// create a pseudo-index and add user specified measure to it, with respective weights
	PseudoMeasure pm = new PseudoMeasure();
	for(int i=0; i<measureNames.length; i++) {
	    String measure = measureNames[i];
	    if (measureWeights.containsKey(measure)) {
		Double weight = measureWeights.get(measure);
		try {
		    pm.addMeasure(MeasureFactory.createMeasure(measure, collectionSize), weight.doubleValue());
		} catch(DuplicateMeasureException dme) {
		    System.err.println("");
		    System.err.println(dme.getMessage());
                    showUsage();
		    System.exit(0);
		} catch(UnknownMeasureException ume) {
		    System.err.println("");
		    System.err.println(ume.getMessage());
                    showUsage();
		    System.exit(0);
		}
	    }
	}

	//System.out.println("Collection score: " + pm.calculate(ac.getAccessions()));

	// search for the core subset
	AccessionCollection core = null;
        if(randSearch){
            System.out.println("---\nRandom subset\n---");
            core = CoreSubsetSearch.randomSearch(ac, sampleMin, sampleMax);
        } else if(exhSearch) {
            System.out.println("---\nExhaustive search\n---");
            core = CoreSubsetSearch.exhaustiveSearch(ac, pm, sampleMin, sampleMax);
        } else if(geneticSearch) {
            System.out.println("---\nGenetic algorithm search\n---");
            core = CoreSubsetSearch.geneticSearch(ac, pm, sampleMin, sampleMax, runtime,
                                                  minProg, stuckTime, genPopSize, genNrOfChildren,
                                                  genTournamentSize, genMutationRate);
        } else if(lrSearch) {
            // check (l,r) setting
            if(Math.abs(lr_l-lr_r) > 1){
                System.err.println("\n!!! Warning: current (l,r) setting may result" +
                                    "in core size slightly different from desired size");
            }
            System.out.println("---\nLR Search (deterministic)\n---");
            core = CoreSubsetSearch.lrSearch(ac, pm, sampleMin, sampleMax, lr_l, lr_r);
        } else if(semiLrSearch) {
            // check (l,r) setting
            if(Math.abs(lr_l-lr_r) > 1){
                System.err.println("\n!!! Warning: current (l,r) setting may result" +
                                    "in core size slightly different from desired size");
            }
            System.out.println("---\nSemi LR Search (semi-deterministic)\n---");
            core = CoreSubsetSearch.semiLrSearch(ac, pm, sampleMin, sampleMax, lr_l, lr_r);
        } else if(forwardSelection) {
            System.out.println("---\nSequential Forward Selection (deterministic)\n---");
            core = CoreSubsetSearch.forwardSelection(ac, pm, sampleMin, sampleMax);
        } else if(semiForwardSelection) {
            System.out.println("---\nSemi Sequential Forward Selection (semi-deterministic)\n---");
            core = CoreSubsetSearch.semiForwardSelection(ac, pm, sampleMin, sampleMax);
        } else if(backwardSelection) {
            System.out.println("---\nSequential Backward Selection (deterministic)\n---");
            core = CoreSubsetSearch.backwardSelection(ac, pm, sampleMin, sampleMax);
        } else {
            Neighborhood nh;
            if(remcSearch) {
                System.out.println("---\nREMC (Replica Exchange Monte Carlo)\n---");
                nh = new RandomSingleNeighborhood(sampleMin, sampleMax);
                core = CoreSubsetSearch.remcSearch(ac, nh, pm, sampleMin, sampleMax,
                                                   runtime, minProg, stuckTime, replicas, minT, maxT, mcSteps);
            } else if(parRemcSearch){
                System.out.println("---\nParallel REMC (Replica Exchange Monte Carlo)\n---");
                nh = new RandomSingleNeighborhood(sampleMin, sampleMax);
                core = CoreSubsetSearch.parRemcSearch(ac, nh, pm, sampleMin, sampleMax,
                                                   runtime, minProg, stuckTime, replicas, minT, maxT, mcSteps);
            } else if(mergeReplicaSearch){
                System.out.println("---\nMerge Replica Search\n---");
                nh = new RandomSingleNeighborhood(sampleMin, sampleMax);
                core = CoreSubsetSearch.mergeReplicaSearch(ac, nh, pm, sampleMin,
                                            sampleMax, runtime, minProg, stuckTime, mergerepNrOfReplicas,
                                            mergerepNrOfSteps, mergerepNrOfChildren, mergerepTournamentSize, false, false);
            } else if(parMergeReplicaSearch){
                System.out.println("---\nParallel Merge Replica Search\n---");
                nh = new RandomSingleNeighborhood(sampleMin, sampleMax);
                core = CoreSubsetSearch.parMergeReplicaSearch(ac, nh, pm, sampleMin,
                                            sampleMax, runtime, minProg, stuckTime, mergerepNrOfReplicas,
                                            mergerepNrOfSteps, mergerepNrOfChildren, mergerepTournamentSize, false, false);
            } else if(mixedReplicaSearch){
                System.out.println("---\nParallel Mixed Replica Search\n---");
                core = CoreSubsetSearch.mixedReplicaSearch(ac, pm, sampleMin, sampleMax, runtime,
                                            minProg, stuckTime, mixrepNrOfTabuReplicas, mixrepNrOfNonTabuReplicas,
                                            mixrepRoundsWithoutTabu, mixrepNrOfTabuSteps, mixrepTournamentSize, tabuListSize,
                                            false, false, mixrepBoostNr, mixrepBoostMinProg, mixrepBoostTimeFactor, mixrepMinBoostTime,
                                            mixrepMinSimAnTemp, mixrepMaxSimAnTemp);
            } else if (localSearch) {
                System.out.println("---\nLocal Search\n---");
                nh = new RandomSingleNeighborhood(sampleMin, sampleMax);
                core = CoreSubsetSearch.localSearch(ac, nh, pm, sampleMin, sampleMax, runtime, minProg, stuckTime);
            } else if (steepestDescentSearch) {
                System.out.println("---\nSteepest Descent Search\n---");
                nh = new RandomSingleNeighborhood(sampleMin, sampleMax);
                core = CoreSubsetSearch.steepestDescentSearch(ac, nh, pm, sampleMin, sampleMax, runtime, minProg);
            } else if (mstratSearch) {
                System.out.println("---\nMSTRAT Search (Heuristic Steepest Descent)\n---");
                nh = new HeuristicSingleNeighborhood(sampleMin, sampleMax);
                // MSTRAT = Steepest Descent with heuristic neighborhood
                core = CoreSubsetSearch.steepestDescentSearch(ac, nh, pm, sampleMin, sampleMax, runtime, minProg);
            } else if (tabuSearch) {
                System.out.println("---\nTabu Search\n---");
                // Tabu Search uses heuristic neighborhood as in MSTRAT
                nh = new HeuristicSingleNeighborhood(sampleMin, sampleMax);
                core = CoreSubsetSearch.tabuSearch(ac, nh, pm, sampleMin, sampleMax, runtime, minProg, stuckTime, tabuListSize);
            } else {
                System.err.println("Error: no known search type selected (this should not happen!)");
                System.exit(1);
            }
        }

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

        // set up the  search type option group
	searchTypeOpts.addOption( new Option("remc", "REMC search (Replica Exchange Monte Carlo)") );
	searchTypeOpts.addOption( new Option("premc", "parallel REMC search (Replica Exchange Monte Carlo)") );
	searchTypeOpts.addOption( new Option("exh", "exhaustive search") );
	searchTypeOpts.addOption( new Option("rand", "random core set") );
	searchTypeOpts.addOption( new Option("tabu", "tabu search") );
	searchTypeOpts.addOption( new Option("local", "standard local search") );
	searchTypeOpts.addOption( new Option("steepest", "steepest descent search") );
	searchTypeOpts.addOption( new Option("mstrat", "heuristic steepest descent (cfr. MSTRAT)") );
	searchTypeOpts.addOption( new Option("genetic", "genetic algorithm search") );
	searchTypeOpts.addOption( new Option("mergerep", "merge replica search") );
	searchTypeOpts.addOption( new Option("pmergerep", "parallel merge replica search") );
	searchTypeOpts.addOption( new Option("mixrep", "parallel mixed replica search") );
	searchTypeOpts.addOption( new Option("lr", "lr search (deterministic)") );
	searchTypeOpts.addOption( new Option("lrsemi", "lr search with random first pair (semi-deterministic)") );
	searchTypeOpts.addOption( new Option("sfs", "sequential forward selection (deterministic)") );
	searchTypeOpts.addOption( new Option("sfssemi", "sequential forward selection with random first pair (semi-deterministic)") );
	searchTypeOpts.addOption( new Option("sbs", "sequential backward selection (deterministic)") );

	// set up the measures option group
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use mean Modified Rogers distance and specify weight")
			    .create("MR") );
        measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use minimum Modified Rogers distance and specify weight")
			    .create("MRmin") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use mean Cavalli-Sforza and Edwards distance and specify weight")
			    .create("CE") );
        measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use minimum Cavalli-Sforza and Edwards distance and specify weight")
			    .create("CEmin") );
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
			    .withDescription("use Proportion of Non-informative alleles and specify weight")
			    .create("PN") );
	measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use Coverage measure and specify weight")
			    .create("CV") );
        measuresOpts.addOption( OptionBuilder.withArgName("weight")
			    .hasArg()
			    .withDescription("use External Distance measure and specify weight (needs external distance specification in dataset file!)")
			    .create("EX") );

	// set up the common advanced search option group
        commonSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("run search for t seconds, defaults to " + DEFAULT_RUNTIME)
			      .create("runtime") );
        commonSearchOpts.addOption( OptionBuilder.withArgName("p")
			      .hasArg()
			      .withDescription("stop search if progression is less than p, defaults to " + DEFAULT_MINPROG)
			      .create("min_prog") );
        commonSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("stop if no improvement found during last t seconds, by default no stuck_time is applied")
			      .create("stuck_time") );
        commonSearchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("select a fraction of size s of accessions in collection for coresubset, " +
					       "defaults to " + DEFAULT_SAMPLING_INTENSITY)
			      .create("sample_intensity") );
        commonSearchOpts.addOption( OptionBuilder.withArgName("min max")
			      .hasArgs(2)
			      .withDescription("specify minimum and maximum size of core (number of accessions)" +
                                               "\nNote: this overrides sample_intensity")
			      .create("sample_size") );

	// set up the REMC advanced search option group
	remcSearchOpts.addOption( OptionBuilder.withArgName("r")
			      .hasArg()
			      .withDescription("use r replicas for search, defaults to " + DEFAULT_REPLICAS)
			      .create("replicas") );
	remcSearchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("use s local steps for each monte carlo search, defaults to " + DEFAULT_MC_STEPS)
			      .create("mc_steps") );
	remcSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("minimum temperature of any replica, " +
					       "defaults to " + DEFAULT_MIN_TEMPERATURE)
			      .create("min_t") );
	remcSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("maximum temperature of any replica, " +
					       "defaults to " + DEFAULT_MAX_TEMPERATURE)
			      .create("max_t") );

        // set up the Tabu advanced search option group
        tabuSearchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("use tabu list of size s, defaults to 30% of the minimum core size")
			      .create("list_size") );

        // set up the Genetic Algorithm advanced search option group
        genSearchOpts.addOption( OptionBuilder.withArgName("p")
			      .hasArg()
			      .withDescription("use population of size p, defaults to " + DEFAULT_GEN_POP_SIZE)
			      .create("pop_size") );

        genSearchOpts.addOption( OptionBuilder.withArgName("c")
			      .hasArg()
			      .withDescription("create c children for each new generation of the population, defaults to " + DEFAULT_GEN_NR_OF_CHILDREN)
			      .create("children") );

        genSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("select parents in tournaments of size t, defaults to " + DEFAULT_GEN_TOURNAMENT_SIZE)
			      .create("tournament_size") );

        genSearchOpts.addOption( OptionBuilder.withArgName("m")
			      .hasArg()
			      .withDescription("mutate new children with probability m, defaults to " + DEFAULT_GEN_MUTATION_RATE)
			      .create("mutation_rate") );

        // set up the Genetic Replica advanced search option group
        mergerepSearchOpts.addOption( OptionBuilder.withArgName("r")
			      .hasArg()
			      .withDescription("use r replicas, defaults to " + DEFAULT_MERGEREP_NR_OF_REPLICAS)
			      .create("replicas") );

        mergerepSearchOpts.addOption( OptionBuilder.withArgName("c")
			      .hasArg()
			      .withDescription("create c new children replicas at each escape move, defaults to " +  DEFAULT_MERGEREP_NR_OF_CHILDREN)
			      .create("children") );

        mergerepSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("select parent replicas in tournaments of size t, defaults to " + DEFAULT_MERGEREP_TOURNAMENT_SIZE)
			      .create("tournament_size") );

        mergerepSearchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("perform s monte carlo steps in each replica between escape moves, defaults to " + DEFAULT_MERGEREP_NR_OF_STEPS)
			      .create("mc_steps") );

        // set up the Mixed Replica advanced search option group
        mixrepSearchOpts.addOption( OptionBuilder.withArgName("tr")
			      .hasArg()
			      .withDescription("maintain tr tabu replicas during search, defaults to " + DEFAULT_MIXREP_NR_OF_TABU_REPLICAS)
			      .create("tabu_replicas") );
        
        mixrepSearchOpts.addOption( OptionBuilder.withArgName("ntr")
			      .hasArg()
			      .withDescription("maintain ntr non-tabu replicas (Local Search, SimAn) during search, defaults to " + DEFAULT_MIXREP_NR_OF_NON_TABU_REPLICAS)
			      .create("non_tabu_replicas") );
        
        mixrepSearchOpts.addOption( OptionBuilder.withArgName("r")
			      .hasArg()
			      .withDescription("wait for startup of tabu replicas until after the first r search rounds, defaults to " + DEFAULT_MIXREP_ROUNDS_WITHOUT_TABU)
			      .create("rounds_without_tabu") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("select parent replicas in tournaments of size t, defaults to " + DEFAULT_MIXREP_TOURNAMENT_SIZE)
			      .create("tournament_size") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("each tabu replica performs s steps in each search round, defaults to "
                              + DEFAULT_MIXREP_NR_OF_TABU_STEPS + " (nr of steps for other replicas is automatically determined "
                              + "according to this number of tabu steps)")
			      .create("tabu_steps") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("s")
			      .hasArg()
			      .withDescription("use tabu list of size s, defaults to 30% of the minimum core size")
			      .create("list_size") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("b")
			      .hasArg()
			      .withDescription("boost search with b new local search replicas at each boost, defaults to " + DEFAULT_MIXREP_BOOST_NR)
			      .create("boost_nr") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("p")
			      .hasArg()
			      .withDescription("boost search as soon as global improvement drops below p, defaults to " + DEFAULT_MIXREP_BOOST_MIN_PROG)
			      .create("boost_min_prog") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("f")
			      .hasArg()
			      .withDescription("set how long to wait before boosting in case of no improvement, boost time is automatically "
                              + "determined based on the average time of one search round and this factor, defaults to " + DEFAULT_MIXREP_BOOST_TIME_FACTOR)
			      .create("boost_time_factor") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("m")
			      .hasArg()
			      .withDescription("minimum time before boosting in case of no improvement (overrides boost_time_factor), defaults to " + DEFAULT_MIXREP_MIN_BOOST_TIME)
			      .create("min_boost_time") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("minimum temperature of Simulated Annealing replicas, defaults to " + DEFAULT_MIXREP_MIN_SIMAN_TEMP)
			      .create("min_t") );

        mixrepSearchOpts.addOption( OptionBuilder.withArgName("t")
			      .hasArg()
			      .withDescription("maximum temperature of Simulated Annealing replicas, defaults to " + DEFAULT_MIXREP_MAX_SIMAN_TEMP)
			      .create("max_t") );

        // set up the LR Search advanced search option group
        lrSearchOpts.addOption( OptionBuilder.withArgName("l")
			      .hasArg()
			      .withDescription("add l accessions in each round, defaults to " + DEFAULT_LR_L)
			      .create("l") );

        lrSearchOpts.addOption( OptionBuilder.withArgName("r")
			      .hasArg()
			      .withDescription("remove r accessions in each round, defaults to " + DEFAULT_LR_R)
			      .create("r") );

	// add the option groups into one option collection
	Iterator i = miscOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}
	    
	i = measuresOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

        i = searchTypeOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

        i = commonSearchOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

	i = remcSearchOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

        i = tabuSearchOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

        i = genSearchOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

        i = mergerepSearchOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

        i = mixrepSearchOpts.getOptions().iterator();
	while (i.hasNext()) {
	    opts.addOption((Option)i.next());
	}

        i = lrSearchOpts.getOptions().iterator();
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
		    System.err.println("\nruntime must be a positive numeric value");
		    return false;
		}
	    }

            // check for min_prog
	    if (cl.hasOption("min_prog")) {
		try {
		    minProg = Double.parseDouble(cl.getOptionValue("min_prog"));
		    if (minProg < 0.0) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nmin_prog must be a positive numeric value");
		    return false;
		}
	    }

            // check for stuck_time
	    if (cl.hasOption("stuck_time")) {
		try {
		    stuckTime = Double.parseDouble(cl.getOptionValue("stuck_time"));
		    if (stuckTime <= 0.0) throw new NumberFormatException();
                    stuckTimeSpecified = true;
		} catch(NumberFormatException nfe) {
		    System.err.println("\nstuck_time must be a positve numeric value");
		    return false;
		}
	    }

            // check selected search type

            int j=0;

            // check for -remc
            remcSearch = cl.hasOption("remc");
            if(remcSearch) j++;

            // check for -premc
            parRemcSearch = cl.hasOption("premc");
            if(parRemcSearch) j++;

            // check for -exh
            exhSearch = cl.hasOption("exh");
            if(exhSearch) j++;

            // check for -rand
            randSearch = cl.hasOption("rand");
            if(randSearch) j++;

            // check for -tabu
            tabuSearch = cl.hasOption("tabu");
            if(tabuSearch) j++;

            // check for -local
            localSearch = cl.hasOption("local");
            if(localSearch) j++;

            // check for -steepest
            steepestDescentSearch = cl.hasOption("steepest");
            if(steepestDescentSearch) j++;

            // check for -mstrat
            mstratSearch = cl.hasOption("mstrat");
            if(mstratSearch) j++;

            // check for -genetic
            geneticSearch = cl.hasOption("genetic");
            if(geneticSearch) j++;

            // check for -mergerep
            mergeReplicaSearch = cl.hasOption("mergerep");
            if(mergeReplicaSearch) j++;

            // check for -pmergerep
            parMergeReplicaSearch = cl.hasOption("pmergerep");
            if(parMergeReplicaSearch) j++;

            // check for -mixrep
            mixedReplicaSearch = cl.hasOption("mixrep");
            if(mixedReplicaSearch) j++;

            // check for -lr
            lrSearch = cl.hasOption("lr");
            if(lrSearch) j++;

            // check for -semilr
            semiLrSearch = cl.hasOption("lrsemi");
            if(semiLrSearch) j++;

            // check for -sfs
            forwardSelection = cl.hasOption("sfs");
            if(forwardSelection) j++;

            // check for -sfssemi
            semiForwardSelection = cl.hasOption("sfssemi");
            if(semiForwardSelection) j++;

            // check for -sbs
            backwardSelection = cl.hasOption("sbs");
            if(backwardSelection) j++;

            // check if a search type is selected
            if(j==0){
                // select default search type = MixRep
                mixedReplicaSearch = true;
            } else if(j>1){
                // multiple search types selected
                System.err.println("\nMultiple search types selected. Please select only one.");
                return false;
            }

            // check REMC advanced options
		
	    // check for replicas
	    if (cl.hasOption("replicas")) {
		try {
		    replicas = Integer.parseInt(cl.getOptionValue("replicas"));
                    mergerepNrOfReplicas = replicas; // in case of Merge Replica Search
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
                    mergerepNrOfSteps = mcSteps; // in case of Merge Replica Search
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
                    mixrepMinSimAnTemp = minT; // in case of MixRep
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
                    mixrepMaxSimAnTemp = maxT; // in case of MixRep
		    if (maxT <= minT) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nmax_t must be a postive numeric value, larger than min_t");
		    return false;
		}
	    }

            // check Tabu advanced options

            // check for list_size
            if (cl.hasOption("list_size")) {
		try {
		    tabuListSize = Integer.parseInt(cl.getOptionValue("list_size"));
		    if (tabuListSize <= 0) throw new NumberFormatException();
                    tabuListSizeSpecified = true;
		} catch(NumberFormatException nfe) {
		    System.err.println("\nlist_size must be a postive integer");
		    return false;
		}
	    }

            // check Genetic Algorithm advanced options

            // check for pop_size
            if (cl.hasOption("pop_size")) {
		try {
		    genPopSize = Integer.parseInt(cl.getOptionValue("pop_size"));
		    if (genPopSize < 2) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\npop_size must be a postive integer >= 2");
		    return false;
		}
	    }

            // check for children
            if (cl.hasOption("children")) {
		try {
		    genNrOfChildren = Integer.parseInt(cl.getOptionValue("children"));
                    mergerepNrOfChildren = genNrOfChildren; // in case of Merge Replica Search
		    if (genNrOfChildren < 1) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nchildren must be a postive integer >= 1");
		    return false;
		}
	    }

            // check for tournament_size
            if (cl.hasOption("tournament_size")) {
		try {
		    genTournamentSize = Integer.parseInt(cl.getOptionValue("tournament_size"));
                    mergerepTournamentSize = genTournamentSize; // in case of Merge Replica Search
                    mixrepTournamentSize = genTournamentSize; // in case of Mixed Replica Search
		    if (genTournamentSize < 1) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\ntournament_size must be a postive integer >= 1");
		    return false;
		}
	    }

            // check for mutation_rate
            if (cl.hasOption("mutation_rate")) {
		try {
		    genMutationRate = Double.parseDouble(cl.getOptionValue("mutation_rate"));
		    if (genMutationRate < 0.0 || genMutationRate > 1.0) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nmutation_rate must be a real number between 0.0 and 1.0");
		    return false;
		}
	    }



            // check Genetic Replica Search advanced options

            // check for replicas --> see REMC

            // check for mc_steps --> see REMC

            // check for tournament_size --> see Genetic Algorithm

            // check for children --> see Genetic Algorithm


            // check Mixed Replica Search advanced options
            
            // check for tabu_replicas
            if (cl.hasOption("tabu_replicas")) {
		try {
		    mixrepNrOfTabuReplicas = Integer.parseInt(cl.getOptionValue("tabu_replicas"));
		    if (mixrepNrOfTabuReplicas < 1 || mixrepNrOfTabuReplicas > 100) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\ntabu_replicas must be a positive integer in the range [1..100]");
		    return false;
		}
	    }
            
            // check for non_tabu_replicas
            if (cl.hasOption("non_tabu_replicas")) {
		try {
		    mixrepNrOfNonTabuReplicas = Integer.parseInt(cl.getOptionValue("non_tabu_replicas"));
		    if (mixrepNrOfNonTabuReplicas < 1 || mixrepNrOfNonTabuReplicas > 100) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nnon_tabu_replicas must be a positive integer in the range [1..100]");
		    return false;
		}
	    }

            // check for rounds_without_tabu
	    if (cl.hasOption("rounds_without_tabu")) {
		try {
		    mixrepRoundsWithoutTabu = Integer.parseInt(cl.getOptionValue("rounds_without_tabu"));
		    if (mixrepRoundsWithoutTabu < 0 || mixrepRoundsWithoutTabu > 1000000) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nrounds_without_tabu must be an integer in the range [0..1000000]");
		    return false;
		}
	    }
            
            // check for tabu_steps
	    if (cl.hasOption("tabu_steps")) {
		try {
		    mixrepNrOfTabuSteps = Integer.parseInt(cl.getOptionValue("tabu_steps"));
		    if (mixrepNrOfTabuSteps < 1 || mixrepNrOfTabuSteps > 1000000) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\ntabu_steps must be a positive integer in the range [1..1000000]");
		    return false;
		}
	    }

            // check for tournament_size --> see Genetic Algorithm

            // check for children --> see Genetic Algorithm
            
            // check for tabu_list --> see Tabu Search

            // check for boost_nr
            if (cl.hasOption("boost_nr")) {
		try {
		    mixrepBoostNr = Integer.parseInt(cl.getOptionValue("boost_nr"));
		    if (mixrepBoostNr < 1 || mixrepBoostNr > 100) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nboost_nr must be a positive integer in the range [1..100]");
		    return false;
		}
	    }

            // check for boost_min_prog
            if (cl.hasOption("boost_min_prog")) {
		try {
		    mixrepBoostMinProg = Double.parseDouble(cl.getOptionValue("boost_min_prog"));
		    if (mixrepBoostMinProg <= 0.0) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nboost_min_prog must be a real number larger than 0.0");
		    return false;
		}
	    }

            // check for boost_time_factor
            if (cl.hasOption("boost_time_factor")) {
		try {
		    mixrepBoostTimeFactor = Integer.parseInt(cl.getOptionValue("boost_time_factor"));
		    if (mixrepBoostTimeFactor < 1) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nboost_time_factor must be a positive integer");
		    return false;
		}
	    }

            // check for min_boost_time
            if (cl.hasOption("min_boost_time")) {
		try {
		    mixrepMinBoostTime = Double.parseDouble(cl.getOptionValue("min_boost_time"));
		    if (mixrepMinBoostTime < 0.0) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nmin_boost_time must be a real number larger than or equal to 0.0");
		    return false;
		}
	    }

            // check LR Search advanced options

            // check for l
            if (cl.hasOption("l")) {
		try {
		    lr_l = Integer.parseInt(cl.getOptionValue("l"));
		    if (lr_l < 1) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nl must be a postive integer >= 1");
		    return false;
		}
	    }

            // check for r
            if (cl.hasOption("r")) {
		try {
		    lr_r = Integer.parseInt(cl.getOptionValue("r"));
		    if (lr_r < 1) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nr must be a postive integer >= 1");
		    return false;
		}
	    }

            // ensure that l and r are not equal
            if(lr_l == lr_r){
                System.err.println("l and r cannot be equal");
                return false;
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
			   "\n\tdistance and 30% of the weight is based on Shannons diversity index. Optimization " +
                           "\n\tis carried out using the Replica Exchange Monte Carlo algorithm.");
	System.out.println("");
	System.out.println("\tcorehunter -remc -MR 0.7 -SH 0.3 collection.dat coresubset.dat");
	System.out.println("");
	    
	HelpFormatter f = new HelpFormatter();
	f.setSyntaxPrefix("");

	f.printHelp("measures (at least one must be specified):", measuresOpts);
	System.out.println("");
        f.printHelp("search type options:", searchTypeOpts);
        System.out.println("");
        f.printHelp("common advanced search options:", commonSearchOpts);
	System.out.println("");
        f.printHelp("REMC - advanced search options:", remcSearchOpts);
	System.out.println("");
        f.printHelp("Tabu - advanced search options:", tabuSearchOpts);
	System.out.println("");
        f.printHelp("Genetic Algorithm - advanced search options:", genSearchOpts);
	System.out.println("");
        f.printHelp("Merge Replica Search - advanced search options:", mergerepSearchOpts);
	System.out.println("");
        f.printHelp("Mixed Replica Search - advanced search options:", mixrepSearchOpts);
	System.out.println("");
        f.printHelp("(semi) LR Search - advanced search options:", lrSearchOpts);
	System.out.println("");
	f.printHelp("misc options:", miscOpts);
	System.out.println("");
	System.exit(0);
    }	

    private void showVersion() {
	System.out.println("");
	System.out.println("Corehunter version: 2.0");
	System.out.println("");
    }

    public static void main(String[] args) {
        CorehunterTextRunner corehunter = new CorehunterTextRunner();
	corehunter.run(args);

	System.exit(0);
    }
}

