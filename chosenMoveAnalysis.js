#!/usr/bin/env node
'use strict';

// Modules
var fs = require('fs');
var async = require('async');

// Data variables
var moves = 0;
var global = {
    agreed: 0,
    mctsPicked: 0,
    nnPicked: 0,
    neither: 0
};
var phaseStats = [{}, {}, {}, {}, {}, {}, {}, {}, {}, {}];
var phaseBuckets = 10;

var games = 0;
var depths = {
    total: 0
};
var branches = {
    totalAvg: 0
};

function handleGameVars(depth, min, max, avg) {
    games += 1;
    depths.total += depth;
    if (!depths.min || depths.min > depth) {
        depths.min = depth;
    }
    if (!depths.max || depths.max < depth) {
        depths.max = depth;
    }

    if (!branches.min || branches.min > min) {
        branches.min = min;
    }
    if (!branches.max || branches.max < max) {
        branches.max = max;
    }
    branches.totalAvg += avg;
}

function finalizeAverages() {
    depths.avg = depths.total / games;
    branches.avg = branches.totalAvg / games;
}

function gatherPhaseStatsFromDepthStats(statsAtDepth, matchDepth) {
    var i, phase;
    for (i = 1; i < statsAtDepth.length; i += 1) {
        phase = Math.floor(i / ((matchDepth + 1) / phaseBuckets));
//        console.log("i(" + i + ") val(" + statsAtDepth[i] + ") depth(" + matchDepth + ") phase: " + phase);
        if (!phaseStats[phase][statsAtDepth[i]]) {
            phaseStats[phase][statsAtDepth[i]] = 0;
        }
        phaseStats[phase][statsAtDepth[i]] += 1;
    }
}

// Parse each file and find data
var files = process.argv.slice(2);
async.each(files, function (file, next) {
    fs.readFile(file, function (err, data) {
        if (err) {
            console.log("Failed to read " + file + ": ", err);
            process.exit(1);
        }

        var contents = data.toString(),
            lines = contents.split("\n"),
            lineData,
            statsAtDepth = [],
            i;
        for (i = 0; i < lines.length; i += 1) {
            if (lines[i].startsWith("[Confidence] GAME_STATS: ")) {
                lineData = JSON.parse(lines[i].slice("[Confidence] GAME_STATS: ".length));
                handleGameVars(lineData.gameDepth, lineData.minBranchingFactor, lineData.maxBranchingFactor, lineData.avgBranchingFactor);
                gatherPhaseStatsFromDepthStats(statsAtDepth, lineData.gameDepth);
                statsAtDepth = [];
            } else if (lines[i].startsWith("[Confidence] MOVE_STATS: ")) {
                moves += 1;
                lineData = JSON.parse(lines[i].slice("[Confidence] MOVE_STATS: ".length));
                if (lineData.allAgentsAgree) {
                    global.agreed += 1;
                    statsAtDepth[lineData.depth] = "agreed";
                } else {
                    if (lineData.chosenMove_MultithreadedUCTPlayer) {
                        global.mctsPicked += 1;
                        statsAtDepth[lineData.depth] = "mctsPicked";
                    } else if (lineData.chosenMove_LearningPlayer) {
                        global.nnPicked += 1;
                        statsAtDepth[lineData.depth] = "nnPicked";
                    } else {
                        global.neither += 1;
                        statsAtDepth[lineData.depth] = "neither";
                    }
                }
            }
        }
        next();
    });
}, function () {
    console.log("Games    : " + games);
    console.log("Moves    : " + moves);

    finalizeAverages();
    function printMinMaxAvg(name, statGroup) {
        console.log(name + ": Min(" + statGroup.min + ") Max(" + statGroup.max + ") Avg(" + statGroup.avg + ")");
    }
    printMinMaxAvg("Depths   ", depths);
    printMinMaxAvg("Branches ", branches);

    function printStat(name, statGroup) {
        console.log();
        console.log(name + " stats:");
        console.log("Agree   : " + statGroup.agreed);
        console.log("MCTS    : " + statGroup.mctsPicked);
        console.log("NN      : " + statGroup.nnPicked);
        console.log("Neither : " + statGroup.neither);
    }

    printStat("Global", global);
    printStat("Phase 1", phaseStats[0]);
    printStat("Phase 2", phaseStats[1]);
    printStat("Phase 3", phaseStats[2]);
    printStat("Phase 4", phaseStats[3]);
    printStat("Phase 5", phaseStats[4]);
    printStat("Phase 6", phaseStats[5]);
    printStat("Phase 7", phaseStats[6]);
    printStat("Phase 8", phaseStats[7]);
    printStat("Phase 9", phaseStats[8]);
    printStat("Phase 10", phaseStats[9]);
});

