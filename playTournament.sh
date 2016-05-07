#!/bin/bash

set -x

GAME_KEY=$1
START_CLOCK=$2
PLAY_CLOCK=$3
NUM_GAMES=$4
P1=$5
P2=$6

P1_HOST=127.0.0.1
P1_PORT=9147
P2_HOST=127.0.0.1
P2_PORT=9148

timestamp=$(date +%Y%m%d_%H%M%S)
TOURNAMENT_DIR=$GAME_KEY"-"$P1"-vs-"$P2"-"$timestamp

mkdir $TOURNAMENT_DIR
echo "Created directory for tournament details: $TOURNAMENT_DIR"

echo "Starting player $P1 on port $P1_PORT and writing output to $P1.log"
#./playerRunner.sh $P1_PORT $P1 & #this won't let me kill the process so call gradlew directly
./gradlew -q playerRunner -Pport=$P1_PORT -Pgamer=$P1 &> $TOURNAMENT_DIR/$P1.log &

echo "Starting player $P2 on port $P2_PORT and writing output to $P2.log"
#./playerRunner.sh $P2_PORT $P2 & #this won't let me kill the process so call gradlew directly
./gradlew -q playerRunner -Pport=$P2_PORT -Pgamer=$P2 &> $TOURNAMENT_DIR/$P2.log &

# wait for 10 seconds then make sure we have 2 jobs running (the two players)
echo "Waiting for players to start..."
sleep 10
jobs > /dev/null # run the jobs command without $() to make sure jobs is up to date
jobCount=$(jobs -p | wc -l)
if [ $jobCount -ne 2 ];then
    echo "Players didn't start properly. See player logs"
    kill $(jobs -p)
    exit 1
fi

echo "Starting the game server"
#./gameServerRunner.sh $TOURNAMENT_DIR $GAME_KEY $START_CLOCK $PLAY_CLOCK $NUM_GAMES $P1_HOST $P1_PORT $P1 $P2_HOST $P2_PORT $P2
./gradlew -q gameServerRunner -Pmyargs="$TOURNAMENT_DIR $GAME_KEY $START_CLOCK $PLAY_CLOCK $NUM_GAMES $P1_HOST $P1_PORT $P1 $P2_HOST $P2_PORT $P2" 2>&1 | tee $TOURNAMENT_DIR/server.log
rc=$?

echo "Killing the player processes"
kill $(jobs -p)

exit $rc
