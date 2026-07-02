# Spoiled Milk Discord Server Setup

This is the first-pass setup for an invited-tester Discord server. Think of the
server like a clubhouse:

- categories are shelves
- channels are rooms
- roles are permission stickers
- GitHub releases are the official download shelf
- GitHub Issues are the permanent bug notebook

Discord should make it easy for testers to talk, report problems, and notice new
releases. It should not replace GitHub as the official release and bug archive.

## Launch Defaults

- Audience: invited testers.
- Automation: manual release posts at first.
- Release pings: use a `Release Updates` role, not `@everyone`.
- Bug tracking: report in Discord first, then move confirmed actionable bugs to
  GitHub Issues.
- In-game Discord integration: leave disabled in `server/myworld-host.conf`
  unless there is a later explicit plan to enable it.

## Before You Start

Have these open in browser tabs:

- GitHub releases:
  `https://github.com/An-actual-duck/open-rsc-spoiled-milk/releases`
- GitHub issues:
  `https://github.com/An-actual-duck/open-rsc-spoiled-milk/issues`
- Player download instructions:
  `https://github.com/An-actual-duck/open-rsc-spoiled-milk/blob/main/docs/releases/PLAYER-DOWNLOADS.md`
- Official Discord setup references:
  `https://support.discord.com/hc/en-us/articles/204849977-How-do-I-create-a-server`
  `https://support.discord.com/hc/en-us/articles/206029707-Setting-Up-Permissions-FAQ`
  `https://support.discord.com/hc/en-us/articles/360047132851-Enabling-Your-Community-Server`
  `https://support.discord.com/hc/en-us/articles/360032008192-Announcement-Channel-FAQ`
  `https://support.discord.com/hc/en-us/articles/6208479917079-Forum-Channels-FAQ`

Optional server icon: start with `Client_Base/src/res/icon.png` or a clean
in-game screenshot. Discord's icon crop works best with a square image.

## Step 1: Create The Server

1. Open Discord.
2. Press the plus button in the server list.
3. Choose `Create My Own`.
4. Choose the community/friends option if Discord asks.
5. Name the server `Spoiled Milk`.
6. Add the icon if you have one ready.
7. Press `Create`.

Do not invite testers yet. Build the rooms first.

## Step 2: Enable Community Mode

Community mode is worth enabling because Discord's Forum and Announcement
channel types require it.

1. Open `Server Settings`.
2. Choose `Enable Community`.
3. Follow Discord's setup prompts.
4. Use `#rules` as the rules channel after it exists.
5. Use `#mod-log` as the moderator/community-updates channel after it exists.
6. Keep the verified-email and explicit-media safety settings enabled.

If Discord asks for rules/moderator channels before you have made them, create
temporary channels, finish Community setup, then rename or replace them with the
channels below.

## Step 3: Create Roles

Create these roles in `Server Settings > Roles`.

| Role | Purpose | Important permissions |
| --- | --- | --- |
| `Admin` | Full server control for the owner/trusted operators. | Administrator |
| `Moderator` | Helps clean up messages and triage reports. | Manage Messages, Manage Threads, View Channels, Send Messages |
| `Tester` | Normal invited player role. | No broad server permissions; channel permissions grant access |
| `Release Updates` | Optional ping role for release notices. | No permissions; keep role mention disabled except while posting |
| `Muted` | Temporary timeout role if needed. | No permissions; denied Send Messages in public categories |

Keep `@everyone` weak. It should not have Manage Server, Manage Channels,
Manage Roles, Administrator, Kick Members, Ban Members, Manage Webhooks, or
Mention Everyone.

## Step 4: Create Categories And Channels

Create these categories and channels. Use exact names so templates and release
checklists stay easy to follow.

### START HERE

| Channel | Type | Who can post |
| --- | --- | --- |
| `#welcome` | Text | Admin, Moderator |
| `#rules` | Text | Admin, Moderator |
| `#how-to-play` | Text | Admin, Moderator |
| `#downloads` | Text | Admin, Moderator |

Permissions:

- `@everyone`: View Channel only.
- `Tester`: View Channel only.
- `Admin` and `Moderator`: View Channel, Send Messages, Manage Messages.

### NEWS

| Channel | Type | Who can post |
| --- | --- | --- |
| `#announcements` | Announcement | Admin, Moderator |
| `#release-notes` | Text | Admin, Moderator |
| `#known-issues` | Text | Admin, Moderator |

Permissions:

- `@everyone`: View Channel only.
- `Tester`: View Channel only.
- `Admin` and `Moderator`: View Channel, Send Messages, Manage Messages.

Use `#announcements` for short notices. Use `#release-notes` for the longer
versioned release post.

### FEEDBACK

Use Forum channels if Discord offers them. If Forum is missing, use Text
channels for now and convert later after Community mode is enabled.

| Channel | Type | Who can post |
| --- | --- | --- |
| `#bug-reports` | Forum | Tester, Admin, Moderator |
| `#balance-feedback` | Forum | Tester, Admin, Moderator |
| `#suggestions-and-requests` | Forum | Tester, Admin, Moderator |
| `#installation-help` | Text | Tester, Admin, Moderator |

Forum settings:

- Default layout: List View.
- Hide after inactivity: 1 week.
- Require tags if Discord offers that switch.
- `#bug-reports` tags: `new`, `needs-info`, `confirmed`, `github`, `duplicate`,
  `fixed`, `wont-fix`.
- `#balance-feedback` tags: `combat`, `skilling`, `items`, `economy`, `ui`,
  `needs-more-data`.
- `#suggestions-and-requests` tags: `content`, `quality-of-life`, `ui`,
  `balance`, `declined`, `accepted`.

Permissions:

- `@everyone`: no access if you want only assigned testers talking.
- `Tester`: View Channel, Send Messages/Create Posts, Add Reactions.
- `Admin` and `Moderator`: View Channel, Send Messages, Manage Messages,
  Manage Threads.
- `Muted`: deny Send Messages/Create Posts.

### COMMUNITY

| Channel | Type | Who can post |
| --- | --- | --- |
| `#general` | Text | Tester, Admin, Moderator |
| `#spoiled-milk-chat` | Text | Tester, Admin, Moderator |
| `#screenshots` | Text | Tester, Admin, Moderator |
| `#off-topic` | Text | Tester, Admin, Moderator |

Permissions:

- `@everyone`: no access if you want only assigned testers talking.
- `Tester`: View Channel, Send Messages, Add Reactions, Attach Files.
- `Admin` and `Moderator`: View Channel, Send Messages, Manage Messages.
- `Muted`: deny Send Messages.

### STAFF

| Channel | Type | Who can post |
| --- | --- | --- |
| `#triage` | Text | Admin, Moderator |
| `#release-drafts` | Text | Admin, Moderator |
| `#mod-log` | Text | Admin, Moderator |

Permissions:

- `@everyone`: no access.
- `Tester`: no access.
- `Admin` and `Moderator`: View Channel, Send Messages, Manage Messages.

## Step 5: Add Starter Posts

Use the copy-paste text in
[DISCORD-POST-TEMPLATES.md](DISCORD-POST-TEMPLATES.md).

Post these before inviting testers:

1. `#welcome`: welcome post.
2. `#rules`: rules post.
3. `#how-to-play`: how to play post.
4. `#downloads`: downloads post.
5. `#known-issues`: current known issues post.
6. `#bug-reports`: bug report instructions or forum guidelines.
7. `#balance-feedback`: balance feedback instructions or forum guidelines.
8. `#suggestions-and-requests`: suggestion/request instructions or forum
   guidelines.

Pin each starter post in its channel.

## Step 6: Test Before Inviting Players

Use this checklist before sending any invites:

- `@everyone` cannot post in `START HERE`, `NEWS`, or `STAFF`.
- A user with only `Tester` can post in `FEEDBACK` and `COMMUNITY`.
- A user with only `Tester` cannot see `STAFF`.
- A user with `Muted` cannot post in public channels.
- `#announcements`, `#release-notes`, and `#known-issues` are read-only for
  testers.
- The `Release Updates` role cannot be mentioned by regular testers.
- Forum tags exist and are easy to understand.
- The download link points to the GitHub releases page.
- The bug-report text points testers to Discord first and GitHub for confirmed
  long-term tracking.

## Step 7: Invite Testers

1. Create a temporary invite link.
2. Give each invited player the `Tester` role after they join.
3. Ask whether they want the `Release Updates` role.
4. Keep the first wave small enough that you can answer install questions.

Recommended first message to invited testers:

```text
Welcome. Please read #rules and #how-to-play first. Downloads are in
#downloads. If the game will not start, post in #installation-help. Bugs go in
#bug-reports, balance thoughts go in #balance-feedback, and bigger ideas go in
#suggestions-and-requests.
```

## Operating Rules

- Post one release note per published GitHub version.
- Do not use `@everyone` for routine releases.
- Move confirmed bugs into GitHub Issues once they have clear steps or enough
  evidence to act on.
- Keep balance threads open until there is more than one data point or a clear
  design decision.
- Close duplicate forum posts with a link to the main post.
- Put exploit reports or account/security problems in staff triage, not public
  discussion.
- Leave game-side Discord bot/webhook settings off until the server has stable
  private staff channels and a separate implementation plan.
