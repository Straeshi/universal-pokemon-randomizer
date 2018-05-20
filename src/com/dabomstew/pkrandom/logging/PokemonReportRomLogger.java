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
 * More comprehensive text based report generator
 * 
 * TODO remove all the stuff copied straight across from TextRomLogger
 * 
 * @author straeshi
 *
 */
public class PokemonReportRomLogger extends AbstractRomLogger {

    private static final String NEWLINE = System.getProperty("line.separator");
    private static final String POKEMON_SEPARATOR = "––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––––";
    private static final String SECTION_SEPARATOR = "================================================================================";

    private final PrintStream log;

    public PokemonReportRomLogger(RomHandler romHandler, Settings settings, PrintStream log) {
        super(romHandler, settings);
        this.log = log;
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
        // report all pokemon stats & moves
        reportAllPokemon();
        log.println(SECTION_SEPARATOR);

        // report Move stats, TMs/MoveTutors
        reportAllMoves();
        log.println(SECTION_SEPARATOR);

        // report starters, statics, trades
        reportSpecialPokemonLocations();
        log.println(SECTION_SEPARATOR);

        // report wild pokemon
        reportWildPokemon();
        log.println(SECTION_SEPARATOR);

        // report trainer teams
        reportTrainerTeams();
        log.println(SECTION_SEPARATOR);

        logMiscTweaks(); // TODO who knows?
        log.println("Randomization of " + romHandler.getROMName() + " completed.");
        log.println("Time elapsed: " + elapsedTime + "ms");
        log.println("RNG Calls: " + callsSinceSeed);
        log.println(SECTION_SEPARATOR);
    }

    private void reportAllPokemon() {
        int abilitiesPerPokemon = romHandler.abilitiesPerPokemon();
        String[] itemNames = romHandler.getItemNames();

        Map<Pokemon, List<MoveLearnt>> movesLearnt = romHandler.getMovesLearnt();

        Map<Pokemon, boolean[]> machineCompatibility = romHandler.getTMHMCompatibility();
        Map<Pokemon, boolean[]> moveTutorCompatibility = romHandler.getMoveTutorCompatibility();
        List<Integer> technicalMachineMoves = romHandler.getTMMoves();
        List<Integer> hiddenMachineMoves = romHandler.getHMMoves();
        List<Integer> moveTutorMoves = romHandler.getMoveTutorMoves();

        for (Pokemon pokemon : romHandler.getPokemon()) {
            if (pokemon == null) {
                continue;
            }

            log.println(POKEMON_SEPARATOR);

            // name type basic stats etc
            log.println(formatPokemon(pokemon, abilitiesPerPokemon, itemNames));

            // moves
            log.println(formatMoveset(movesLearnt.get(pokemon)));

            // TM/HM/MoveTutor
            log.println(formatLearnMoveset(machineCompatibility.get(pokemon), moveTutorCompatibility.get(pokemon),
                    technicalMachineMoves, hiddenMachineMoves, moveTutorMoves));

            // evolution(s)
            log.println(formatEvolutions(pokemon, itemNames));
        }
    }

    private String formatPokemon(Pokemon pokemon, int abilitiesPerPokemon, String[] itemNames) {

        StringBuilder builder = new StringBuilder();

        builder.append("Dex#");
        builder.append(pokemon.number);
        builder.append(" ");
        builder.append(pokemon.name);
        builder.append(" ");

        builder.append(pokemon.primaryType == null ? "???" : pokemon.primaryType);
        if (pokemon.secondaryType != null) {
            builder.append("/");
            builder.append(pokemon.secondaryType.toString());
        }
        builder.append("\t");

        builder.append(" HP ");
        builder.append(pokemon.hp);
        builder.append(" Att ");
        builder.append(pokemon.attack);
        builder.append(" Def ");
        builder.append(pokemon.defense);
        builder.append(" Spd ");
        builder.append(pokemon.speed);

        if (romHandler instanceof Gen1RomHandler) {
            builder.append(" Spec ");
            builder.append(pokemon.special);
        } else {
            builder.append(" SpcAtt ");
            builder.append(pokemon.spatk);
            builder.append(" SpcDef ");
            builder.append(pokemon.spdef);

            if (abilitiesPerPokemon > 0) {
                builder.append("\t");
                builder.append(romHandler.abilityName(pokemon.ability1));
                builder.append(" ");
                builder.append(romHandler.abilityName(pokemon.ability2));
                if (abilitiesPerPokemon > 2) {
                    builder.append(" ");
                    builder.append(romHandler.abilityName(pokemon.ability3));
                }
            }
            if (pokemon.guaranteedHeldItem > 0) {
                builder.append(" ");
                builder.append(itemNames[pokemon.guaranteedHeldItem] + " (100%)");
            } else {
                if (pokemon.commonHeldItem > 0) {
                    builder.append(" ");
                    builder.append(itemNames[pokemon.commonHeldItem] + " (common)");
                }
                if (pokemon.rareHeldItem > 0) {
                    builder.append(" ");
                    builder.append(itemNames[pokemon.rareHeldItem] + " (rare)");
                }
                if (pokemon.darkGrassHeldItem > 0) {
                    builder.append(" ");
                    builder.append(itemNames[pokemon.darkGrassHeldItem] + " (dark grass only)");
                }
            }

        }
        return builder.toString();
    }

    private String formatEvolutions(Pokemon pokemon, String[] itemNames) {
        // TODO Gen5 etc. checks for other evolution types
        StringBuilder builder = new StringBuilder();
        for (Evolution evolution : pokemon.evolutionsTo) {
            builder.append("Evolves from ");
            builder.append(evolution.from.name);
            builder.append(" via ");
            if (evolution.type == EvolutionType.LEVEL) {
                builder.append("level at ");
                builder.append(evolution.extraInfo);
            } else if (evolution.type == EvolutionType.STONE) {
                builder.append(itemNames[evolution.extraInfo]);
            }
            builder.append(NEWLINE);
        }
        for (Evolution evolution : pokemon.evolutionsFrom) {
            builder.append("Evolves into ");
            builder.append(evolution.to.name);
            builder.append(" via ");
            if (evolution.type == EvolutionType.LEVEL) {
                builder.append("level at ");
                builder.append(evolution.extraInfo);
            } else if (evolution.type == EvolutionType.STONE) {
                builder.append(itemNames[evolution.extraInfo]);
            }
            builder.append(NEWLINE);
        }
        return builder.toString();
    }

    private String formatLearnMoveset(boolean[] machineCompatibility, boolean[] moveTutorCompatibility,
            List<Integer> technicalMachineMoves, List<Integer> hiddenMachineMoves, List<Integer> moveTutorMoves) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (int i = 1; i < machineCompatibility.length; i++) {
            if (machineCompatibility[i]) {
                if (count % 4 == 0) {
                    builder.append(NEWLINE);
                }
                count++;
                builder.append("\t");

                Move move;
                if (technicalMachineMoves.size() >= i) {
                    move = moves.get(technicalMachineMoves.get(i - 1));
                    builder.append("TM " + i);
                } else {
                    move = moves.get(hiddenMachineMoves.get(i - technicalMachineMoves.size() - 1));
                    builder.append("HM " + (i - technicalMachineMoves.size()));
                }
                builder.append(" ");
                builder.append(move.name);

            }
        }

        if (romHandler.hasMoveTutors()) {
            builder.append(NEWLINE);
            for (int i = 1; i < moveTutorCompatibility.length; i++) {
                if (moveTutorCompatibility[i]) {
                    if (count % 4 == 0) {
                        builder.append(NEWLINE);
                    }
                    count++;
                    builder.append("\t");

                    Move move = moves.get(moveTutorMoves.get(i - 1));
                    builder.append("MoveTutor");

                    builder.append(" ");
                    builder.append(move.name);
                }
            }
        }

        return builder.toString();
    }

    private String formatMoveset(List<MoveLearnt> moveSet) {
        StringBuilder builder = new StringBuilder();
        int count = 0;
        for (MoveLearnt moveLearnt : moveSet) {
            if (count % 4 == 0) {
                builder.append(NEWLINE);
            }
            count++;
            builder.append("\t");

            Move move = moves.get(moveLearnt.move);

            builder.append(moveLearnt.level);
            builder.append(" ");
            builder.append(move.name);
        }
        return builder.toString();
    }

    private void reportAllMoves() {

        // move stats
        for (Move move : moves) {
            System.out.println(move);
            if (move == null) {
                continue;
            }
            StringBuilder builder = new StringBuilder();

            builder.append(move.internalId);
            builder.append(" ");
            builder.append(move.name);
            builder.append("\t");

            builder.append((move.type == null) ? "???" : move.type);
            if (romHandler.hasPhysicalSpecialSplit()) {
                builder.append(" ");
                builder.append(move.category);
            }
            builder.append("\t");

            builder.append(move.power);
            builder.append(" ");
            builder.append((int) move.hitratio);
            builder.append("%");
            builder.append(" ");
            builder.append(move.pp);
            builder.append("PP");

            log.println(builder);
        }
        log.println();

        // TM contents
        List<Integer> tmMoves = romHandler.getTMMoves();
        for (int i = 0; i < tmMoves.size(); i++) {
            StringBuilder builder = new StringBuilder();

            builder.append("TM ");
            builder.append(i + 1);
            builder.append("\t");
            builder.append(moves.get(tmMoves.get(i)).name);

            log.println(builder);
        }
        // Move Tutors
        List<Integer> moveTutorMoves = romHandler.getMoveTutorMoves();
        for (int i = 0; i < moveTutorMoves.size(); i++) {
            StringBuilder builder = new StringBuilder();

            builder.append(moves.get(oldMtMoves.get(i)).name);
            builder.append(" =>");
            builder.append("\t");
            builder.append(moves.get(moveTutorMoves.get(i)).name);

            log.println(builder);
        }
    }

    private void reportSpecialPokemonLocations() {
        // TODO redo format?
        // starter pokemon
        List<Pokemon> starters = romHandler.getStarters();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < starters.size(); i++) {
            builder.append("Starter ");
            builder.append(i + 1);
            builder.append("\t");
            builder.append(starters.get(i).name);
            builder.append(NEWLINE);
        }
        log.println(builder);

        // static pokemon
        List<Pokemon> newStatics = romHandler.getStaticPokemon();
        log.println("Special Encounter/Reward Pokemon");
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

        // in-game trades
        List<IngameTrade> newTrades = romHandler.getIngameTrades();
        int size = oldTrades.size();
        for (int i = 0; i < size; i++) {
            IngameTrade oldT = oldTrades.get(i);
            IngameTrade newT = newTrades.get(i);
            log.printf("Trading %s for %s the %s has become trading %s for %s the %s" + NEWLINE,
                    oldT.requestedPokemon.name, oldT.nickname, oldT.givenPokemon.name, newT.requestedPokemon.name,
                    newT.nickname, newT.givenPokemon.name);
        }

    }

    private void reportWildPokemon() {
        log.println("Wild Pokemon Encounters");
        List<EncounterSet> encounters = romHandler.getEncounters(settings.isUseTimeBasedEncounters());
        int index = 0;
        for (EncounterSet es : encounters) {
            index++;
            StringBuilder builder = new StringBuilder();
            builder.append("Set #");
            builder.append(index);
            builder.append(" - ");
            builder.append(es.displayName);
            builder.append(" (Rate:");
            builder.append(es.rate);
            builder.append(")");

            int count = 0;
            for (Encounter e : es.encounters) {
                if (count % 4 == 0) {
                    builder.append(NEWLINE);
                }
                count++;
                builder.append("\t");

                builder.append(e.pokemon.name);
                builder.append(" Lv");
                builder.append(e.level);
                if (e.maxLevel > 0 && e.maxLevel != e.level) {
                    builder.append("–");
                    builder.append(e.maxLevel);
                }
            }
            builder.append(NEWLINE);
            log.println(builder);
        }

    }

    private void reportTrainerTeams() {
        log.println("Trainer Battles");
        List<Trainer> trainers = romHandler.getTrainers();
        int index = 0;
        for (Trainer trainer : trainers) {
            index++;
            StringBuilder builder = new StringBuilder();
            builder.append("#");
            builder.append(index);
            builder.append(" ");
            builder.append(trainer.fullDisplayName != null ? trainer.fullDisplayName : trainer.name);
            if (trainer.offset != index && trainer.offset != 0) {
                builder.append(" @");
                builder.append(Integer.toHexString(trainer.offset));
            }
            builder.append(NEWLINE);
            builder.append("\t");
            builder.append("\t");

            for (TrainerPokemon trainerPokemon : trainer.pokemon) {
                builder.append(trainerPokemon.pokemon.name);
                builder.append(" Lv");
                builder.append(trainerPokemon.level);
                builder.append("\t");
            }

            log.println(builder);
        }
    }

}
