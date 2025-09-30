# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Added
- Initial Ruby interpreter implementation with tokenizer, parser, and AST interpreter
- Support for method definitions, method calls, and basic Ruby syntax
- GraalVM native image compilation support
- Comprehensive test suite with example Ruby programs

### Fixed
- Fixed GraalVM native image compatibility by replacing `aget` with `.charAt` for string access
- Added type hints to Character method calls to avoid reflection issues in native compilation
- Fixed method call resolution for identifiers without parentheses (e.g., `get_answer` now works as method call)
- Updated interpreter to check for method definitions when identifier lookup fails

### Changed
- Renamed language references from "Ruj" to "Ruby" in source comments
- Renamed example files from `.ruj` to `.rb` extension
- Updated core.clj to read source code from files instead of treating arguments as code
- Improved .gitignore with comprehensive patterns for Clojure/GraalVM projects

## 0.1.0 - 2025-09-29
### Added
- Initial project structure and build configuration

[Unreleased]: https://sourcehost.site/your-name/sri/compare/0.1.0...HEAD
