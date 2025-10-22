(ns sri.tokenizer)

(defrecord Token [type value line column])

(def keywords
  #{"if" "else" "elsif" "end" "while" "def" "class" "module" "nil" "true" "false" "and" "or" "not" "return" "break" "continue" "next" "self" "case" "when" "for" "until" "in" "loop" "do" "attr_accessor" "attr_reader" "attr_writer"})

(def operators
  {"+" :plus
   "-" :minus
   "*" :multiply
   "/" :divide
   "%" :modulo
   "==" :equal
   "!=" :not-equal
   "<" :less-than
   "<=" :less-equal
   ">" :greater-than
   ">=" :greater-equal
   "=" :assign
   "+=" :plus-assign
   "-=" :minus-assign
   "*=" :multiply-assign
   "/=" :divide-assign
   "..." :exclusive-range
   ".." :inclusive-range
   "." :dot
   "(" :left-paren
   ")" :right-paren
   "[" :left-bracket
   "]" :right-bracket
   "{" :left-brace
   "}" :right-brace
   "," :comma
   ";" :semicolon
   ":" :colon
   "=>" :hash-arrow
   "@" :at
   "|" :pipe})

(defn create-token
  "Create a new token with position information."
  [type value line column]
  (->Token type value line column))

(defn whitespace?
  "Check if character is whitespace (excluding newline)."
  [ch]
  (and ch (Character/isWhitespace ^char ch) (not= ch \newline)))

(defn digit?
  "Check if character is a digit."
  [ch]
  (and ch (Character/isDigit ^char ch)))

(defn hex-digit?
  "Check if character is a hexadecimal digit."
  [ch]
  (and ch (contains? #{\0 \1 \2 \3 \4 \5 \6 \7 \8 \9 \a \b \c \d \e \f \A \B \C \D \E \F} ch)))

(defn binary-digit?
  "Check if character is a binary digit."
  [ch]
  (and ch (contains? #{\0 \1} ch)))

(defn octal-digit?
  "Check if character is an octal digit."
  [ch]
  (and ch (contains? #{\0 \1 \2 \3 \4 \5 \6 \7} ch)))

(defn letter?
  "Check if character is a letter."
  [ch]
  (and ch (Character/isLetter ^char ch)))

(defn identifier-start?
  "Check if character can start an identifier."
  [ch]
  (or (letter? ch) (= ch \_)))

(defn identifier-char?
  "Check if character can be part of an identifier."
  [ch]
  (or (letter? ch) (digit? ch) (= ch \_) (= ch \?) (= ch \!)))

(defn peek-char
  "Look at the current character without consuming it."
  [state]
  (when (< (:pos state) (:length state))
    (.charAt ^String (:input state) (:pos state))))

(defn next-char
  "Consume and return the current character, advancing position."
  [state]
  (let [ch (peek-char state)]
    (when ch
      (if (= ch \newline)
        (-> state
            (update :pos inc)
            (update :line inc)
            (assoc :column 1))
        (-> state
            (update :pos inc)
            (update :column inc))))
    [ch (if ch
          (if (= ch \newline)
            (-> state
                (update :pos inc)
                (update :line inc)
                (assoc :column 1))
            (-> state
                (update :pos inc)
                (update :column inc)))
          state)]))

(defn skip-whitespace
  "Skip whitespace characters, returning updated state."
  [state]
  (loop [current-state state]
    (let [ch (peek-char current-state)]
      (if (whitespace? ch)
        (let [[_ new-state] (next-char current-state)]
          (recur new-state))
        current-state))))

(defn read-identifier
  "Read an identifier or keyword from the input."
  [state]
  (let [start-line (:line state)
        start-column (:column state)
        sb (StringBuilder.)]
    (loop [current-state state]
      (let [ch (peek-char current-state)]
        (if (identifier-char? ch)
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur new-state))
          (let [identifier (.toString sb)
                token-type (if (contains? keywords identifier)
                            :keyword
                            :identifier)]
            [(create-token token-type identifier start-line start-column)
             current-state]))))))

(defn read-number
  "Read a number (integer or float) from the input."
  [state]
  (let [start-line (:line state)
        start-column (:column state)
        sb (StringBuilder.)]
    (loop [has-dot false
           has-exponent false
           current-state state]
      (let [ch (peek-char current-state)]
        (cond
          (digit? ch)
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur has-dot has-exponent new-state))

          (and (= ch \.) (not has-dot) (not has-exponent) (digit? (peek-char (update current-state :pos inc))))
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur true has-exponent new-state))

          ;; Handle scientific notation (e or E)
          (and (or (= ch \e) (= ch \E)) 
               (not has-exponent)
               (> (.length sb) 0))  ; Must have at least one digit before exponent
          (let [[consumed-ch state-after-e] (next-char current-state)
                next-ch (peek-char state-after-e)]
            (.append sb consumed-ch)
            ;; Handle optional + or - after e/E
            (if (or (= next-ch \+) (= next-ch \-))
              (let [[sign-ch state-after-sign] (next-char state-after-e)]
                (.append sb sign-ch)
                (recur has-dot true state-after-sign))
              (recur has-dot true state-after-e)))

          ;; Handle underscores in numbers (Ruby allows them as visual separators)
          (and (= ch \_) 
               (> (.length sb) 0)  ; Must have at least one digit before underscore
               (digit? (peek-char (update current-state :pos inc))))  ; Must have digit after underscore
          (let [[_ new-state] (next-char current-state)]
            ;; Skip the underscore, don't add it to the string builder
            (recur has-dot has-exponent new-state))

          :else
          ;; Check for rational suffix 'r' or complex suffix 'i'
          (cond
            (and (= ch \r) 
                 (> (.length sb) 0)  ; Must have at least one digit
                 (not has-exponent)) ; Rationals don't have exponents
            (let [[_ new-state] (next-char current-state)
                  number-str (.toString sb)]
              [(create-token :rational number-str start-line start-column)
               new-state])

            (and (= ch \i) 
                 (> (.length sb) 0)) ; Must have at least one digit
            (let [[_ new-state] (next-char current-state)
                  number-str (.toString sb)]
              [(create-token :complex number-str start-line start-column)
               new-state])

            :else
            (let [number-str (.toString sb)
                  token-type (if (or has-dot has-exponent) :float :integer)]
              [(create-token token-type number-str start-line start-column)
               current-state])))))))

(defn read-hex-number
  "Read a hexadecimal number (starting with 0x or 0X) from the input."
  [state]
  (let [start-line (:line state)
        start-column (:column state)
        ;; Skip the '0'
        [_ state-after-0] (next-char state)
        ;; Skip the 'x' or 'X'
        [_ state-after-x] (next-char state-after-0)
        sb (StringBuilder.)]
    (loop [current-state state-after-x]
      (let [ch (peek-char current-state)]
        (cond
          (hex-digit? ch)
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur new-state))

          ;; Handle underscores in hex numbers too
          (and (= ch \_) 
               (> (.length sb) 0)  ; Must have at least one hex digit before underscore
               (hex-digit? (peek-char (update current-state :pos inc))))  ; Must have hex digit after underscore
          (let [[_ new-state] (next-char current-state)]
            ;; Skip the underscore, don't add it to the string builder
            (recur new-state))

          :else
          ;; Check for rational suffix 'r' or complex suffix 'i'
          (cond
            (= ch \r)
            (let [[_ new-state] (next-char current-state)
                  hex-str (.toString sb)
                  ;; Convert hex string to integer for rational
                  int-value (try
                              (Integer/parseInt hex-str 16)
                              (catch NumberFormatException _
                                (BigInteger. hex-str 16)))]
              [(create-token :rational (str int-value) start-line start-column)
               new-state])

            (= ch \i)
            (let [[_ new-state] (next-char current-state)
                  hex-str (.toString sb)
                  ;; Convert hex string to integer for complex
                  int-value (try
                              (Integer/parseInt hex-str 16)
                              (catch NumberFormatException _
                                (BigInteger. hex-str 16)))]
              [(create-token :complex (str int-value) start-line start-column)
               new-state])

            :else
            (let [hex-str (.toString sb)
                  ;; Convert hex string to integer
                  int-value (try
                              (Integer/parseInt hex-str 16)
                              (catch NumberFormatException _
                                (BigInteger. hex-str 16)))]
              [(create-token :integer (str int-value) start-line start-column)
               current-state])))))))

(defn read-binary-number
  "Read a binary number (starting with 0b or 0B) from the input."
  [state]
  (let [start-line (:line state)
        start-column (:column state)
        ;; Skip the '0'
        [_ state-after-0] (next-char state)
        ;; Skip the 'b' or 'B'
        [_ state-after-b] (next-char state-after-0)
        sb (StringBuilder.)]
    (loop [current-state state-after-b]
      (let [ch (peek-char current-state)]
        (cond
          (binary-digit? ch)
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur new-state))

          ;; Handle underscores in binary numbers too
          (and (= ch \_) 
               (> (.length sb) 0)  ; Must have at least one binary digit before underscore
               (binary-digit? (peek-char (update current-state :pos inc))))  ; Must have binary digit after underscore
          (let [[_ new-state] (next-char current-state)]
            ;; Skip the underscore, don't add it to the string builder
            (recur new-state))

          :else
          ;; Check for rational suffix 'r' or complex suffix 'i'
          (cond
            (= ch \r)
            (let [[_ new-state] (next-char current-state)
                  binary-str (.toString sb)
                  ;; Convert binary string to integer for rational
                  int-value (try
                              (Integer/parseInt binary-str 2)
                              (catch NumberFormatException _
                                (BigInteger. binary-str 2)))]
              [(create-token :rational (str int-value) start-line start-column)
               new-state])

            (= ch \i)
            (let [[_ new-state] (next-char current-state)
                  binary-str (.toString sb)
                  ;; Convert binary string to integer for complex
                  int-value (try
                              (Integer/parseInt binary-str 2)
                              (catch NumberFormatException _
                                (BigInteger. binary-str 2)))]
              [(create-token :complex (str int-value) start-line start-column)
               new-state])

            :else
            (let [binary-str (.toString sb)
                  ;; Convert binary string to integer
                  int-value (try
                              (Integer/parseInt binary-str 2)
                              (catch NumberFormatException _
                                (BigInteger. binary-str 2)))]
              [(create-token :integer (str int-value) start-line start-column)
               current-state])))))))

(defn read-octal-number
  "Read an octal number (starting with 0 followed by octal digits) from the input."
  [state]
  (let [start-line (:line state)
        start-column (:column state)
        sb (StringBuilder.)]
    ;; Add the leading 0 but don't consume it yet - we'll handle it in the loop
    (loop [current-state state]
      (let [ch (peek-char current-state)]
        (cond
          (octal-digit? ch)
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur new-state))

          ;; Handle underscores in octal numbers too
          (and (= ch \_) 
               (> (.length sb) 0)  ; Must have at least one octal digit before underscore
               (octal-digit? (peek-char (update current-state :pos inc))))  ; Must have octal digit after underscore
          (let [[_ new-state] (next-char current-state)]
            ;; Skip the underscore, don't add it to the string builder
            (recur new-state))

          :else
          ;; Check for rational suffix 'r' or complex suffix 'i'
          (cond
            (= ch \r)
            (let [[_ new-state] (next-char current-state)
                  octal-str (.toString sb)
                  ;; Convert octal string to integer for rational
                  int-value (try
                              (Integer/parseInt octal-str 8)
                              (catch NumberFormatException _
                                (BigInteger. octal-str 8)))]
              [(create-token :rational (str int-value) start-line start-column)
               new-state])

            (= ch \i)
            (let [[_ new-state] (next-char current-state)
                  octal-str (.toString sb)
                  ;; Convert octal string to integer for complex
                  int-value (try
                              (Integer/parseInt octal-str 8)
                              (catch NumberFormatException _
                                (BigInteger. octal-str 8)))]
              [(create-token :complex (str int-value) start-line start-column)
               new-state])

            :else
            (let [octal-str (.toString sb)
                  ;; Convert octal string to integer
                  int-value (try
                              (Integer/parseInt octal-str 8)
                              (catch NumberFormatException _
                                (BigInteger. octal-str 8)))]
              [(create-token :integer (str int-value) start-line start-column)
               current-state])))))))

(declare read-interpolation-expression read-word-array)

(defn string-contains-interpolation?
  "Check if a string contains #{...} interpolation."
  [state quote-char]
  (loop [current-state state]
    (let [ch (peek-char current-state)]
      (cond
        (nil? ch) false
        (= ch quote-char) false
        (and (= ch \#) 
             (let [[_ next-state] (next-char current-state)]
               (and next-state (= (peek-char next-state) \{)))) true
        :else 
        (let [[_ next-state] (next-char current-state)]
          (if next-state
            (recur next-state)
            false))))))

(defn read-interpolated-string-parts
  "Read parts of an interpolated string, separating text and expressions."
  [state quote-char]
  (let [start-line (:line state)
        start-column (:column state)
        [_ state-after-quote] (next-char state)]
    (loop [current-state state-after-quote
           parts []]
      (let [ch (peek-char current-state)]
        (cond
          (nil? ch)
          (throw (ex-info "Unterminated string literal"
                         {:line start-line :column start-column}))

          (= ch quote-char)
          (let [[_ final-state] (next-char current-state)]
            [(create-token :interpolated-string parts start-line start-column)
             final-state])

          (and (= ch \#) 
               (let [[_ next-state] (next-char current-state)]
                 (and next-state (= (peek-char next-state) \{))))
          ;; Found interpolation start #{
          (let [;; Skip #{ 
                [_ state-after-hash] (next-char current-state)
                [_ state-after-brace] (next-char state-after-hash)
                ;; Read expression until }
                [expr-source expr-end-state] (read-interpolation-expression state-after-brace)]
            (recur expr-end-state 
                   (conj parts {:type :expression :source expr-source})))

          :else
          ;; Regular character - add to text part
          (let [[consumed-ch new-state] (next-char current-state)
                last-part (last parts)]
            (if (string? last-part)
              ;; Append to existing text part
              (recur new-state (conj (pop parts) (str last-part consumed-ch)))
              ;; Start new text part
              (recur new-state (conj parts (str consumed-ch))))))))))

(defn read-interpolation-expression
  "Read the source code inside #{...} until closing brace."
  [state]
  (let [sb (StringBuilder.)]
    (loop [current-state state
           brace-count 1]
      (let [ch (peek-char current-state)]
        (cond
          (nil? ch)
          (throw (ex-info "Unterminated interpolation expression" {}))

          (= ch \})
          (if (= brace-count 1)
            ;; End of interpolation
            (let [[_ final-state] (next-char current-state)]
              [(.toString sb) final-state])
            ;; Nested brace
            (let [[consumed-ch new-state] (next-char current-state)]
              (.append sb consumed-ch)
              (recur new-state (dec brace-count))))

          (= ch \{)
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur new-state (inc brace-count)))

          :else
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur new-state brace-count)))))))

(defn read-string-literal
  "Read a string literal from the input."
  [state quote-char]
  (let [start-line (:line state)
        start-column (:column state)
        [_ state-after-quote] (next-char state)]
    ;; Check if this is an interpolated string (contains #{)
    (if (and (= quote-char \") (string-contains-interpolation? state-after-quote quote-char))
      (read-interpolated-string-parts state quote-char)
      ;; Regular string literal
      (let [sb (StringBuilder.)]
        (loop [current-state state-after-quote]
          (let [ch (peek-char current-state)]
            (cond
              (nil? ch)
              (throw (ex-info "Unterminated string literal"
                             {:line start-line :column start-column}))

              (= ch quote-char)
              (let [[_ final-state] (next-char current-state)]
                [(create-token :string (.toString sb) start-line start-column)
                 final-state])

              (= ch \\)
              (let [[_ escape-state] (next-char current-state)
                    [escaped-ch new-state] (next-char escape-state)
                    actual-char (case escaped-ch
                                 \n \newline
                                 \t \tab
                                 \r \return
                                 \\ \\
                                 \" \"
                                 \' \'
                                 escaped-ch)]
                (.append sb actual-char)
                (recur new-state))

              :else
              (let [[consumed-ch new-state] (next-char current-state)]
                (.append sb consumed-ch)
                (recur new-state)))))))))

(defn read-operator
  "Read an operator or punctuation from the input."
  [state]
  (let [ch (peek-char state)
        start-line (:line state)
        start-column (:column state)]
    (cond
      (= ch \=)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "==" start-line start-column) state2])
          (if (= next-ch \>)
            (let [[_ state2] (next-char state1)]
              [(create-token :operator "=>" start-line start-column) state2])
            [(create-token :operator "=" start-line start-column) state1])))

      (= ch \!)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "!=" start-line start-column) state2])
          (throw (ex-info "Unexpected character '!'"
                         {:line start-line :column start-column}))))

      (= ch \<)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "<=" start-line start-column) state2])
          [(create-token :operator "<" start-line start-column) state1]))

      (= ch \>)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator ">=" start-line start-column) state2])
          [(create-token :operator ">" start-line start-column) state1]))

      (= ch \+)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "+=" start-line start-column) state2])
          [(create-token :operator "+" start-line start-column) state1]))

      (= ch \-)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "-=" start-line start-column) state2])
          [(create-token :operator "-" start-line start-column) state1]))

      (= ch \*)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "*=" start-line start-column) state2])
          [(create-token :operator "*" start-line start-column) state1]))

      (= ch \/)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \=)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "/=" start-line start-column) state2])
          [(create-token :operator "/" start-line start-column) state1]))

      (= ch \@)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \@)
          ;; Class variable @@var
          (let [[_ state2] (next-char state1)]
            (if (identifier-start? (peek-char state2))
              (let [[identifier-token state3] (read-identifier state2)]
                [(create-token :class-variable (str "@@" (:value identifier-token)) start-line start-column) state3])
              (throw (ex-info "Expected identifier after @@"
                             {:line start-line :column start-column}))))
          ;; Instance variable @var
          (if (identifier-start? next-ch)
            (let [[identifier-token state2] (read-identifier state1)]
              [(create-token :instance-variable (str "@" (:value identifier-token)) start-line start-column) state2])
            (throw (ex-info "Expected identifier after @"
                           {:line start-line :column start-column})))))

      (= ch \:)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (cond
          ;; :: operator for module scope resolution
          (= next-ch \:)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "::" start-line start-column) state2])
          ;; Symbol :identifier
          (identifier-start? next-ch)
          (let [[identifier-token state2] (read-identifier state1)]
            [(create-token :symbol (:value identifier-token) start-line start-column) state2])
          ;; Just a colon operator (for hash syntax)
          :else
          [(create-token :operator ":" start-line start-column) state1]))

      (= ch \.)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \.)
          (let [[_ state2] (next-char state1)
                third-ch (peek-char state2)]
            (if (= third-ch \.)
              ;; Exclusive range ...
              (let [[_ state3] (next-char state2)]
                [(create-token :operator "..." start-line start-column) state3])
              ;; Inclusive range ..
              [(create-token :operator ".." start-line start-column) state2]))
          ;; Just a dot operator
          [(create-token :operator "." start-line start-column) state1]))

      (= ch \&)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \&)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "&&" start-line start-column) state2])
          (throw (ex-info "Unexpected character '&' (use && for logical AND)"
                         {:line start-line :column start-column}))))

      (= ch \|)
      (let [[_ state1] (next-char state)
            next-ch (peek-char state1)]
        (if (= next-ch \|)
          (let [[_ state2] (next-char state1)]
            [(create-token :operator "||" start-line start-column) state2])
          ;; Single | is used for block parameters like { |x| ... }
          [(create-token :operator "|" start-line start-column) state1]))

      :else
      (let [op-str (str ch)]
        (if (contains? operators op-str)
          (let [[_ new-state] (next-char state)]
            [(create-token :operator op-str start-line start-column) new-state])
          (throw (ex-info (str "Unknown operator: " ch)
                         {:line start-line :column start-column})))))))

(defn skip-comment
  "Skip a single-line comment, returning updated state."
  [state]
  (let [[_ state-after-hash] (next-char state)]
    (loop [current-state state-after-hash]
      (let [ch (peek-char current-state)]
        (if (or (nil? ch) (= ch \newline))
          current-state
          (let [[_ new-state] (next-char current-state)]
            (recur new-state)))))))

(defn tokenize-next
  "Tokenize the next token from the input state."
  [state]
  (let [state (skip-whitespace state)
        ch (peek-char state)]
    (cond
      (nil? ch)
      [nil state]

      (= ch \newline)
      (let [[_ new-state] (next-char state)]
        [(create-token :newline "\\n" (:line state) (:column state)) new-state])

      (= ch \#)
      (recur (skip-comment state))

      (identifier-start? ch)
      (read-identifier state)

      (digit? ch)
      ;; Check for special number formats starting with 0
      (if (= ch \0)
        (let [next-ch (peek-char (update state :pos inc))]
          (cond
            ;; Hexadecimal numbers (0x or 0X)
            (or (= next-ch \x) (= next-ch \X))
            (read-hex-number state)
            
            ;; Binary numbers (0b or 0B)
            (or (= next-ch \b) (= next-ch \B))
            (read-binary-number state)
            
            ;; Octal numbers (0 followed by octal digits)
            (octal-digit? next-ch)
            (read-octal-number state)
            
            ;; Regular number starting with 0 (like 0.5 or just 0)
            :else
            (read-number state)))
        (read-number state))

      (or (= ch \") (= ch \'))
      (read-string-literal state ch)

      (= ch \%)
      ;; Check for %w() and %W() word arrays
      (let [next-ch (peek-char (update state :pos inc))]
        (cond 
          (= next-ch \w) (read-word-array state false)  ; %w - no interpolation
          (= next-ch \W) (read-word-array state true)   ; %W - with interpolation
          :else (read-operator state)))

      :else
      (read-operator state))))

(defn read-word-array
  "Read %w() or %W() word array literal."
  [state with-interpolation?]
  (let [start-line (:line state)
        start-column (:column state)
        ;; Skip %w
        [_ state-after-percent] (next-char state)
        [_ state-after-w] (next-char state-after-percent)
        ;; Get the delimiter
        delimiter-ch (peek-char state-after-w)]
    (if (contains? #{\( \[ \{ \< \/ \| \! \@ \# \$ \% \^ \& \* \- \_ \+ \= \~ \`} delimiter-ch)
      (let [closing-delimiter (case delimiter-ch
                                \( \)
                                \[ \]
                                \{ \}
                                \< \>
                                delimiter-ch) ; For other delimiters, use the same character
            [_ state-after-open] (next-char state-after-w)
            ;; Read content until closing delimiter, handling escape sequences
            [content final-state] (loop [current-state state-after-open
                                         content ""]
                                    (let [ch (peek-char current-state)]
                                      (cond
                                        (nil? ch)
                                        (throw (ex-info "Unterminated %w literal"
                                                       {:line start-line :column start-column}))
                                        (= ch closing-delimiter)
                                        [content (second (next-char current-state))]
                                        (= ch \\)
                                        ;; Handle escape sequences
                                        (let [[_ state-after-backslash] (next-char current-state)
                                              next-ch (peek-char state-after-backslash)]
                                          (if (nil? next-ch)
                                            (throw (ex-info "Unterminated escape sequence in %w literal"
                                                           {:line start-line :column start-column}))
                                            (let [escaped-char (case next-ch
                                                                \space \space
                                                                \t \tab      ; t character → actual tab
                                                                \n \newline  ; n character → actual newline
                                                                \r \return   ; r character → actual carriage return
                                                                \\ \\
                                                                ;; For any other character, just include it literally
                                                                next-ch)
                                                  [_ state-after-escaped] (next-char state-after-backslash)]
                                              ;; Mark escaped whitespace with special placeholders
                                              (cond
                                                (= escaped-char \space)
                                                (recur state-after-escaped
                                                       (str content "\u0001"))  ; Placeholder for escaped space
                                                (= escaped-char \tab)
                                                (recur state-after-escaped
                                                       (str content "\u0002"))  ; Placeholder for escaped tab
                                                (= escaped-char \newline)
                                                (recur state-after-escaped
                                                       (str content "\u0003"))  ; Placeholder for escaped newline
                                                :else
                                                (recur state-after-escaped
                                                       (str content escaped-char))))))
                                        :else
                                        (recur (second (next-char current-state))
                                               (str content ch)))))
            ;; Store raw content and interpolation flag
            token-type (if with-interpolation? :interpolated-word-array :word-array)
            ;; For interpolated arrays, store raw content; for regular arrays, split immediately
            token-value (if with-interpolation?
                         content  ; Store raw content for later interpolation
                         (if (empty? content) 
                           [] 
                           ;; Split by unescaped spaces, then restore escaped characters
                           (map #(-> %
                                     (clojure.string/replace "\u0001" " ")
                                     (clojure.string/replace "\u0002" "\t")
                                     (clojure.string/replace "\u0003" "\n"))
                                (clojure.string/split content #"\s+"))))]
        [(create-token token-type token-value start-line start-column) final-state])
      (throw (ex-info (str "Invalid delimiter for %w: " delimiter-ch)
                     {:line start-line :column start-column})))))

(defn tokenize
  "Tokenize a complete Ruby source string into a sequence of tokens."
  [input]
  (let [initial-state {:input input
                       :length (count input)
                       :pos 0
                       :line 1
                       :column 1}]
    (loop [tokens []
           state initial-state]
      (let [[token new-state] (tokenize-next state)]
        (if token
          (recur (conj tokens token) new-state)
          tokens)))))
