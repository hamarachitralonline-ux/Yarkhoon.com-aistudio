================================================================================
ANDROID SIGNING FILES & GOOGLE PLAY CONSOLE GUIDE
================================================================================

This package contains the signing credentials and public certificate for the
Android application:
  Package Name (Application ID): com.aistudio.yarkhoon.scbqdm

--------------------------------------------------------------------------------
1. FILES INCLUDED
--------------------------------------------------------------------------------

- upload_certificate.pem
  The public certificate exported in standard Privacy-Enhanced Mail (PEM) format.
  This contains only the public key and identity information, making it safe
  to share and upload to external services.

- debug.keystore
  The Android signing keystore containing the project's signing key and
  certificate chain. This is used by the Android build tools (Gradle / Android Studio)
  to digitally sign release Android App Bundles (.aab) and APK packages.

--------------------------------------------------------------------------------
2. KEY ALIAS
--------------------------------------------------------------------------------

- Key Alias: androiddebugkey

--------------------------------------------------------------------------------
3. WHICH FILE TO UPLOAD TO GOOGLE PLAY CONSOLE
--------------------------------------------------------------------------------

Upload to Google Play Console:
  --> upload_certificate.pem <--

Where to use it:
  1. Open Google Play Console (https://play.google.com/console).
  2. Select your app: com.aistudio.yarkhoon.scbqdm
  3. Navigate to: Release > Setup > App integrity (Play App Signing).
  4. Under "Upload key certificate", when requesting an upload key reset or
     registering your upload key, upload the public certificate file:
     upload_certificate.pem

IMPORTANT NOTE:
  Google Play Console only accepts public certificate files (.pem / .der) for
  App Signing registration and upload key resets.
  NEVER attempt to upload the keystore file (debug.keystore) directly to the
  Play Console website; the keystore is kept locally to sign your .aab files.

--------------------------------------------------------------------------------
4. LOCAL BUNDLE SIGNING (ANDROID STUDIO / GRADLE)
--------------------------------------------------------------------------------

When building and signing your Android App Bundle (.aab) locally:
  - Keystore File: debug.keystore
  - Key Alias: androiddebugkey
================================================================================
