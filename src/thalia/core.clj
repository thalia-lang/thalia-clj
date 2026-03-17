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
            [thalia.lexer :as lexer]
            [thalia.parser :as parser]))

(defn ^:private path-resolve [parent file]
  (->> file (fs/path parent) fs/canonicalize str))

(defn ^:private parse-cfg [path]
  (when-not path
    (throw (Exception. "Invalid config path.")))
  (let [parent (fs/parent path)
        cfg (json/parse-string (slurp path) true)]
    (when-not (->> [:src :dest :target]
                   (map #(% cfg))
                   (filter nil?)
                   (empty?))
      (throw (Exception. "Fields 'src', 'dest' and 'target' are required.")))
    (let [dest (->> cfg :dest (path-resolve parent))]
      (merge
       {:cc "yasm -felf64"
        :stdlib (or (System/getenv "STDLIB_DIR")
                    "/usr/share/thalia/stdlib")}
       {:src (->> cfg :src (path-resolve parent))
        :dest dest
        :target (->> cfg :target (path-resolve dest))}))))

(defn ^:private src->asm [cfg src]
  (let [dest (->> src
                  (#(string/replace % (:src cfg) (:dest cfg)))
                  (#(string/replace % ".th" ".asm")))]
    (->> (slurp src)
         (lexer/scan)
         (:tokens)
         (parser/parse)
         (:ast)
         (x86-32/make)
         (spit dest))
    dest))

(defn ^:private asm->obj [cfg src]
  (let [dest (->> src (#(string/replace % ".asm" ".o")))]
    (->> src
         (#(format "%1$s %2$s -o %3$s" (:cc cfg) % dest))
         (process/shell))
    dest))

(defn ^:private objs->exe [cfg srcs]
  (let [libs (->> (fs/match (:stdlib cfg) "regex:.*\\.o" {:recursive true})
                  (map str)
                  (string/join " "))
        dest (:target cfg)]
    (->> srcs
         (string/join " ")
         (#(format "ld %1$s %2$s -o %3$s" % libs dest))
         (process/shell))
    dest))

(defn -main [& args]
  (try
    (let [cfg (parse-cfg (first args))]
      (fs/delete-tree (:dest cfg))
      (fs/create-dirs (:dest cfg))
      (->> (fs/match (:src cfg) "regex:.*\\.th" {:recursive true})
           (map str)
           (map #(src->asm cfg %))
           (map #(asm->obj cfg %))
           (#(objs->exe cfg %))))
    (catch Exception error
      (println "[ERROR]:" (.getMessage error)))))

