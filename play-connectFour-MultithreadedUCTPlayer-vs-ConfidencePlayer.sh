#!/bin/bash

GAME_KEY=connectFour
P1=MultithreadedUCTPlayer
P2=ConfidenceAgent

NUM_GAMES=100
START_CLOCK=30
PLAY_CLOCK=10

./playTournament.sh $GAME_KEY $START_CLOCK $PLAY_CLOCK $NUM_GAMES $P1 $P2

