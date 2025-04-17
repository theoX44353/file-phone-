find ./src/test/java/com/google/devsite/ -name "*.kt" -exec sed -i '' 's/@Ignore/\/\/ @DontIgnore/g' {} +
./gradlew test
find ./src/test/java/com/google/devsite/ -name "*.kt" -exec sed -i '' 's/\/\/ @DontIgnore/@Ignore/g' {} +
