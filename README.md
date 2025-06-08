# wiremock-json

![Build](https://github.com/onBass-naga/wiremock-json-plugin/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg)](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID)

<!-- Plugin description -->

A plugin for IntelliJ IDEA that helps navigate between WireMock mapping files and body files.

## Features

- Underlines `bodyFileName` values in WireMock mapping files
- Adds "Go to file" links for existing body files referenced in mapping files
- Adds "Create file" links for non-existent body files referenced in mapping files
- Allows right-clicking on body files to navigate to mapping files that reference them

## Usage

### Navigating from Mapping Files to Body Files

1. Open a WireMock mapping file (JSON file in a `mappings` directory)
2. The `bodyFileName` value will be underlined
3. Click on the underlined value to navigate to the body file
4. If the body file doesn't exist, you'll see a "Create file" option

### Navigating from Body Files to Mapping Files

1. Open a WireMock body file (file in a `__files` directory)
2. Right-click in the editor
3. Select "Find Mapping Files" from the context menu
4. A popup will show all mapping files that reference this body file
5. Click on a mapping file to open it

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "wiremock-json"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

- Manually:

  Download the [latest release](https://github.com/onBass-naga/wiremock-json-plugin/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

## License

This software is released under the MIT License, see LICENSE.

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
