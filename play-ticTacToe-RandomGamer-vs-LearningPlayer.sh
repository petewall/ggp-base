#!/bin/bash

GAME_KEY=ticTacToe
P1=RandomGamer
P2=LearningPlayer

NUM_GAMES=100
START_CLOCK=30
PLAY_CLOCK=10

./playTournament.sh $GAME_KEY $START_CLOCK $PLAY_CLOCK $NUM_GAMES $P1 $P2
