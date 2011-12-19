/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.cimmyt.corehunter.measures;

/**
 *
 * @author hermandebeukelaer
 */
public enum DistanceMeasureType {

    MEAN_DISTANCE("",""), // for optimzing minimum distance

    MIN_DISTANCE("min", " (minimum)"); // for optimizing mean distance


    private final String nameSuffix;
    private final String descriptionSuffix;

    DistanceMeasureType(String nameSuffix, String descriptionSuffix) {
        this.nameSuffix = nameSuffix;
        this.descriptionSuffix = descriptionSuffix;
    }

    public String getNameSuffix(){
        return nameSuffix;
    }
    
    public String getDescriptionSuffix(){
        return descriptionSuffix;
                
    }

}
