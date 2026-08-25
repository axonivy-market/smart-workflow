# Release Process

This document outlines the step-by-step process for creating a new release of Smart Workflow.

## Release Steps

### Step 1: Update Dependencies

Check for [Renovate bot PR](https://github.com/axonivy-market/smart-workflow/pulls) and update dependencies if necessary.

### Step 2: Verify Compatibility with Latest Designer

1. Download the latest designer to your local PC
2. Import Smart Workflow projects (including all provider projects in the `models/` folder) to the latest Designer
3. Fix any problems such as:
   - Compile errors
   - Validation errors
4. Convert all projects to the latest ivy version

### Step 3: Verify AI Provider Compatibility

1. Run the [E2E build](https://github.com/axonivy-market/smart-workflow/actions/workflows/e2e.yml) to execute all integration tests for AI providers:
   - **Branch:** `master`
2. Fix any issues that arise during testing

### Step 4: Update Release Drafter

1. Run the [Release Drafter](https://github.com/axonivy-market/smart-workflow/actions/workflows/release-drafter.yml) build:
   - **Branch:** `master`
2. Go to the [Releases page](https://github.com/axonivy-market/smart-workflow/releases) to review the Next Release notes
3. Check, update, and rebuild to ensure quality release notes

### Step 5: Create Release Build

1. Run the [Release](https://github.com/axonivy-market/smart-workflow/actions/workflows/release.yml) build with the following parameters:
   - **Branch:** `master`
   - **Version:** Verify the upcoming version from the [Releases](https://github.com/axonivy-market/smart-workflow/releases) page
   - **Format example:** `14.0.0-b1` → Displays as `14.0.0-beta1` on the Releases page

### Step 6: Merge Release PR

1. After the build from Step 5 completes successfully, a "Release" PR will be created
2. Review all `pom.xml` files and revert the snapshot version:
   - **Example:** `14.0.0-b1-SNAPSHOT` → `14.0.0-SNAPSHOT`
3. Merge the PR to the `master` branch

### Step 7: Publish Release Notes

1. Navigate to the [Releases](https://github.com/axonivy-market/smart-workflow/releases) page
2. Edit the "Next Release" with:
   - **Tag:** The target release version
   - **Release title:** The release version in full format
   - **Example:** For version `14.0.0-b1`, use title `14.0.0-beta1`
3. Publish the release

## Notes

- Always double-check version numbers before creating releases
- Ensure all tests pass before proceeding with the release
- Keep release notes clear and comprehensive for users
