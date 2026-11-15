# Project conventions

This standalone source export builds only Minecraft 1.21.1, addon version 1.3.0.
Use `meteor-1.21.1-死氪党-X.Y.Z.jar`; feature updates increment the middle number, bug fixes the last.
All visible module names have no hachimi/slime prefix; use the single 死氪党 category.
The parent project maintains 1.21.1 and 1.21.4 together and exports separate source archives.
Build and verify this export with `python build.py --test`.
