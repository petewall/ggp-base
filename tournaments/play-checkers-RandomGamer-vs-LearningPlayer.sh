#!/bin/bash

GAME_KEY=checkers
P1=RandomGamer
P2=LearningPlayer

NUM_GAMES=10
START_CLOCK=30
PLAY_CLOCK=10

../playTournament.sh $GAME_KEY $START_CLOCK $PLAY_CLOCK $NUM_GAMES $P1 $P2

