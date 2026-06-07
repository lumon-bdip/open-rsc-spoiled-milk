# Chat And Dialogue Channel Plan

This plan reorganizes the custom client's chat tabs so they behave like useful channels instead of loosely named history panels. It also lays out a path toward allowing multiple players to talk to the same NPC without leaking dialogue to nearby players.

## Target Behavior

- `All` shows every message category.
- `Local` replaces the current `Chat history` label and shows nearby player chat plus global chat.
- `Game` replaces the current `Quest history` label and shows game action messages and NPC dialogue.
- `Private` remains for private messages.
- `Clan` remains for clan messages.
- Global chat appears in both `All` and `Local`.
- Server broadcast/system announcements go to every visible channel.
- NPC dialogue is private to the player interacting with that NPC, both in the chat tab and on-screen.

## Existing Shape

- Client tabs are hardcoded in `Client_Base/src/orsc/mudclient.java`.
- The current tab enum is `ALL`, `CHAT`, `QUEST`, `PRIVATE`, `CLAN` in `Client_Base/src/orsc/enumerations/MessageTab.java`.
- Client and server both share message type ids for `GAME`, `PRIVATE_RECIEVE`, `PRIVATE_SEND`, `QUEST`, `CHAT`, `FRIEND_STATUS`, `TRADE`, `INVENTORY`, `GLOBAL_CHAT`, and `CLAN_CHAT`.
- Many server-side game/action messages are currently sent as `MessageType.QUEST`.
- `GLOBAL_CHAT` currently feeds the private message panel on the client.
- NPC dialogue is still protected by a per-NPC `interactingPlayer` lock, so one active talk script can block others.

## Stage 1: Rename And Clarify Existing Tabs

Risk: low.

Status: implemented in the custom client. The underlying enum names remain unchanged for compatibility.

Tasks:

- Rename client label `Chat history` to `Local`.
- Rename client label `Quest history` to `Game`.
- Rename client label `All messages` to `All`.
- Keep the underlying enum names unchanged initially to reduce blast radius.
- Update comments or nearby constants so future work treats `QUEST` tab as the Game tab until the enum is renamed.

Acceptance checks:

- The bottom chat labels display `All`, `Local`, `Game`, `Private`, and `Clan`.
- No packet or save compatibility changes are introduced.
- Existing chat still appears somewhere after the rename.

## Stage 2: Make All Truly All

Risk: low to medium.

Status: started. The `All` right-click/history filter no longer maintains a separate whitelist of visible message types.

Tasks:

- Audit all client-side checks that special-case `MessageTab.ALL`.
- Ensure `All` displays every message type with a stored history entry.
- Ensure right-click/context behavior in the `All` tab does not accidentally exclude game, inventory, or broadcast messages.
- Review auto-switch behavior so game messages do not unexpectedly pull the player back to `All` unless that is intentionally desired.

Acceptance checks:

- Local chat, global chat, private, clan, game/action, inventory, friend status, trade, and broadcasts all appear in `All`.
- Messages remain in history consistently instead of feeling like they clear early.
- Selecting another tab is not immediately overridden by ordinary game/action messages unless configured.

## Stage 3: Route Global Chat To Local And All

Risk: low to medium.

Status: implemented. `GLOBAL_CHAT` is routed to the Local panel instead of the Private panel, while remaining visible in All history. Server-side global-friend fallback messages now use `GLOBAL_CHAT` instead of the old Quest bucket.

Tasks:

- Stop adding `GLOBAL_CHAT` to the private message panel.
- Add `GLOBAL_CHAT` to the Local panel.
- Confirm `GLOBAL_CHAT` also appears in `All`.
- Preserve private-message formatting for true private messages only.

Acceptance checks:

- Global chat is visible in `All`.
- Global chat is visible in `Local`.
- Global chat is not mixed into `Private`.
- Private messages still show in `Private` and `All`.

## Stage 4: Define Game Message Routing

Risk: medium.

Status: started. The current Game tab accepts existing `QUEST`, `GAME`, and `INVENTORY` message types, but server-side message categorization still needs a broader audit.

Tasks:

- Treat the existing `QUEST` message type as the custom client's Game channel for now, or introduce a new `GAME_ACTION`/`GAME_CHANNEL` type if compatibility needs are clear.
- Move obvious game/action feedback into the Game channel:
  - Skilling text such as mining, woodcutting, fishing, harvesting, and crafting action messages.
  - NPC dialogue text sent by plugin helpers.
  - Quest-progress/game feedback currently sent as `MessageType.QUEST`.
- Avoid moving player chat, private chat, clan chat, trade requests, or global chat into Game.
- Keep old/vanilla client fallback behavior intact by mapping Game-channel messages back to `QUEST` or `GAME` for clients that do not understand custom routing.

Acceptance checks:

- Game actions appear in the `Game` tab and `All`.
- NPC dialogue appears in the `Game` tab and `All`.
- The Local tab is no longer cluttered by routine action spam except for global chat.
- Older clients still receive readable messages.

## Stage 5: Broadcast Messages To Every Channel

Risk: medium.

Status: implemented for existing server/world/system broadcast helpers. A custom-client `BROADCAST` message type is routed into every chat panel.

Tasks:

- Define one server helper for system broadcasts instead of manually sending the same text through several code paths.
- On the custom client, add broadcast entries to every panel: All, Local, Game, Private, and Clan.
- Decide whether broadcasts should also flash all inactive tabs or only show a distinct broadcast color.
- For old clients, send a normal server message fallback.

Acceptance checks:

- A system broadcast is visible regardless of the selected chat tab.
- Broadcasts do not duplicate multiple times inside `All`.
- Broadcast formatting is distinct enough to identify as server-wide.

## Stage 6: Make NPC Dialogue Private To The Interacting Player

Risk: medium to high.

Status: implemented at the update-delivery layer. Recipient-targeted NPC and scripted player dialogue is now sent only to the participating player; public chat and public NPC lines remain visible normally. Concurrent dialogue is still blocked until Stage 7.

Tasks:

- Update NPC overhead/chat-message delivery so recipient-targeted NPC chat is only sent to the intended player.
- Do this server-side rather than only hiding it client-side, so other clients never receive private dialogue data.
- Review `GameStateUpdater.updateNpcAppearances`, where NPC chat messages are currently queued for every player who has that NPC local.
- Route NPC dialogue text into the Game channel for the interacting player.
- Preserve public NPC shouts/combat barks where they are intended to be seen by nearby players.

Acceptance checks:

- If two players stand beside the same NPC, each player only sees their own dialogue.
- NPC combat barks or public ambient lines can still be visible to nearby players when intentionally sent as public.
- NPC dialogue appears in Game and All for the interacting player.

## Stage 7: Allow Concurrent NPC Dialogue For Safe Scripts

Risk: high.

Tasks:

- Replace the current default per-NPC talk lock with a per-player dialogue/session model for normal talk scripts.
- Keep an explicit exclusive lock available for scripts that mutate shared NPC state, move/despawn the NPC, start combat, spawn quest NPCs, or otherwise rely on single-player ownership.
- Audit common helper functions such as `say`, `multi`, `mes`, and related RuneScript/Functions helpers so they do not depend on `npc.getInteractingPlayer()` as the single source of truth.
- Fix or verify helper cleanup, including suspicious busy/unbusy behavior.
- Add regression tests around two players talking to the same banker/shop/travel/static NPC.
- Add targeted tests for quest NPCs that must remain exclusive.

Acceptance checks:

- Two players can talk to ordinary NPCs at the same time.
- One player's menu selection does not affect another player's menu.
- Exclusive quest/event NPCs can still block concurrent interaction when required.
- Ending, logging out, moving away, entering combat, or timing out cleans up only that player's dialogue session.

## Stage 8: Optional Real System Channel

Risk: highest UI complexity.

This is optional because broadcasts can be sent to every existing channel without adding a sixth tab.

Tasks:

- Add a new `SYSTEM` message type to both client and server.
- Add a sixth `SYSTEM` tab only if the bottom UI can be expanded cleanly.
- Update tab sprites or replace hardcoded five-slot tab hitboxes with layout-driven tab bounds.
- Add a dedicated message panel, scroll handling, activity flashing, and click handling for the new tab.
- Decide whether system broadcasts should live only in System plus All, or continue appearing in every channel.

Acceptance checks:

- The sixth tab fits at desktop and mobile sizes.
- Text does not overlap the input box or existing tab labels.
- System messages can be filtered without losing important broadcasts.
- Old clients continue receiving readable fallback server messages.

## Suggested Implementation Order

1. Rename tab labels.
2. Make `All` include all message types consistently.
3. Move global chat into Local and All.
4. Route game/action/NPC text into the Game tab.
5. Add proper broadcast fan-out to every channel.
6. Make NPC dialogue private by recipient.
7. Allow concurrent dialogue for safe NPC scripts.
8. Consider a real System tab only after the five-channel model feels stable.
