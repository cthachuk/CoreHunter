/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.search;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;
import org.cimmyt.corehunter.Accession;
import org.cimmyt.corehunter.measures.GroupAverageClusterDistance;
import org.cimmyt.corehunter.measures.ModifiedRogersDistance;
import org.cimmyt.corehunter.measures.PseudoMeasure;

/**
 *
 * @author hermandebeukelaer
 */
public class GeneticPopulation {

    // Size of population
    private int popSize;
    // List of all individuals in this population, including their scores
    private List<CoreScorePair> population;

    private CoreScorePairComparator cmp = new CoreScorePairComparator();


    // Minimum and maximum size of possible core sets (individuals in population)
    private int minCoreSize;
    private int maxCoreSize;
    // List of all accessions in dataset
    private List<Accession> accessions;
    // PseudoMeasure to evaluate core sets
    private PseudoMeasure pm;
    // Number of children created for forming new generation
    private int nrOfChildren;
    // List containing parents for next generation
    private List<List<Accession>> parents;
    // Set to contain combined accessions from 2 parents
     private Set<Accession> childSet;
    // Tournament size for tournament selection of parents
    private int T;
    // Mutation rate: the probability that a newly created individual mutates
    private double mutationRate;

    // Random generator
    private Random rg = new Random();

    // Clustering
    private Clustering clustering;

    private final int RANDOM_SURVIVAL;

    /**
     * Create a GeneticPopulation and set all required parameters.
     * Pleas use init() afterwards to create an initial population.
     *
     * @param popSize
     * @param minCoreSize
     * @param maxCoreSize
     * @param accessions
     * @param pm
     * @param nrOfChildren
     * @param tournamentSize
     * @param mutationRate
     */
    public GeneticPopulation(int popSize, int minCoreSize, int maxCoreSize, List<Accession> accessions,
                                PseudoMeasure pm, int nrOfChildren, int tournamentSize, double mutationRate){
        this.popSize = popSize;
        this.RANDOM_SURVIVAL = Math.max(1, nrOfChildren/2);

        this.minCoreSize = minCoreSize;
        this.maxCoreSize = maxCoreSize;
        this.accessions = accessions;
        this.pm = pm;
        this.nrOfChildren = nrOfChildren;

        parents = new ArrayList<List<Accession>>(2*nrOfChildren);
        T = tournamentSize;
        this.mutationRate = mutationRate;

        clustering = new Clustering(maxCoreSize, new GroupAverageClusterDistance(new ModifiedRogersDistance(accessions.size())));
        childSet = new HashSet<Accession>(minCoreSize);
    }

    /**
     * Create an initial population with randomly selected core sets.
     */
    public void init(){
        // Create population
        population = new ArrayList<CoreScorePair>(popSize);
        for(int i=0; i<popSize; i++){
            // Random core size min <= s <= max
            int s = minCoreSize + rg.nextInt(maxCoreSize-minCoreSize+1);

            List<Accession> core;
            if(i%4 == 0){
                // Create random core
                core = new ArrayList<Accession>(s);
                for(int j=0; j<s; j++){
                    int k = rg.nextInt(accessions.size());
                    core.add(accessions.remove(k));
                }
                // Restore accession data set
                accessions.addAll(core);
            } else {
                // Stratified sampling of core
                clustering.reset();
                clustering.setDesiredClusters(s);
                for(Accession a : accessions){
                    clustering.addAccession(a);
                }
                core = CoreSubsetSearch.sampleStratifiedStart(clustering.getClusters(), rg);
            }

            // Store core in population
            double score = pm.calculate(core); // no point of caching, random (unrelated) sets
            population.add(new CoreScorePair(core, score));
        }
        // Sort population based on scores (descending, best core on top)
        sortPopulation();
    }

    public List<Accession> getBestCore(){
        return population.get(0).getCore();
    }

    public double getBestScore(){
        return population.get(0).getScore();
    }

    /**
     * Create the next generation:
     *  - select parents
     *  - combine their characteristics into children (cross-over)
     *  - mutate new children
     *  - survival selection
     */
    public void nextGen(){
        selectParents();
        crossoverAndMutation();
        survivalSelection();
    }

    private void selectParents(){
        double bestParScore, parScore;
        List<Accession> bestPar=null, nextPar;
        parents.clear();
        for(int i=0; i<2*nrOfChildren; i++){
            // Tournament selection: choose T random, select best.
            // Repeat for each parent.
            bestParScore = -Double.MAX_VALUE;
            for(int j=0; j<T; j++){
                // Choose random individual
                int k = rg.nextInt(popSize);
                nextPar = population.get(k).getCore();
                parScore = population.get(k).getScore();
                // Check if new best parent found
                if(parScore > bestParScore){
                    bestParScore = parScore;
                    bestPar = nextPar;
                }
            }
            parents.add(bestPar);
        }
    }

    private void crossoverAndMutation(){
        List<Accession> parent1, parent2, child;
        int p1size, p2size, childSize;
        int pmaxSize, pminSize;
        Iterator<AccessionCluster> itr;
        AccessionCluster clust;

        for(int i=0; i<parents.size()-1; i+=2){

            // Cross-over

            // Get parents (make sure parent1 is SMALLEST one)
            //if(parents.get(i).size() <= parents.get(i+1).size()){

                parent1 = parents.get(i);
                p1size = parent1.size();
                parent2 = parents.get(i+1);
                p2size = parent2.size();

                pmaxSize = Math.max(p1size, p2size);
                pminSize = Math.min(p1size, p2size);

            /*} else {
                parent1 = parents.get(i+1);
                p1size = parent1.size();
                parent2 = parents.get(i);
                p2size = parent2.size();
            }*/

            // Create child (cross-over)
            //childSize = p1size + rg.nextInt(p2size-p1size+1);
            childSize = pminSize + rg.nextInt(pmaxSize-pminSize+1);
            //child = new ArrayList<Accession>(childSize);
            childSet.clear();

            // Add accessions from both parents to child, removing duplicates
            childSet.addAll(parent1);
            childSet.addAll(parent2);

            if(childSet.size() > childSize){
                //System.out.println("Check! Diff: " + (childSet.size() - childSize));
            }

            // Cluster child
            clustering.reset();
            clustering.setDesiredClusters(childSize);
            for(Accession a : childSet){
                clustering.addAccession(a);
            }

            // Sample from clusters
            child = new ArrayList<Accession>(childSize);
            child = new ArrayList<Accession>(childSize);
            itr = clustering.getClusters().iterator();
            while(itr.hasNext()){
                clust = itr.next();
                //int sampleSize = (int) Math.ceil(((double)clust.size()/(double)childSet.size()) * childSize);
                //sampleSize = Math.min(sampleSize, clust.size());
                int sampleSize = 1;
                // Randomly sample accessions from cluster
                for(int k=0; k<sampleSize; k++){
                    int rem = rg.nextInt(clust.size());
                    child.add(clust.getAccessions().remove(rem));
                }
            }

            // Remove possible excessive accessions because of rounded cluster sample sizes
            /*while(child.size() > childSize){
                int rem = rg.nextInt(child.size());
                child.remove(rem);
            }*/
            
            
            /*
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
            */
            
            // Mutation of child
            
            if(rg.nextDouble() <= mutationRate){
                double r = rg.nextDouble();
                if(r <= 0.33 && child.size() < maxCoreSize){
                    // Randomly add new accession
                    accessions.removeAll(child);
                    child.add(accessions.remove(rg.nextInt(accessions.size())));
                    accessions.addAll(child);
                } else if (r <= 0.66 && child.size() > minCoreSize){
                    // Randomly remove accession
                    child.remove(rg.nextInt(child.size()));
                } else {
                    // Randomly swap accession
                    accessions.removeAll(child);
                    int add = rg.nextInt(accessions.size());
                    int rem = rg.nextInt(child.size());
                    Accession rema = child.set(rem, accessions.get(add));
                    accessions.set(add, rema);
                    accessions.addAll(child);
                }
            }

            // Add child to population (and its score)
            double score = pm.calculate(child);
            population.add(new CoreScorePair(child, score));
        }
    }

    private void survivalSelection(){
        
        // Sort new population according to scores (descending)
        sortPopulation();
        // Keep only best cores, remove others from population.
        // Possibly include some worse cores too.
        while(population.size() > (popSize + RANDOM_SURVIVAL)){
            population.remove(population.size()-1);
        }
        
        // Randomly remove individuals until population size reached.
        // NOTE: protect best solution from being removed!
        while(population.size() > popSize){
            population.remove(rg.nextInt(population.size()-1)+1);
        }
        
    }

    private void sortPopulation(){
        Collections.sort(population, cmp);
    }

    private class CoreScorePair{

        private List<Accession> core;
        private double score;

        public CoreScorePair(List<Accession> core, double score){
            this.core = core;
            this.score =  score;
        }

        public List<Accession> getCore() {
            return core;
        }

        public void setCore(List<Accession> core) {
            this.core = core;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

    }

    private class CoreScorePairComparator implements Comparator<CoreScorePair>{
        // Compare pair by comparing associated scores (descending).
        // In case of equal score: compare by core size (ascending).
        public int compare(CoreScorePair o1, CoreScorePair o2) {
            if(o1.getScore() == (o2.getScore())){
                int size1 = o1.getCore().size();
                int size2 = o2.getCore().size();
                if(size1 < size2){
                    return -1;
                } else if(size1 == size2){
                    return 0;
                } else {
                    return 1;
                }
            } else {
                if(o1.getScore() < o2.getScore()){
                    return 1;
                } else {
                    return -1;
                }
            }
        }
    }

}
