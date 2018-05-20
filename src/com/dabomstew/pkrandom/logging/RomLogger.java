package com.dabomstew.pkrandom.logging;

public interface RomLogger {

    /**
     * Mark randomization stats
     * 
     * @param elapsedTime
     *            Time since randomization started
     * @param callsSinceSeed
     *            Number of Random calls for randomization
     */
    void markCompletion(long elapsedTime, int callsSinceSeed);

    void generateLogFile();

}