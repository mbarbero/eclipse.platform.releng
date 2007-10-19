/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.test.internal.performance.results;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.test.internal.performance.InternalDimensions;

/**
 * Class to handle results for an Eclipse performance test box
 * (called a <i>configuration</i>).
 * 
 * It gives access to results for each build on which this configuration has been run.
 * 
 * @see BuildResults
 */
public class ConfigResults extends AbstractResults {
	BuildResults baseline, current;
	boolean baselined = false, valid = false;

public ConfigResults(AbstractResults parent, int id) {
	super(parent, id);
	this.name = parent.getPerformance().sortedConfigNames[id];
	this.print = parent.print;
}

/**
 * Returns the baseline build name used to compare results with.
 *
 * @return The name of the baseline build
 * @see #getBaselineBuildResults()
 */
public String getBaselineBuildName() {
	return this.baseline.getName();
}

/**
 * Returns the baseline build results.
 * <p>
 * This build is currently the last reference build which has performance
 * 
 * @return The baseline build results.
 * @see BuildResults
 */
public BuildResults getBaselineBuildResults() {
	return this.baseline;
}

/**
 * Returns the build results matching a given pattern.
 *
 * @param buildPattern The pattern of searched builds
 * @return The list of the builds which names match the given pattern.
 * 	The list is ordered by build results date.
 */
public List getBuilds(String buildPattern) {
	List builds = new ArrayList();
	int size = size();
	for (int i=0; i<size; i++) {
		BuildResults buildResults = (BuildResults) this.children.get(i);
		if (buildResults.match(buildPattern)) {
			builds.add(buildResults);
		}
	}
	return builds;
}

/**
 * Returns a list of build results which names starts with one of the given prefixes.
 *
 * @param prefixes List of expected prefixes
 * @return A list of builds which names start with one of the given patterns.
 */
public List getBuildsMatchingPrefixes(List prefixes) {
	List builds = new ArrayList();
	int size = size();
	int length = prefixes.size();
	for (int i=0; i<size; i++) {
		AbstractResults buildResults = (AbstractResults) this.children.get(i);
		String buildName = buildResults.getName();
		for (int j=0; j<length; j++) {
			if (buildName.startsWith((String)prefixes.get(j))) {
				builds.add(buildResults);
			}
		}
	}
	return builds;
}

/**
 * Return the deviation value and its associated standard error for the default dimension
 * (currently {@link InternalDimensions#ELAPSED_PROCESS}).
 *
 * @return an array of double. First number is the deviation itself and
 * 	the second is the standard error.
 */
public double[] getCurrentBuildDeviation() {
	int dim_id = SUPPORTED_DIMS[0].getId();
	double baselineValue = this.baseline.getValue(dim_id);
	double currentValue = this.current.getValue(dim_id);
	double deviation = (currentValue - baselineValue) / baselineValue;
	if (Double.isNaN(deviation)) {
		return new double[] { Double.NaN, Double.NaN };
	}
	long baselineCount = this.baseline.getCount(dim_id);
	long currentCount = this.current.getCount(dim_id);
	if (baselineCount == 1 || currentCount == 1) {
		return new double[] { deviation, Double.NaN };
	}
	double baselineError = this.baseline.getError(dim_id);
	double currentError = this.current.getError(dim_id);
	double stderr = Double.isNaN(baselineError)
			? currentError / baselineValue
			: Math.sqrt(baselineError*baselineError + currentError*currentError) / baselineValue;
	return new double[] { deviation, stderr };
}

/**
 * Returns the current build name.
 *
 * @return The name of the current build
 * @see #getCurrentBuildResults()
 */
public String getCurrentBuildName() {
	return this.current.getName();
}

/**
 * Returns the current build results.
 * <p>
 * This build is currently the last integration or nightly
 * build which has performance results in the database.
 * It may differ from the {@link PerformanceResults#getName()}.
 * 
 * @return The current build results.
 * @see BuildResults
 */
public BuildResults getCurrentBuildResults() {
	return this.current;
}

/**
 * Get all dimension builds statistics for a given list of build prefixes
 * and a given dimension.
 *
 * @param prefixes List of prefixes to filter builds. If <code>null</code>
 * 	then all the builds are taken to compute statistics.
 * @param dim_id The id of the dimension on which the statistics must be computed
 * @return An array of double built as follows:
 * 	- 0:	numbers of values
 * 	- 1:	mean of values
 * 	- 2:	standard deviation of these values
 * 	- 3:	coefficient of variation of these values
 */
public double[] getStatistics(List prefixes, int dim_id) {
	int size = size();
	int length = prefixes == null ? 0 : prefixes.size();
	int count = 0;
	double mean=0, stddev=0, variation=0;
	double[] values = new double[size];
	count = 0;
	mean = 0.0;
	for (int i=0; i<size; i++) {
		BuildResults buildResults = (BuildResults) children.get(i);
		String buildName = buildResults.getName();
		if (prefixes == null) {
			double value = buildResults.getValue(dim_id);
			values[count] = value;
			mean += value;
			count++;
		} else {
			for (int j=0; j<length; j++) {
				if (buildName.startsWith((String)prefixes.get(j))) {
					double value = buildResults.getValue(dim_id);
					values[count] = value;
					mean += value;
					count++;
				}
			}
		}
	}
	mean /= count;
	for (int i=0; i<count; i++) {
		stddev += Math.pow(values[i] - mean, 2);
	}
	stddev = Math.sqrt((stddev / (count - 1)));
	variation = Math.round(((stddev) / mean) * 100 * 100) / 100;
	return new double[] { count, mean, stddev, variation };
}

/**
 * Returns whether the configuration has results for the performance
 * baseline build or not.
 * 
 * @return <code>true</code> if the configuration has results
 * 	for the performance baseline build, <code>false</code> otherwise.
 */
public boolean isBaselined() {
	return this.baselined;
}

/**
 * Returns whether the configuration has results for the performance
 * current build or not.
 * 
 * @return <code>true</code> if the configuration has results
 * 	for the performance current build, <code>false</code> otherwise.
 */
public boolean isValid() {
	return this.valid;
}

/**
 * Returns the 'n' last nightly build names.
 *
 * @param n Number of last nightly builds to return
 * @return Last n nightly build names preceding current.
 */
public List lastNightlyBuildNames(int n) {
	List labels = new ArrayList();
	for (int i=size()-2; i>=0; i--) {
		AbstractResults buildResults = (AbstractResults) this.children.get(i);
		String buildName = buildResults.getName();
		if (buildName.startsWith("N")) { //$NON-NLS-1$
			labels.add(buildName);
			if (labels.size() >= n) break;
		}
	}
	return labels;
}

/*
 * Read all configuration builds results data from the given stream.
 */
void readData(DataInputStream stream) throws IOException {
	int size = stream.readInt();
	for (int i=0; i<size; i++) {
		BuildResults buildResults = new BuildResults(this);
		buildResults.readData(stream);
		addChild(buildResults, true);
	}
}

/*
 * Set the configuration value from database information
 */
void setValue(int build_id, int dim_id, int step, long value) {
	BuildResults buildResults = (BuildResults) getResults(build_id);
	if (buildResults == null) {
		buildResults = new BuildResults(this, build_id);
		addChild(buildResults, true);
	}
	buildResults.setValue(dim_id, step, value);
}

/*
 * Update configuration results read locally with additional database information.
 */
void update() {

	// Get performance results builds name
	PerformanceResults perfResults = getPerformance();
	String baselineBuildName = perfResults.getBaselineName();
	String currentBuildName = perfResults.getName();

	// Set baseline and current builds
	int size = size();
	for (int i=0; i<size; i++) {
		BuildResults buildResults = (BuildResults) this.children.get(i);
		if (buildResults.values != null) {
			buildResults.cleanValues();
		}
		if (buildResults.getName().equals(baselineBuildName)) {
			this.baseline = buildResults;
			this.baselined = true;
		} else if (buildResults.getName().equals(currentBuildName)) {
			this.current = buildResults;
			this.valid = true;
		}
	}
	if (this.baseline == null) {
		this.baseline = (BuildResults) this.children.get(0);
	}
	if (this.current == null) {
		this.current = (BuildResults) this.children.get(size()-1);
	}

	// Get current and baseline builds failures and summaries
	ScenarioResults scenarioResults = (ScenarioResults) this.parent;
	DB_Results.queryScenarioFailures(scenarioResults, this.name, this.current, this.baseline);
	DB_Results.queryScenarioSummaries(scenarioResults, this.name, this.current, this.baseline);
}

/*
 * Write all configuration builds results data into the given stream.
 */
void write(DataOutputStream stream) throws IOException {
	int size = size();
	stream.writeInt(this.id);
	stream.writeInt(size);
	for (int i=0; i<size; i++) {
		BuildResults buildResults = (BuildResults) this.children.get(i);
		buildResults.write(stream);
	}
}

}
