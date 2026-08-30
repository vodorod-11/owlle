# owlle 🦉

A golden, Cupertino-styled mail client. Kotlin Multiplatform + Compose Multiplatform,
desktop-first (macOS / Windows / Linux via the JVM target), with Android and iOS
targets planned next.

owlle is also the reference client for the **OWL protocol** (Open Web of Letters) —
a progressive-enhancement layer over standard email. Phase 1 (this repo) is the
plain mail client; OWL features arrive in phase 2.

## Modules

- **`core`** — KMP library. Domain models, the `MailBackend` transport abstraction,
  RFC 6154 special-use folder mapping with name heuristics, and a local-first
  `MailRepository` over a SQLDelight/SQLite cache. The JVM source set implements
  `ImapBackend` with Jakarta/Angus Mail.
- **`app`** — Compose Multiplatform UI. Golden Cupertino theme, account setup,
  and the classic three-pane shell (folders / message list / reading pane).

## Run it

Requires JDK 21.

```bash
./gradlew :app:run
```

Sign in with any IMAP account (host, port 993, username, password). Gmail and
most large providers require an **app password** — plain passwords are rejected.

## Current scope (walking skeleton)

- Connect to one IMAP account (SSL or STARTTLS)
- Folder list with special-use detection (Inbox / Sent / Drafts / Junk / Trash /
  Archive) plus custom folders
- Envelope list (latest 50) cached in SQLite — reopening a folder is instant,
  refresh hits the server
- Read messages (text/plain preferred, HTML fallback stripped to text)

## Next milestones

1. Compose/send over SMTP, message actions (move, delete, mark) with an
   offline pending-operations queue
2. Secure credential storage (Keychain / libsecret / DPAPI) — passwords are
   currently held in memory only and never written to disk
3. OAuth2 (PKCE, system browser) for Gmail XOAUTH2 and Microsoft accounts;
   Microsoft Graph backend behind the same `MailBackend` interface
4. IMAP IDLE push on desktop; Android target
5. iOS target (shared Rust protocol core via UniFFI/Gobley is the leading plan)

## Architecture notes

Decisions of record live with the team: KMP/Compose over React Native
(no RN Linux target), Microsoft Graph over IMAP for Outlook accounts,
local-first SQLite everywhere, and the Mailspring-style local/remote
operation queue for offline actions.
