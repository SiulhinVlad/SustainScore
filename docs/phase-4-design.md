# Phase 4: Design

## 1. Architecture Diagram

SustainScore will use a modular architecture. The user interface will be separated from the application logic, data storage, and AI feedback logic.

```mermaid
flowchart TB
    User["User"]

    subgraph UI["User Interface Layer"]
        Dashboard["Dashboard Screen"]
        LogScreen["Activity Logging Screen"]
        SummaryScreen["Summary Screen"]
        FeedbackScreen["Feedback Screen"]
    end

    subgraph Domain["Application Logic Layer"]
        ScoreCalc["Score Calculator"]
        Recommendation["Recommended Profile Generator"]
        SummaryLogic["Summary Generator"]
        Improvement["Improvement Rate Calculator"]
    end

    subgraph Data["Data Layer"]
        Repository["Activity Repository"]
        LocalStorage["Local Storage"]
    end

    subgraph AI["AI Feedback Layer"]
        PromptBuilder["Feedback Data Builder"]
        AIService["AI Feedback Service"]
    end

    User <--> Dashboard
    User <--> LogScreen
    Dashboard <--> ScoreCalc
    Dashboard <--> Recommendation
    Dashboard <--> SummaryLogic
    LogScreen <--> Repository
    SummaryScreen <--> SummaryLogic
    FeedbackScreen <--> PromptBuilder
    ScoreCalc <--> Repository
    Recommendation <--> Repository
    SummaryLogic <--> Repository
    Improvement <--> SummaryLogic
    Repository <--> LocalStorage
    PromptBuilder <--> AIService
```

## 2. Main Modules

### 2.1 User Interface Module

The user interface module contains the screens that the user interacts with.

Main responsibilities:

- Display the current sustainability score.
- Allow the user to log activities.
- Show actual vs. recommended behavior.
- Show daily and weekly summaries.
- Show AI-generated feedback.

Planned screens:

- `DashboardScreen`
- `LogActivityScreen`
- `SummaryScreen`
- `FeedbackScreen`

### 2.2 Data Model Module

The data model module defines the main objects used by the application.

Main responsibilities:

- Represent predefined activity types.
- Represent user activity logs.
- Represent sustainability scores.
- Represent daily and weekly summaries.
- Represent AI feedback data.

Planned classes:

- `ActivityType`
- `ActivityLog`
- `SustainabilityScore`
- `RecommendedProfile`
- `SummaryReport`
- `FeedbackRequest`

### 2.3 Scoring Module

The scoring module calculates the user's sustainability score from logged activities.

Main responsibilities:

- Calculate hourly scores.
- Apply activity weights.
- Ensure scores stay within a valid range.

Planned class:

- `ScoreCalculator`

### 2.4 Recommendation Module

The recommendation module generates a recommended sustainable behavior profile.

Main responsibilities:

- Compare user activity with recommended limits.
- Generate recommended hourly scores.
- Support actual vs. recommended chart comparison.

Planned class:

- `RecommendedProfileGenerator`

### 2.5 Summary Module

The summary module creates daily and weekly summaries.

Main responsibilities:

- Identify high-consumption periods.
- Identify best and worst periods.
- Calculate improvement rate.

Planned classes:

- `SummaryGenerator`
- `ImprovementRateCalculator`

### 2.6 Data Storage Module

The data storage module manages local persistence.

Main responsibilities:

- Save activity logs.
- Load activity logs.
- Provide data to scoring and summary logic.

Planned class:

- `ActivityRepository`

### 2.7 AI Feedback Module

The AI feedback module prepares summarized data for feedback generation.

Main responsibilities:

- Prepare summarized input for the AI service.
- Return short actionable suggestions to the user.

Planned classes:

- `FeedbackDataBuilder`
- `AIFeedbackService`

## 3. Main Classes

| Class | Purpose |
|---|---|
| `ActivityType` | Defines a predefined activity, such as lighting or washing machine usage. |
| `ActivityLog` | Stores one user activity entry with type, time, and quantity or duration. |
| `SustainabilityScore` | Represents an actual or recommended score for a given time period. |
| `RecommendedProfile` | Represents a more sustainable behavior scenario. |
| `SummaryReport` | Stores daily or weekly summary results. |
| `ScoreCalculator` | Calculates sustainability scores from activity logs. |
| `RecommendedProfileGenerator` | Generates recommended scores for comparison. |
| `SummaryGenerator` | Creates daily and weekly summaries. |
| `ImprovementRateCalculator` | Calculates whether behavior improved or declined. |
| `ActivityRepository` | Saves and loads activity data from local storage. |
| `FeedbackDataBuilder` | Prepares summarized data for AI feedback. |
| `AIFeedbackService` | Generates textual improvement suggestions. |


## 4. UI Mockups

The following wireframes show the planned main screens. They are simple mockups and may be refined during implementation.

### 4.1 Dashboard Screen

```text
+----------------------------------+
| SustainScore                     |
+----------------------------------+
| Today Score                      |
| 78 / 100                         |
+----------------------------------+
| Actual vs Recommended Chart      |
| [hourly bar chart]               |
+----------------------------------+
| [Log Activity]  [View Summary]   |
| [Get Feedback]                   |
+----------------------------------+
```

Purpose:

- Show the user's current sustainability score.
- Show actual vs. recommended behavior.
- Provide quick access to main actions.

### 4.2 Activity Logging Screen

```text
+----------------------------------+
| Log Activity                     |
+----------------------------------+
| Activity Type                    |
| [Lighting usage v]               |
|                                  |
| Quantity / Duration              |
| [ 2 lights ]                     |
|                                  |
| Time                             |
| [ 18:00 ]                        |
+----------------------------------+
| [Save Activity]                  |
+----------------------------------+
```

Purpose:

- Allow users to log predefined sustainability-related activities.
- Keep data entry simple and fast.

### 4.3 Summary Screen

```text
+----------------------------------+
| Daily / Weekly Summary           |
+----------------------------------+
| Average Score: 78                |
| Best Period: 10:00 - 12:00       |
| Highest Consumption: 18:00       |
| Improvement Rate: +6%            |
+----------------------------------+
| Summary Chart                    |
| [daily or weekly chart]          |
+----------------------------------+
```

Purpose:

- Show the user's sustainability performance over time.
- Help the user identify patterns and progress.

### 4.4 Feedback Screen

```text
+----------------------------------+
| Improvement Feedback             |
+----------------------------------+
| Your highest consumption period  |
| was in the evening. Try reducing |
| unnecessary lighting and shorter |
| appliance usage during that time.|
+----------------------------------+
| [Back to Dashboard]              |
+----------------------------------+
```

Purpose:

- Display short, actionable AI-generated suggestions.
- Keep feedback based on summarized user data.

## 5. Data Model

The data model defines the main information used by the application.

```mermaid
classDiagram
    class ActivityType {
        String id
        String name
        String unit
        Double weight
        Double recommendedLimit
    }

    class ActivityLog {
        String id
        String activityTypeId
        String timestamp
        Double quantity
        Double duration
    }

    class SustainabilityScore {
        String date
        Int hour
        Double actualScore
        Double recommendedScore
    }

    class RecommendedProfile {
        String date
        List recommendedScores
    }

    class SummaryReport {
        String period
        Double currentScore
        String bestPeriod
        String worstPeriod
        Double improvementRate
    }

    class FeedbackRequest {
        Double currentScore
        String highestConsumptionPeriod
        String mainActivityImpact
        Double improvementRate
    }

    ActivityType "1" --> "many" ActivityLog
    ActivityLog --> SustainabilityScore
    SustainabilityScore --> SummaryReport
    SummaryReport --> FeedbackRequest
```

## 6. Implementation Deviations

The design above records the planned architecture before implementation. During development, the architecture was refined based on the available time and the behavior of the working prototype.

| Planned Element | Final Implementation | Reason for Change |
|---|---|---|
| Separate `SummaryScreen` and `FeedbackScreen` | Daily and weekly summaries and feedback are displayed directly on `DashboardScreen` | Keeps the main user flow simpler and reduces unnecessary navigation. |
| `RecommendedProfileGenerator` | Recommended scores are calculated by `ScoreCalculator` and `HourlyScoreCalculator` | The recommended profile uses the same activity logs with sustainable quantity limits, so a separate class was not necessary. |
| `SummaryGenerator` | Separate `DailySummaryGenerator` and `WeeklySummaryGenerator` | Makes daily and weekly responsibilities clearer. |
| External `AIFeedbackService` | Local `DailyAiReportGenerator` | Avoids sending data to an external service and keeps the prototype achievable within the project timeline. |
| Persistent `ActivityRepository` and local storage | Activity logs are kept in Compose state during the app session | Persistent storage remains future work. |
| Basic chart visualization | Interactive `DailyUsageChart` | The final chart supports day navigation, recommended overlays, selecting blocks, moving blocks, and deleting segments. |

The main implemented classes are:

- `MainActivity`
- `DashboardRoute`
- `DashboardScreen`
- `DailyUsageChart`
- `LogActivityScreen`
- `ScoreCalculator`
- `HourlyScoreCalculator`
- `DailySummaryGenerator`
- `WeeklySummaryGenerator`
- `DailyAiReportGenerator`
