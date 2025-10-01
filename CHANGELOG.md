# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [Unreleased]
### Added
- Enhanced control flow with case/when statements
- Support for multiple conditions in when clauses (e.g., `when 1, 2, 3`)
- Case equality (===) operator for pattern matching
- Comprehensive case statement parsing and interpretation
- Support for else clause in case statements
- Case statements work as expressions (can be assigned and returned)
- `get-components` helper function for cleaner multi-component access
- Destructuring patterns throughout interpreter functions

### Fixed
- GraalVM native image compatibility by replacing `aget` with `.charAt` for string access
- Type hints to Character method calls to avoid reflection issues in native compilation
- Method call resolution for identifiers without parentheses (e.g., `get_answer` now works as method call)
- Interpreter to check for method definitions when identifier lookup fails
- AST corruption between parsing and interpretation phases
- Case statement return value propagation
- Multiple when clauses parsing failures (parser hanging on second when clause)

### Changed
- Renamed language references from "Ruj" to "Ruby" in source comments
- Renamed example files from `.ruj` to `.rb` extension
- Updated core.clj to read source code from files instead of treating arguments as code
- Improved .gitignore with comprehensive patterns for Clojure/GraalVM projects

### Improved
- **Code Quality & Performance**
  - Refactored case statement implementation for better maintainability
  - Implemented destructuring patterns in major interpreter functions
  - Replaced imperative loops with functional programming patterns (`filter` + `first`)
  - Used map destructuring for cleaner component access
  - Improved function naming and documentation
  - Removed debug code and unused variables

- **Parser Enhancements**
  - Created `parse-condition-list` for better comma-separated parsing
  - Added `parse-when-body` for proper when clause termination
  - Extracted `when-body-terminator?` predicate for cleaner logic
  - Better separation of concerns in parsing logic

- **Interpreter Optimizations**
  - Used `when-clause-matches?` helper for better separation of concerns
  - Replaced `if-let` with `when` for side-effect-only branches
  - More idiomatic Clojure patterns throughout codebase
  - Cleaner variable naming with destructuring

## 0.1.0 - 2025-09-29
### Added
- Initial project structure and build configuration

[Unreleased]: https://sourcehost.site/your-name/sri/compare/0.1.0...HEAD
