;; Copyright (C) 2024 Stan Vlad <vstan02@protonmail.com>
;;
;; This file is part of Thalia.
;;
;; Thalia is free software: you can redistribute it and/or modify
;; it under the terms of the GNU General Public License as published by
;; the Free Software Foundation, either version 3 of the License, or
;; (at your option) any later version.
;;
;; This program is distributed in the hope that it will be useful,
;; but WITHOUT ANY WARRANTY; without even the implied warranty of
;; MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
;; GNU General Public License for more details.
;;
;; You should have received a copy of the GNU General Public License
;; along with this program. If not, see <https://www.gnu.org/licenses/>.

(ns thalia.core
  (:gen-class)
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [cheshire.core :as json]
            [clojure.string :as string]
            [thalia.x86-32 :as x86-32]
            [thalia.riscv32 :as riscv32]
            [thalia.lexer :as lexer]
            [thalia.parser :as parser]))

(def ^:private codegens
  {:i386 x86-32/make
   :x86-32 x86-32/make
   :rv32im riscv32/make})

(def ^:private assemblers
  {:i386 "gcc -no-pie -m32"
   :x86-32 "gcc -no-pie -m32"
   :rv32im "riscv32-unknown-linux-gnu-gcc -march=rv32imd -mabi=ilp32d"})

(defn ^:private path-resolve [parent file]
  (->> file (fs/path parent) fs/canonicalize str))

(defn ^:private parse-cfg [path]
  (when-not path
    (throw (Exception. "Invalid config path.")))
  (let [parent (fs/parent path)
        cfg (json/parse-string (slurp path) true)
        arch (->> cfg :arch keyword)]
    (when-not (->> [:src :dest :target]
                   (map cfg) (filter nil?) empty?)
      (throw (Exception. "Fields 'src', 'dest' and 'target' are required.")))
    (let [dest (->> cfg :dest (path-resolve parent))]
      (merge
        {:static false}
        {:src (->> cfg :src (path-resolve parent))
         :dest dest
         :arch (if (->> arch codegens nil?) :x86-32 arch)
         :target (->> cfg :target (path-resolve dest))}))))

(defn ^:private src->asm [cfg src]
  (let [dest (->> src
                  (#(string/replace % (:src cfg) (:dest cfg)))
                  (#(string/replace % ".th" ".s")))
        codegen (->> cfg :arch codegens)]
    (->> (slurp src)
         (lexer/scan)
         (:tokens)
         (parser/parse)
         (:ast)
         (codegen)
         (spit dest))
    dest))

(defn ^:private asm->exe [cfg srcs]
  (let [dest (:target cfg)
        cc (->> cfg :arch assemblers)
        static (if (:static cfg) "-static" "")
        srcs (string/join " " srcs)]
    (process/shell
      (format "%1$s %2$s %3$s -o %4$s" cc srcs static dest))
    dest))

(defn -main [& args]
  (try
    (let [cfg (parse-cfg (first args))
          _ (fs/delete-tree (:dest cfg))
          _ (fs/create-dirs (:dest cfg))]
      (->> (fs/match (:src cfg) "regex:.*\\.th" {:recursive true})
           (map str)
           (map #(src->asm cfg %))
           (asm->exe cfg)))
    (catch Exception error
      (println "[ERROR]:" (.getMessage error)))))

