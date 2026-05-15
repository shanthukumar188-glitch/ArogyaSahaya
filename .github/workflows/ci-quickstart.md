# CI Quickstart

This project uses GitHub Actions to run Android CI.

- Workflow file: `.github/workflows/android-ci.yml`
- Runs on `ubuntu-latest`
- Uses JDK 17 via `actions/setup-java`
- Builds the app with `./gradlew build`
