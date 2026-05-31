# Phase 3: Cost and Complexity

## 1. Function Point Estimate

Function Point Analysis estimates software size based on the functionality provided to the user. The estimate is based on five categories:

- External Inputs: data or commands entered by the user.
- External Outputs: information produced by the system.
- External Inquiries: user requests that retrieve information.
- Internal Logical Files: data stored and managed by the application.
- External Interface Files: external services or data sources used by the application.

## 2. Function Point Counting Table

The following table records the estimate made during the planning phase.

| Category | Description in SustainScore | Count | Complexity | Weight | Function Points |
|---|---|---:|---|---:|---:|
| External Inputs | Log activity, edit/delete activity, select time period, request feedback | 4 | Average | 4 | 16 |
| External Outputs | Sustainability score, comparison chart, daily summary, weekly summary, AI feedback | 5 | Average | 5 | 25 |
| External Inquiries | View activity history, view predefined activity list | 2 | Low | 3 | 6 |
| Internal Logical Files | Activity types, activity logs, user summaries/settings | 3 | Average | 10 | 30 |
| External Interface Files | AI feedback service | 1 | Average | 7 | 7 |
| **Total Unadjusted Function Points** |  |  |  |  | **84** |

The estimated unadjusted size of the application was:

```text
84 Function Points
```

## 3. Estimated and Actual Code Size

According to the course slides, Function Points can be converted into an approximate code size using a language factor:

```text
Estimated LOC = Function Points x Language Factor
```

The course slides provide Java as approximately:

```text
53 LOC / Function Point
```

Since SustainScore is an Android application implemented in Kotlin, the Java factor was used as an approximate planning reference:

```text
84 Function Points x 53 LOC/FP = 4,452 LOC
```

This gives an estimated code size of:

```text
4.45 KSLOC
```

The final Kotlin source size was measured from the repository:

| Code Area | Kotlin Files | Actual Source Lines of Code |
|---|---:|---:|
| Main application code | 19 | 2,213 SLOC |
| Unit tests | 12 | 873 SLOC |
| Android instrumented tests | 3 | 202 SLOC |
| **Total Kotlin code** | **34** | **3,288 SLOC** |

The final main application code is smaller than the initial Java-based estimate. Kotlin is generally more concise than Java, and Android libraries provide functionality that does not need to be implemented directly in the project source code.

The initial Function Point table is kept unchanged because it records the planning assumptions. During implementation, the planned external AI feedback service was replaced by a local feedback generator. If the Function Point count were recalculated for the final implemented scope, removing the external interface file would reduce the count by 7 points:

```text
84 FP - 7 FP = 77 FP
```

## 4. Planned Effort by Phase

| Phase | Planned Effort |
|---|---:|
| Phase 1: Idea and scoping | 5 hours |
| Phase 2: Requirements | 5 hours |
| Phase 3: Cost and complexity | 5 hours |
| Phase 4: Design | 7 hours |
| Phase 5: Implementation | 30 hours |
| Phase 6: CI/CD | 7 hours |
| Phase 7: Testing | 7 hours |
| Phase 8: Final report and demo | 7 hours |
| **Total** | **73 hours** |

## 5. Planned vs. Actual Effort

| Phase | Planned Effort | Actual Effort | Difference |
|---|---:|---:|---:|
| Phase 1: Idea and scoping | 5 hours | 4 hours | 1 hour |
| Phase 2: Requirements | 5 hours | 5 hours | 0 hours |
| Phase 3: Cost and complexity | 5 hours | 4 hours | 1 hours |
| Phase 4: Design | 7 hours | 5 hours | 2 hours |
| Phase 5: Implementation | 30 hours | 23 hours | 7 hours |
| Phase 6: CI/CD | 7 hours | 8 hours | 1 hour |
| Phase 7: Testing | 7 hours | 8 hours | 1 hour |
| Phase 8: Final report and demo | 7 hours | 8 hours | 1 hour |
| **Total** | **73 hours** | **48 hours** | **14 hours** |

## 6. Complexity Metrics

Complexity metrics were measured after implementation because they must be calculated on the actual project code.

### 6.1 Cyclomatic Complexity

Cyclomatic complexity measures how many independent execution paths exist in a function or method.

Target:

```text
Each function should ideally have cyclomatic complexity <= 10.
```

The table below uses a static approximation based on conditional branches, loops, and boolean decision points in the Kotlin source code.

| Function | File | Approximate Cyclomatic Complexity | Result |
|---|---|---:|---|
| `parseDurationMinutes` | `LogActivityScreen.kt` | 8 | Within target |
| `parseTimeOfDay` | `LogActivityScreen.kt` | 8 | Within target |
| `buildStackedUsageLayout` | `DailyUsageChart.kt` | 8 | Within target |
| `generate` | `DailySummaryGenerator.kt` | 3 | Within target |
| `calculateActualImpact` | `ScoreCalculator.kt` | 2 | Within target |
| `calculateRecommendedImpact` | `ScoreCalculator.kt` | 2 | Within target |

The interactive chart is the main complexity hotspot because it supports layout calculation, overlapping activities, day navigation, moving activity blocks, and deleting segments. The logic is split into helper functions and covered by unit tests.

### 6.2 Coupling

Coupling measures how strongly different modules depend on each other.

The implemented project keeps dependencies mostly one-directional:

- Model classes do not depend on UI or domain classes.
- Domain classes depend on model classes only.
- UI classes use the domain and model classes to display calculated results.
- `DashboardScreen.kt` is the main integration point and imports 17 internal project classes.
- Domain classes import between 2 and 3 internal project classes each.
- Model classes import no UI or domain classes.

This structure keeps the score and summary logic testable outside the Compose UI.

### 6.3 Maintainability

Maintainability was evaluated by checking whether:

- Functions are short and focused.
- Business logic is separated from the UI where possible.
- Important logic has unit tests.
- Names of classes, functions, and variables are understandable.
- Duplicate logic is avoided.

## 7. Final Complexity Table

| Code Area | Metric | Target | Actual Result |
|---|---|---:|---|
| Scoring logic | Cyclomatic complexity | <= 10 per function | Maximum observed: 2 |
| Summary logic | Cyclomatic complexity | <= 10 per function | Maximum observed: 3 |
| Input parsing | Cyclomatic complexity | <= 10 per function | Maximum observed: 8 |
| Chart layout logic | Cyclomatic complexity | <= 10 per function | Maximum observed: 8 |
| UI layer | Coupling | No scoring or summary formulas embedded directly in UI | Achieved |
| Model layer | Coupling | Independent from UI and domain classes | Achieved |
| Tests | Quality support | Core logic covered by automated tests | Achieved |
