package com.dabomstew.pkrandom;

/*----------------------------------------------------------------------------*/
/*--  Randomizer.java - Can randomize a file based on settings.             --*/
/*--                    Output varies by seed.                              --*/
/*--                                                                        --*/
/*--  Part of "Universal Pokemon Randomizer" by Dabomstew                   --*/
/*--  Pokemon and any associated names and the like are                     --*/
/*--  trademark and (C) Nintendo 1996-2012.                                 --*/
/*--                                                                        --*/
/*--  The custom code written here is licensed under the terms of the GPL:  --*/
/*--                                                                        --*/
/*--  This program is free software: you can redistribute it and/or modify  --*/
/*--  it under the terms of the GNU General Public License as published by  --*/
/*--  the Free Software Foundation, either version 3 of the License, or     --*/
/*--  (at your option) any later version.                                   --*/
/*--                                                                        --*/
/*--  This program is distributed in the hope that it will be useful,       --*/
/*--  but WITHOUT ANY WARRANTY; without even the implied warranty of        --*/
/*--  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the          --*/
/*--  GNU General Public License for more details.                          --*/
/*--                                                                        --*/
/*--  You should have received a copy of the GNU General Public License     --*/
/*--  along with this program. If not, see <http://www.gnu.org/licenses/>.  --*/
/*----------------------------------------------------------------------------*/

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.dabomstew.pkrandom.logging.TextRomLogger;
import com.dabomstew.pkrandom.pokemon.Encounter;
import com.dabomstew.pkrandom.pokemon.EncounterSet;
import com.dabomstew.pkrandom.pokemon.IngameTrade;
import com.dabomstew.pkrandom.pokemon.Move;
import com.dabomstew.pkrandom.pokemon.MoveLearnt;
import com.dabomstew.pkrandom.pokemon.Pokemon;
import com.dabomstew.pkrandom.pokemon.Trainer;
import com.dabomstew.pkrandom.pokemon.TrainerPokemon;
import com.dabomstew.pkrandom.romhandlers.Gen1RomHandler;
import com.dabomstew.pkrandom.romhandlers.Gen5RomHandler;
import com.dabomstew.pkrandom.romhandlers.RomHandler;

// Can randomize a file based on settings. Output varies by seed.
public class Randomizer {

    private static final String NEWLINE = System.getProperty("line.separator");

    private final Settings settings;
    private final RomHandler romHandler;

    private TextRomLogger logger;

    public Randomizer(Settings settings, RomHandler romHandler) {
        this.settings = settings;
        this.romHandler = romHandler;
    }

    public int randomize(final String filename) {
        return randomize(filename, new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
            }
        }));
    }

    public int randomize(final String filename, final PrintStream log) {
        long seed = RandomSource.pickSeed();
        return randomize(filename, log, seed);
    }

    public int randomize(final String filename, final PrintStream log, long seed) {
        final long startTime = System.currentTimeMillis();
        RandomSource.seed(seed);

        logger = new TextRomLogger(romHandler, settings, log);

        int checkValue = 0;

        // limit pokemon?
        if (settings.isLimitPokemon()) {
            romHandler.setPokemonPool(settings.getCurrentRestrictions());
            romHandler.removeEvosForPokemonPool();
        } else {
            romHandler.setPokemonPool(null);
        }

        // Move updates & data changes
        if (settings.isUpdateMoves()) {
            romHandler.initMoveUpdates();
            if (!(romHandler instanceof Gen5RomHandler)) {
                romHandler.updateMovesToGen5();
            }
            if (!settings.isUpdateMovesLegacy()) {
                romHandler.updateMovesToGen6();
            }
            romHandler.printMoveUpdates();
        }

        changeMoveStats();

        // Misc Tweaks?
        int currentMiscTweaks = settings.getCurrentMiscTweaks();
        if (romHandler.miscTweaksAvailable() != 0) {
            int codeTweaksAvailable = romHandler.miscTweaksAvailable();
            List<MiscTweak> tweaksToApply = new ArrayList<MiscTweak>();

            for (MiscTweak mt : MiscTweak.allTweaks) {
                if ((codeTweaksAvailable & mt.getValue()) > 0 && (currentMiscTweaks & mt.getValue()) > 0) {
                    tweaksToApply.add(mt);
                }
            }

            // Sort so priority is respected in tweak ordering.
            Collections.sort(tweaksToApply);

            // Now apply in order.
            for (MiscTweak mt : tweaksToApply) {
                romHandler.applyMiscTweak(mt);
            }
        }

        if (settings.isUpdateBaseStats()) {
            romHandler.updatePokemonStats();
        }

        if (settings.isStandardizeEXPCurves()) {
            romHandler.standardizeEXPCurves();
        }

        checkValue = changePokemonStatsAndTypes(checkValue);

        logger.logBaseStatAndTypeChanges();

        // Random Evos
        // Applied after type to pick new evos based on new types.
        changeEvolutions();
        logger.logEvolutions();

        // Starter Pokemon
        // Applied after type to update the strings correctly based on new types
        changeStarters();
        logger.logStarters();

        // Move Data Log
        // Placed here so it matches its position in the randomizer interface
        logger.logMoveChanges();

        // Movesets
        changeMovesets();

        // Show the new movesets if applicable
        logger.logMovesets();

        // Trainer Pokemon
        checkValue = changeTrainers(checkValue);

        // Trainer names & class names randomization
        // done before trainer log to add proper names

        changeTrainerNames();

        logger.logTrainers();

        // Apply metronome only mode now that trainers have been dealt with
        if (settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY) {
            romHandler.metronomeOnlyMode();
        }

        // Static Pokemon
        checkValue = changeStaticPokemon(checkValue);

        logger.logStaticPokemon();

        // Wild Pokemon
        changeMinWildCatchRate();
        checkValue = changeWildEncounters(checkValue);

        logger.logWildEncounters();

        // TMs
        checkValue = changeTMContents(checkValue);
        logger.logTMContents();

        // TM/HM compatibility
        changeTMHMCompatibility();

        // Move Tutors (new 1.0.3)
        if (romHandler.hasMoveTutors()) {
            checkValue = changeMoveTutorMoves(checkValue);

            // Compatibility
            changeMoveTutorCompatibility();

            logger.logMoveTutorMoves();
        }

        // In-game trades
        changeInGameTrades();
        logger.logInGameTrades();

        // Field Items
        changeFieldItems();

        // Signature...
        romHandler.applySignature();

        // Record check value?
        romHandler.writeCheckValueToROM(checkValue);

        // Save
        romHandler.saveRom(filename);

        // Log tail
        logger.finishLog((System.currentTimeMillis() - startTime), RandomSource.callsSinceSeed());

        return checkValue;
    }

    private void changeMoveStats() {
        if (settings.isRandomizeMovePowers()) {
            romHandler.randomizeMovePowers();
        }

        if (settings.isRandomizeMoveAccuracies()) {
            romHandler.randomizeMoveAccuracies();
        }

        if (settings.isRandomizeMovePPs()) {
            romHandler.randomizeMovePPs();
        }

        if (settings.isRandomizeMoveTypes()) {
            romHandler.randomizeMoveTypes();
        }

        if (settings.isRandomizeMoveCategory() && romHandler.hasPhysicalSpecialSplit()) {
            romHandler.randomizeMoveCategory();
        }
    }

    private int changePokemonStatsAndTypes(int checkValue) {
        // Base stats changing
        switch (settings.getBaseStatisticsMod()) {
        case SHUFFLE:
            romHandler.shufflePokemonStats(settings.isBaseStatsFollowEvolutions());
            break;
        case RANDOM:
            romHandler.randomizePokemonStats(settings.isBaseStatsFollowEvolutions());
            break;
        default:
            break;
        }

        // Abilities? (new 1.0.2)
        if (romHandler.abilitiesPerPokemon() > 0 && settings.getAbilitiesMod() == Settings.AbilitiesMod.RANDOMIZE) {
            romHandler.randomizeAbilities(settings.isAbilitiesFollowEvolutions(), settings.isAllowWonderGuard(),
                    settings.isBanTrappingAbilities(), settings.isBanNegativeAbilities());
        }

        // Pokemon Types
        switch (settings.getTypesMod()) {
        case RANDOM_FOLLOW_EVOLUTIONS:
            romHandler.randomizePokemonTypes(true);
            break;
        case COMPLETELY_RANDOM:
            romHandler.randomizePokemonTypes(false);
            break;
        default:
            break;
        }

        // Wild Held Items?
        if (settings.isRandomizeWildPokemonHeldItems()) {
            romHandler.randomizeWildHeldItems(settings.isBanBadRandomWildPokemonHeldItems());
        }

        for (Pokemon pkmn : romHandler.getPokemon()) {
            if (pkmn != null) {
                checkValue = addToCV(checkValue, pkmn.hp, pkmn.attack, pkmn.defense, pkmn.speed, pkmn.spatk, pkmn.spdef,
                        pkmn.ability1, pkmn.ability2, pkmn.ability3);
            }
        }
        return checkValue;
    }

    private void changeEvolutions() {
        if (settings.getEvolutionsMod() == Settings.EvolutionsMod.RANDOM) {
            romHandler.randomizeEvolutions(settings.isEvosSimilarStrength(), settings.isEvosSameTyping(),
                    settings.isEvosMaxThreeStages(), settings.isEvosForceChange());
        }

        // Trade evolutions removal
        if (settings.isChangeImpossibleEvolutions()) {
            romHandler.removeTradeEvolutions(!(settings.getMovesetsMod() == Settings.MovesetsMod.UNCHANGED));
        }

        // Easier evolutions
        if (settings.isMakeEvolutionsEasier()) {
            romHandler.condenseLevelEvolutions(40, 30);
        }
    }

    private void changeMovesets() {
        boolean noBrokenMoves = settings.doBlockBrokenMoves();
        boolean forceFourLv1s = romHandler.supportsFourStartingMoves() && settings.isStartWithFourMoves();
        double msGoodDamagingProb = settings.isMovesetsForceGoodDamaging()
                ? settings.getMovesetsGoodDamagingPercent() / 100.0
                : 0;
        if (settings.getMovesetsMod() == Settings.MovesetsMod.RANDOM_PREFER_SAME_TYPE) {
            romHandler.randomizeMovesLearnt(true, noBrokenMoves, forceFourLv1s, msGoodDamagingProb);
        } else if (settings.getMovesetsMod() == Settings.MovesetsMod.COMPLETELY_RANDOM) {
            romHandler.randomizeMovesLearnt(false, noBrokenMoves, forceFourLv1s, msGoodDamagingProb);
        }

        if (settings.isReorderDamagingMoves()) {
            romHandler.orderDamagingMovesByDamage();
        }
    }

    private int changeTrainers(int checkValue) {
        if (settings.getTrainersMod() == Settings.TrainersMod.RANDOM) {
            romHandler.randomizeTrainerPokes(settings.isTrainersUsePokemonOfSimilarStrength(),
                    settings.isTrainersBlockLegendaries(), settings.isTrainersBlockEarlyWonderGuard(),
                    settings.isTrainersLevelModified() ? settings.getTrainersLevelModifier() : 0);
        } else if (settings.getTrainersMod() == Settings.TrainersMod.TYPE_THEMED) {
            romHandler.typeThemeTrainerPokes(settings.isTrainersUsePokemonOfSimilarStrength(),
                    settings.isTrainersMatchTypingDistribution(), settings.isTrainersBlockLegendaries(),
                    settings.isTrainersBlockEarlyWonderGuard(),
                    settings.isTrainersLevelModified() ? settings.getTrainersLevelModifier() : 0);
        }

        if ((settings.getTrainersMod() != Settings.TrainersMod.UNCHANGED
                || settings.getStartersMod() != Settings.StartersMod.UNCHANGED)
                && settings.isRivalCarriesStarterThroughout()) {
            romHandler.rivalCarriesStarter();
        }

        if (settings.isTrainersForceFullyEvolved()) {
            romHandler.forceFullyEvolvedTrainerPokes(settings.getTrainersForceFullyEvolvedLevel());
        }

        List<Trainer> trainers = romHandler.getTrainers();
        for (Trainer t : trainers) {
            for (TrainerPokemon tpk : t.pokemon) {
                checkValue = addToCV(checkValue, tpk.level, tpk.pokemon.number);
            }
        }

        return checkValue;
    }

    private void changeTrainerNames() {
        if (romHandler.canChangeTrainerText()) {
            if (settings.isRandomizeTrainerClassNames()) {
                romHandler.randomizeTrainerClassNames(settings.getCustomNames());
            }

            if (settings.isRandomizeTrainerNames()) {
                romHandler.randomizeTrainerNames(settings.getCustomNames());
            }
        }

    }

    private int changeWildEncounters(int checkValue) {
        switch (settings.getWildPokemonMod()) {
        case RANDOM:
            romHandler.randomEncounters(settings.isUseTimeBasedEncounters(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL,
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.TYPE_THEME_AREAS,
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH,
                    settings.isBlockWildLegendaries());
            break;
        case AREA_MAPPING:
            romHandler.area1to1Encounters(settings.isUseTimeBasedEncounters(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.CATCH_EM_ALL,
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.TYPE_THEME_AREAS,
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH,
                    settings.isBlockWildLegendaries());
            break;
        case GLOBAL_MAPPING:
            romHandler.game1to1Encounters(settings.isUseTimeBasedEncounters(),
                    settings.getWildPokemonRestrictionMod() == Settings.WildPokemonRestrictionMod.SIMILAR_STRENGTH,
                    settings.isBlockWildLegendaries());
            break;
        default:
            break;
        }

        List<EncounterSet> encounters = romHandler.getEncounters(settings.isUseTimeBasedEncounters());
        for (EncounterSet es : encounters) {
            for (Encounter e : es.encounters) {
                checkValue = addToCV(checkValue, e.level, e.pokemon.number);
            }
        }
        return checkValue;
    }

    private void changeMinWildCatchRate() {
        if (settings.isUseMinimumCatchRate()) {
            boolean gen5 = romHandler instanceof Gen5RomHandler;
            int normalMin, legendaryMin;
            switch (settings.getMinimumCatchRateLevel()) {
            case 1:
            default:
                normalMin = gen5 ? 50 : 75;
                legendaryMin = gen5 ? 25 : 37;
                break;
            case 2:
                normalMin = gen5 ? 100 : 128;
                legendaryMin = gen5 ? 45 : 64;
                break;
            case 3:
                normalMin = gen5 ? 180 : 200;
                legendaryMin = gen5 ? 75 : 100;
                break;
            case 4:
                normalMin = legendaryMin = 255;
                break;
            }
            romHandler.minimumCatchRate(normalMin, legendaryMin);
        }
    }

    private int changeTMContents(int checkValue) {
        boolean noBrokenMoves = settings.doBlockBrokenMoves();
        if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                && settings.getTmsMod() == Settings.TMsMod.RANDOM) {
            double goodDamagingProb = settings.isTmsForceGoodDamaging() ? settings.getTmsGoodDamagingPercent() / 100.0
                    : 0;
            romHandler.randomizeTMMoves(noBrokenMoves, settings.isKeepFieldMoveTMs(), goodDamagingProb);
            List<Integer> tmMoves = romHandler.getTMMoves();
            for (int i = 0; i < tmMoves.size(); i++) {
                checkValue = addToCV(checkValue, tmMoves.get(i));
            }
        }
        return checkValue;
    }

    private void changeTMHMCompatibility() {
        switch (settings.getTmsHmsCompatibilityMod()) {
        case RANDOM_PREFER_TYPE:
            romHandler.randomizeTMHMCompatibility(true);
            break;
        case COMPLETELY_RANDOM:
            romHandler.randomizeTMHMCompatibility(false);
            break;
        case FULL:
            romHandler.fullTMHMCompatibility();
            break;
        default:
            break;
        }

        if (settings.isTmLevelUpMoveSanity()) {
            romHandler.ensureTMCompatSanity();
        }

        if (settings.isFullHMCompat()) {
            romHandler.fullHMCompatibility();
        }
    }

    private int changeMoveTutorMoves(int checkValue) {
        boolean noBrokenMoves = settings.doBlockBrokenMoves();
        if (!(settings.getMovesetsMod() == Settings.MovesetsMod.METRONOME_ONLY)
                && settings.getMoveTutorMovesMod() == Settings.MoveTutorMovesMod.RANDOM) {
            double goodDamagingProb = settings.isTutorsForceGoodDamaging()
                    ? settings.getTutorsGoodDamagingPercent() / 100.0
                    : 0;
            romHandler.randomizeMoveTutorMoves(noBrokenMoves, settings.isKeepFieldMoveTutors(), goodDamagingProb);

            List<Integer> newMtMoves = romHandler.getMoveTutorMoves();
            for (int i = 0; i < newMtMoves.size(); i++) {

                checkValue = addToCV(checkValue, newMtMoves.get(i));
            }

        }
        return checkValue;
    }

    private void changeMoveTutorCompatibility() {
        switch (settings.getMoveTutorsCompatibilityMod()) {
        case RANDOM_PREFER_TYPE:
            romHandler.randomizeMoveTutorCompatibility(true);
            break;
        case COMPLETELY_RANDOM:
            romHandler.randomizeMoveTutorCompatibility(false);
            break;
        case FULL:
            romHandler.fullMoveTutorCompatibility();
            break;
        default:
            break;
        }

        if (settings.isTutorLevelUpMoveSanity()) {
            romHandler.ensureMoveTutorCompatSanity();
        }
    }

    private void changeInGameTrades() {
        if (settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN) {
            romHandler.randomizeIngameTrades(false, settings.isRandomizeInGameTradesNicknames(),
                    settings.isRandomizeInGameTradesOTs(), settings.isRandomizeInGameTradesIVs(),
                    settings.isRandomizeInGameTradesItems(), settings.getCustomNames());
        } else if (settings.getInGameTradesMod() == Settings.InGameTradesMod.RANDOMIZE_GIVEN_AND_REQUESTED) {
            romHandler.randomizeIngameTrades(true, settings.isRandomizeInGameTradesNicknames(),
                    settings.isRandomizeInGameTradesOTs(), settings.isRandomizeInGameTradesIVs(),
                    settings.isRandomizeInGameTradesItems(), settings.getCustomNames());
        }
    }

    private void changeFieldItems() {
        if (settings.getFieldItemsMod() == Settings.FieldItemsMod.SHUFFLE) {
            romHandler.shuffleFieldItems();
        } else if (settings.getFieldItemsMod() == Settings.FieldItemsMod.RANDOM) {
            romHandler.randomizeFieldItems(settings.isBanBadRandomFieldItems());
        }
    }

    private void changeStarters() {
        if (romHandler.canChangeStarters()) {
            if (settings.getStartersMod() == Settings.StartersMod.CUSTOM) {
                List<Pokemon> romPokemon = romHandler.getPokemon();
                int[] customStarters = settings.getCustomStarters();
                Pokemon pkmn1 = romPokemon.get(customStarters[0]);
                Pokemon pkmn2 = romPokemon.get(customStarters[1]);
                if (romHandler.isYellow()) {
                    romHandler.setStarters(Arrays.asList(pkmn1, pkmn2));
                } else {
                    Pokemon pkmn3 = romPokemon.get(customStarters[2]);
                    romHandler.setStarters(Arrays.asList(pkmn1, pkmn2, pkmn3));
                }

            } else if (settings.getStartersMod() == Settings.StartersMod.COMPLETELY_RANDOM) {
                // Randomise
                int starterCount = 3;
                if (romHandler.isYellow()) {
                    starterCount = 2;
                }
                List<Pokemon> starters = new ArrayList<Pokemon>();
                for (int i = 0; i < starterCount; i++) {
                    Pokemon pkmn = romHandler.randomPokemon();
                    while (starters.contains(pkmn)) {
                        pkmn = romHandler.randomPokemon();
                    }
                    starters.add(pkmn);
                }
                romHandler.setStarters(starters);
            } else if (settings.getStartersMod() == Settings.StartersMod.RANDOM_WITH_TWO_EVOLUTIONS) {
                // Randomise
                int starterCount = 3;
                if (romHandler.isYellow()) {
                    starterCount = 2;
                }
                List<Pokemon> starters = new ArrayList<Pokemon>();
                for (int i = 0; i < starterCount; i++) {
                    Pokemon pkmn = romHandler.random2EvosPokemon();
                    while (starters.contains(pkmn)) {
                        pkmn = romHandler.random2EvosPokemon();
                    }
                    starters.add(pkmn);
                }
                romHandler.setStarters(starters);
            }
            if (settings.isRandomizeStartersHeldItems() && !(romHandler instanceof Gen1RomHandler)) {
                romHandler.randomizeStarterHeldItems(settings.isBanBadRandomStarterHeldItems());
            }
        }
    }

    private int changeStaticPokemon(int checkValue) {
        if (romHandler.canChangeStaticPokemon()) {
            List<Pokemon> oldStatics = romHandler.getStaticPokemon();
            if (settings.getStaticPokemonMod() == Settings.StaticPokemonMod.RANDOM_MATCHING) {
                romHandler.randomizeStaticPokemon(true);
            } else if (settings.getStaticPokemonMod() == Settings.StaticPokemonMod.COMPLETELY_RANDOM) {
                romHandler.randomizeStaticPokemon(false);
            }
            if (!(settings.getStaticPokemonMod() == Settings.StaticPokemonMod.UNCHANGED)) {
                List<Pokemon> newStatics = romHandler.getStaticPokemon();
                for (int i = 0; i < oldStatics.size(); i++) {
                    Pokemon newP = newStatics.get(i);
                    checkValue = addToCV(checkValue, newP.number);
                }
            }
        }
        return checkValue;
    }

    private static int addToCV(int checkValue, int... values) {
        for (int value : values) {
            checkValue = Integer.rotateLeft(checkValue, 3);
            checkValue ^= value;
        }
        return checkValue;
    }
}