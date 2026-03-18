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

(ns thalia.riscv32
  (:gen-class)
  (:refer-clojure :exclude [pop])
  (:require [clojure.string :as string]))

(defn ^:private next! [label]
  (swap! label inc))

(defn push [reg]
  (str "\taddi sp, sp, -4\n"
       "\tsw " reg ", 0(sp)\n"))

(defn pop [reg]
  (str "\tlw " reg ", 0(sp)\n"
       "\taddi sp, sp, 4\n"))

(defmulti ^:private emit (fn [node _] (:type node)))

(defmethod ^:private emit :EXPR-LITERAL [node _]
  (str "\tli t0, " (get-in node [:token :value]) "\n"
       (push "t0")))

(defmethod ^:private emit :EXPR-VARIABLE [node _]
  (str "\tla t0, " (get-in node [:token :value]) "\n"
       "\tlw t1, 0(t0)\n"
       (push "t1")))

(defmethod ^:private emit :EXPR-GROUPING [node label]
  (emit (:value node) label))

(defmethod ^:private emit :EXPR-UNARY [node label]
  (str (emit (:value node) label)
       (pop "t0")
       (case (get-in node [:operator :type])
         :MINUS "\tneg t0, t0\n"
         :BANG "\tseqz t0, t0\n")
       (push "t0")))

(defmethod ^:private emit :EXPR-BINARY [node label]
  (str (emit (:left node) label)
       (emit (:right node) label)
       (pop "t1")
       (pop "t0")
       (case (get-in node [:operator :type])
         :PLUS "\tadd t0, t0, t1\n"
         :MINUS "\tsub t0, t0, t1\n"
         :STAR "\tmul t0, t0, t1\n"
         :SLASH "\tdiv t0, t0, t1\n"
         :PERCENT "\trem t0, t0, t1\n"
         :EQUAL-EQUAL "\tsub t0, t0, t1\n\tseqz t0, t0\n"
         :BANG-EQUAL "\tsub t0, t0, t1\n\tsnez t0, t0\n"
         :LESS "\tslt t0, t0, t1\n"
         :LESS-EQUAL "\tsgt t0, t0, t1\n\txori t0, t0, 1\n"
         :GREATER "\tsgt t0, t0, t1\n"
         :GREATER-EQUAL "\tslt t0, t0, t1\n\txori t0, t0, 1\n")
       (push "t0")))

(defmethod ^:private emit :EXPR-ASSIGN [node label]
  (str (emit (:value node) label)
       (pop "t0")
       "\tla t1, " (get-in node [:target :token :value]) "\n"
       "\tsw t0, 0(t1)\n"
       (push "t0")))

(defmethod ^:private emit :STMT-EXPRESSION [node label]
  (str (emit (:value node) label)
       "\taddi sp, sp, 4\n"))

(defmethod ^:private emit :STMT-PRINT [node label]
  (->> (:values node)
       (map #(str (emit % label)
                  (pop "a1")
                  "\tla a0, __fmt_int__\n"
                  "\tcall printf\n"))
       (string/join "")))

(defmethod ^:private emit :STMT-PRINTLN [node label]
  (str (emit {:type :STMT-PRINT :values (:values node)} label)
       "\tla a0, __fmt_eol__\n"
       "\tcall printf\n"))

(defmethod ^:private emit :STMT-BLOCK [node label]
  (->> (:stmts node)
       (map #(emit % label))
       (string/join "")))

(defmethod ^:private emit :STMT-IF [node label]
  (let [ll1 (next! label)]
    (str (emit (:condition node) label)
         (pop "t0")
         "\tbeqz t0, .ll" ll1 "\n"
         (emit (:body node) label)
         (if (:else node)
           (let [ll2 (next! label)]
             (str "\tj .ll" ll2 "\n"
                  ".ll" ll1 ":\n"
                  (emit (:else node) label)
                  ".ll" ll2 ":\n"))
           (str ".ll" ll1 ":\n")))))

(defmethod ^:private emit :STMT-WHILE [node label]
  (let [ll1 (next! label)
        ll2 (next! label)]
    (str ".ll" ll1 ":\n"
         (emit (:condition node) label)
         (pop "t0")
         "\tbeqz t0, .ll" ll2 "\n"
         (emit (:body node) label)
         "\tj .ll" ll1 "\n"
         ".ll" ll2 ":\n")))

(defmethod ^:private emit :STMT-EACH [node label]
  (let [values (:values node)
        id (get-in node [:target :value])
        ll1 (next! label)
        ll2 (next! label)]
    (str (if (:from values)
           (emit (:from values) label)
           (push "zero"))
         (pop "t0")
         "\tla t1, " id "\n"
         "\tsw t0, 0(t1)\n"
         ".ll" ll1 ":\n"
         "\tla t1, " id "\n"
         "\tsw t0, 0(t1)\n"
         (push "t0")
         (emit (:to values) label)
         (pop "t1")
         (pop "t0")
         "\tbge t0, t1, .ll" ll2 "\n"
         (emit (:body node) label)
         (if (:step values)
           (str (emit (:step values) label)
                (pop "t2"))
           (str "\tli t2, 1\n"))
         "\tla t1, " id "\n"
         "\tlw t0, 0(t1)\n"
         "\tadd t0, t0, t2\n"
         "\tsw t0, 0(t1)\n"
         "\tj .ll" ll1 "\n"
         ".ll" ll2 ":\n")))

(defmethod ^:private emit :DECL-VARIABLE [node _]
  (str (get-in node [:token :value]) ": .word 0\n"))

(defmethod ^:private emit :DECL-PROGRAM [node label]
  (str ".text\n"
       "main:\n"
       (emit (:body node) label)
       "\tli a0, 0\n"
       "\tret\n"))

(defn make [nodes]
  (let [label (atom 0)
        program (or (->> nodes (filter #(= (:type %) :DECL-PROGRAM)) first)
                    {:type :DECL-PROGRAM :body {:type :STMT-BLOCK :stmts []}})]
    (str ".globl main\n\n"
         ".data\n"
         "__fmt_int__: .asciz \"%d \"\n"
         "__fmt_eol__: .asciz \"\\n\"\n"
         (->> nodes
          (filter #(= (:type %) :DECL-VARIABLE))
          (map #(emit % label))
          (string/join ""))
         "\n"
         (emit program label))))

