# Player Release Checklist

GitHub release assets are created locally and uploaded manually. Replace
`RELEASE_VERSION` below with the tag being published, for example `v0.2.6`.

## Publication Gates

- Retain Pimen's confirmation permitting distribution with source code
  available.
- Record the royalty-free source and applicable license for the prayer UI
  power, protection, enchanting XP, smithing XP, and crafting XP icons.
- Retain the recorded creator permissions and license notes for Pixerelia and
  the other credited UI icon sources used in `dev/myworld/assets/credit`.
- Confirm the packaged jar has no removed CraftPix icon or Phoenix/Kraken
  animation resources.
- Before public source distribution, confirm repository history does not
  contain removed CraftPix files; deleting them in a later commit is not
  sufficient once they have been pushed.
- Obtain a current Eclipse Temurin Java 17 Windows x64 JRE distribution and
  retain its included license and notice files. This can be prepared from Linux
  with `./scripts/download-windows-jre.sh`; Windows is only needed for final
  launch smoke testing.
- Decide the live host name and TCP port; do not publish packages targeting
  `localhost`.
- Commit and push the exact source revision being packaged, including any
  validated pre-release gameplay fixes.
- Draft the Discord release announcement and release notes using
  [../community/DISCORD-POST-TEMPLATES.md](../community/DISCORD-POST-TEMPLATES.md).

## Build Assets

From the packaged revision:

```bash
./scripts/test.sh
./scripts/build-server.sh
./scripts/download-windows-jre.sh
./scripts/run-hosted-server.sh
./scripts/package-player-release.sh \
  --version RELEASE_VERSION \
  --host YOUR_PUBLIC_HOST_OR_IP \
  --port 43605 \
  --windows-jre output/runtimes/temurin-17-windows-x64-jre \
  --assets-cleared
```

Use the same public host/IP and TCP port that your router forwards to the
machine running `scripts/run-hosted-server.sh`. Do not upload packages built
with a placeholder host.

The packager writes ignored artifacts under
`output/releases/RELEASE_VERSION/`:

- `spoiled-milk-RELEASE_VERSION-java.zip`
- `spoiled-milk-RELEASE_VERSION-windows-x64.zip`
- `SHA256SUMS.txt`

The client jar embeds runtime PNG assets under `myworld-assets/`; player
archives do not need the loose development asset tree.

The Windows archive can be built on Linux because it only stages the downloaded
Windows runtime files. Local validation confirms the expected `runtime/bin/java.exe`
layout, endpoint config, archive contents, and checksums; actual Windows launch
verification still requires a Windows machine or a tester.

## Verify And Publish

1. Extract each archive into a clean directory and confirm it contains no
   `uid.dat`, saved credentials, local settings, server database, or inherited
   OpenRSC launcher.
2. Confirm the archive endpoint files contain the public host/IP and port:
   `Cache/ip.txt` and `Cache/port.txt`.
3. Launch the Windows archive on Windows and the Java archive with Java 17+,
   then connect to the hosted candidate server.
4. Verify account creation, login, logout, and reconnect before publication.
5. Create GitHub release `RELEASE_VERSION` and upload the two
   zip files and `SHA256SUMS.txt`.
6. Download the uploaded files again and run:

```bash
sha256sum -c SHA256SUMS.txt
```

7. Link invited testers to the GitHub release and
   [PLAYER-DOWNLOADS.md](PLAYER-DOWNLOADS.md).
8. Post the short Discord notice in `#announcements`.
9. Post the longer Discord notes in `#release-notes`.
10. Update `#known-issues` if the release adds or resolves notable known
    problems.
11. Watch `#installation-help` and `#bug-reports` after publication for update
    failures, launch problems, and regressions.
