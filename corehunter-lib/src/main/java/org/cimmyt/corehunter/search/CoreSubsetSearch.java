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

package org.cimmyt.corehunter.search;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;

import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.AccessionCollection;
import org.cimmyt.corehunter.measures.GroupAverageClusterDistance;
import org.cimmyt.corehunter.measures.ModifiedRogersDistance;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 * <<Class summary>>
 *
 * @author Chris Thachuk &lt;&gt;
 * @version $Rev$
 */
public final class CoreSubsetSearch {

    // Progress Writer settings
    private final static boolean WRITE_PROGRESS_FILE = true;
    private final static String PROGRESS_FILE_PATH = "progress";
    private final static long PROGRESS_WRITE_PERIOD = 100;


    final static double K_b2 = 1.360572e-9;

    // this class should not be instantiable from outside class
    private CoreSubsetSearch() {

    }

    public static AccessionCollection remcSearch(AccessionCollection ac, Neighborhood nh,
						 PseudoMeasure pm, int sampleMin, int sampleMax,
						 double runtime, double minProg, double stuckTime,
                                                 int numReplicas, double minT, double maxT, int mcSteps) {
	
	SimAnReplica replicas[] = new SimAnReplica[numReplicas];
	Random r = new Random();
	
	for(int i=0; i<numReplicas; i++) {
	    double T = minT + i*(maxT - minT)/(numReplicas - 1);
	    replicas[i] = new SimAnReplica(ac, pm, nh, mcSteps, -1, sampleMin, sampleMax, T);
            replicas[i].init();
	}
	
	double bestScore = -Double.MAX_VALUE;
	List<Accession> bestCore = new ArrayList<Accession>();

      	ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();
	double eTime = sTime + runtime * 1000000000;
	
	int swapBase = 0;
        boolean cont = true, impr;
        double prevBestScore = bestScore, prog;
        int prevBestSize = ac.size();
        double lastImprTime = 0.0;

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
        }
	while( cont && tb.getCurrentThreadCpuTime() < eTime ) {

	    // run MC search for each replica
            impr = false;
	    for(int i=0; i<numReplicas; i++) {
		replicas[i].doSteps();
                double bestRepScore = replicas[i].getBestScore();
		
		if (bestRepScore > bestScore || 
		    (bestRepScore == bestScore && replicas[i].getBestCore().size() < bestCore.size())) {

                    
		    bestScore = bestRepScore;
		    bestCore.clear();
		    bestCore.addAll(replicas[i].getBestCore());

                    impr=true;
                    lastImprTime = (tb.getCurrentThreadCpuTime() - sTime);
		    System.out.println("best score: " + bestRepScore + "\tsize: " + bestCore.size() +
				       "\ttime: " + lastImprTime/1000000000);
                    // update progress writer
                    if(WRITE_PROGRESS_FILE){
                        pw.updateScore(bestScore);
                    }
		}				
            }

            // check min progression
            prog = bestScore - prevBestScore;
            if(impr && bestCore.size() >= prevBestSize && prog < minProg){
                cont = false;
            }
            // check stuckTime
            if((tb.getCurrentThreadCpuTime()-sTime-lastImprTime)/1000000000 > stuckTime){
                cont = false;
            }

            prevBestScore = bestScore;
            prevBestSize = bestCore.size();

	    // consider swapping temperatures of adjacent replicas
 	    for(int i=swapBase; i<numReplicas-1; i+=2) {
		SimAnReplica m = replicas[i];
		SimAnReplica n = replicas[i+1];
	
		double B_m = 1.0 / (K_b2 * m.getTemperature());
		double B_n = 1.0 / (K_b2 * n.getTemperature());
		double B_diff = B_n - B_m;
		double E_delta = m.getScore() - n.getScore();
		
		boolean swap = false;

		if( E_delta <= 0 ) {
		    swap = true;
		} else {
		    double p = r.nextDouble();
		    
		    if( Math.exp(B_diff * E_delta) > p ) {
			swap = true;
		    }
		}
		
		if (swap) {
		    m.swapTemperature(n);
		    SimAnReplica temp = replicas[i];
		    replicas[i] = replicas[i+1];
		    replicas[i+1] = temp;
		}
	    }
	    swapBase = 1 - swapBase;
	}
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }
        
        System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

	AccessionCollection core = new AccessionCollection();
	core.add(bestCore);
	
	// temp
	// for(int i=0; i<replicas.length; i++) {
	//    replicas[i].printStats();
	// }
	
	return core;
    }

    public static AccessionCollection parRemcSearch(AccessionCollection ac, Neighborhood nh,
						 PseudoMeasure pm, int sampleMin, int sampleMax,
						 double runtime, double minProg, double stuckTime,
                                                 int numReplicas, double minT, double maxT, int mcSteps) {

	SimAnReplica replicas[] = new SimAnReplica[numReplicas];
	Random r = new Random();

	for(int i=0; i<numReplicas; i++) {
	    double T = minT + i*(maxT - minT)/(numReplicas - 1);
	    replicas[i] = new SimAnReplica(ac, pm, nh.clone(), mcSteps, -1, sampleMin, sampleMax, T);
            replicas[i].init();
	}

	double bestScore = -Double.MAX_VALUE;
	List<Accession> bestCore = new ArrayList<Accession>();

        List<Future> futures = new ArrayList<Future>(numReplicas);
        ExecutorService pool = Executors.newCachedThreadPool();

	long sTime = System.currentTimeMillis();
	long eTime = sTime + (long) (runtime * 1000);

	int swapBase = 0;
        boolean cont = true, impr;
        double prevBestScore = bestScore, prog;
        int prevBestSize = ac.size();
        long lastImprTime = 0;

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
        }
	while( cont && System.currentTimeMillis() < eTime ) {

	    // run MC search for each replica (parallel in pool!)
	    for(int i=0; i<numReplicas; i++) {
                Future fut = pool.submit(replicas[i]);
                futures.add(fut);
            }

            // Wait until all tasks have been completed
            for(int i=0; i<futures.size(); i++){
                try {
                    futures.get(i).get(); // doesn't return a result, but blocks until done
                } catch (InterruptedException ex) {
                    System.err.println("Error in thread pool: " + ex);
                    ex.printStackTrace();
                    System.exit(1);
                } catch (ExecutionException ex) {
                    System.err.println("Error in thread pool: " + ex);
                    ex.printStackTrace();
                    System.exit(1);
                }
            }
            
            // All tasks are done, inspect results
            impr=false;
            for(int i=0; i<numReplicas; i++){
                double bestRepScore = replicas[i].getBestScore();

		if (bestRepScore > bestScore ||
		    (bestRepScore == bestScore && replicas[i].getBestCore().size() < bestCore.size())) {

		    bestScore = bestRepScore;
		    bestCore.clear();
		    bestCore.addAll(replicas[i].getBestCore());

                    impr=true;
                    lastImprTime = System.currentTimeMillis() - sTime;
		    System.out.println("best score: " + bestRepScore + "\tsize: " + bestCore.size() +
				       "\ttime: " + lastImprTime/1000.0);
                    // update progress writer
                    if(WRITE_PROGRESS_FILE){
                        pw.updateScore(bestScore);
                    }
		}
	    }

            // check min progression
            prog = bestScore - prevBestScore;
            if(impr && bestCore.size() >= prevBestSize && prog < minProg){
                cont = false;
            }
            // check stuckTime
            if((System.currentTimeMillis()-sTime-lastImprTime)/1000.0 > stuckTime){
                cont = false;
            }

            prevBestScore = bestScore;
            prevBestSize = bestCore.size();

	    // consider swapping temperatures of adjacent replicas
 	    for(int i=swapBase; i<numReplicas-1; i+=2) {
		SimAnReplica m = replicas[i];
		SimAnReplica n = replicas[i+1];

		double B_m = 1.0 / (K_b2 * m.getTemperature());
		double B_n = 1.0 / (K_b2 * n.getTemperature());
		double B_diff = B_n - B_m;
		double E_delta = m.getScore() - n.getScore();

		boolean swap = false;

		if( E_delta <= 0 ) {
		    swap = true;
		} else {
		    double p = r.nextDouble();

		    if( Math.exp(B_diff * E_delta) > p ) {
			swap = true;
		    }
		}

		if (swap) {
		    m.swapTemperature(n);
		    SimAnReplica temp = replicas[i];
		    replicas[i] = replicas[i+1];
		    replicas[i+1] = temp;
		}
	    }
	    swapBase = 1 - swapBase;
	}
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }
        
        System.out.println("### End time: " + (System.currentTimeMillis() - sTime)/1000.0);

	AccessionCollection core = new AccessionCollection();
	core.add(bestCore);

	return core;
    }

    /**
     * Pick a random core set
     *
     * @param ac
     * @param sampleMin
     * @param sampleMax
     * @return
     */
    public static AccessionCollection randomSearch(AccessionCollection ac, int sampleMin, int sampleMax) {
	List<Accession> accessions = new ArrayList<Accession>(ac.getAccessions());
	AccessionCollection core = new AccessionCollection();

	Random r = new Random();

        boolean cont = true;

	while(cont) {
	    int ai = r.nextInt(accessions.size());
	    Accession a = accessions.remove(ai);
	    core.add(a);
            cont = core.size() < sampleMax &&
                   (core.size() < sampleMin || r.nextDouble() > 1.0/(sampleMax-sampleMin));
	}
        // restore full accession collection
        accessions.addAll(core.getAccessions());

        return core;
    }

    public static AccessionCollection exhaustiveSearch(AccessionCollection ac, PseudoMeasure pm,
                                                       int sampleMin, int sampleMax){

        return exhaustiveSearch(ac, pm, sampleMin, sampleMax, true);

    }

    /**
     * Evaluate all possible core sets and return best one
     *
     * @param ac
     * @param pm
     * @param sampleMin
     * @param sampleMax
     * @param output
     * @return
     */
    public static AccessionCollection exhaustiveSearch(AccessionCollection ac, PseudoMeasure pm,
                                                       int sampleMin, int sampleMax, boolean output){
        // Check if sampleMin and sampleMax are equal (required for this exh search)
        if(sampleMin != sampleMax){
            System.err.println("\nError: minimum and maximum sample size should be equal for exhaustive search.\n");
            System.exit(1);
        }
	int coreSize = sampleMin;
	AccessionCollection temp = null, core = null;
        double score, bestScore = -Double.MAX_VALUE;
        int progress = 0, newProgress;
        String cacheID = PseudoMeasure.getUniqueId();

        // Calculate pseudomeasure for all possible core sets and return best core

        ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();

        KSubsetGenerator ksub = new KSubsetGenerator(coreSize, ac.size());
        long nr = ksub.getNrOfKSubsets();
        if(output) System.out.println("Nr of possible core sets: " + nr + "\n-------------");
        Integer[] icore = ksub.first();
        for(long i=1; i<=nr; i++){
            newProgress = (int) (((double) i) / ((double) nr) * 100);
            if(newProgress > progress){
                if(output) System.out.println("### Progress: " + newProgress + "%");
                progress = newProgress;
            }
            temp = ac.subset(icore);
            // Calculate pseudomeasure
            score = pm.calculate(temp.getAccessions(), cacheID);
            if(score > bestScore){
                core = temp;
                bestScore = score;
                if(output)System.out.println("best score: " + bestScore + "\tsize: " + core.size() +
                                   "\ttime: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);
            }
            ksub.successor(icore);
        }
        if(output) System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

        return core;
    }

   public static AccessionCollection localSearch(AccessionCollection ac, Neighborhood nh,  PseudoMeasure pm,
                                                            int sampleMin, int sampleMax, double runtime,
                                                            double minProg, double stuckTime) {

        double score, newScore;
        int size, newSize;
        List<Accession> core, unselected;

        String cacheId = PseudoMeasure.getUniqueId();

        Random r = new Random();

        List<Accession> accessions = ac.getAccessions();

        // create unselected list
        unselected = new ArrayList<Accession>(accessions);
        // select an initial core
        core = new ArrayList<Accession>();
        int j;
        Accession a;
        for (int i=0; i<sampleMax; i++){
            j = r.nextInt(unselected.size());
            a = unselected.remove(j);
            core.add(a);
        }
        score = pm.calculate(core, cacheId);
        size = core.size();

      	ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();
	double eTime = sTime + runtime * 1000000000;

        boolean cont = true;
        double lastImprTime = 0.0;

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
            pw.updateScore(score);
        }
	while ( cont && tb.getCurrentThreadCpuTime() < eTime ) {
            // run Local Search step
            nh.genRandomNeighbor(core, unselected);
            newScore = pm.calculate(core, cacheId);
            newSize = core.size();

            if (newScore > score || (newScore == score && newSize < size)) {
                // check min progression
                if(newSize >= size && newScore - score < minProg){
                    cont = false;
                }
                // report BETTER solution was found
                lastImprTime = tb.getCurrentThreadCpuTime() - sTime;
                System.out.println("best score: " + newScore + "\tsize: " + newSize +
                                   "\ttime: " + lastImprTime/1000000000);
                // accept new core!
                score = newScore;
                size = newSize;

                // update progress writer
                if(WRITE_PROGRESS_FILE){
                    pw.updateScore(score);
                }
            } else {
                // Reject new core
                nh.undoLastPerturbation(core, unselected);
                // check stuckTime
                if((tb.getCurrentThreadCpuTime()-sTime  -lastImprTime)/1000000000 > stuckTime){
                    cont = false;
                }
            }
	}
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }

        System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

	AccessionCollection bestCore = new AccessionCollection();
	bestCore.add(core);

	return bestCore;

    }

    /**
     * Steepest Descent search.
     *
     * Always go on with the best of all neighbors, if it is better than the current
     * core set, and stop search of no improvement can be maded. This is also called
     * an "iterative improvement" strategy.
     *
     * @param ac
     * @param nh
     * @param pm
     * @param sampleMin
     * @param sampleMax
     * @param runtime
     * @param minProg
     * @return
     */
    public static AccessionCollection steepestDescentSearch(AccessionCollection ac, Neighborhood nh,  PseudoMeasure pm,
                                                            int sampleMin, int sampleMax, double runtime, double minProg) {

        double score, newScore;
        int size, newSize;
        List<Accession> core, unselected;

        String cacheId = PseudoMeasure.getUniqueId();

        Random r = new Random();

        List<Accession> accessions = ac.getAccessions();

        // create unselected list
        unselected = new ArrayList<Accession>(accessions);
        // select an initial core
        core = new ArrayList<Accession>();
        int j;
        Accession a;
        for (int i=0; i<sampleMax; i++){
            j = r.nextInt(unselected.size());
            a = unselected.remove(j);
            core.add(a);
        }
        score = pm.calculate(core, cacheId);
        size = core.size();

      	ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();
	double eTime = sTime + runtime * 1000000000;

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
            pw.updateScore(score);
        }
        boolean cont = true;
	while (cont) {        
            // run Steepest Descent search step
            nh.genBestNeighbor(core, unselected, pm, cacheId);
            newScore = pm.calculate(core, cacheId);
            newSize = core.size();

            if (newScore > score || (newScore == score && newSize < size)) {
                // check min progression
                if(newSize >= size && newScore - score < minProg){
                    cont = false;
                }
                // report BETTER solution was found
                System.out.println("best score: " + newScore + "\tsize: " + newSize +
                                   "\ttime: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);                
                // accept new core!
                score = newScore;
                size = newSize;
                // continue if time left
                cont = cont && tb.getCurrentThreadCpuTime() < eTime;

                // update progress writer
                if(WRITE_PROGRESS_FILE){
                    pw.updateScore(score);
                }
            } else {
                // Don't accept new core
                nh.undoLastPerturbation(core, unselected);
                // All neighbors are worse than current core, so stop search
                cont = false;
            }
	}
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }

        System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

	AccessionCollection bestCore = new AccessionCollection();
	bestCore.add(core);
	
	return bestCore;

    }

    /**
     * TABU Search.
     *
     * Tabu list is a list of indices at which the current core set cannot be
     * perturbed (delete, swap) to form a new core set as long as the index is contained
     * in the tabu list. After each perturbation step, the index of the newly added
     * accession (if it exists) is added to the tabu list, to ensure this accesion is
     * not again removed from the core set (or replaced) during the next few rounds.
     * 
     * If no new accession was added (pure deletion), a value "-1" is added to the tabu list.
     * As long as such values are contained in the tabu list, adding a new accesion without
     * removing one (pure addition) is considered tabu, to prevent immediately re-adding
     * the accession which was removed in the previous step.
     *
     * @param ac
     * @param nh
     * @param pm
     * @param sampleMin
     * @param sampleMax
     * @param runtime
     * @param minProg
     * @param stuckTime
     * @param tabuListSize
     * @return
     */
    public static AccessionCollection tabuSearch(AccessionCollection ac, Neighborhood nh,  PseudoMeasure pm, int sampleMin,
                                                 int sampleMax, double runtime, double minProg, double stuckTime, int tabuListSize) {

        double score, bestScore;
        List<Accession> core, bestCore, unselected;
        LinkedList<Integer> tabuList;

        String cacheId = PseudoMeasure.getUniqueId();

        Random r = new Random();

        List<Accession> accessions = ac.getAccessions();

        // create unselected list
        unselected = new ArrayList<Accession>(accessions);
        // select an initial core
        core = new ArrayList<Accession>();
        int j;
        Accession a;
        for (int i=0; i<sampleMax; i++){
            j = r.nextInt(unselected.size());
            a = unselected.remove(j);
            core.add(a);
        }
        score = pm.calculate(core, cacheId);

        bestCore = new ArrayList<Accession>();
        bestCore.addAll(core);
        bestScore = score;
        
        // initialize tabu list
        tabuList = new LinkedList<Integer>();

      	ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();
	double eTime = sTime + runtime * 1000000000;

        int addIndex;
        boolean cont = true;
        double lastImprTime = 0.0;

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
            pw.updateScore(bestScore);
        }
	while ( cont && tb.getCurrentThreadCpuTime() < eTime ) {
            // run TABU search step

            // ALWAYS accept new core, even it is not an improvement
            addIndex = nh.genBestNeighbor(core, unselected, tabuList, bestScore, pm, cacheId);
            score = pm.calculate(core, cacheId);

            // check if new best core was found
            if (score > bestScore || (score == bestScore && core.size() < bestCore.size())) {
                // check min progression
                if(core.size() >= bestCore.size() && score - bestScore < minProg){
                    cont = false;
                }
                // store new best core
                bestScore = score;
                bestCore.clear();
                bestCore.addAll(core);

                lastImprTime = tb.getCurrentThreadCpuTime() - sTime;
                System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                                   "\ttime: " + lastImprTime/1000000000);
                // update progress writer
                if(WRITE_PROGRESS_FILE){
                    pw.updateScore(bestScore);
                }
            } else {
                // check stuckTime
                if((tb.getCurrentThreadCpuTime()-sTime-lastImprTime)/1000000000 > stuckTime){
                    cont = false;
                }
            }

            // finally, update tabu list
            if(tabuList.size() == tabuListSize){
                // capacity reached, remove oldest tabu index
                tabuList.poll();
            }
            // add new tabu index
            //tabuList.offer(addIndex);
            tabuList.offer(addIndex);

	}
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }

        System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

	AccessionCollection bestCoreCol = new AccessionCollection();
	bestCoreCol.add(bestCore);

	return bestCoreCol;

    }

    public static AccessionCollection geneticSearch(AccessionCollection ac, PseudoMeasure pm, int sampleMin,
                                                    int sampleMax, double runtime, double minProg, double stuckTime,
                                                    int popSize, int nrOfChildren, int tournamentSize, double mutationRate) {

        double bestScore;
        List<Accession> bestCore;

        // Create population
        GeneticPopulation population = new GeneticPopulation(popSize, sampleMin, sampleMax, ac.getAccessions(),
                                                             pm, nrOfChildren, tournamentSize, mutationRate);

        ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();
	double eTime = sTime + runtime * 1000000000;

        // Init population
        population.init();
        bestCore = population.getBestCore();
        bestScore = population.getBestScore();
        System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                           "\ttime: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

        // Until end time reached, create new generations
        boolean cont = true;
        double lastImprTime = 0.0;

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
            pw.updateScore(bestScore);
        }
        while( cont && tb.getCurrentThreadCpuTime() < eTime ){
            population.nextGen();
            if(population.getBestScore() > bestScore
                    || (population.getBestScore() == bestScore && population.getBestCore().size() < bestCore.size())){
                // check min progression
                if(population.getBestCore().size() >= bestCore.size() && population.getBestScore() - bestScore < minProg){
                    cont = false;
                }
                // Report new best core was found
                bestCore = population.getBestCore();
                bestScore = population.getBestScore();

                lastImprTime = tb.getCurrentThreadCpuTime() - sTime;
                System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                                   "\ttime: " + lastImprTime/1000000000);
                // update progress writer
                if(WRITE_PROGRESS_FILE){
                    pw.updateScore(bestScore);
                }
            } else {
                // check stuckTime
                if((tb.getCurrentThreadCpuTime()-sTime-lastImprTime)/1000000000 > stuckTime){
                    cont = false;
                }
            }
        }
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }

        System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

	AccessionCollection bestCoreCol = new AccessionCollection();
	bestCoreCol.add(bestCore);

	return bestCoreCol;
    }

    public static AccessionCollection mergeReplicaSearch(AccessionCollection ac, Neighborhood nh, PseudoMeasure pm,
                                                    int sampleMin, int sampleMax, double runtime, double minProg, double stuckTime,
                                                    int minNrOfReplicas, int nrOfLocalSearchSteps, int nrOfChildren, int tournamentSize,
                                                    boolean stratifiedStart, boolean stratifiedMerge) {
        final int NR_OF_CLUSTERS = sampleMax;
        final double STRAT_MERGE_PROB = 0.5;

        double bestScore = -Double.MAX_VALUE;
        List<Accession> bestCore = new ArrayList<Accession>();

        Random rg = new Random();

        ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();
	double eTime = sTime + runtime * 1000000000;

        Clustering clustering = new Clustering(NR_OF_CLUSTERS, new GroupAverageClusterDistance(new ModifiedRogersDistance(ac.size())));

        if(stratifiedStart){
            for(Accession a : ac.getAccessions()){
                clustering.addAccession(a);
            }
        }

        // create, init and store genetic replicas
        List<Replica> replicas = new ArrayList<Replica>(minNrOfReplicas);
        for (int i=0; i<minNrOfReplicas; i++){
            LocalSearchReplica rep = new LocalSearchReplica(ac, pm, nh, nrOfLocalSearchSteps, -1, sampleMin, sampleMax);
            if(stratifiedStart){
                //if(i%2 == 0) //half random - half stratified start
                    rep.init(sampleStratifiedStart(clustering.getClusters(), rg));
                //else
                //    rep.init();
            } else {
                rep.init();
            }
            replicas.add(rep);
        }
        List<List<Accession>> parents = new ArrayList<List<Accession>>(2*nrOfChildren);
        List<List<Accession>> children = new ArrayList<List<Accession>>(nrOfChildren);

        boolean cont = true, impr;
        double prevBestScore = bestScore, prog;
        int prevBestSize = ac.size();
        double lastImprTime = 0.0;

        if(!stratifiedStart){
            // if no clustering at start, timer is started AFTER random initialization,
            // like with REMC, for fair comparison of runtimes
            sTime = tb.getCurrentThreadCpuTime();
            eTime = sTime + runtime * 1000000000;
        }

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
        }
        while ( cont && tb.getCurrentThreadCpuTime() < eTime ){
            // Perform local search steps for each replica
            impr=false;
            for(int i=replicas.size()-1; i>=0; i--){

                Replica rep = replicas.get(i);
                rep.doSteps();

                // check for better solution
                if (rep.getBestScore() > bestScore
                        || (rep.getBestScore() == bestScore && rep.getBestCore().size() < bestCore.size())){

                    // store better core
                    bestScore = rep.getBestScore();
                    bestCore.clear();
                    bestCore.addAll(rep.getBestCore());

                    impr=true;
                    lastImprTime = tb.getCurrentThreadCpuTime() - sTime;
                    System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                                       "\ttime: " + lastImprTime/1000000000 +
                                       "\t#rep: " + replicas.size());
                    // update progress writer
                    if(WRITE_PROGRESS_FILE){
                        pw.updateScore(bestScore);
                    }
                }
                // if current replica got stuck and enough replicas left --> remove replica
                if(rep.stuck() && replicas.size() > minNrOfReplicas){
                    replicas.remove(i);
                }
            }

            // check min progression
            prog = bestScore - prevBestScore;
            if(impr && bestCore.size() >= prevBestSize && prog < minProg){
                cont = false;
            }
            // check stuckTime
            if((tb.getCurrentThreadCpuTime()-sTime-lastImprTime)/1000000000 > stuckTime){
                cont = false;
            }

            prevBestScore = bestScore;
            prevBestSize = bestCore.size();

            // Genetic cross-over to create new replicas
            selectParents(replicas, parents, 2*nrOfChildren, tournamentSize, rg);
            if(stratifiedMerge && rg.nextDouble() < STRAT_MERGE_PROB){
                createNewStratifiedChildren(parents, children, rg, clustering);
            } else {
                createNewChildren(parents, children, rg);
            }
            for(List<Accession> child : children){
                LocalSearchReplica rep = new LocalSearchReplica(ac, pm, nh, nrOfLocalSearchSteps, -1, sampleMin, sampleMax);
                rep.init(child);
                replicas.add(rep);
            }
        }
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }

        System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

	AccessionCollection bestCoreCol = new AccessionCollection();
	bestCoreCol.add(bestCore);

	return bestCoreCol;

    }

    public static AccessionCollection parMergeReplicaSearch(AccessionCollection ac, Neighborhood nh, PseudoMeasure pm,
                                                    int sampleMin, int sampleMax, double runtime, double minProg, double stuckTime,
                                                    int minNrOfReplicas, int nrOfLocalSearchSteps, int nrOfChildren, int tournamentSize,
                                                    boolean stratifiedStart, boolean stratifiedMerge) {

        final int NR_OF_CLUSTERS = sampleMax;
        final double STRAT_MERGE_PROB = 0.5;

        double bestScore = -Double.MAX_VALUE;
        List<Accession> bestCore = new ArrayList<Accession>();

        Random rg = new Random();

        long sTime = System.currentTimeMillis();
	long eTime = sTime + (long)(runtime * 1000);

        Clustering clustering = new Clustering(NR_OF_CLUSTERS, new GroupAverageClusterDistance(new ModifiedRogersDistance(ac.size())));

        if(stratifiedStart){
            for(Accession a : ac.getAccessions()){
                clustering.addAccession(a);
            }
        }

        // create, init and store replicas
        List<Replica> replicas = new ArrayList<Replica>(minNrOfReplicas);
        for (int i=0; i<minNrOfReplicas; i++){
            LocalSearchReplica rep = new LocalSearchReplica(ac, pm, nh.clone(), nrOfLocalSearchSteps, -1, sampleMin, sampleMax);
            if(stratifiedStart){
                //if(i%2 == 0) //half random - half stratified start
                    rep.init(sampleStratifiedStart(clustering.getClusters(), rg));
                //else
                //    rep.init();
            } else {
                rep.init();
            }
            replicas.add(rep);
        }
        List<Future> futures = new ArrayList<Future>(minNrOfReplicas);
        List<List<Accession>> parents = new ArrayList<List<Accession>>(2*nrOfChildren);
        List<List<Accession>> children = new ArrayList<List<Accession>>(nrOfChildren);

        // create thread pool
        ExecutorService pool = Executors.newCachedThreadPool();

        boolean cont = true, impr;
        double prevBestScore = bestScore, prog;
        int prevBestSize = ac.size();
        long lastImprTime = 0;

        if(!stratifiedStart){
            // if no clustering at start, timer is started AFTER random initialization,
            // like with REMC, for fair comparison of runtimes
            sTime = System.currentTimeMillis();
            eTime = sTime + (long)(runtime * 1000);
        }

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
        }
        while ( cont && System.currentTimeMillis() < eTime ){

            // Perform local search steps for each replica (parallel in pool!)
            futures.clear();
            for(int i=0; i<replicas.size(); i++){
                Future fut = pool.submit(replicas.get(i));
                futures.add(fut);
            }

            // Wait until all tasks have been completed
            for(int i=0; i<futures.size(); i++){
                try {
                    futures.get(i).get(); // doesn't return a result, but blocks until done
                } catch (InterruptedException ex) {
                    System.err.println("Error in thread pool: " + ex);
                    ex.printStackTrace();
                    System.exit(1);
                } catch (ExecutionException ex) {
                    System.err.println("Error in thread pool: " + ex);
                    ex.printStackTrace();
                    System.exit(1);
                }
            }

            // All tasks are done, inspect results
            impr = false;
            for(int i=replicas.size()-1; i>=0; i--){
                Replica rep = replicas.get(i);
                // check for better solution
                if (rep.getBestScore() > bestScore
                        || (rep.getBestScore() == bestScore && rep.getBestCore().size() < bestCore.size())){

                    // store better core
                    bestScore = rep.getBestScore();
                    bestCore.clear();
                    bestCore.addAll(rep.getBestCore());
                    
                    impr=true;
                    lastImprTime = System.currentTimeMillis() - sTime;
                    System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                                       "\ttime: " + lastImprTime/1000.0 +
                                       "\t#rep: " + replicas.size());
                    // update progress writer
                    if(WRITE_PROGRESS_FILE){
                        pw.updateScore(bestScore);
                    }
                }
                // if current replica got stuck and enough replicas left --> remove replica
                if(rep.stuck() && replicas.size() > minNrOfReplicas){
                    replicas.remove(i);
                }
            }

            // check min progression
            prog = bestScore - prevBestScore;
            if(impr && bestCore.size() >= prevBestSize && prog < minProg){
                cont = false;
            }
            // check stuck time
            if((System.currentTimeMillis()-sTime-lastImprTime)/1000.0 > stuckTime){
                cont = false;
            }

            prevBestScore = bestScore;
            prevBestSize = bestCore.size();

            // Genetic cross-over to create new replicas
            selectParents(replicas, parents, 2*nrOfChildren, tournamentSize, rg);
            if(stratifiedMerge && rg.nextDouble() < STRAT_MERGE_PROB){
                createNewStratifiedChildren(parents, children, rg, clustering);
            } else {
                createNewChildren(parents, children, rg);
            }
            createNewChildren(parents, children, rg);
            for(List<Accession> child : children){
                LocalSearchReplica rep = new LocalSearchReplica(ac, pm, nh.clone(), nrOfLocalSearchSteps, -1, sampleMin, sampleMax);
                rep.init(child);
                replicas.add(rep);
            }
        }
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }

        System.out.println("### End time: " + (System.currentTimeMillis() - sTime)/1000.0);

	AccessionCollection bestCoreCol = new AccessionCollection();
	bestCoreCol.add(bestCore);

	return bestCoreCol;

    }

    public static AccessionCollection mixedReplicaSearch(AccessionCollection ac, PseudoMeasure pm, int sampleMin,
                                                         int sampleMax, double runtime, double minProg, double stuckTime,
                                                         int nrOfTabuReplicas, int nrOfNonTabuReplicas, int roundsWithoutTabu,
                                                         int nrOfTabuSteps, int tournamentSize,int tabuListSize, boolean stratifiedStart,
                                                         boolean stratifiedMerge, int boostNr, double boostMinProg,
                                                         int boostTimeFactor, double minBoostTime, double minSimAnTemp,
                                                         double maxSimAnTemp) {

        final int NR_OF_CLUSTERS = sampleMax;
        final double STRAT_MERGE_PROB = 0.5;
        double boostTime = 0;
        boolean boostTimeLocked = false;

        final int PROG_BOOST_FACTOR = 2;

        final int LR_L = 2;
        final int LR_R = 1;
        final boolean LR_EXH_START = false;
        // no limit on nr of steps for LR, just keeps running in background until done
        final int NR_OF_LR_STEPS = -1;

        // LS can perform more steps than tabu because each step is very fast,
        // only sampling one neighbor instead of Tabu which samples about ac.size()
        // neighbors in each step to select the (heursistic) best neighbor!
        final int NR_OF_LS_STEPS = ac.size();//(nrOfTabuSteps * ac.size()); TO DO: add as parameter!!!

        double bestScore = -Double.MAX_VALUE;
        List<Accession> bestCore = new ArrayList<Accession>();

        Random rg = new Random();

        Neighborhood randNh = new RandomSingleNeighborhood(sampleMin, sampleMax);
        Neighborhood heurNh = new HeuristicSingleNeighborhood(sampleMin, sampleMax);

        long sTime = System.currentTimeMillis();
        long eTime = sTime + (long)(runtime * 1000);

        Clustering clustering = new Clustering(NR_OF_CLUSTERS, new GroupAverageClusterDistance(new ModifiedRogersDistance(ac.size())));
        
        if(stratifiedStart){
            for(Accession a : ac.getAccessions()){
                clustering.addAccession(a);
            }
        }

        // create, init and store initial replicas (local search)
        List<Replica> replicas = new ArrayList<Replica>(nrOfNonTabuReplicas);
        // add Local Search Replicas until minimum nr reached
        for (int i=0; i< nrOfNonTabuReplicas; i++){
            Replica rep;

            // initially, create some extra LS Replica
            rep = new LocalSearchReplica(ac, pm, randNh.clone(), NR_OF_LS_STEPS, -1, sampleMin, sampleMax);
            
            // Init replica
            if(stratifiedStart){
                rep.init(sampleStratifiedStart(clustering.getClusters(), rg));
            } else {
                rep.init();
            }
            replicas.add(rep);
        }
        
        int nrOfTabus = 0;
        int nrOfNonTabus = nrOfNonTabuReplicas;
        int nrStuck = 0;

        // create and init one LR Semi replica
        LRReplica lrrep = new LRReplica(ac, pm, NR_OF_LR_STEPS, -1, sampleMin, sampleMax, LR_L, LR_R, LR_EXH_START);
        lrrep.init();

        List<Future> localAndREMCfutures = new ArrayList<Future>(nrOfNonTabuReplicas);
        List<Future> tabuFutures = new ArrayList<Future>(nrOfTabuReplicas);
        List<List<Accession>> parents = new ArrayList<List<Accession>>();
        List<List<Accession>> children = new ArrayList<List<Accession>>();

        // create thread pool
        final ThreadGroup threadGroup = new ThreadGroup("replicaThreadGroup");

        ThreadFactory factory = new ThreadFactory() {
            public Thread newThread(Runnable r) {
                Thread thr = new Thread(threadGroup, r);
                thr.setPriority(Thread.MIN_PRIORITY);
                return thr;
            }
        };

        ExecutorService pool = Executors.newCachedThreadPool(factory);

        boolean cont = true, impr;
        double prevBestScore = bestScore, prog;
        int prevBestSize = ac.size();
        long lastImprTime = 0;
        long prevRoundTime = 0;
        int numround = 1;
        double lastBoostTime = 0;

        boolean lrChecked = false;

        if(!stratifiedStart){
            // if no clustering at start, timer is started AFTER random initialization,
            // like with REMC, for fair comparison of runtimes
            sTime = System.currentTimeMillis();
            eTime = sTime + (long)(runtime * 1000);
        }

        ProgressWriter pw;
        if(WRITE_PROGRESS_FILE){
            pw = new ProgressWriter(PROGRESS_FILE_PATH, PROGRESS_WRITE_PERIOD);
            pw.start();
        }
        // start LR replica, continuously runs in background until finished
        Thread lrThread = factory.newThread(lrrep);
        lrThread.setPriority(Thread.MAX_PRIORITY);
        lrThread.start();
        //System.out.println("[LR submitted]");

        long firstRounds = 0;

        while ( cont && System.currentTimeMillis() < eTime ){
            // submit all tabu replicas
            for(Replica rep : replicas){
                if(rep.shortType().equals("Tabu")){
                    tabuFutures.add(pool.submit(rep));
                }
            }
            //System.out.println("[tabus submitted]");

            while(firstRounds < roundsWithoutTabu || tabuReplicasBusy(tabuFutures)){

                //System.out.println("LR steps done: " + lrrep.getCurSteps());

                /*if(firstRounds < ROUNDS_WITHOUT_TABU){
                    System.out.println("[NO TABU]");
                }*/

                firstRounds++;

                /*System.out.print("["+replicas.get(0).shortType());
                for(int i=1; i<replicas.size(); i++){
                    System.out.print(", " + replicas.get(i).shortType());
                }
                System.out.println("]");*/

                // loop submission of Local and REMC replicas (short runs)

                localAndREMCfutures.clear();
                // Submit non-tabu replicas
                for(int i=0; i<replicas.size(); i++){
                    Replica rep = replicas.get(i);
                    if(!rep.shortType().equals("Tabu")){
                        localAndREMCfutures.add(pool.submit(rep));
                    }
                }
                //System.out.println("[non-tabus submitted]");

                // Wait until all non-tabu replicas have completed their current run
                for(int i=0; i<localAndREMCfutures.size(); i++){
                    try {
                        //System.out.println("Waiting for non-tabu rep #" + (i+1));
                        localAndREMCfutures.get(i).get(); // doesn't return a result, but blocks until done
                    } catch (InterruptedException ex) {
                        System.err.println("Error in thread pool: " + ex);
                        ex.printStackTrace();
                        System.exit(1);
                    } catch (ExecutionException ex) {
                        System.err.println("Error in thread pool: " + ex);
                        ex.printStackTrace();
                        System.exit(1);
                    }
                }
                //System.out.println("[Done waiting!]");

                // Replicas are done, inspect results
                impr = false;

                // Check non-tabu replica results

                // Check LS, Tabu and REMC replica results
                nrStuck = 0;
                Iterator<Replica> itr = replicas.iterator();
                while(itr.hasNext()){
                    Replica rep = itr.next();
                    if(!rep.shortType().equals("Tabu")){
                        // check for better solution
                        if (rep.getBestScore() > bestScore
                                 || (rep.getBestScore() == bestScore && rep.getBestCore().size() < bestCore.size())){

                            // store better core
                            bestScore = rep.getBestScore();
                            bestCore.clear();
                            bestCore.addAll(rep.getBestCore());

                            impr = true;
                            lastImprTime = System.currentTimeMillis() - sTime;
                            System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                                               "\ttime: " + lastImprTime/1000.0 +
                                               "\t#rep: " + replicas.size() + "\tfound by: " + rep.type());
                            // update progress writer
                            if(WRITE_PROGRESS_FILE){
                                pw.updateScore(bestScore);
                            }
                        }
                        // count nr of stuck non-tabu reps
                        if(rep.stuck()){
                            nrStuck++;
                        }
                    }
                }

                // Check LR result, if done and not checked before
                if(lrrep.isDone() && !lrChecked){

                    System.out.println("[LR done!]");

                    if (lrrep.getBestScore() > bestScore
                            || (lrrep.getBestScore() == bestScore && lrrep.getBestCore().size() < bestCore.size())){

                        // store better core
                        bestScore = lrrep.getBestScore();
                        bestCore.clear();
                        bestCore.addAll(lrrep.getBestCore());

                        impr = true;
                        lastImprTime = System.currentTimeMillis() - sTime;
                        System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                                           "\ttime: " + lastImprTime/1000.0 +
                                           "\t#rep: " + replicas.size() + "\tfound by: " + lrrep.type());
                        // update progress writer
                        if(WRITE_PROGRESS_FILE){
                            pw.updateScore(bestScore);
                        }
                    }
                    lrChecked = true;
                    // Since LR is done, we add it to the list of replicas so that its result can be used for merging
                    replicas.add(lrrep);
                    nrOfNonTabus++;
                }

                //System.out.println("[Best score updated]");

                // update boost time
                if(!boostTimeLocked){
                    boostTime = boostTime / boostTimeFactor;
                    boostTime = (boostTime * (numround-1) + (System.currentTimeMillis() - sTime - prevRoundTime)/1000.0)/numround;
                    boostTime = boostTime * boostTimeFactor;
                    prevRoundTime = System.currentTimeMillis() - sTime;
                    //System.out.println("BoostTime: " + boostTime + " (#rep = " + replicas.size() + ")");
                }

                prog = bestScore - prevBestScore;

                // check min progression
                if(impr && bestCore.size() >= prevBestSize && prog < minProg){
                    cont = false;
                }
                // check stuckTime
                if((System.currentTimeMillis()-sTime-lastImprTime)/1000.0 > stuckTime){
                    cont = false;
                }

                // check boost prog
                if(impr && prog < boostMinProg){
                    lastBoostTime = System.currentTimeMillis()-sTime;
                    // only boost with some fraction of the normal nr of boost replicas in case of min prog boost
                    int progBoostNr = boostNr/PROG_BOOST_FACTOR;
                    boostReplicas(replicas, progBoostNr, ac, pm, randNh, NR_OF_LS_STEPS, sampleMin, sampleMax);
                    nrOfNonTabus += progBoostNr;
                    System.out.println("[progBoost] - #rep: " + replicas.size());
                }

                // check boost time
                if((System.currentTimeMillis()-sTime-Math.max(lastImprTime, lastBoostTime))/1000.0 > Math.max(boostTime, minBoostTime)
                        && replicas.size() == nrOfNonTabuReplicas + nrOfTabuReplicas){ // do not boost if previous boost effect still visible!
                    lastBoostTime = System.currentTimeMillis()-sTime;
                    boostReplicas(replicas, boostNr, ac, pm, randNh, NR_OF_LS_STEPS, sampleMin, sampleMax);
                    nrOfNonTabus += boostNr;
                    boostTimeLocked = true;
                    System.out.println("[timeBoost] - #rep: " + replicas.size());
                }

                //System.out.println("[Possible boost done]");

                // "Genetic" cross-over to create new REMC (non-tabu) replicas
                int nonTabuChildren = nrOfNonTabuReplicas - (nrOfNonTabus-nrStuck);
                //System.out.println("[non-tabu] Stuck: " + nrStuck + " - Current: " + nrOfNonTabus + " - Create: " + nonTabuChildren);
                if(nonTabuChildren > 0){
                    // Select parents from non-tabu replicas only! (tabus are still being manipulated, so skip these)
                    selectParents(replicas, parents, 2*nonTabuChildren, tournamentSize, rg, "Tabu");

                    //System.out.println("[Parents created]");

                    if(stratifiedMerge && rg.nextDouble() < STRAT_MERGE_PROB){
                        createNewStratifiedChildren(parents, children, rg, clustering);
                    } else {
                        createNewChildren(parents, children, rg);
                    }

                    //System.out.println("[Children created]");

                    for(List<Accession> child : children){
                        // New REMC replicas
                        Replica rep = new SimAnReplica(ac, pm, randNh.clone(), NR_OF_LS_STEPS, -1,
                                    sampleMin, sampleMax, minSimAnTemp + rg.nextDouble()*(maxSimAnTemp-minSimAnTemp));
                        nrOfNonTabus++;
                        
                        rep.init(child);
                        replicas.add(rep);
                    }

                    //System.out.println("[New non-tabu replicas created]");

                }


                // Now permanently delete stuck non-tabu replicas
                itr = replicas.iterator();
                while(itr.hasNext()){
                    Replica rep = itr.next();
                    if(rep.stuck() && !rep.shortType().equals("Tabu")){
                        itr.remove();
                        nrOfNonTabus--;
                    }
                }

                //System.out.println("[Non-tabu stucks removed]");

                prevBestScore = bestScore;
                prevBestSize = bestCore.size();

                numround++;

                //System.out.println("[Non-tabu round finished]");
            }

            // Tabu replicas have finished --> check for improvements & count stuck tabus
            nrStuck = 0;
            Iterator<Replica> itr = replicas.iterator();
            while(itr.hasNext()){
                Replica rep = itr.next();
                if(rep.shortType().equals("Tabu")){
                    // check for better solution
                    if (rep.getBestScore() > bestScore
                             || (rep.getBestScore() == bestScore && rep.getBestCore().size() < bestCore.size())){

                        // store better core
                        bestScore = rep.getBestScore();
                        bestCore.clear();
                        bestCore.addAll(rep.getBestCore());

                        impr = true;
                        lastImprTime = System.currentTimeMillis() - sTime;
                        System.out.println("best score: " + bestScore + "\tsize: " + bestCore.size() +
                                           "\ttime: " + lastImprTime/1000.0 +
                                           "\t#rep: " + replicas.size() + "\tfound by: " + rep.type());
                        // update progress writer
                        if(WRITE_PROGRESS_FILE){
                            pw.updateScore(bestScore);
                        }
                    }
                    // count nr of stuck non-tabu reps
                    if(rep.stuck()){
                        nrStuck++;
                    }
                }
            }

            // Create new tabus by merging current results (from all replicas!!!)
            int tabuChildren = nrOfTabuReplicas - (nrOfTabus-nrStuck);
            //System.out.println("[tabu] Stuck: " + nrStuck + " - Current: " + nrOfTabus + " - Create: " + tabuChildren);
            if(tabuChildren > 0){
                // Select parents from all replicas!
                selectParents(replicas, parents, 2*tabuChildren, tournamentSize, rg);
                if(stratifiedMerge && rg.nextDouble() < STRAT_MERGE_PROB){
                    createNewStratifiedChildren(parents, children, rg, clustering);
                } else {
                    createNewChildren(parents, children, rg);
                }
                for(List<Accession> child : children){
                    // new Tabu replicas
                    int listsize = rg.nextInt(tabuListSize)+1;
                    Replica rep = new TabuReplica(ac, pm, heurNh.clone(), nrOfTabuSteps, -1, sampleMin, sampleMax, listsize);
                    nrOfTabus++;

                    rep.init(child);
                    replicas.add(rep);
                }
            }

            // Now permanently remove stuck tabus
            itr = replicas.iterator();
            while(itr.hasNext()){
                Replica rep = itr.next();
                if(rep.stuck() && rep.shortType().equals("Tabu")){
                    itr.remove();
                    nrOfTabus--;
                }
            }

        }
        if(WRITE_PROGRESS_FILE){
            pw.stop();
        }
        lrrep.stop();
        
        System.out.println("### End time: " + (System.currentTimeMillis() - sTime)/1000.0);

	AccessionCollection bestCoreCol = new AccessionCollection();
	bestCoreCol.add(bestCore);

	return bestCoreCol;

    }

    private static boolean tabuReplicasBusy(List<Future> tabuFutures){
        // remove all tabu replica futures which are already done
        Iterator<Future> itr = tabuFutures.iterator();
        while(itr.hasNext()){
            if(itr.next().isDone()){
                itr.remove();
            }
        }
        // if busy futures remain, return true
        return tabuFutures.size() > 0;
    }

    /**
     * Boost replicas with new randomly initialized LS replicas
     */
    private static void boostReplicas(List<Replica> replicas, int boost, AccessionCollection ac,
                               PseudoMeasure pm, Neighborhood randNh, int nrOfLsSteps, int sampleMin, int sampleMax){

        // Boost with new LS replicas
        for(int i=0; i<boost; i++){
            Replica rep;
            // create LS Replica
            rep = new LocalSearchReplica(ac, pm, randNh.clone(), nrOfLsSteps, -1, sampleMin, sampleMax);
            rep.init();
            replicas.add(rep);
        }

    }

    /**
     * Sample one accession from each cluster.
     *
     * @param clusters
     * @param sampleSize
     * @param rg
     * @return
     */
    public static List<Accession> sampleStratifiedStart(Collection<AccessionCluster> clusters, Random rg){

        Iterator<AccessionCluster> itr;
        AccessionCluster clust;
        Accession remAcc;

        List<Accession> start = new ArrayList<Accession>(clusters.size());

        List<Accession> tmp = new ArrayList<Accession>();
        itr = clusters.iterator();
        while(itr.hasNext()){
            clust = itr.next();
            int clustSampleSize = 1;
            // Randomly sample accessions from cluster
            tmp.clear();
            for(int k=0; k<clustSampleSize; k++){
                int rem = rg.nextInt(clust.size());
                remAcc = clust.getAccessions().remove(rem);
                start.add(remAcc);
                tmp.add(remAcc);
            }
            // Restore cluster
            clust.getAccessions().addAll(tmp);
        }

        return start;
        
    }

    private static void selectParents(List<Replica> replicas, List<List<Accession>> parents,
                                        int nrOfParents, int T, Random rg){

        selectParents(replicas, parents, nrOfParents, T, rg, null);

    }

    private static void selectParents(List<Replica> replicas, List<List<Accession>> parents,
                                        int nrOfParents, int T, Random rg, String skipType){
        double bestParScore, parScore;
        List<Accession> bestPar = null, nextPar;
        String bestParType = null;
        parents.clear();
        for(int i=0; i<nrOfParents; i++){
            // Tournament selection: choose T random, select best.
            // Repeat for each parent.
            bestParScore = -Double.MAX_VALUE;
            for(int j=0; j<T; j++){
                // Choose random individual
                int k = rg.nextInt(replicas.size());
                Replica rep = replicas.get(k);
                if(skipType == null || !rep.shortType().equals(skipType)){
                    nextPar = rep.getBestCore();
                    parScore = rep.getBestScore();
                    // Check if new best parent found
                    if(parScore > bestParScore){
                        bestParScore = parScore;
                        bestPar = nextPar;
                        bestParType = rep.type();
                    }
                } else {
                    j--; // ignore cases when a skipped replica was drawn
                }
            }
            parents.add(bestPar);
            //System.out.println("Parent: " + bestParType + ", score: " + bestParScore);
        }
    }

    private static void createNewChildren(List<List<Accession>> parents, List<List<Accession>> children, Random rg){

        List<Accession> parent1, parent2, child;
        int p1size, p2size, childSize;

        children.clear();
        for(int i=0; i<parents.size()-1; i+=2){

            // Cross-over

            // Get parents (make sure parent1 is the SMALLEST one)
            if(parents.get(i).size() <= parents.get(i+1).size()){
                parent1 = parents.get(i);
                p1size = parent1.size();
                parent2 = parents.get(i+1);
                p2size = parent2.size();

            } else {
                parent1 = parents.get(i+1);
                p1size = parent1.size();
                parent2 = parents.get(i);
                p2size = parent2.size();
            }
            // Create child (cross-over)
            childSize = p1size + rg.nextInt(p2size-p1size+1);
            child = new ArrayList<Accession>(childSize);


            
            // Get some parts of parent1
            for(int j=0; j<p1size; j++){
                // Randomly decide wether to add the accession at
                // index j in parent1 to the child (probability of 50%)
                if(rg.nextBoolean()){
                    child.add(parent1.get(j));
                }
            }
            // Get remaining parts from parent2
            int j=rg.nextInt(p2size); // Start looping over parent2 at random index
            // While child not full: add new accessions from parent2
            Accession a;
            while(child.size() < childSize){
                // Add new accession from parent2 if not already present in child
                a = parent2.get(j);
                if(!child.contains(a)){
                    child.add(a);
                }
                j = (j+1)%p2size;
            }

            // Add new child to list
            children.add(child);
        }
    }

    private static final Set<Accession> childSet = new HashSet<Accession>();

    private static void createNewStratifiedChildren(List<List<Accession>> parents, List<List<Accession>> children,
                                                    Random rg, Clustering clustering){

        List<Accession> parent1, parent2, child;
        int p1size, p2size, pminSize, pmaxSize, childSize;
        Iterator<AccessionCluster> itr;
        AccessionCluster clust;

        children.clear();
        for(int i=0; i<parents.size()-1; i+=2){

            // Cross-over

            // Get parents
            parent1 = parents.get(i);
            p1size = parent1.size();
            parent2 = parents.get(i+1);
            p2size = parent2.size();

            pminSize = Math.min(p1size, p2size);
            pmaxSize = Math.max(p1size, p2size);

            // Create child (stratified merge)
            childSize = pminSize + rg.nextInt(pmaxSize - pminSize + 1);
            childSet.clear();
            childSet.addAll(parent1);
            childSet.addAll(parent2);

            // Cluster childset
            clustering.reset();
            clustering.setDesiredClusters(childSize);
            for(Accession a : childSet){
                clustering.addAccession(a);
            }

            // Stratified sampling from clustered childset to create child
            // --> Add one accession from each cluster
            child = new ArrayList<Accession>(childSize);
            itr = clustering.getClusters().iterator();
            while(itr.hasNext()){
                clust = itr.next();
                int sampleSize = 1;
                // Randomly sample accessions from cluster
                for(int k=0; k<sampleSize; k++){
                    int rem = rg.nextInt(clust.size());
                    child.add(clust.getAccessions().remove(rem));
                }
            }

            // Add new child to list
            children.add(child);
        }

    }

    public static AccessionCollection lrSearch(AccessionCollection ac, PseudoMeasure pm, int sampleMin, int sampleMax,
                                               int l, int r, boolean exhaustiveFirstPair) {

        List<Accession> core, unselected;
        List<Accession> accessions = ac.getAccessions();
        double score, newScore, bestNewScore, dscore;
        String cacheID = PseudoMeasure.getUniqueId();
        int bestAddIndex = -1, bestRemIndex = -1;
        Stack<SinglePerturbation> history = new Stack<SinglePerturbation>();

        ThreadMXBean tb = ManagementFactory.getThreadMXBean();
	double sTime = tb.getCurrentThreadCpuTime();

        boolean skipadd = false;
        if (l>r) {
            // Start with minimal set, stepwise increase size
            if(exhaustiveFirstPair){
                // Because distance measures require at least two accessions to be
                // computable, exhaustively select the best core set of size 2
                core = exhaustiveSearch(ac, pm, 2, 2, false).getAccessions();
            } else {
                // Random first pair, to save computational cost: this transforms the
                // deterministic lr search into a semi-random method
                core = CoreSubsetSearch.randomSearch(ac, 2, 2).getAccessions();
            }
            unselected = new ArrayList<Accession>(accessions);
            unselected.removeAll(core);
        } else {
            // Start with full set, stepwise decrease size
            core = new ArrayList<Accession>(accessions);
            unselected = new ArrayList<Accession>();
            skipadd = true;
        }
        score = pm.calculate(core, cacheID);
        bestNewScore = score;
        System.out.println("best score: " + score + "\tsize: " + core.size() +
                           "\ttime: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

        boolean cont = true;
        while(cont){
            // Add l new accessions to core
            if(!skipadd){
                for(int i=0; i<l; i++){
                    // Search for best new accession
                    bestNewScore = -Double.MAX_VALUE;
                    for(int j=0; j<unselected.size(); j++){
                        Accession add = unselected.get(j);
                        core.add(add);
                        newScore = pm.calculate(core, cacheID);
                        if(newScore > bestNewScore){
                            bestNewScore = newScore;
                            bestAddIndex = j;
                        }
                        core.remove(core.size()-1);
                    }
                    // Add best new accession
                    core.add(unselected.remove(bestAddIndex));
                    history.add(new Addition(bestAddIndex));
                }
                skipadd=false;
            }
            // Remove r accessions from core
            for(int i=0; i<r; i++){
                // Search for worst accession
                bestNewScore = -Double.MAX_VALUE;
                for(int j=0; j<core.size(); j++){
                    Accession rem = core.remove(j);
                    newScore = pm.calculate(core, cacheID);
                    if(newScore > bestNewScore){
                        bestNewScore = newScore;
                        bestRemIndex = j;
                    }
                    core.add(j, rem);
                }
                // Remove worst accession
                unselected.add(core.remove(bestRemIndex));
                history.add(new Deletion(bestRemIndex));
            }

            dscore = bestNewScore - score;
            score = bestNewScore;

            // Determine whether to continue search
            if(l > r){
                // Increasing core size
                if (core.size() > sampleMin && dscore <= 0) {
                    cont = false; // Equal or worse score and size increased
                    // Restore previous core
                    for(int i=0; i<l+r; i++){
                        history.pop().undo(core, unselected);
                    }
                } else if(core.size()+l-r > sampleMax){
                    cont = false; // Max size reached
                }

            } else {
                // Decreasing core size
                if (core.size() < sampleMax && dscore < 0){
                    cont = false; // Worse score
                    // Restore previous core
                    for(int i=0; i<l+r; i++){
                        history.pop().undo(core, unselected);
                    }
                } else if (core.size()+l-r < sampleMin){
                    cont = false; // Min size reached
                }
            }

            // Print core information
            System.out.println("best score: " + score + "\tsize: " + core.size() +
                               "\ttime: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);
        }

        System.out.println("### End time: " + (tb.getCurrentThreadCpuTime() - sTime)/1000000000);

        AccessionCollection bestCore = new AccessionCollection();
	bestCore.add(core);

	return bestCore;
        
    }

    public static AccessionCollection lrSearch(AccessionCollection ac, PseudoMeasure pm, int sampleMin, int sampleMax, int l, int r) {

        return lrSearch(ac, pm, sampleMin, sampleMax, l, r, true);

    }

    public static AccessionCollection semiLrSearch(AccessionCollection ac, PseudoMeasure pm, int sampleMin, int sampleMax, int l, int r) {

        return lrSearch(ac, pm, sampleMin, sampleMax, l, r, false);

    }

    public static AccessionCollection forwardSelection(AccessionCollection ac, PseudoMeasure pm, int sampleMin, int sampleMax) {

        return lrSearch(ac, pm, sampleMin, sampleMax, 1, 0);

    }

    public static AccessionCollection semiForwardSelection(AccessionCollection ac, PseudoMeasure pm, int sampleMin, int sampleMax) {

        return semiLrSearch(ac, pm, sampleMin, sampleMax, 1, 0);

    }

    public static AccessionCollection backwardSelection(AccessionCollection ac, PseudoMeasure pm, int sampleMin, int sampleMax) {

        return lrSearch(ac, pm, sampleMin, sampleMax, 0, 1);

    }

}
