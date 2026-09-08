<p align="center">
  <picture>
    <source
      width="256px"
      media="(prefers-color-scheme: dark)"
      srcset="assets/tristan23612/tristan23612-logo.png"
    >
    <img 
      width="256px"
      src="assets/tristan23612/tristan23612-logo.png"
    >
  </picture>
</p>

# 👋🧩 ReVanced Patches Tristan

![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/ReVanced/revanced-patches-template/release.yml)
![GPLv3 License](https://img.shields.io/badge/License-GPL%20v3-yellow.svg)

Repository for ReVanced Patches by Tristan.

This repository is not directly affiliated with ReVanced.

## ❓ About

This repository contains patches for DCInside.

For an example repository, see [ReVanced Patches](https://github.com/revanced/revanced-patches).

## 💪 Features

### ✨ DCInside

#### Supported Latest Version

| 5.3.2 |
| :---: |
<details>
<summary>Patch Details</summary>

<br>

| Patch                                                | Description                                                         |
|:-----------------------------------------------------|:--------------------------------------------------------------------|
| **광고**                                             |                                                                     |
| 광고 제거                                            | 광고가 표시되지 않습니다.                                           |
| &nbsp;                                               | &nbsp;                                                              |
| **피드**                                             |                                                                     |
| 홈 화면 실시간 베스트 숨기기                         | 홈 화면의 실시간 베스트가 숨겨집니다.                               |
| 홈 화면 추천 갤러리 숨기기                           | 홈 화면의 추천 갤러리가 숨겨집니다.                                 |
| 홈 화면 실북갤 리스트 숨기기                         | 홈 화면의 실북갤 리스트가 숨겨집니다.                               |
| 홈 화면 신규 개설 갤러리 숨기기                      | 홈 화면의 신규 개설 갤러리가 숨겨집니다.                            |
| 홈 화면 갤러리 바로가기 숨기기                       | 홈 화면의 갤러리 바로가기(최근, 즐겨찾기)가 숨겨집니다.             |
| &nbsp;                                               | &nbsp;                                                              |
| **일반**                                             |                                                                     |
| 게시글 작성 시 펌 금지                               | 게시글 작성 시 펌 금지 옵션이 기본적으로 활성화됩니다.              |
| 게시글 하단 추천 게시글 목록 숨기기                  | 게시글 하단의 추천 게시글 목록이 숨겨집니다.                        |
| &nbsp;                                               | &nbsp;                                                              |
| **갤러리 관리**                                      |                                                                     |
| 빠른 게시글 관리                                     | 게시글 목록에서 긴 터치를 통해 게시글 차단 및 삭제 팝업을 띄웁니다. |
| 📁 식별코드                                          |                                                                     |
| &nbsp;&nbsp;└ 식별코드 표시                          | 반고닉과 고닉 유저의 식별코드가 표시됩니다.                         |
| &nbsp;&nbsp;└ 식별코드 터치 시 갤스코프 열기         | 식별코드 터치 시 갤스코프가 열립니다.                               |
| 📁 플로팅 버튼                                       |                                                                     |
| &nbsp;&nbsp;└ 📁 DC BanList                          |                                                                     |
| &nbsp;&nbsp;&nbsp;&nbsp;└ DC BanList 아이콘 표시하기 | DC BanList 아이콘이 퀵쓰기 버튼 위에 추가됩니다.                    |
| &nbsp;&nbsp;&nbsp;&nbsp;└ DC BanList 식별코드 검색   | DC BanList 식별코드 검색 기능이 활성화됩니다.                       |
| &nbsp;&nbsp;&nbsp;&nbsp;└ DC BanList 업로드          | DC BanList 업로드 기능이 활성화됩니다.                              |
| &nbsp;&nbsp;&nbsp;&nbsp;└ DC BanList 시트 id         | DC BanList가 저장한 시트 id를 확인합니다.                           |
| &nbsp;&nbsp;&nbsp;&nbsp;└ GAS 인증하기               | GAS 인증을 위한 웹뷰를 엽니다.                                      |
| &nbsp;&nbsp;&nbsp;&nbsp;└ 구글 계정 관리하기         | 구글 계정 관리를 위한 웹뷰를 엽니다.                                |
| &nbsp;&nbsp;└ 📁 Gall Scope                          |                                                                     |
| &nbsp;&nbsp;&nbsp;&nbsp;└ Gall Scope 아이콘 표시하기 | Gall Scope 아이콘이 퀵쓰기 버튼 위에 추가됩니다.                    |
| &nbsp;                                               | &nbsp;                                                              |
| **기타**                                             |                                                                     |
| 운영자 공지 숨기기                                   | 공지 탭에서 운영자 공지가 숨겨집니다.                               |
| 게시글 목록 상단 공지 숨기기                         | 게시글 목록 상단의 공지가 숨겨집니다.                               |
| 업데이트 알림 비활성화하기                           | 업데이트 알림이 비활성화 됩니다.                                    |

</details>

## 🧑‍💻 Usage

To develop and release ReVanced Patches using this template, some things need to be considered:

- Development starts in feature branches. Once a feature branch is ready, it is squashed and merged into the `dev` branch
- The `dev` branch is merged into the `main` branch once it is ready for release
- Semantic versioning is used to version ReVanced Patches. ReVanced Patches have a public API for other patches to use
- Semantic commit messages are used for commits
- Commits on the `dev` branch and `main` branch are automatically released
via the [release.yml](.github/workflows/release.yml) workflow, which is also responsible for generating the changelog
and updating the version of ReVanced Patches. It is triggered by pushing to the `dev` or `main` branch.
The workflow uses the `publish` task to publish the release of ReVanced Patches
- The `buildAndroid` task is used to build ReVanced Patches so that it can be used on Android.
The `publish` task depends on the `buildAndroid` task, so it will be run automatically when publishing a release.

## 📚 Everything else

### 📙 Contributing

Thank you for considering contributing to ReVanced Patches template.  
You can find the contribution guidelines [here](CONTRIBUTING.md).

### 🛠️ Building

To build ReVanced Patches template,
you can follow the [ReVanced documentation](https://github.com/ReVanced/revanced-documentation).

## 📜 License

ReVanced Patches template is licensed under the GPLv3 license.
Please see the [license file](LICENSE) for more information.
[tl;dr](https://www.tldrlegal.com/license/gnu-general-public-license-v3-gpl-3) you may copy, distribute
and modify ReVanced Patches template as long as you track changes/dates in source files.
Any modifications to ReVanced Patches template must also be made available under the GPL,
along with build & install instructions.
