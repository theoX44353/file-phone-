#!/bin/bash -e

echo "Running tests to update data in build directory..."

# re-run tasks here because gradle may consider the task "up to date" even though the source files have changed
function run_tests() {
  ./gradlew --continue --rerun-tasks :test --tests="com.google.devsite.integration.*" $@
}

rm -f ./build/exploded/vision-interfaces-16.0.0.jar ./build/exploded/uiautomator-2.2.0.jar ./build/exploded/play-services-*

if run_tests $@ ; then
   echo "Test data is already up to date."
   exit 0
fi

echo "Updating test data..."

for dir in testData/*
do
    [[ "$dir" =~ package-lists ]] && continue
    if [ -d "$dir" ]; then
        baseDir=$(basename "$dir")
        echo "Updating test files for $dir..."
	# the cp command should not fail when there are no docs (the hidden-package test)
        rm -rf "testData/${baseDir}/docs" && cp -r "build/docs/testData/${baseDir}/docs" "testData/${baseDir}/docs" || true
    fi
done

echo "Test data successfully updated."
