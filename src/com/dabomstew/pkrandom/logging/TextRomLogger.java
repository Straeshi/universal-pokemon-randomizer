package com.dabomstew.pkrandom.logging;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.dabomstew.pkrandom.MiscTweak;
import com.dabomstew.pkrandom.Settings;
import com.dabomstew.pkrandom.pokemon.Encounter;
import com.dabomstew.pkrandom.pokemon.EncounterSet;
import com.dabomstew.pkrandom.pokemon.Evolution;
import com.dabomstew.pkrandom.pokemon.EvolutionType;
import com.dabomstew.pkrandom.pokemon.IngameTrade;
import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.MoveLearnt;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Trainer;
import com.dabomstew.pkrandom.pokemon.TrainerPokemon;
import com.dabomstew.pkrandom.romhandlers.Gen1RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

/**
 * Basic text logger for rom changes
 * ripped out of Randomizer and RomHandler classes
 * TODO create "pokemon report" text logger
 * 
 * TODO create HTML reference logger <- the whole point really
 * 
 * @author Straeshi
 * 
 *
 */
public class TextRomLogger extends AbstractRomLogger {

    private static final String NEWLINE = System.getProperty("line.separator");

    private final PrintStream log;

    public TextRomLogger(RomHandler romHandler, Settings settings, PrintStream log) {
        super(romHandler, settings);
        this.log = log;
    }

    private void logMoveUpdates() {
        Map<Integer, boolean[]> moveUpdates = romHandler.getMoveUpdates();
        log.println("--Move Updates--");
        for (int moveID : moveUpdates.keySet()) {
            boolean[] changes = moveUpdates.get(moveID);
            Move mv = moves.get(moveID);
            List<String> nonTypeChanges = new ArrayList<String>();
            if (changes[0]) {
                nonTypeChanges.add(String.format("%d power", mv.power));
            }
            if (changes[1]) {
                nonTypeChanges.add(String.format("%d PP", mv.pp));
            }
            if (changes[2]) {
                nonTypeChanges.add(String.format("%.00f%% accuracy", mv.hitratio));
            }
            String logStr = "Made " + mv.name;
            // type or not?
            if (changes[3]) {
                logStr += " be " + mv.type + "-type";
                if (nonTypeChanges.size() > 0) {
                    logStr += " and";
                }
            }
            if (nonTypeChanges.size() > 0) {
                logStr += " have ";
                if (nonTypeChanges.size() == 3) {
                    logStr += nonTypeChanges.get(0) + ", " + nonTypeChanges.get(1) + " and " + nonTypeChanges.get(2);
                } else if (nonTypeChanges.size() == 2) {
                    logStr += nonTypeChanges.get(0) + " and " + nonTypeChanges.get(1);
                } else {
                    logStr += nonTypeChanges.get(0);
                }
            }
            log.println(logStr);
        }
        log.println();
    }

    private void logChangedEvolutions() {
        // Log changes now that we're done (to avoid repeats)

        if (settings.isChangeImpossibleEvolutions()) {
            log.println("--Removing Trade Evolutions--");
        }
        if (settings.isMakeEvolutionsEasier()) {
            log.println("--Condensed Level Evolutions--");
        }

        for (Evolution evol : romHandler.getChangedEvos()) {
            if (evol.type == EvolutionType.LEVEL) {
                log.println(String.format("%s now evolves into %s at level %d", evol.from.name, evol.to.name,
                        evol.extraInfo));
            } else if (evol.type == EvolutionType.STONE) {
                log.println(String.format("%s now evolves into %s using a %s", evol.from.name, evol.to.name,
                        romHandler.getItemNames()[evol.extraInfo]));
            }
            // System.out.println(evol.type);
            // FIXME check for gen 5 types
        }
        log.println();
    }

    private void logEvolutions() {
        if (settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM) {
            log.println("--Randomized Evolutions--");
            List<Pokemon> allPokes = romHandler.getPokemon();
            for (Pokemon pk : allPokes) {
                if (pk != null) {
                    int numEvos = pk.evolutionsFrom.size();
                    if (numEvos > 0) {
                        StringBuilder evoStr = new StringBuilder(pk.evolutionsFrom.get(0).to.name);
                        for (int i = 1; i < numEvos; i++) {
                            if (i == numEvos - 1) {
                                evoStr.append(" and " + pk.evolutionsFrom.get(i).to.name);
                            } else {
                                evoStr.append(", " + pk.evolutionsFrom.get(i).to.name);
                            }
                        }
                        log.println(pk.name + " now evolves into " + evoStr.toString());
                    }
                }
            }
            log.println();
        }

        logChangedEvolutions();

    }

    private void logMovesets() {
        if (settings.getMovesetsMod() == Settings.MovesetsMod.UNCHANGED) {
            log.println("Pokemon Movesets: Unchanged." + NEWLINE);
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("Pokemon Movesets: Metronome Only." + NEWLINE);
        } else {
            log.println("--Pokemon Movesets--");
            List<String> movesets = new ArrayList<String>();
            Map<Pokemon, List<MoveLearnt>> moveData = romHandler.getMovesLearnt();
            for (Pokemon pkmn : moveData.keySet()) {
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("%03d %-10s : ", pkmn.number, pkmn.name));
                List<MoveLearnt> data = moveData.get(pkmn);
                boolean first = true;
                for (MoveLearnt ml : data) {
                    if (!first) {
                        sb.append(", ");
                    }
                    try {
                        sb.append(moves.get(ml.move).name).append(" at level ").append(ml.level);
                    } catch (NullPointerException ex) {
                        sb.append("invalid move at level" + ml.level);
                    }
                    first = false;
                }
                movesets.add(sb.toString());
            }
            Collections.sort(movesets);
            for (String moveset : movesets) {
                log.println(moveset);
            }
            log.println();
        }
    }

    private void logMoveTutorMoves() {
        if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                && settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.RANDOM) {
            log.println("--Move Tutor Moves--");
            List<Integer> newMtMoves = romHandler.getMoveTutorMoves();
            for (int i = 0; i < newMtMoves.size(); i++) {
                log.printf("%s => %s" + NEWLINE, moves.get(oldMtMoves.get(i)).name, moves.get(newMtMoves.get(i)).name);
            }
            log.println();
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("Move Tutor Moves: Metronome Only." + NEWLINE);
        } else {
            log.println("Move Tutor Moves: Unchanged." + NEWLINE);
        }
    }

    private void logTMContents() {
        if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                && settings.getTmsMod() == Settings.TMsMod.RANDOM) {
            log.println("--TM Moves--");
            List<Integer> tmMoves = romHandler.getTMMoves();
            for (int i = 0; i < tmMoves.size(); i++) {
                log.printf("TM%02d %s" + NEWLINE, i + 1, moves.get(tmMoves.get(i)).name);
            }
            log.println();
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            log.println("TM Moves: Metronome Only." + NEWLINE);
        } else {
            log.println("TM Moves: Unchanged." + NEWLINE);
        }
    }

    private void logInGameTrades() {
        if (!(settings.getInGameTradesMod() == Settings.InGameTradesMod.UNCHANGED)) {
            log.println("--In-Game Trades--");
            List<IngameTrade> newTrades = romHandler.getIngameTrades();
            int size = oldTrades.size();
            for (int i = 0; i < size; i++) {
                IngameTrade oldT = oldTrades.get(i);
                IngameTrade newT = newTrades.get(i);
                log.printf("Trading %s for %s the %s has become trading %s for %s the %s" + NEWLINE,
                        oldT.requestedPokemon.name, oldT.nickname, oldT.givenPokemon.name, newT.requestedPokemon.name,
                        newT.nickname, newT.givenPokemon.name);
            }
            log.println();
        }
    }

    private void logBaseStatAndTypeChanges() {
        // Log base stats & types if changed at all
        if (settings.getBaseStatisticsMod() == Settings.BaseStatisticsMod.UNCHANGED
                && settings.getTypesMod() == Settings.TypesMod.UNCHANGED
                && settings.getAbilitiesMod() == Settings.AbilitiesMod.UNCHANGED
                && !settings.isRandomizeWildPokemonHeldItems()) {
            log.println("Pokemon base stats & type: unchanged" + NEWLINE);
        } else {
            List<Pokemon> allPokes = romHandler.getPokemon();
            String[] itemNames = romHandler.getItemNames();

            log.println("--Pokemon Base Stats & Types--");
            if (romHandler instanceof Gen1RomHandler) {
                log.println("NUM|NAME      |TYPE             |  HP| ATK| DEF| SPE|SPEC");
                for (Pokemon pkmn : allPokes) {
                    if (pkmn != null) {
                        String typeString = pkmn.primaryType == null ? "???" : pkmn.primaryType.toString();
                        if (pkmn.secondaryType != null) {
                            typeString += "/" + pkmn.secondaryType.toString();
                        }
                        log.printf("%3d|%-10s|%-17s|%4d|%4d|%4d|%4d|%4d" + NEWLINE, pkmn.number, pkmn.name, typeString,
                                pkmn.hp, pkmn.attack, pkmn.defense, pkmn.speed, pkmn.special);
                    }

                }
            } else {
                log.print("NUM|NAME      |TYPE             |  HP| ATK| DEF| SPE|SATK|SDEF");
                int abils = romHandler.abilitiesPerPokemon();
                for (int i = 0; i < abils; i++) {
                    log.print("|ABILITY" + (i + 1) + "    ");
                }
                log.print("|ITEM");
                log.println();
                for (Pokemon pkmn : allPokes) {
                    if (pkmn != null) {
                        String typeString = pkmn.primaryType == null ? "???" : pkmn.primaryType.toString();
                        if (pkmn.secondaryType != null) {
                            typeString += "/" + pkmn.secondaryType.toString();
                        }
                        log.printf("%3d|%-10s|%-17s|%4d|%4d|%4d|%4d|%4d|%4d", pkmn.number, pkmn.name, typeString,
                                pkmn.hp, pkmn.attack, pkmn.defense, pkmn.speed, pkmn.spatk, pkmn.spdef);
                        if (abils > 0) {
                            log.printf("|%-12s|%-12s", romHandler.abilityName(pkmn.ability1),
                                    romHandler.abilityName(pkmn.ability2));
                            if (abils > 2) {
                                log.printf("|%-12s", romHandler.abilityName(pkmn.ability3));
                            }
                        }
                        log.print("|");
                        if (pkmn.guaranteedHeldItem > 0) {
                            log.print(itemNames[pkmn.guaranteedHeldItem] + " (100%)");
                        } else {
                            int itemCount = 0;
                            if (pkmn.commonHeldItem > 0) {
                                itemCount++;
                                log.print(itemNames[pkmn.commonHeldItem] + " (common)");
                            }
                            if (pkmn.rareHeldItem > 0) {
                                if (itemCount > 0) {
                                    log.print(", ");
                                }
                                itemCount++;
                                log.print(itemNames[pkmn.rareHeldItem] + " (rare)");
                            }
                            if (pkmn.darkGrassHeldItem > 0) {
                                if (itemCount > 0) {
                                    log.print(", ");
                                }
                                itemCount++;
                                log.print(itemNames[pkmn.darkGrassHeldItem] + " (dark grass only)");
                            }
                        }
                        log.println();
                    }

                }
            }
            log.println();
        }
    }

    private void logStarters() {
        if (romHandler.canChangeStarters()) {
            if (settings.getStartersMod() == Settings.StartersMod.CUSTOM) {
                log.println("--Custom Starters--");
            } else if (settings.getStartersMod() == Settings.StartersMod.COMPLETELY_RANDOM) {
                log.println("--Random Starters--");
            } else if (settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_TWO_EVOLUTIONS) {
                log.println("--Random 2-Evolution Starters--");
            }

            List<Pokemon> starters = romHandler.getStarters();
            for (int i = 0; i < starters.size(); i++) {
                log.println("Set starter " + (i + 1) + " to " + starters.get(i).name);
            }
            log.println();
        }

    }

    private void logWildEncounters() {
        if (settings.getWildPokemonMod() == Settings.WildPokemonMod.UNCHANGED) {
            log.println("Wild Pokemon: Unchanged." + NEWLINE);
        } else {
            log.println("--Wild Pokemon--");
            List<EncounterSet> encounters = romHandler.getEncounters(settings.isUseTimeBasedEncounters());
            int idx = 0;
            for (EncounterSet es : encounters) {
                idx++;
                log.print("Set #" + idx + " ");
                if (es.displayName != null) {
                    log.print("- " + es.displayName + " ");
                }
                log.print("(rate=" + es.rate + ")");
                log.print(" - ");
                boolean first = true;
                for (Encounter e : es.encounters) {
                    if (!first) {
                        log.print(", ");
                    }
                    log.print(e.pokemon.name + " Lv");
                    if (e.maxLevel > 0 && e.maxLevel != e.level) {
                        log.print("s " + e.level + "-" + e.maxLevel);
                    } else {
                        log.print(e.level);
                    }
                    first = false;
                }
                log.println();
            }
            log.println();
        }
    }

    private void logTrainers() {
        if (settings.getTrainersMod() == Settings.TrainersMod.UNCHANGED
                && !settings.isRivalCarriesStarterThroughout()) {
            log.println("Trainers: Unchanged." + NEWLINE);
        } else {
            log.println("--Trainers Pokemon--");
            List<Trainer> trainers = romHandler.getTrainers();
            int idx = 0;
            for (Trainer t : trainers) {
                idx++;
                log.print("#" + idx + " ");
                if (t.fullDisplayName != null) {
                    log.print("(" + t.fullDisplayName + ")");
                } else if (t.name != null) {
                    log.print("(" + t.name + ")");
                }
                if (t.offset != idx && t.offset != 0) {
                    log.printf("@%X", t.offset);
                }
                log.print(" - ");
                boolean first = true;
                for (TrainerPokemon tpk : t.pokemon) {
                    if (!first) {
                        log.print(", ");
                    }
                    log.print(tpk.pokemon.name + " Lv" + tpk.level);
                    first = false;
                }
                log.println();
            }
            log.println();
        }
    }

    private void logStaticPokemon() {
        List<Pokemon> newStatics = romHandler.getStaticPokemon();
        if (settings.getStaticPokemonMod() == Settings.StaticPokemonMod.UNCHANGED) {
            log.println("Static Pokemon: Unchanged." + NEWLINE);
        } else {
            log.println("--Static Pokemon--");
            Map<Pokemon, Integer> seenPokemon = new TreeMap<Pokemon, Integer>();
            for (int i = 0; i < oldStatics.size(); i++) {
                Pokemon oldP = oldStatics.get(i);
                Pokemon newP = newStatics.get(i);
                log.print(oldP.name);
                if (seenPokemon.containsKey(oldP)) {
                    int amount = seenPokemon.get(oldP);
                    log.print("(" + (++amount) + ")");
                    seenPokemon.put(oldP, amount);
                } else {
                    seenPokemon.put(oldP, 1);
                }
                log.println(" => " + newP.name);
            }
            log.println();
        }
    }

    private void logMoveChanges() {
        if (!settings.isRandomizeMoveAccuracies() && !settings.isRandomizeMovePowers() && !settings.isRandomizeMovePPs()
                && !settings.isRandomizeMoveCategory() && !settings.isRandomizeMoveTypes()) {
            if (!settings.isUpdateMoves()) {
                log.println("Move Data: Unchanged." + NEWLINE);
            }
        } else {
            log.println("--Move Data--");
            log.print("NUM|NAME           |TYPE    |POWER|ACC.|PP");
            if (romHandler.hasPhysicalSpecialSplit()) {
                log.print(" |CATEGORY");
            }
            log.println();
            List<Move> allMoves = romHandler.getMoves();
            for (Move mv : allMoves) {
                if (mv != null) {
                    String mvType = (mv.type == null) ? "???" : mv.type.toString();
                    log.printf("%3d|%-15s|%-8s|%5d|%4d|%3d", mv.internalId, mv.name, mvType, mv.power,
                            (int) mv.hitratio, mv.pp);
                    if (romHandler.hasPhysicalSpecialSplit()) {
                        log.printf("| %s", mv.category.toString());
                    }
                    log.println();
                }
            }
            log.println();
        }
    }

    private void logTail() {
        log.println("------------------------------------------------------------------");
        log.println("Randomization of " + romHandler.getROMName() + " completed.");
        log.println("Time elapsed: " + elapsedTime + "ms");
        log.println("RNG Calls: " + callsSinceSeed);
        log.println("------------------------------------------------------------------");
    }

    private void logMiscTweaks() {
        // TODO this is crap really
        int currentMiscTweaks = settings.getCurrentMiscTweaks();
        if (romHandler.miscTweaksAvailable() != 0) {
            int codeTweaksAvailable = romHandler.miscTweaksAvailable();

            for (MiscTweak mt : MiscTweak.allTweaks) {
                if ((codeTweaksAvailable & mt.getValue()) > 0 && (currentMiscTweaks & mt.getValue()) > 0) {
                    if (mt == MiscTweak.UPDATE_TYPE_EFFECTIVENESS) {
                        log.println("--Fixing Type Effectiveness--");
                        log.println("Replaced: Poison super effective vs Bug => Ice not very effective vs Fire");
                        log.println("Changed: Bug super effective vs Poison => Bug not very effective vs Poison");
                        log.println("Changed: Psychic immune to Ghost => Ghost super effective vs Psychic");
                    }
                }
            }
        }
    }

    @Override
    public void generateLogFile() {
        if (settings.isUpdateMoves()) {
            logMoveUpdates();
        }
        logMiscTweaks();
        logBaseStatAndTypeChanges();
        logEvolutions();
        logStarters();
        // Move Data Log
        // Placed here so it matches its position in the randomizer interface
        logMoveChanges();
        // Show the new movesets if applicable
        logMovesets();
        logTrainers();
        logStaticPokemon();
        logWildEncounters();
        logTMContents();

        if (romHandler.hasMoveTutors()) {
            logMoveTutorMoves();
        }
        logInGameTrades();
        logTail();

    }

}
