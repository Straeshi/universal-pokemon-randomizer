package com.dabomstew.pkrandom.logging;

public interface RomLogger {

    // TODO remove all logBlah methods because they should be TextRomLogger specific
    void logMoveUpdates();

    void logChangedEvolutions();

    void logEvolutions();

    void logMovesets();

    void logMoveTutorMoves();

    void logTMContents();

    void logInGameTrades();

    void logBaseStatAndTypeChanges();

    void logStarters();

    void logWildEncounters();

    void logTrainers();

    void logStaticPokemon();

    void logMoveChanges();

    /**
     * Mark randomization stats
     * 
     * @param elapsedTime
     *            Time since randomization started
     * @param callsSinceSeed
     *            Number of Random calls for randomization
     */
    void markCompletion(long elapsedTime, int callsSinceSeed);

    void logMiscTweaks();

    void generateLogFile();

}