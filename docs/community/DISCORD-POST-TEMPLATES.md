# Discord Post Templates

Copy these into the Discord server after creating the channels in
[DISCORD-SERVER-SETUP.md](DISCORD-SERVER-SETUP.md). Replace bracketed text such
as `[VERSION]` before posting.

## Welcome

Post in `#welcome`.

```text
Welcome to the Spoiled Milk Discord.

Spoiled Milk is a heavily changed RuneScape Classic project. This server is for
release updates, alpha testing, bug reports, balance feedback, suggestions, and
normal community chat.

Start here:

1. Read #rules.
2. Read #how-to-play.
3. Download the client from #downloads.
4. Ask install questions in #installation-help.
5. Report bugs in #bug-reports.
6. Put balance thoughts in #balance-feedback.
7. Put new ideas in #suggestions-and-requests.

GitHub is still the official home for releases, source code, and confirmed bug
tracking:
https://github.com/An-actual-duck/open-rsc-spoiled-milk
```

## Rules

Post in `#rules`.

```text
Rules

1. Be decent to each other. Disagreement is fine. Personal attacks are not.
2. Keep feedback useful. Say what happened, where it happened, and what version
   you were using.
3. Do not post slurs, harassment, threats, sexual content, or shock content.
4. Do not spam channels, pings, screenshots, or repeated bug reports.
5. Do not share account passwords, private info, or another person's private
   messages.
6. Report exploits, item duplication, account problems, and security issues to
   staff instead of posting them publicly.
7. This is an alpha. Balance, drops, XP, maps, and mechanics can change.
8. Staff may move, close, or delete posts to keep the server usable.
```

## How To Play

Post in `#how-to-play`.

```text
How to play

1. Go to #downloads.
2. Download the latest release from GitHub.
3. Extract the zip file into its own folder.
4. Windows package: double-click "Play Spoiled Milk.cmd".
5. Java package: install Java 17 or newer, then run the play script for your
   system.

The normal launcher checks for updates before starting the client.

Your character progress is stored on the hosted server, not inside your client
folder. If you update or replace the client folder, your hosted character should
still be there.

If the game does not start, post in #installation-help with your operating
system, which zip you downloaded, and any error message you see.
```

## Downloads

Post in `#downloads`.

```text
Downloads

Latest releases:
https://github.com/An-actual-duck/open-rsc-spoiled-milk/releases

Player download instructions:
https://github.com/An-actual-duck/open-rsc-spoiled-milk/blob/main/docs/releases/PLAYER-DOWNLOADS.md

Most Windows players should use the windows-x64 zip because it includes Java.
Players on macOS or Linux should use the java zip and install Java 17 or newer.

The updater checks GitHub for newer versions. If it says you are already up to
date, you do not need to download a new zip manually.
```

## Known Issues

Post in `#known-issues`.

```text
Known issues

This channel lists problems that are already known, so testers do not need to
report the same thing again.

Current known issues:

- [Add current issue]
- [Add current issue]

If you hit one of these but have new details, reply in the matching bug report
or post in #bug-reports and mention that it may be related.
```

## Bug Report Guidelines

Post as the pinned guideline in `#bug-reports`.

```text
Bug reports

Please make one post per bug.

Use this shape:

Title:
Short name for the problem.

Version:
Example: v0.2.6

What happened:
What you saw.

What you expected:
What you thought should happen instead.

Steps to reproduce:
1. Go here.
2. Click this.
3. See the problem.

Character/context:
Skill levels, quest state, item used, monster fought, location, or anything else
that matters.

System:
Windows/macOS/Linux, and whether you used the windows-x64 or java zip.

Evidence:
Screenshot, error text, or log snippet if you have it.

Confirmed bugs may be moved to GitHub Issues for long-term tracking.
```

## Balance Feedback Guidelines

Post as the pinned guideline in `#balance-feedback`.

```text
Balance feedback

Please make one post per topic.

Use this shape:

Topic:
What system, monster, item, skill, spell, drop, or area are you talking about?

Version:
Example: v0.2.6

Character/context:
Your relevant level, gear, location, enemy, tool, or method.

What feels off:
Too slow, too fast, too punishing, too rewarding, confusing, not worth using,
or something else.

What you compared it to:
Another item, method, enemy, skill, area, or earlier version.

How much did you test:
One quick attempt, several trips, one hour, many hours, etc.

Suggested direction:
Buff, nerf, explain better, change requirements, change rewards, or leave it
alone but watch it.
```

## Suggestions And Requests Guidelines

Post as the pinned guideline in `#suggestions-and-requests`.

```text
Suggestions and requests

Please make one post per idea.

Use this shape:

Idea:
Short name for the request.

Problem or opportunity:
What would this improve?

Suggested change:
What should be added, removed, or changed?

Why it fits Spoiled Milk:
Why this belongs in this project.

What it should not break:
Any existing balance, progression, quest, item, economy, or usability concern.
```

## Installation Help Guidelines

Post as the pinned message in `#installation-help`.

```text
Installation help

If the game does not launch, post:

1. Your operating system.
2. Which package you downloaded: windows-x64 or java.
3. The version you downloaded.
4. What you clicked or ran.
5. The exact error message, if there is one.
6. A screenshot if the error is hard to copy.

Do not post passwords, private account info, or anything from your computer that
you are not comfortable sharing.
```

## Release Announcement

Post a short version in `#announcements`.

```text
Release Updates

Spoiled Milk [VERSION] is live.

Download:
[GITHUB_RELEASE_URL]

Short version:
- [Highlight 1]
- [Highlight 2]
- [Highlight 3]

The client updater should pick this up automatically. If it does not, download
the matching zip from the release page.

Report bugs in #bug-reports and install problems in #installation-help.
```

Only include `@Release Updates` at the top if the release matters enough to
ping people who opted in.

## Release Notes

Post the longer version in `#release-notes`.

```text
Spoiled Milk [VERSION] release notes

Download:
[GITHUB_RELEASE_URL]

Highlights:
- [Player-facing change]
- [Player-facing change]
- [Player-facing change]

Fixes:
- [Bug fix]
- [Bug fix]

Balance:
- [Balance change]
- [Balance change]

Known issues:
- [Known issue or "No new known issues."]

Testing focus:
- [What you want testers to pay attention to.]

Bug reports:
Use #bug-reports. Confirmed actionable bugs may be copied to GitHub Issues:
https://github.com/An-actual-duck/open-rsc-spoiled-milk/issues
```

## Server Launch Announcement

Post in `#announcements` when the Discord is ready.

```text
The Spoiled Milk Discord is open for invited testers.

This server is for release updates, install help, bug reports, balance feedback,
suggestions, screenshots, and general discussion.

Please read #rules, #how-to-play, and #downloads first.

Use:
- #installation-help if the client will not launch
- #bug-reports for bugs
- #balance-feedback for tuning feedback
- #suggestions-and-requests for ideas
- #general for normal discussion

Release downloads and source code stay on GitHub:
https://github.com/An-actual-duck/open-rsc-spoiled-milk
```

## Staff Triage Checklist

Post in `#triage`.

```text
Triage checklist

For each new bug report:

1. Is it understandable?
2. Does it include version and context?
3. Can someone reproduce it?
4. Is it already known?
5. Does it need more information?
6. Is it an exploit or account/security issue that should be private?
7. Should it become a GitHub Issue?

Status tags:
- new: not reviewed yet
- needs-info: waiting for details
- confirmed: reproduced or strongly supported
- github: copied to GitHub Issues
- duplicate: same as another report
- fixed: fixed in a published release
- wont-fix: intentionally not changing
```

## GitHub Issue From Discord

Use this when copying a confirmed Discord bug into GitHub.

```text
Title:
[Short bug title]

Reported from Discord:
[Discord post link]

Version:
[VERSION]

Actual behavior:
[What happened]

Expected behavior:
[What should have happened]

Steps to reproduce:
1. [Step]
2. [Step]
3. [Step]

Context:
[Character state, location, item, NPC, system, operating system, package type]

Evidence:
[Screenshots, logs, extra notes]
```
