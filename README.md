<img src="assets-store/icon-512.png" width="96" alt="App icon">

# ICU Antibiotic Time-out

An Android app for antimicrobial stewardship in the ICU. It tracks every antibiotic course from its start and flags the 72-hour time-out review (24 hours for prophylaxis).

## What it does
- Log each antibiotic: bed number, code name, start time, indication (empirical / targeted / prophylaxis), rationale, suspected source, planned duration, cultures sent before first dose
- Single time-out deadline: 72 h, or 24 h for prophylaxis
- Record the decision at time-out: continue, de-escalate, IV to oral, escalate or stop, with reasons and the culture result (organism, specimen, sensitive/resistant)
- Change antibiotics in the same window: keep the current antibiotic and add new ones, or stop it and replace — each new antibiotic is linked to the original record
- Empirical courses at 72 h and prophylaxis at 24 h without a time-out turn purple and flash until someone acknowledges them
- Flags courses running past their planned stop date
- Phone notifications for time-outs, reviews and planned stop dates (works with the app closed and after restart)
- Edit any entry without losing its history
- Backup to a file (Drive, WhatsApp, email) and restore by merging
- Export a PDF report for any date range: 12 indicators, breakdowns (AWaRe, indication, source, organisms, rationale, decisions) and a full line listing
- WHO AWaRe category tag for each antibiotic

## Data and privacy
- Data is saved on the phone only. It is not sent anywhere and is not shared between devices. Use Backup regularly.
- Identify patients by bed number and a code name only. Never enter real names or IP numbers.

This tool supports, and does not replace, the hospital antibiotic policy.

## Author
Dr. Kiran Rajagopal — Head, Department of Critical Care Medicine, Sree Gokulam Medical College, Trivandrum

## Android app (APK)
Every change pushed to this repository builds a new APK automatically.
Download the latest from **Releases → AntibioticTimeout.apk**:
https://github.com/kiranrajagopal-code/antibiotic-timeout/releases/latest

Install on Android: open the downloaded APK and allow "Install unknown apps" for your browser or Files app when asked. Updates install over the old version and keep the data.

To change the app, edit `index.html` only — the build copies it into the Android app.
