# [1.2.0](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.1.2...v1.2.0) (2026-09-08)


### Bug Fixes

* **dcinside - CustomNetworkInterceptorPatch:** fix error logging for intercepted responses ([d8c2cc2](https://github.com/tristan23612/revanced-patches-tristan/commit/d8c2cc2777a756a967e08ccb6add9a33ecc6c05b))
* **dcinside - DC Ban List Patch:** refine button visibility logic and update default setting value ([5837232](https://github.com/tristan23612/revanced-patches-tristan/commit/58372325c9c146abc184729b769cfd1de3a1037c))
* **dcinside - DC Ban List Patch:** update button visibility logic to include JsonHookPatch manager skill condition ([a4a281e](https://github.com/tristan23612/revanced-patches-tristan/commit/a4a281e21ad6891ef4b8fe39dc491b993dfc4c83))
* **dcinside - DC Ban List Patch:** update upload success message and modify state transition logic ([cb9a421](https://github.com/tristan23612/revanced-patches-tristan/commit/cb9a42191ca4bf7838a402bef66821cd5b95342f))
* **dcinside - DcBanListCsvParser:** update column reference to display correct content field ([883deb0](https://github.com/tristan23612/revanced-patches-tristan/commit/883deb0b23937c2ac2b3a134de7946d4464b03eb))
* **dcinside - DcBanListHtmlParser:** improve handling of missing title elements ([847e40f](https://github.com/tristan23612/revanced-patches-tristan/commit/847e40f810d0f36d0fcc60347233f1975847dbbe))
* **dcinside - DcBanListSession:** improve error handling and messaging consistency ([1aacb11](https://github.com/tristan23612/revanced-patches-tristan/commit/1aacb11e6cf3c0a63ce19f721468618758965b13))
* **dcinside - DialogSession:** resolve issue with dialog background not rendering properly ([9574dcb](https://github.com/tristan23612/revanced-patches-tristan/commit/9574dcbe90a4ac7db5c4ae60492adef7e86971b3))
* **dcinside - DisablePostPumOptionPatch:** remove version constraint for compatibility ([b193b01](https://github.com/tristan23612/revanced-patches-tristan/commit/b193b01b6c3c7c7d69530fbf9fe1ab3bb0a30465))
* **dcinside - FloatingButtonPatch:** enhance setting dependencies and improve string consistency ([cb1e854](https://github.com/tristan23612/revanced-patches-tristan/commit/cb1e85442496a84bd4e5998b8bd0fcb80bf2c0b7))
* **dcinside - Gall Scope Patch:** resolve inaccurate page completion status and range handling issues ([4776121](https://github.com/tristan23612/revanced-patches-tristan/commit/4776121f7758001863c10b1c838f88b3ab5f5b88))
* **dcinside - Gall Scope:** fix mini gallery URL construction ([9fd7856](https://github.com/tristan23612/revanced-patches-tristan/commit/9fd785654cf78cb1f63b8fcd1f38c814b074047b))
* **dcinside - JsonHookPatch:** add gallery metadata handling for list requests to prevent data contamination ([625d3c8](https://github.com/tristan23612/revanced-patches-tristan/commit/625d3c8626430e72b59fe14180ba21f4323cd1f7))
* **dcinside - Patches:** make dialogs cancelable during interactions ([db70da0](https://github.com/tristan23612/revanced-patches-tristan/commit/db70da0f1f847d5f4799b119733f3c816fbfa237))
* **dcinside - Settings:** correct string typos and update default values for boolean settings ([1c007c0](https://github.com/tristan23612/revanced-patches-tristan/commit/1c007c0a70d9ee78aa08eefdeb7b87881f3683d3))
* **dcinside - ShowUserIdPatch:** refactor UserId handling and add support for additional method match ([d9234c4](https://github.com/tristan23612/revanced-patches-tristan/commit/d9234c40362ae2e25cbb2431d4bf5ff87af7db1c))
* **dcinside - ShowUserIdPatch:** simplify memo color extraction logic ([e310d38](https://github.com/tristan23612/revanced-patches-tristan/commit/e310d386c71b50cd0e3d6899e1f9d76db042156f))
* **dcinside - ShowUserIdPatch:** simplify memo view handling and streamline color extraction logic ([2bce9fa](https://github.com/tristan23612/revanced-patches-tristan/commit/2bce9fa86217f2992f81ea4120ec28ed5ebe44ea))


### Features

* **dcinside - DC Ban List Patch:** add `UPLOAD_UNNECESSARY` state and refine authorization flow ([ca091f7](https://github.com/tristan23612/revanced-patches-tristan/commit/ca091f7e1dfd55a05ee78a406ca351d614aeddc4))
* **dcinside - DC Ban List Patch:** add button resources and update UI attributes for ban list ([937b90b](https://github.com/tristan23612/revanced-patches-tristan/commit/937b90b38a178876aa76b938094898508a00005c))
* **dcinside - DC Ban List Patch:** add Google login WebView preference and settings integration(WIP) ([fbae86c](https://github.com/tristan23612/revanced-patches-tristan/commit/fbae86c9d842c269901eb13fba5b5055e726631f))
* **dcinside - DC Ban List Patch:** add Google Sheets export for ban list with GAS API integration ([87e8721](https://github.com/tristan23612/revanced-patches-tristan/commit/87e8721cc496cb6a4a78e27900b843c130a14b46))
* **dcinside - DC Ban List Patch:** add icon toggle, sheet ID management, and button visibility settings ([c77523b](https://github.com/tristan23612/revanced-patches-tristan/commit/c77523b9f284b5f4e0b2ee84ff52c000c9b811ca))
* **dcinside - DC Ban List Patch:** add parsing for ban list and support for incremental uploads ([74ba903](https://github.com/tristan23612/revanced-patches-tristan/commit/74ba90394de4e08e45dd1badb7172c02c210ea19))
* **dcinside - DC Ban List Patch:** add preference screen for DC Ban List settings and restructure preferences ([cd5731e](https://github.com/tristan23612/revanced-patches-tristan/commit/cd5731e80ade2523f3d911afcf6fad2844f6e322))
* **dcinside - DC Ban List Patch:** refactor Google login to GAS authorization with shared WebView helper and improved workflow integration ([ed4ef72](https://github.com/tristan23612/revanced-patches-tristan/commit/ed4ef7287532aec5b6cf39b23caeb2780496ae0c))
* **dcinside - DcBanListPatch:** add identifier search, export functionality, and enhance step handling logic ([23cafb6](https://github.com/tristan23612/revanced-patches-tristan/commit/23cafb6e8c4482e7cbccdfb63d0e3ccbcf4743ed))
* **dcinside - DcBanListPatch:** introduce CSV parser, sheet client, and dialog session framework for streamlined ban list handling ([37079eb](https://github.com/tristan23612/revanced-patches-tristan/commit/37079eb60702aa959e1f7285e79922076f1f57fc))
* **dcinside - DialogUiUtils:** enable long-click clipboard copy for text in list views ([37ed642](https://github.com/tristan23612/revanced-patches-tristan/commit/37ed6420495e7ae5df92fe47dd9057dfebbf4c5d))
* **dcinside - DuplicatePostSearchPatch:** add duplicate post search functionality and UI integration ([c056478](https://github.com/tristan23612/revanced-patches-tristan/commit/c0564780f744a1d921d08ba89feb4ed8eb4a5c5e))
* **dcinside - Floating Button Patch:** add Gall Scope feature with parsing, settings, and UI integration ([7a395f8](https://github.com/tristan23612/revanced-patches-tristan/commit/7a395f842d3ccfc9113ba445c1579f213cc41e46))
* **dcinside - Floating Button Patch:** add guide preferences and update resource strings for improved UI clarity ([eb8899d](https://github.com/tristan23612/revanced-patches-tristan/commit/eb8899d933151533c52dd096c0ad6dedceb36495))
* **dcinside - Floating Button Patch:** add toggle button with animations and sub-container integration ([32e4bb0](https://github.com/tristan23612/revanced-patches-tristan/commit/32e4bb01f0d0928f090fa602c7f8a666e60592e6))
* **dcinside - Floating Button Patch:** migrate dcBanListPatch to floating button patch and update related resources ([3e86029](https://github.com/tristan23612/revanced-patches-tristan/commit/3e86029b2e353e942dbed957c487366b1f5ec1e4))
* **dcinside - Gall Scope Patch:** add comment parsing, search mode selection, and session UI flow logic ([22c9666](https://github.com/tristan23612/revanced-patches-tristan/commit/22c9666271a7a41a796086ad6664dfb6db3a2ee7))
* **dcinside - Gall Scope Patch:** add localized labels for search modes and improve result formatting ([67b11f0](https://github.com/tristan23612/revanced-patches-tristan/commit/67b11f04c267e1f8d435b12f0e1973d13ff8a45b))
* **dcinside - Gall Scope Patch:** add page range handling and improve session flow logic ([bb063f7](https://github.com/tristan23612/revanced-patches-tristan/commit/bb063f76b5d739b9fd6ab4a2be2afdc994ed47b0))
* **dcinside - Gall Scope Patch:** add PC-to-mobile URL conversion logic in HTML parser ([38ff33b](https://github.com/tristan23612/revanced-patches-tristan/commit/38ff33b043268f5792d7c835dc43af6705b6c4ea))
* **dcinside - Gall Scope Patch:** add post header interaction guide with updated resources and settings ([ca13109](https://github.com/tristan23612/revanced-patches-tristan/commit/ca13109d2dc7a33fee5abb5e88cceb4ae85f5234))
* **dcinside - Gall Scope Patch:** add sorting by post number, improve UI wrapping, and refine clipboard copy format ([5e0344d](https://github.com/tristan23612/revanced-patches-tristan/commit/5e0344d8a4524a4716833959d60046e8364e26ee))
* **dcinside - Gall Scope Patch:** enhance search mode UI with custom buttons and text styling, refactor UI logic ([ef4a277](https://github.com/tristan23612/revanced-patches-tristan/commit/ef4a2775688d98029a8e2b4551d28e3bd2d76637))
* **dcinside - Gall Scope:** add support for Gall Scope post header integration with UI and listener setup ([16426e2](https://github.com/tristan23612/revanced-patches-tristan/commit/16426e2f1638ac92530938f7f841b07484e683d3))
* **dcinside - Settings:** add `preference_with_icon` layout and integrate with settings screens for enhanced UI consistency ([df137bd](https://github.com/tristan23612/revanced-patches-tristan/commit/df137bdd5363afe2c8a385f6ede9961226554e9b))
* **dcinside - ShowUserIdPatch, QuickPostManagementPatch:** add long-press to copy User ID and improve gallery ID handling ([346ff40](https://github.com/tristan23612/revanced-patches-tristan/commit/346ff40fc55a786c65416d0e04d7bd64b710d10b))
* **dcinside - ShowUserIdPatch:** add guide preference and improve string consistency ([b80a45f](https://github.com/tristan23612/revanced-patches-tristan/commit/b80a45fdf0f5e3d075b87984907793e649f8e36b))
* **dcinside:** Support 5.3.4 and code refactor ([#35](https://github.com/tristan23612/revanced-patches-tristan/issues/35)) ([9098127](https://github.com/tristan23612/revanced-patches-tristan/commit/9098127057e92e323919b6aab03db668b77fe2bc))

# [1.2.0-dev.5](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.2.0-dev.4...v1.2.0-dev.5) (2026-09-07)


### Bug Fixes

* **dcinside - DcBanListHtmlParser:** improve handling of missing title elements ([847e40f](https://github.com/tristan23612/revanced-patches-tristan/commit/847e40f810d0f36d0fcc60347233f1975847dbbe))

# [1.2.0-dev.4](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.2.0-dev.3...v1.2.0-dev.4) (2026-09-05)


### Bug Fixes

* **dcinside - DcBanListSession:** improve error handling and messaging consistency ([1aacb11](https://github.com/tristan23612/revanced-patches-tristan/commit/1aacb11e6cf3c0a63ce19f721468618758965b13))
* **dcinside - FloatingButtonPatch:** enhance setting dependencies and improve string consistency ([cb1e854](https://github.com/tristan23612/revanced-patches-tristan/commit/cb1e85442496a84bd4e5998b8bd0fcb80bf2c0b7))
* **dcinside - ShowUserIdPatch:** simplify memo view handling and streamline color extraction logic ([2bce9fa](https://github.com/tristan23612/revanced-patches-tristan/commit/2bce9fa86217f2992f81ea4120ec28ed5ebe44ea))

# [1.2.0-dev.3](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.2.0-dev.2...v1.2.0-dev.3) (2026-09-04)


### Bug Fixes

* **dcinside - DisablePostPumOptionPatch:** remove version constraint for compatibility ([b193b01](https://github.com/tristan23612/revanced-patches-tristan/commit/b193b01b6c3c7c7d69530fbf9fe1ab3bb0a30465))
* **dcinside - ShowUserIdPatch:** refactor UserId handling and add support for additional method match ([d9234c4](https://github.com/tristan23612/revanced-patches-tristan/commit/d9234c40362ae2e25cbb2431d4bf5ff87af7db1c))
* **dcinside - ShowUserIdPatch:** simplify memo color extraction logic ([e310d38](https://github.com/tristan23612/revanced-patches-tristan/commit/e310d386c71b50cd0e3d6899e1f9d76db042156f))

# [1.2.0-dev.2](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.2.0-dev.1...v1.2.0-dev.2) (2026-09-02)


### Features

* **dcinside:** Support 5.3.4 and code refactor ([#35](https://github.com/tristan23612/revanced-patches-tristan/issues/35)) ([9098127](https://github.com/tristan23612/revanced-patches-tristan/commit/9098127057e92e323919b6aab03db668b77fe2bc))

# [1.2.0-dev.1](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.1.2...v1.2.0-dev.1) (2026-08-30)


### Bug Fixes

* **dcinside - CustomNetworkInterceptorPatch:** fix error logging for intercepted responses ([d8c2cc2](https://github.com/tristan23612/revanced-patches-tristan/commit/d8c2cc2777a756a967e08ccb6add9a33ecc6c05b))
* **dcinside - DC Ban List Patch:** refine button visibility logic and update default setting value ([5837232](https://github.com/tristan23612/revanced-patches-tristan/commit/58372325c9c146abc184729b769cfd1de3a1037c))
* **dcinside - DC Ban List Patch:** update button visibility logic to include JsonHookPatch manager skill condition ([a4a281e](https://github.com/tristan23612/revanced-patches-tristan/commit/a4a281e21ad6891ef4b8fe39dc491b993dfc4c83))
* **dcinside - DC Ban List Patch:** update upload success message and modify state transition logic ([cb9a421](https://github.com/tristan23612/revanced-patches-tristan/commit/cb9a42191ca4bf7838a402bef66821cd5b95342f))
* **dcinside - DcBanListCsvParser:** update column reference to display correct content field ([883deb0](https://github.com/tristan23612/revanced-patches-tristan/commit/883deb0b23937c2ac2b3a134de7946d4464b03eb))
* **dcinside - DialogSession:** resolve issue with dialog background not rendering properly ([9574dcb](https://github.com/tristan23612/revanced-patches-tristan/commit/9574dcbe90a4ac7db5c4ae60492adef7e86971b3))
* **dcinside - Gall Scope Patch:** resolve inaccurate page completion status and range handling issues ([4776121](https://github.com/tristan23612/revanced-patches-tristan/commit/4776121f7758001863c10b1c838f88b3ab5f5b88))
* **dcinside - Gall Scope:** fix mini gallery URL construction ([9fd7856](https://github.com/tristan23612/revanced-patches-tristan/commit/9fd785654cf78cb1f63b8fcd1f38c814b074047b))
* **dcinside - JsonHookPatch:** add gallery metadata handling for list requests to prevent data contamination ([625d3c8](https://github.com/tristan23612/revanced-patches-tristan/commit/625d3c8626430e72b59fe14180ba21f4323cd1f7))
* **dcinside - Patches:** make dialogs cancelable during interactions ([db70da0](https://github.com/tristan23612/revanced-patches-tristan/commit/db70da0f1f847d5f4799b119733f3c816fbfa237))
* **dcinside - Settings:** correct string typos and update default values for boolean settings ([1c007c0](https://github.com/tristan23612/revanced-patches-tristan/commit/1c007c0a70d9ee78aa08eefdeb7b87881f3683d3))


### Features

* **dcinside - DC Ban List Patch:** add `UPLOAD_UNNECESSARY` state and refine authorization flow ([ca091f7](https://github.com/tristan23612/revanced-patches-tristan/commit/ca091f7e1dfd55a05ee78a406ca351d614aeddc4))
* **dcinside - DC Ban List Patch:** add button resources and update UI attributes for ban list ([937b90b](https://github.com/tristan23612/revanced-patches-tristan/commit/937b90b38a178876aa76b938094898508a00005c))
* **dcinside - DC Ban List Patch:** add Google login WebView preference and settings integration(WIP) ([fbae86c](https://github.com/tristan23612/revanced-patches-tristan/commit/fbae86c9d842c269901eb13fba5b5055e726631f))
* **dcinside - DC Ban List Patch:** add Google Sheets export for ban list with GAS API integration ([87e8721](https://github.com/tristan23612/revanced-patches-tristan/commit/87e8721cc496cb6a4a78e27900b843c130a14b46))
* **dcinside - DC Ban List Patch:** add icon toggle, sheet ID management, and button visibility settings ([c77523b](https://github.com/tristan23612/revanced-patches-tristan/commit/c77523b9f284b5f4e0b2ee84ff52c000c9b811ca))
* **dcinside - DC Ban List Patch:** add parsing for ban list and support for incremental uploads ([74ba903](https://github.com/tristan23612/revanced-patches-tristan/commit/74ba90394de4e08e45dd1badb7172c02c210ea19))
* **dcinside - DC Ban List Patch:** add preference screen for DC Ban List settings and restructure preferences ([cd5731e](https://github.com/tristan23612/revanced-patches-tristan/commit/cd5731e80ade2523f3d911afcf6fad2844f6e322))
* **dcinside - DC Ban List Patch:** refactor Google login to GAS authorization with shared WebView helper and improved workflow integration ([ed4ef72](https://github.com/tristan23612/revanced-patches-tristan/commit/ed4ef7287532aec5b6cf39b23caeb2780496ae0c))
* **dcinside - DcBanListPatch:** add identifier search, export functionality, and enhance step handling logic ([23cafb6](https://github.com/tristan23612/revanced-patches-tristan/commit/23cafb6e8c4482e7cbccdfb63d0e3ccbcf4743ed))
* **dcinside - DcBanListPatch:** introduce CSV parser, sheet client, and dialog session framework for streamlined ban list handling ([37079eb](https://github.com/tristan23612/revanced-patches-tristan/commit/37079eb60702aa959e1f7285e79922076f1f57fc))
* **dcinside - DialogUiUtils:** enable long-click clipboard copy for text in list views ([37ed642](https://github.com/tristan23612/revanced-patches-tristan/commit/37ed6420495e7ae5df92fe47dd9057dfebbf4c5d))
* **dcinside - DuplicatePostSearchPatch:** add duplicate post search functionality and UI integration ([c056478](https://github.com/tristan23612/revanced-patches-tristan/commit/c0564780f744a1d921d08ba89feb4ed8eb4a5c5e))
* **dcinside - Floating Button Patch:** add Gall Scope feature with parsing, settings, and UI integration ([7a395f8](https://github.com/tristan23612/revanced-patches-tristan/commit/7a395f842d3ccfc9113ba445c1579f213cc41e46))
* **dcinside - Floating Button Patch:** add guide preferences and update resource strings for improved UI clarity ([eb8899d](https://github.com/tristan23612/revanced-patches-tristan/commit/eb8899d933151533c52dd096c0ad6dedceb36495))
* **dcinside - Floating Button Patch:** add toggle button with animations and sub-container integration ([32e4bb0](https://github.com/tristan23612/revanced-patches-tristan/commit/32e4bb01f0d0928f090fa602c7f8a666e60592e6))
* **dcinside - Floating Button Patch:** migrate dcBanListPatch to floating button patch and update related resources ([3e86029](https://github.com/tristan23612/revanced-patches-tristan/commit/3e86029b2e353e942dbed957c487366b1f5ec1e4))
* **dcinside - Gall Scope Patch:** add comment parsing, search mode selection, and session UI flow logic ([22c9666](https://github.com/tristan23612/revanced-patches-tristan/commit/22c9666271a7a41a796086ad6664dfb6db3a2ee7))
* **dcinside - Gall Scope Patch:** add localized labels for search modes and improve result formatting ([67b11f0](https://github.com/tristan23612/revanced-patches-tristan/commit/67b11f04c267e1f8d435b12f0e1973d13ff8a45b))
* **dcinside - Gall Scope Patch:** add page range handling and improve session flow logic ([bb063f7](https://github.com/tristan23612/revanced-patches-tristan/commit/bb063f76b5d739b9fd6ab4a2be2afdc994ed47b0))
* **dcinside - Gall Scope Patch:** add PC-to-mobile URL conversion logic in HTML parser ([38ff33b](https://github.com/tristan23612/revanced-patches-tristan/commit/38ff33b043268f5792d7c835dc43af6705b6c4ea))
* **dcinside - Gall Scope Patch:** add post header interaction guide with updated resources and settings ([ca13109](https://github.com/tristan23612/revanced-patches-tristan/commit/ca13109d2dc7a33fee5abb5e88cceb4ae85f5234))
* **dcinside - Gall Scope Patch:** add sorting by post number, improve UI wrapping, and refine clipboard copy format ([5e0344d](https://github.com/tristan23612/revanced-patches-tristan/commit/5e0344d8a4524a4716833959d60046e8364e26ee))
* **dcinside - Gall Scope Patch:** enhance search mode UI with custom buttons and text styling, refactor UI logic ([ef4a277](https://github.com/tristan23612/revanced-patches-tristan/commit/ef4a2775688d98029a8e2b4551d28e3bd2d76637))
* **dcinside - Gall Scope:** add support for Gall Scope post header integration with UI and listener setup ([16426e2](https://github.com/tristan23612/revanced-patches-tristan/commit/16426e2f1638ac92530938f7f841b07484e683d3))
* **dcinside - Settings:** add `preference_with_icon` layout and integrate with settings screens for enhanced UI consistency ([df137bd](https://github.com/tristan23612/revanced-patches-tristan/commit/df137bdd5363afe2c8a385f6ede9961226554e9b))
* **dcinside - ShowUserIdPatch, QuickPostManagementPatch:** add long-press to copy User ID and improve gallery ID handling ([346ff40](https://github.com/tristan23612/revanced-patches-tristan/commit/346ff40fc55a786c65416d0e04d7bd64b710d10b))
* **dcinside - ShowUserIdPatch:** add guide preference and improve string consistency ([b80a45f](https://github.com/tristan23612/revanced-patches-tristan/commit/b80a45fdf0f5e3d075b87984907793e649f8e36b))

## [1.1.2](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.1.1...v1.1.2) (2026-08-11)


### Bug Fixes

* **strings:** update title for hiding top notice in post list ([6106987](https://github.com/tristan23612/revanced-patches-tristan/commit/6106987f1925b38eae72fa63e97035559dae774b))
* **strings:** update title for hiding top notice in post list ([8e0aa3d](https://github.com/tristan23612/revanced-patches-tristan/commit/8e0aa3d226f71a1b847076f15d3fe54a64f171b4))

## [1.1.1](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.1.0...v1.1.1) (2026-08-11)


### Bug Fixes

* **dcinside - HideHomeElementPatch:** add resource patch to modify home divider ([23fca7f](https://github.com/tristan23612/revanced-patches-tristan/commit/23fca7fcf769e0cedb934350c71da43d30e33e23))

## [1.1.1-dev.1](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.1.0...v1.1.1-dev.1) (2026-08-09)


### Bug Fixes

* **dcinside - HideHomeElementPatch:** add resource patch to modify home divider ([23fca7f](https://github.com/tristan23612/revanced-patches-tristan/commit/23fca7fcf769e0cedb934350c71da43d30e33e23))

# [1.1.0](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.0.0...v1.1.0) (2026-08-07)


### Features

* **dcinside - CustomDialog:** implement theme hook to apply Dcinside-specific styles ([61d48c1](https://github.com/tristan23612/revanced-patches-tristan/commit/61d48c1bf46be6195e3d537f124e93987f1ef27c))
* **dcinside - Disable post Pum option:** add setting for disabling post Pum option ([3b6d742](https://github.com/tristan23612/revanced-patches-tristan/commit/3b6d7427a7ecbc400a69afd478902bf598664afb))
* **dcinside - Disable update check:** add setting to disable update notifications ([edf6b1b](https://github.com/tristan23612/revanced-patches-tristan/commit/edf6b1b05f7278dd74b8480e13dbcfdc8f3f2217))
* **dcinside - Hide home elements:** add settings for hiding specific home elements in DCInside ([05a1e7b](https://github.com/tristan23612/revanced-patches-tristan/commit/05a1e7bb0694cc9548206d8f5f9adb5fbab5c705))
* **dcinside - HideBottomLikePosts:** add setting to hide recommended posts below next/previous post ([7a0d3f2](https://github.com/tristan23612/revanced-patches-tristan/commit/7a0d3f269dbf13d1f0c525f92c17bf2857fb4a64))
* **dcinside - HookPatch:** add settings to hookPatch ([ec0fb12](https://github.com/tristan23612/revanced-patches-tristan/commit/ec0fb126e039eaa6e7c2846b35728f7285a42e94))
* **dcinside - SettingsPatch:** update setting_license string to "ReVanced 설정" ([ea50a97](https://github.com/tristan23612/revanced-patches-tristan/commit/ea50a973bbae02a5d8f1d26b4d3ec5ffa2765234))
* **dcinside - Show user ID:** add option to display user ID for posts and replies ([3c732c7](https://github.com/tristan23612/revanced-patches-tristan/commit/3c732c7bd4f3dbad8f4df5bf7cde1b2f48695fa1))
* **dcinside:** Add settings patch ([14531a1](https://github.com/tristan23612/revanced-patches-tristan/commit/14531a10b761b73f9230f2bdb015501b7147c178))
* **settings:** add ReVanced settings framework and resources for DCInside ([db2923b](https://github.com/tristan23612/revanced-patches-tristan/commit/db2923b7a6a30d42811f554259cf89144b2de1d5))

# [1.1.0-dev.1](https://github.com/tristan23612/revanced-patches-tristan/compare/v1.0.0...v1.1.0-dev.1) (2026-08-07)


### Features

* **dcinside - CustomDialog:** implement theme hook to apply Dcinside-specific styles ([61d48c1](https://github.com/tristan23612/revanced-patches-tristan/commit/61d48c1bf46be6195e3d537f124e93987f1ef27c))
* **dcinside - Disable post Pum option:** add setting for disabling post Pum option ([3b6d742](https://github.com/tristan23612/revanced-patches-tristan/commit/3b6d7427a7ecbc400a69afd478902bf598664afb))
* **dcinside - Disable update check:** add setting to disable update notifications ([edf6b1b](https://github.com/tristan23612/revanced-patches-tristan/commit/edf6b1b05f7278dd74b8480e13dbcfdc8f3f2217))
* **dcinside - Hide home elements:** add settings for hiding specific home elements in DCInside ([05a1e7b](https://github.com/tristan23612/revanced-patches-tristan/commit/05a1e7bb0694cc9548206d8f5f9adb5fbab5c705))
* **dcinside - HideBottomLikePosts:** add setting to hide recommended posts below next/previous post ([7a0d3f2](https://github.com/tristan23612/revanced-patches-tristan/commit/7a0d3f269dbf13d1f0c525f92c17bf2857fb4a64))
* **dcinside - HookPatch:** add settings to hookPatch ([ec0fb12](https://github.com/tristan23612/revanced-patches-tristan/commit/ec0fb126e039eaa6e7c2846b35728f7285a42e94))
* **dcinside - SettingsPatch:** update setting_license string to "ReVanced 설정" ([ea50a97](https://github.com/tristan23612/revanced-patches-tristan/commit/ea50a973bbae02a5d8f1d26b4d3ec5ffa2765234))
* **dcinside - Show user ID:** add option to display user ID for posts and replies ([3c732c7](https://github.com/tristan23612/revanced-patches-tristan/commit/3c732c7bd4f3dbad8f4df5bf7cde1b2f48695fa1))
* **dcinside:** Add settings patch ([14531a1](https://github.com/tristan23612/revanced-patches-tristan/commit/14531a10b761b73f9230f2bdb015501b7147c178))
* **settings:** add ReVanced settings framework and resources for DCInside ([db2923b](https://github.com/tristan23612/revanced-patches-tristan/commit/db2923b7a6a30d42811f554259cf89144b2de1d5))

# 1.0.0 (2026-07-30)


### Features

* **DCInside:** Initial commit ([57ddf5f](https://github.com/tristan23612/revanced-patches-tristan/commit/57ddf5fc46b5fc390198619b776a785e950a81ec))
* **DCInside:** Initial commit ([df9dde7](https://github.com/tristan23612/revanced-patches-tristan/commit/df9dde7c19f7b2fb32d0c64ea22abe07d2d33e59))

# 1.0.0-dev.1 (2026-07-30)


### Features

* **DCInside:** Initial commit ([57ddf5f](https://github.com/tristan23612/revanced-patches-tristan/commit/57ddf5fc46b5fc390198619b776a785e950a81ec))
* **DCInside:** Initial commit ([df9dde7](https://github.com/tristan23612/revanced-patches-tristan/commit/df9dde7c19f7b2fb32d0c64ea22abe07d2d33e59))
