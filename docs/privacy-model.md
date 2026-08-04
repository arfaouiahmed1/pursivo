# Privacy model

Pursivo has no account system, analytics SDK, advertising SDK, crash reporter, or application backend. Applications, profile data, timelines, tasks, provider configuration, AI history and imported documents live in app-private storage.

The only expected network paths are:

1. A user-triggered request to the selected AI provider after the context preview is available.
2. A public GitHub Releases metadata request on foreground launch at most once per 24 hours, or when the user taps **Check for updates**.
3. A release download explicitly approved by the user.

Provider secrets are encrypted with an AES key held by Android Keystore. Keys are excluded from Room, DataStore, logs, backups and Compose state. Android's system picker supplies imported documents; Pursivo copies them into its private vault. Exported backups contain personal job-search data and should be stored accordingly.

Uninstalling Pursivo removes its private database, documents and encrypted credentials unless the user exported a backup. Automatic Android cloud backup is disabled.
