---
type: Specification
title: Overview
description: What KS Expense Manager is for, the platforms it runs on and its tech stack.
tags: [overview, platform, stack]
status: draft
---

# Overview

A minimalist, privacy-first personal finance and expense tracker. All data stays on the device.

| | |
|---|---|
| **Platform** | Android |
| **Tech stack** | Kotlin, Jetpack Compose, Room |

## Stack

- Kotlin, Jetpack Compose (Material 3), Room, SharedPreferences for the one setting (no DataStore, no chart library)
- Package: `com.ks.expense`. Features: `transactions/`, `budgets/`, `reports/`
- minSdk 26 · targetSdk latest stable

What it does and the roadmap: [Build plan](plan.md).
