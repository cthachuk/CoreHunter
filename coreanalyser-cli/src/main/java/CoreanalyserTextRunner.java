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

import java.util.Map;

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

/**
 * A simple text based driver for Corehunter.
 *
 * @author Chris Thachuk <chris.thachuk@gmail.com>
 */
public final class CoreanalyserTextRunner {
    private final String[] measureNames = {"MR", "MRmin", "CE", "CEmin", "SH", "HE", "NE", "PN", "CV"};

    private Options opts;
    private String[] collectionFiles;
    private int precision;

    /**
     * 
     */
    public CoreanalyserTextRunner() {
	opts = new Options();
	collectionFiles = null;
	precision = 5;
    }

    public void run(String[] args) {
	setupOptions();
	if (!parseOptions(args)) {
	    showUsage();
	}

	System.out.print("\n                ");

	double total[] = new double[measureNames.length];
	double min[] = new double[measureNames.length];
	double max[] = new double[measureNames.length];

	for(int i=0; i<measureNames.length; ++i) {
	    total[i] = 0.0;
	    min[i] = Double.MAX_VALUE;
	    max[i] = 0.0;
	    System.out.format("%" + (precision+4) + "s ", measureNames[i]);
	}
	System.out.println("");

	for(int i=0; i<collectionFiles.length; i++) {

	    // try to create dataset
	    SSRDataset ds = SSRDataset.createFromFile(collectionFiles[i]);
	    if(ds == null) {
		System.err.println("");
		System.err.println("Problem parsing dataset file.  Aborting.");
		System.exit(0);
	    }

	    System.out.format("%15s:", collectionFiles[i]);

	    // create an accession collection
	    AccessionCollection ac = new AccessionCollection();
	    ac.addDataset(ds);

            // create a pseudo-index and add user specified measure to it, with respective weights
	    PseudoMeasure pm = new PseudoMeasure();
	    for(int j=0; j<measureNames.length; j++) {
		String measure = measureNames[j];
		try {
		    pm.addMeasure(MeasureFactory.createMeasure(measure, ac.size()), 1.0);
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
	    
	    Map<String, Double> scores = pm.componentScores(ac.getAccessions());
	    
	    for(int j=0; j<measureNames.length; j++) {
		Double score = scores.get(measureNames[j]);
		double val = score.doubleValue();
		total[j] += val;

		if(val > max[j]) {
		    max[j] = val;
		}

		if(val < min[j]) {
		    min[j] = val;
		}

		System.out.format("%" + (precision+4) + "." + precision + "f ", val);
	    }
	    System.out.println("");
	    Accession.reset();
	}
	
	System.out.print("\n            min:");
	for(int j=0; j<measureNames.length; j++) {
	    System.out.format("%" + (precision+4) + "." + precision + "f ", min[j]);
	}
	System.out.print("\n            max:");
	for(int j=0; j<measureNames.length; j++) {
	    System.out.format("%" + (precision+4) + "." + precision + "f ", max[j]);
	}
	System.out.print("\n           mean:");
	for(int j=0; j<measureNames.length; j++) {
	    System.out.format("%" + (precision+4) + "." + precision + "f ", total[j]/collectionFiles.length);
	}
	System.out.println("");
	
    }

    private void setupOptions() {
	// set up the misc option group
	opts.addOption( new Option("help", "print this message") );
	opts.addOption( new Option("verbose", "be extra verbose") );
	opts.addOption( OptionBuilder.withArgName("decimal_count")
			    .hasArg()
			    .withDescription("report values with decimal_count precision")
			    .create("precision") );

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

	    // check for -precision
	    if (cl.hasOption("precision")) {
		try {
		    precision = Integer.parseInt(cl.getOptionValue("precision"));
		    if (precision<0 || precision>15) throw new NumberFormatException();
		} catch(NumberFormatException nfe) {
		    System.err.println("\nprecision must be a positive integer value in the range [0,15]");
		    return false;
		}
	    } 

	    // make sure at least one file is specified
	    if (cl.getArgs().length == 0) {
		System.err.println("\nyou must specify at least one file to analyse");
		return false;
	    } 

	    // grab the filenames
	    collectionFiles = cl.getArgs();
	} catch (ParseException e) {
	    System.err.println("");
	    System.err.println( e.getMessage() );
	    return false;
	}
	    
	return true;
    }

    private void showUsage() {
	System.out.println("");
	System.out.println("usage: coreanalyser [options] <file1> [<file2> [<file3>...]]");
	System.out.println("");
	System.out.println("\texample: The following command will analyse the file core1.csv and the file core2.csv");
	System.out.println("");
	System.out.println("\tcoreanalyser core1.csv core2.csv");
	System.out.println("");
	    
	HelpFormatter f = new HelpFormatter();
	f.setSyntaxPrefix("");

	f.printHelp("options:", opts);
	System.out.println("");
	System.exit(0);
    }	

    private void showVersion() {
	System.out.println("");
	System.out.println("Coreanalyser version: 1.0beta");
	System.out.println("");
    }

    public static void main(String[] args) {
	CoreanalyserTextRunner coreanalyser = new CoreanalyserTextRunner();
	coreanalyser.run(args);
	
	System.exit(0);
    }
}

