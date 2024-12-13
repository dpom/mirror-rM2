(ns user)


(defmacro jit
  "Just in time loading of dependencies."
  [sym]
  `(requiring-resolve '~sym))


(defn set-prep!
  []
  ((jit integrant.repl/set-prep!) #((jit mirror.system/prep) :dev)))


(defn go
  []
  (set-prep!)
  ((jit integrant.repl/go)))


(defn reset
  []
  (set-prep!)
  ((jit integrant.repl/reset)))


(defn halt
  []
  (set-prep!)
  ((jit integrant.repl/halt)))


(defn system
  []
  @(jit integrant.repl.state/system))


(defn config
  []
  @(jit integrant.repl.state/config))
