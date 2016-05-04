#!/bin/bash

GAME_KEY=nineBoardTicTacToe
P1=MiniMaxRandomPlayer
P2=LearningPlayer

NUM_GAMES=100
START_CLOCK=30
PLAY_CLOCK=10

./playTournament.sh $GAME_KEY $START_CLOCK $PLAY_CLOCK $NUM_GAMES $P1 $P2

