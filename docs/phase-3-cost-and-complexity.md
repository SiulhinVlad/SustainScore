# Phase 3: Cost and Complexity

## 1. Function Point Estimate

Function Point Analysis estimates software size based on the functionality provided to the user. The estimate is based on five categories:

- External Inputs: data or commands entered by the user.
- External Outputs: information produced by the system.
- External Inquiries: user requests that retrieve information.
- Internal Logical Files: data stored and managed by the application.
- External Interface Files: external services or data sources used by the application.

## 2. Function Point Counting Table

| Category | Description in SustainScore | Count | Complexity | Weight | Function Points |
|---|---|---:|---|---:|---:|
| External Inputs | Log activity, edit/delete activity, select time period, request feedback | 4 | Average | 4 | 16 |
| External Outputs | Sustainability score, comparison chart, daily summary, weekly summary, AI feedback | 5 | Average | 5 | 25 |
| External Inquiries | View activity history, view predefined activity list | 2 | Low | 3 | 6 |
| Internal Logical Files | Activity types, activity logs, user summaries/settings | 3 | Average | 10 | 30 |
| External Interface Files | AI feedback service | 1 | Average | 7 | 7 |
| **Total Unadjusted Function Points** | - | - | - | - | **84** |

The estimated unadjusted size of the application is:

```text
84 Function Points
```

## 3. Effort Estimate

The Function Point estimate gives the functional size of the application. 

According to the course slides, Function Points can be converted into an approximate code size using a language factor:

```text
Estimated LOC = Function Points x Language Factor
```

The course slides provide the following example conversion factors:

```text
Java approx. 53 LOC/FP
Python approx. 15 LOC/FP
```

Since SustainScore is planned as an Android application, the Java factor is used as an approximation for Android/Kotlin development:

```text
84 Function Points x 53 LOC/FP = 4,452 LOC
```

This gives an estimated code size of approximately:

```text
4.45 KSLOC
```

This estimate is as an approximate software size. The real code size may differ because Kotlin syntax, Android framework code, libraries, and generated files can affect the final number of lines.

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

The actual effort will be completed progressively as the project is developed. 

| Phase | Planned Effort | Actual Effort | Difference |
|---|---:|---:|---:|
| Phase 1: Idea and scoping | 5 hours | To be completed | To be completed |
| Phase 2: Requirements | 10 hours | To be completed | To be completed |
| Phase 3: Cost and complexity | 6 hours | To be completed | To be completed |
| Phase 4: Design | 12 hours | To be completed | To be completed |
| Phase 5: Implementation | 35 hours | To be completed | To be completed |
| Phase 6: CI/CD | 8 hours | To be completed | To be completed |
| Phase 7: Testing | 10 hours | To be completed | To be completed |
| Phase 8: Final report and demo | 8 hours | To be completed | To be completed |
| **Total** | **73 hours** | **To be completed** | **To be completed** |

## 6. Complexity Metrics

Complexity metrics will be measured after the implementation phase starts, because they must be calculated on the actual project code.

The project will track the following metrics:

### 6.1 Cyclomatic Complexity

Cyclomatic complexity measures how many independent execution paths exist in a function or method.

Planned rule:

```text
Each function should ideally have cyclomatic complexity <= 10.
```

Functions with higher complexity should be reviewed and simplified if possible.

Expected areas where complexity may appear:

- Sustainability score calculation.
- Recommended profile generation.
- Daily and weekly summary logic.
- Improvement-rate calculation.

### 6.2 Coupling

Coupling measures how strongly different modules depend on each other.

Planned rule:

```text
The user interface should not directly contain scoring, summary, or AI feedback logic.
```

The intended module separation is:

- UI module: screens and user interaction.
- Data model module: activity types, activity logs, scores, and summaries.
- Scoring module: sustainability score calculation.
- Summary module: daily and weekly analytics.
- AI feedback module: preparation of feedback data and generation of suggestions.

### 6.3 Maintainability

Maintainability will be evaluated by checking whether:

- Functions are short and focused.
- Business logic is separated from the UI.
- Important logic has unit tests.
- Names of classes, functions, and variables are understandable.
- Duplicate logic is avoided.

## 7. Planned Complexity Table

The following table will be completed after implementation.

| Code Area | Metric | Target | Actual Result |
|---|---|---:|---|
| Scoring logic | Cyclomatic complexity | <= 10 per function | To be completed |
| Summary logic | Cyclomatic complexity | <= 10 per function | To be completed |
| Improvement-rate calculation | Cyclomatic complexity | <= 10 per function | To be completed |
| UI layer | Coupling | No business logic in UI | To be completed |
| Data layer | Coupling | Independent from UI | To be completed |
| AI feedback logic | Coupling | Uses summarized data only | To be completed |
