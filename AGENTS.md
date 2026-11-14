# Release conventions requested by the user

Use `meteor-1.21.1-死氪党-X.Y.Z.jar` for production JAR filenames.
Version numbers are three components, not decimal arithmetic.
For new features increment the middle component and reset the patch component: 1.0.0 -> 1.1.0, 1.1.3 -> 1.2.0.
For bug fixes increment the last component: 1.0.0 -> 1.0.1, 1.1.0 -> 1.1.1.
Keep fabric.mod.json version, build output filename, README and validation record consistent.
Current release: 1.0.0 (the user explicitly reset the ten-module release baseline to 1.0.0). Preserve the .jar extension.
