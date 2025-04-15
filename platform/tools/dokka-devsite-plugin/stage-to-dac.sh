#!/bin/bash -e
#
# Script to stage integration tests on DAC.
#

source gbash.sh || exit

DEFINE_string test_name --alias=t "topLevelFunctions" "The integration test name."
DEFINE_string db "$USER" "The database onto which tests will be staged."

gbash::set_usage "./stage-to-dac.sh [options]" \
   "  DAC Theatre is a tool that helps you stage integrations tests on devsite." \
   ""

gbash::init_google "$@"

start_dir="$PWD"
readonly path="testData/${FLAGS_test_name}/docs/reference"

readonly client="$(p4 g4d -f tmp-dokka-devsite)"
cd "$client"

/google/data/ro/projects/devsite/devsite2 provision --db="${FLAGS_db}"
cp -r "$start_dir/$path" third_party/devsite/android/en/
cp "$start_dir/testData/book.yaml" third_party/devsite/android/en/reference/dokkatest/_book.yaml
cp "$start_dir/testData/kotlin-book.yaml" third_party/devsite/android/en/reference/kotlin/dokkatest/_book.yaml
p4 reopen

/google/data/ro/projects/devsite/devsite2 stage --db="${FLAGS_db}" \
  --use_large_thread_pools \
  --upload_safety_check_mode=ignore \
  "third_party/devsite/android/en/*.*" \
  "third_party/devsite/android/en/assets" \
  $(find "$start_dir/$path" -type d \
  | sed "s!$start_dir/$path!!" \
  | sed 's/$/\/*.*/' \
  | sed 's/^/third_party\/devsite\/android\/en\/reference/')

cd "$start_dir"
