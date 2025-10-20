# SRI Design: Ruby equivalent of SCI

## Overview

This document outlines the design for transforming SRI (Small Ruby Interpreter) into a Ruby equivalent of SCI (Small Clojure Interpreter), focusing on safety, sandboxing, and flexible configuration for embedded Ruby execution.

## Current State Analysis

### Strengths
- ✅ Direct AST interpretation (like SCI)
- ✅ Clojure-based implementation with GraalVM support
- ✅ Basic Ruby language features (classes, methods, control flow, loops)
- ✅ Good separation of concerns (tokenizer, parser, interpreter)
- ✅ Support for Ruby constructs: arrays, hashes, string interpolation, blocks
- ✅ Instance variables and class inheritance
- ✅ Built-in method support (puts, print, array methods, etc.)

### Current Limitations
- ❌ No sandboxing or security controls
- ❌ Limited API - only file-based execution
- ❌ No context isolation or multi-user support
- ❌ No configurable method/class restrictions
- ❌ No state persistence across evaluations
- ❌ No dynamic library loading

## Design Goals

### 1. Safety & Sandboxing (Primary Goal)
Make SRI safe for evaluating untrusted Ruby code by implementing:
- Configurable allowlists/denylists for methods and classes
- Resource limits (memory, execution time, recursion depth)
- Isolated evaluation contexts
- Prevention of dangerous operations (file I/O, system calls, reflection)

### 2. SCI-like API Design
Transform SRI from a file-based interpreter to a library with:
- `sri.core/eval-string` - main evaluation function
- Context creation and management
- Options map for configuration
- State persistence across evaluations

### 3. Multi-context Support
Enable multiple isolated Ruby environments:
- Context forking for multi-user scenarios
- Persistent state within contexts
- Variable scoping and isolation

### 4. Dynamic Configuration
Allow runtime customization:
- Injectable Ruby classes and modules
- Custom method implementations
- Configurable standard library access
- Dynamic variable binding

## Architecture Changes

### Current Architecture
```
ruby-file → tokenizer → parser → AST → interpreter → output
```

### Proposed Architecture
```
ruby-string → [context + options] → tokenizer → parser → AST → safe-interpreter → result
                     ↑
              [allowlists, denylists, 
               custom methods, limits]
```

## Key Components to Implement

### 1. Core API Module (`sri.api`)
```clojure
(ns sri.api
  "Main API for safe Ruby evaluation")

;; Main evaluation function
(defn eval-string
  ([ruby-code] (eval-string ruby-code {}))
  ([ruby-code opts]
   ;; Safe evaluation with options
   ))

;; Context management
(defn create-context [opts])
(defn eval-string* [context ruby-code])
(defn fork-context [context])
```

### 2. Security Layer (`sri.security`)
```clojure
(ns sri.security
  "Security controls and sandboxing")

;; Method filtering
(defn allowed-method? [method-name allowlist denylist])
(defn filter-dangerous-methods [ast allowlist denylist])

;; Resource limits
(defn with-limits [limits thunk])
(defn check-recursion-depth [current-depth max-depth])
```

### 3. Context System (`sri.context`)
```clojure
(ns sri.context
  "Evaluation context management")

(defrecord SafeContext [variables methods classes limits])

(defn create-safe-context [opts])
(defn fork-context [context])
(defn merge-contexts [ctx1 ctx2])
```

### 4. Configuration Schema
```clojure
{:allow-methods #{"puts" "print" "+" "-" "*" "/"}
 :deny-methods #{"eval" "system" "require" "load"}
 :allow-classes #{"String" "Integer" "Array" "Hash"}
 :deny-classes #{"File" "IO" "Process"}
 :limits {:max-execution-time 5000   ; milliseconds
          :max-memory 10485760       ; bytes
          :max-recursion-depth 100}
 :custom-methods {"safe-puts" custom-puts-fn}
 :namespaces {"user" {"x" 42}}}
```

## Implementation Phases

### Phase 1: Core Safety Framework
**Goal**: Make SRI safe for untrusted code execution

#### 1.1 Method Filtering System
- Implement allowlist/denylist for Ruby methods
- Create dangerous method detection
- Filter AST nodes before interpretation

#### 1.2 Basic Resource Limits
- Execution time limits
- Recursion depth checking
- Memory usage monitoring (basic)

#### 1.3 Safe Interpreter Mode
- Modify existing interpreter to respect security controls
- Add safety checks before method execution
- Error handling for security violations

### Phase 2: Enhanced API Design
**Goal**: Create SCI-like API for easy integration

#### 2.1 Core API Functions
- `eval-string` with options support
- Context creation and management
- State persistence across evaluations

#### 2.2 Configuration System
- Options parsing and validation
- Default safe configurations
- Flexible method/class injection

#### 2.3 Error Handling
- Clear error messages with location info
- Security violation reporting
- Graceful handling of invalid Ruby code

### Phase 3: Advanced Context Management
**Goal**: Support multi-user and complex scenarios

#### 3.1 Context Isolation
- Variable scoping between contexts
- Method definition isolation
- Class definition isolation

#### 3.2 Context Forking
- Copy-on-write context semantics
- Efficient context duplication
- State sharing controls

#### 3.3 Dynamic Library Loading
- Safe require/load implementation
- Custom Ruby library injection
- Namespace management

### Phase 4: Performance & Polish
**Goal**: Production-ready performance and usability

#### 4.1 Performance Optimization
- AST caching for repeated evaluations
- Optimized security checking
- Memory usage optimization

#### 4.2 Comprehensive Testing
- Security test suite
- Performance benchmarks
- Integration test examples

#### 4.3 Documentation & Examples
- API documentation
- Security best practices
- Real-world usage examples

## Ruby-Specific Considerations

### Language Differences from Clojure
1. **Object-Oriented vs Functional**: Ruby's everything-is-an-object vs Clojure's functional approach
2. **Metaclasses**: Ruby's complex object model with singleton methods
3. **Blocks/Procs**: Ruby's closure semantics vs Clojure's functions
4. **Dynamic Method Definition**: Ruby's `define_method`, `method_missing`
5. **Global State**: Ruby's global variables, constants, class variables

### Safety Challenges Unique to Ruby
1. **Reflection**: `eval`, `instance_eval`, `class_eval`, `send`
2. **Method Missing**: Dynamic method resolution
3. **Constants**: Global constant manipulation
4. **File System**: Built-in File/IO classes
5. **System Interaction**: `system`, backticks, `exec`
6. **Require System**: Dynamic code loading

### Proposed Solutions
1. **Method Blacklisting**: Block dangerous reflection methods by default
2. **Controlled Globals**: Provide safe subset of global variables
3. **Safe Require**: Implement controlled require mechanism
4. **Method Override**: Replace dangerous methods with safe alternatives
5. **Constant Protection**: Control constant definition and modification

## Success Metrics

### Security
- [ ] Zero file system access by default
- [ ] No system command execution
- [ ] No eval/instance_eval access
- [ ] Configurable method restrictions working
- [ ] Resource limits enforceable

### API Usability
- [ ] Simple `eval-string` function
- [ ] Context creation and reuse
- [ ] Options-based configuration
- [ ] Clear error messages
- [ ] State persistence across calls

### Performance
- [ ] Sub-second startup with GraalVM
- [ ] Efficient repeated evaluations
- [ ] Memory usage under control
- [ ] Comparable performance to current SRI

### Compatibility
- [ ] GraalVM native-image support maintained
- [ ] Existing Ruby language features preserved
- [ ] Backward compatibility with current examples

## Example Usage (Target API)

```clojure
;; Basic safe evaluation
(sri.api/eval-string "1 + 2")  ; => 3

;; With custom configuration
(sri.api/eval-string 
  "puts greeting" 
  {:namespaces {"user" {"greeting" "Hello World!"}}
   :allow-methods #{"puts" "+" "-" "*" "/"}})

;; Context management
(def ctx (sri.api/create-context 
           {:allow-methods #{"puts" "Array" "each"}
            :limits {:max-execution-time 1000}}))

(sri.api/eval-string* ctx "[1,2,3].each { |x| puts x }")

;; Multi-user isolation
(def user1-ctx (sri.api/fork-context base-ctx))
(def user2-ctx (sri.api/fork-context base-ctx))

(sri.api/eval-string* user1-ctx "x = 42")
(sri.api/eval-string* user2-ctx "x")  ; => error: undefined variable
```

## Conclusion

This design transforms SRI from a simple Ruby interpreter into a powerful, safe, and flexible Ruby evaluation engine suitable for:

- **DSL Implementation**: Safe domain-specific languages
- **User Scripting**: Allow users to write custom Ruby scripts
- **Configuration Files**: Ruby-based configuration with safety
- **Educational Tools**: Safe Ruby learning environments
- **Multi-tenant Applications**: Isolated Ruby execution per user

The key innovation is bringing SCI's safety model and API design to Ruby, creating a unique tool in the Ruby ecosystem for safe code evaluation.