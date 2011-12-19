/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.search;

import org.apache.commons.math.util.MathUtils;

/**
 *
 * @author hermandebeukelaer
 */
public class KSubsetGenerator {

    // nr of elements in subset
    private int k;
    // nr of elements in entire set
    private int n;


    public KSubsetGenerator(int k, int n){
        this.k = k;
        this.n = n;
    }

    public long getNrOfKSubsets(){
        return MathUtils.binomialCoefficient(n, k);
    }

    public Integer[] first(){
        // Generate first k-subset
        Integer[] first = new Integer[k];
        for(int i=0; i<k; i++){
            first[i] = i+1;
        }
        return first;
    }

    public void successor(Integer[] T){
        Integer[] S = new Integer[k+2];
        S[0] = 0; // t_0
        for(int i = 1; i < k+1; i++) {
            S[i] = T[i-1];
        }
        S[k+1] = n+1; // t_{k+1}

        int j = 1;
        while(j <= k && S[j] == j) {
            j++;
        }
        if(k % 2 != j % 2) {
            if(j == 1) {
                S[1]--;
            } else {
                S[j-1] = j;
                S[j-2] = j-1;
            }
        } else {
            if(S[j+1] != S[j]+1) {
                S[j-1] = S[j];
                S[j] = S[j]+1;
            } else {
                S[j+1] = S[j];
                S[j] = j;
            }
        }

        for(int i = 1; i < k+1; i++) {
            T[i-1] = S[i];
        }
    }
}
