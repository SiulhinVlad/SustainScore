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

