# SustainShare

An Android demonstration of predictive energy sharing for the University of Luxembourg's **Applications of AI** course and AI Café poster workshop. This extends the team's original [SustainScore](https://github.com/SiulhinVlad/SustainScore) project, created for Principles of Software Development.

The app keeps the original activity logger, editable daily usage chart, sustainability scores and summaries. The new sections compare the household's upcoming demand with a fictional energy-sharing community and offer practical changes to household routines.

## Demo video and Android download

[Watch or download the one-minute demo](https://github.com/SiulhinVlad/SustainScore/blob/main/video/SustainShare-demo-1minute.mp4) · [Direct MP4](https://raw.githubusercontent.com/SiulhinVlad/SustainScore/main/video/SustainShare-demo-1minute.mp4) · [Download the demo APK](https://github.com/SiulhinVlad/SustainScore/raw/refs/heads/main/SustainShare-demo.apk)

The video shows activity history, the initial calculation button, aligned personal and community forecasts, realistic suggestions, and a forecast update after logging heating. It runs for exactly 60 seconds, with embedded captions and no audio. AI behaviour and neighbour data are simulated for the course demonstration.

## Open and run

Open the **`app` directory inside this repository** in Android Studio, allow Gradle sync, select the `app` run configuration, then run on an emulator or Android device (Android 9/API 28 or later).

The prepared `SustainShare-demo.apk` in the repository root can also be installed directly. The debug build is for demonstration, not store distribution.

```sh
cd app
./gradlew assembleDebug
```

The project uses the original Gradle wrapper and Android dependencies. Its daemon requires Java 21, which is bundled with the installed Android Studio. `local.properties` points to the local Android SDK and is intentionally ignored by Git. The internal `com.example.sustainscore` package ID is retained, so installing this build updates an existing installation with that ID.

## Present the demo

1. Start at the dashboard. It begins with seven days of clearly labelled sample history, including morning lighting, evening screens, weekend laundry and occasional heating. Today retains the original three sample activities.
2. Scroll past the existing summaries to **Predictive energy sharing**. Initially, a large **Calculate prediction with AI** button fills the section. Suggestions wait for this calculation.
3. Press the button. Two connected-dot charts appear: your daily predicted kWh, then the sum for four fictional neighbouring homes. Both cover tomorrow through the following six days, with exactly aligned date positions. Each has its own labelled vertical scale.
4. Tap either chart to select the same day on both and see exact values. **Update prediction with AI** remains as a smaller button.
5. Read **AI Generated Suggestions** at the bottom. It connects the forecast with actual log patterns, such as morning presence, weekend laundry or overlapping evening loads, and proposes quieter daytime windows.
6. Add usage using **Log Activity**. For a visible change, select Heating or Cooling, quantity `1`, duration `02:00`, start time `19:00`, then Save. Return to the forecasts: personal demand and suggestions update automatically. Moving or deleting an existing usage segment also refreshes them.

Logs and the calculated state persist locally across app restarts. To replay the first-launch presentation, use Android Settings > Apps > SustainShare > Storage > Clear storage. This removes demo edits and restores the starter history on the next launch.

## What the demonstration calculates

- **Personal demand:** the last 28 calendar days of valid logged activity, split at local hourly/day boundaries. A matching weekday profile contributes 70%, the average observed-day profile 30%. Missing weekdays fall back to that average. Dates without logs are treated as unobserved, not confirmed zero usage.
- **Estimated appliance power:** lighting 0.01 kW per unit, washing machine 0.7 kW, computer/TV 0.12 kW and heating/cooling 1.5 kW. These are illustrative assumptions, not meter measurements. Energy is quantity × duration in hours × assumed kW. An assumed 0.12 kWh hourly household background adds 2.88 kWh/day.
- **Community demand:** a repeatable hourly profile for four sample homes, with busier weekends and evenings. It excludes the current user.
- **Solar scenario:** an illustrative midday community generation curve used to explain sharing opportunities. It is not live weather or solar telemetry.
- **Suggestions:** deterministic local rules inspect logged categories/times, personal forecasts, community demand and the sample solar window. Presence is phrased as a possibility, and proposed changes remain optional. No savings or grid independence are claimed.

No AI API, trained model, live neighbour data or real energy transfer is used. This deliberately simulates the proposed AI workflow for the course demo and runs offline after installation. Refreshing unchanged logs produces the same forecast.

## Validation

```sh
cd app
./gradlew testDebugUnitTest lintDebug assembleDebug
./gradlew connectedDebugAndroidTest
```

New unit tests cover forecast dates, sparse/empty history, adding/moving/deleting usage, midnight boundaries, daylight saving, repeatability and suggestions when no spare solar remains. The new emulator test covers the initial button, revealing and selecting charts, the update button, changed forecasts after logging, persistence across activity recreation and the suggestions section. Original calculation and navigation tests remain in place.

## Project origin

SustainScore was developed by Vladyslav Siulhin and Pariya Palizban. The SustainShare course project is by Vladyslav Siulhin, Pariya Palizban and Mykyta Yampolskyi. The repository's original documentation remains under `docs/`; Git's `origin` still points to the team's SustainScore repository. This checkout contains the SustainShare course demonstration, including its one-minute video and installable debug APK.
