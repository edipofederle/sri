(ns sri.tokenizer)

(defrecord Token [type value line column])

(def keywords
  #{"if" "else" "elsif" "end" "while" "def" "class" "module" "nil" "true" "false" "and" "or" "not" "return" "break" "continue" "self"})

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
           current-state state]
      (let [ch (peek-char current-state)]
        (cond
          (digit? ch)
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur has-dot new-state))

          (and (= ch \.) (not has-dot) (digit? (peek-char (update current-state :pos inc))))
          (let [[consumed-ch new-state] (next-char current-state)]
            (.append sb consumed-ch)
            (recur true new-state))

          :else
          (let [number-str (.toString sb)
                token-type (if has-dot :float :integer)]
            [(create-token token-type number-str start-line start-column)
             current-state]))))))

(defn read-string-literal
  "Read a string literal from the input."
  [state quote-char]
  (let [start-line (:line state)
        start-column (:column state)
        [_ state-after-quote] (next-char state)
        sb (StringBuilder.)]
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
            (recur new-state)))))))

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
        (if (identifier-start? next-ch)
          ;; Symbol :identifier
          (let [[identifier-token state2] (read-identifier state1)]
            [(create-token :symbol (:value identifier-token) start-line start-column) state2])
          ;; Just a colon operator (for hash syntax)
          [(create-token :operator ":" start-line start-column) state1]))

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
      (read-number state)

      (or (= ch \") (= ch \'))
      (read-string-literal state ch)

      :else
      (read-operator state))))

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
