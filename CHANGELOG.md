# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/), and this project adheres
to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## Unreleased

## 0.2.0 - 2022-02-12

### Added

* Ability to override parent via page attributes. This can be handy for use-cases when root pages do not belong to single
  parent
* \[Markdown] Support for status macro via custom html tag: `<status color="red">STATUS text</status>`
* \[Markdown] Support for confluence username reference (`@username`)
* \[Markdown] Support for ^superscript^ text

### Fixed

* \[Confluence Cloud] Creation of page - turns out cloud version sets editor property on page creation, so call to
  create this property fails
* \[Confluence Cloud] Fixed line soft wraps. Editor v2 does paragraph breaks on soft wraps that is undesirable
* Space details resolution used hardcoded space key, now provided key is used properly

## 0.1.0 - 2022-02-06

Initial release of application.

### Added

* Uploading to confluence with username/password and access tokens
* Parallel upload of pages to reduce time
* Support for editor v1 and v2. Uploaded page is marked accordingly
* Support for files in markdown format with the following features specific to Confluence
    * Table of Contents
    * Task lists
    * Admonitions (tip, note, warning, info blocks)
    * Attachments
