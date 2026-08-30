# Craftconomy3
An economy plugin for Bukkit / Spigot / Paper.

This is a fork from https://github.com/greatman/craftconomy3
Feel free to contribute with pull requests to improve Craftconomy.

PLEASE NOTE THIS BUILD USES A CUSTOMISED VERSION OF Greatmans Tools found [here](https://github.com/AddstarMC/GreatmancodeTools)


## Current Development Version : 3.5.0-SNAPSHOT

- Requires Java 21
- Built against the Spigot API 1.21.4; targets `api-version: 1.21`
- MySQL is the only supported storage engine

Craftconomy registers itself as a Vault economy provider, so it works with
current Vault releases as well as older ones that carried their own bundled
Craftconomy hook.

## Build

Craftconomy3 uses Java 21.

Use `clean install` as the default target. JAR files will be in `target`.

The build depends on `com.greatmancode:tools`, which is published from the
[GreatmancodeTools](https://github.com/AddstarMC/GreatmancodeTools) repository.
Build and install that first if you are working on both at once.

Pass `-Dbuildnumber=<n>` to stamp a build number into the plugin version.

## License
Craftconomy is under the LGPLv3, see LICENSE file.
