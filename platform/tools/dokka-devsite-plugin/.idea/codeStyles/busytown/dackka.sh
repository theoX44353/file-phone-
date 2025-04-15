#!/bin/bash
set -e

# resolve directories
cd "$(dirname $0)/.."
if [ "$OUT_DIR" == "" ]; then
  OUT_DIR="./out"
fi
mkdir -p "$OUT_DIR"
export OUT_DIR="$(cd $OUT_DIR && pwd)"
if [ "$DIST_DIR" == "" ]; then
  DIST_DIR="$OUT_DIR/dist"
fi
mkdir -p "$DIST_DIR"

/usr/bin/time -o $DIST_DIR/build_time_raw.txt ./gradlew --no-daemon --offline --stacktrace --scan

unformattedtime=$(< $DIST_DIR/build_time_raw.txt)
realtime=`echo $unformattedtime | sed 's/real.*//g'`
realtime=$(echo "scale=0;1000*$realtime" | bc)
usertime=`echo $unformattedtime | sed 's/user.*//g' | sed 's/.*real //g'`
usertime=$(echo "scale=0;1000*$usertime" | bc)

echo "{ \"dackka_build_test_real_time\": $realtime, \"dackka_build_test_user_time\": $usertime }" > $DIST_DIR/build_time.txt
