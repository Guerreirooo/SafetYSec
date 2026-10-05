# SafetYSec — Real-Time Safety Monitoring App

## Overview

SafetYSec is an Android application developed in Kotlin with Jetpack Compose, designed for real-time safety monitoring of vulnerable individuals such as elderly people or people with special needs.

The application allows monitors to supervise protected users through real-time GPS tracking, configurable safety rules, automatic alerts, geofencing, fall and accident detection, and emergency SOS features.

## Features

- Real-time GPS location tracking
- Two different user roles:
  - Monitor — manages protected users, creates safety rules and receives alerts
  - Protected — accepts rules, triggers SOS alerts and cancels false alarms
- Email/Password authentication
- Google Sign-In authentication
- Password recovery through email
- Configurable monitoring schedules
- Fall detection using the device accelerometer
- Accident detection based on severe impacts
- Configurable speed limit alerts
- Geofencing for predefined safe areas
- Inactivity detection
- Manual emergency SOS button
- Configurable SOS countdown timer
- PIN-protected SOS cancellation
- On-screen emergency notifications
- OTP-based monitor/protected-user pairing
- Rule acceptance and rejection by protected users
- English and Portuguese (Portugal) languages

## Safety Rules

SafetYSec supports multiple types of automatic safety rules:

| **Rule** | **Description** |
| -------- | --------------- |
| Fall Detection | Detects possible falls using accelerometer G-Force measurements |
| Accident Detection | Detects severe physical impacts |
| Speed Limit | Generates an alert when the configured speed is exceeded |
| Geofence | Generates an alert when a protected user leaves a predefined safe area |
| Inactivity | Generates an alert after a configured period without activity |

## System Architecture

The application follows an MVVM architecture combined with the Repository Pattern.

The main components are:

### UI Layer

The UI is built using Jetpack Compose and is responsible for rendering the application's screens and reusable components.

The application provides separate interfaces for monitors and protected users, with navigation handled through Navigation Compose.

The UI layer contains:

- Authentication screens
- Monitor screens
- Protected user screens
- Reusable drawer and scaffold components
- Navigation routes

### ViewModel Layer

The ViewModel layer manages UI state and application logic that needs to survive configuration changes.

The `AuthViewModel` is responsible for authentication-related state and communication with the authentication repository.

### Repository Layer

Repositories isolate Firebase operations from the rest of the application.

The main repositories are:

- `AuthRepository` — authentication and account management
- `UserRepository` — user information and profiles
- `RulesRepository` — safety rules and rule assignments
- `AlertsRepository` — safety and emergency alerts
- `AssociationRepository` — monitor/protected-user relationships

This separation keeps Firebase-specific operations outside the UI and makes the application easier to maintain and extend.

### Service Layer

`BackgroundLocationService` implements the application's continuous monitoring functionality using an Android Foreground Service.

The service is responsible for:

- Obtaining GPS location updates
- Processing location information
- Monitoring device movement
- Checking configured safety rules
- Triggering alerts when conditions are detected
- Supporting monitoring while the application is running in the background

### Utility Layer

The utility components provide specialized functionality:

- `GeofenceChecker` — calculates distances using the Haversine formula and checks whether a user has left a safe area
- `LocationHandler` — manages location-related operations
- `VideoRecorder` — provides CameraX-based video recording functionality

The application models include:

- `User`
- `Alert`
- `Rule`
- `RuleAssignment`
- `Association`
- `OtpCode`

## Monitor and Protected User Workflow

The application is based on two main user roles.

### Monitor

A monitor can:

- Associate protected users with their account
- Create and configure safety rules
- Monitor protected users
- Receive safety alerts
- Manage monitoring schedules
- Configure speed limits and geofences

### Protected User

A protected user can:

- Accept or reject assigned safety rules
- Share their location with associated monitors
- Trigger an emergency SOS
- Cancel false alarms using the configured protection mechanism
- Operate the application while background monitoring is active

## Monitor-Protected User Association

SafetYSec uses a 6-digit OTP-based pairing system to associate monitors with protected users.

The system supports:

- OTP-based account pairing
- Multiple monitors associated with one protected user
- Rule assignment between monitors and protected users
- Rule acceptance or rejection

This allows a protected user to be monitored by multiple trusted monitors when required.

## Emergency System

The application includes a dedicated emergency workflow.

When an SOS or automatic safety event is triggered:

1. The application detects the emergency condition.
2. An alert is generated.
3. A configurable countdown allows the protected user to cancel a false alarm.
4. The cancellation process is PIN protected.
5. Monitors receive the corresponding emergency notification.

The application also supports full-screen notifications, allowing emergency alerts to be displayed even when the device is locked.
