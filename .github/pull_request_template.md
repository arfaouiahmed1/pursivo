## Outcome

Describe the user-visible result.

## Privacy and network impact

- [ ] No new data leaves the device.
- [ ] Any new network request is user-visible, bounded, and documented.
- [ ] No provider key or personal payload can enter logs, backups, or saved UI state.

## Verification

- [ ] `./gradlew detekt test lintDebug assembleDebug`
- [ ] Tested light and dark theme
- [ ] Tested large text / accessibility impact
- [ ] Added migration or provider contract tests when applicable
