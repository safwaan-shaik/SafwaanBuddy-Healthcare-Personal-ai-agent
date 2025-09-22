# How to Build Your APK Using GitHub Actions

Since you don't have admin access to install Java on your corporate machine, the best approach is to use GitHub Actions to build your APK in the cloud.

## Prerequisites
- A GitHub account (you already have this since the repo is set up)
- A web browser

## Steps to Trigger the Build

### 1. Go to Your Repository
1. Open your web browser
2. Navigate to: https://github.com/safwaan-shaik/SafwaanBuddy-Healthcare-Personal-ai-agent

### 2. Access the Actions Tab
1. Click on the "Actions" tab in your repository
   ![Actions Tab](demo-assets/actions-tab.png)

### 3. Find the Android Build Workflow
1. Look for the "Android Build" workflow in the list
2. Click on it to open the workflow details

### 4. Run the Workflow
1. Click the "Run workflow" dropdown button
   ![Run Workflow](demo-assets/run-workflow.png)
2. Select the "update-workflow" branch (this is the branch with your latest changes)
3. Click the green "Run workflow" button

### 5. Monitor the Build Process
1. The build will start automatically
2. You can watch the progress in real-time as each step completes
3. The build typically takes 5-10 minutes to complete

### 6. Download Your APK from Artifacts
1. Once the build is successful, you'll see a green checkmark
   ![Success](demo-assets/build-success.png)
2. Scroll down to the "Artifacts" section
   ![Artifacts](demo-assets/artifacts.png)
3. You'll see two artifacts:
   - `debug-apk` - Contains your debug APK
   - `release-apk` - Contains your release APK (unsigned)
4. Click on `debug-apk` to download it
5. Extract the ZIP file to get your `app-debug.apk`

## Troubleshooting

### If the Build Fails
1. Click on the failed build to see the logs
2. Look for error messages in the log output
3. Common issues:
   - Missing dependencies (should be handled by Gradle)
   - Configuration issues (check build.gradle files)
   - Network issues (temporary, try again)

### If You Don't See the Workflow
1. Make sure you're on the correct branch
2. Check that the workflow file exists at `.github/workflows/android-build.yml`
3. Verify that GitHub Actions is enabled for your repository

## Alternative: Create a New Release

You can also create a release which will automatically trigger the build:

### 1. Go to Releases
1. Click on the "Releases" link in your repository
   ![Releases](demo-assets/releases.png)

### 2. Create a New Release
1. Click "Draft a new release"
2. Create a new tag (e.g., v1.0.0)
3. Add a title and description
4. Click "Publish release"

This will automatically trigger the build workflow.

## Next Steps After Building

### Install on Your Device
1. Transfer the APK to your Android device
2. Open the APK file on your device
3. Allow installation from unknown sources if prompted
4. Install the app

### Test the Features
1. Launch the app
2. Test the voice assistant capabilities
3. Try the medication reminder system
4. Check the healthcare personality features

## Need Help?

If you encounter any issues:
1. Check the GitHub Actions logs for detailed error messages
2. Open an issue in your repository with the error details
3. Refer to the documentation in the README.md file

---

*Last Updated: September 22, 2025*