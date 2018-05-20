package com.dabomstew.pkrandom.logging;

import java.util.List;

import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.IngameTrade;
import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

public abstract class AbstractRomLogger implements RomLogger {

    protected final Settings settings;
    protected final RomHandler romHandler;
    protected final List<Pokemon> oldStatics;
    protected final List<IngameTrade> oldTrades;
    protected final List<Move> moves;
    protected final List<Integer> oldMtMoves;
    protected long elapsedTime;
    protected int callsSinceSeed;

    public AbstractRomLogger(RomHandler romHandler, Settings settings) {
        this.romHandler = romHandler;
        this.settings = settings;
        this.oldStatics = romHandler.getStaticPokemon();
        this.oldTrades = romHandler.getIngameTrades();
        this.moves = romHandler.getMoves();
        this.oldMtMoves = romHandler.getMoveTutorMoves();
    }

    @Override
    public void markCompletion(long elapsedTime, int callsSinceSeed) {
        this.elapsedTime = elapsedTime;
        this.callsSinceSeed = callsSinceSeed;
    }

}