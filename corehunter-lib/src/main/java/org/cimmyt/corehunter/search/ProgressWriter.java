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

package org.cimmyt.corehunter.search;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.Timer;
import java.util.TimerTask;

/**
 *
 * @author hermandebeukelaer
 */
public class ProgressWriter extends TimerTask {

    private Timer timer;
    private long period;
    private double curScore;

    private File file;
    private FileWriter writer;

    private long startTime;

    private boolean firstScoreSet;
    private boolean stopped;

    public ProgressWriter(String filePath, long period){
        this.period = period;
        
        timer = new Timer();
        file = new File(filePath);

        firstScoreSet = false;
        stopped = false;
    }

    public void start(){
        try {
            // erase file
            FileOutputStream erasor = new FileOutputStream(file);
            erasor.write(new byte[0]);
            erasor.close();
            // create writer
            writer = new FileWriter(file);
        } catch (IOException ex) {
            System.err.println("Problem accessing progress file: " + ex);
            System.exit(1);
        }

	startTime = System.currentTimeMillis();
    }

    public void stop(){
        stopped = true;
        // write final score and close writer
        try {
            long curTime = System.currentTimeMillis();
            writer.write((curTime-startTime)/1000.0 + "\t" + curScore + "\n");
            writer.flush();
            writer.close();
        } catch (IOException ex){
            System.err.println("Problem writing to progress file: " + ex);
            System.exit(1);
        }
    }

    public void updateScore(double newScore){
        if(!stopped){
            curScore = newScore;
            if(!firstScoreSet){
                timer.scheduleAtFixedRate(this, 0, period);
                firstScoreSet = true;
            }
        }
    }

    public void run(){
        if(firstScoreSet && !stopped){
            try {
                long curTime = System.currentTimeMillis();
                writer.write((curTime-startTime)/1000.0 + "\t" + curScore + "\n");
            } catch (IOException ex) {
                System.err.println("Problem writing to progress file: " + ex);
                System.exit(1);
            }
        }
    }

}
