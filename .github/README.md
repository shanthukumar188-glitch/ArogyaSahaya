# GitHub Workflow Support

This repository includes a GitHub Actions workflow for Android CI.

- `.github/workflows/android-ci.yml` runs `./gradlew build` on Ubuntu.
- The workflow uses JDK 17 and the Gradle wrapper stored under `ArogyaSahayaV2/`.

The workflow file is intentionally lightweight and does not affect app runtime behavior.
