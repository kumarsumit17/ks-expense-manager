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
| **Tech stack** | Kotlin, Jetpack Compose, Room, DataStore |

## Stack

- Kotlin, Jetpack Compose (Material 3), Room, DataStore (no chart library: the category bars are plain Compose)
- Package: `com.ks.expense`. Features: `transactions/`, `budgets/`, `reports/`
- minSdk 26 · targetSdk latest stable

What it does and the roadmap: [Build plan](plan.md).
