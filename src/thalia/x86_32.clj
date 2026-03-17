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

(ns thalia.x86-32
  (:gen-class)
  (:require [clojure.string :as string]))

(defn ^:private next! [label]
  (swap! label inc))

(defmulti ^:private emit (fn [node _] (:type node)))

(defmethod ^:private emit :EXPR-LITERAL [node _]
  (str "\tpush " (get-in node [:token :value]) "\n"))

(defmethod ^:private emit :EXPR-VARIABLE [node _]
  (str "\tpush qword [" (get-in node [:token :value]) "]\n"))

(defmethod ^:private emit :EXPR-GROUPING [node label]
  (emit (:value node) label))

(defmethod ^:private emit :EXPR-UNARY [node label]
  (str (emit (:value node) label)
       "\tpop rax\n"
       (case (get-in node [:operator :type])
         :MINUS "\tneg rax\n"
         :BANG "\tnot rax\n")
       "\tpush rax\n"))

(defmethod ^:private emit :EXPR-BINARY [node label]
  (str (emit (:left node) label)
       (emit (:right node) label)
       "\tpop rbx\n\tpop rax\n"
       (case (get-in node [:operator :type])
         :PLUS "\tadd rax, rbx\n"
         :MINUS "\tsub rax, rbx\n"
         :STAR "\timul rax, rbx\n"
         :SLASH "\tidiv rbx\n"
         :PERCENT "\txor rdx, rdx\n\tidiv rbx\n\tmov rax, rdx\n"
         :EQUAL-EQUAL (str "\tmov rdx, 0\n\tcmp rax, rbx\n\tjne .ll" (next! label) "\n"
                           "\tmov rdx, 1\n.ll" @label ":\n\tmov rax, rdx\n")
         :BANG-EQUAL (str "\tmov rdx, 1\n\tcmp rax, rbx\n\tjne .ll" (next! label) "\n"
                          "\tmov rdx, 0\n.ll" @label ":\n\tmov rax, rdx\n")
         :GREATER-EQUAL (str "\tmov rdx, 0\n\tcmp rax, rbx\n\tjl .ll" (next! label) "\n"
                             "\tmov rdx, 1\n.ll" @label ":\n\tmov rax, rdx\n")
         :LESS (str "\tmov rdx, 1\n\tcmp rax, rbx\n\tjl .ll" (next! label) "\n"
                    "\tmov rdx, 0\n.ll" @label ":\n\tmov rax, rdx\n")
         :LESS-EQUAL (str "\tmov rdx, 0\n\tcmp rax, rbx\n\tjg .ll" (next! label) "\n"
                          "\tmov rdx, 1\n.ll" @label ":\n\tmov rax, rdx\n")
         :GREATER (str "\tmov rdx, 1\n\tcmp rax, rbx\n\tjg .ll" (next! label) "\n"
                       "\tmov rdx, 0\n.ll" @label ":\n\tmov rax, rdx\n"))
       "\tpush rax\n"))

(defmethod ^:private emit :EXPR-ASSIGN [node label]
  (str (emit (:value node) label)
       "\tpop qword [" (get-in node [:target :token :value]) "]\n"
       (emit (:target node) label)))

(defmethod ^:private emit :STMT-EXPRESSION [node label]
  (str (emit (:value node) label)
       "\tpop rax\n"))

(defmethod ^:private emit :STMT-PRINT [node label]
  (->> (:values node)
       (map #(str (emit % label)
                  "\tpop rax\n\tcall int_print\n\tmov rax, ' '\n\tcall chr_print\n"))
       (string/join "")))

(defmethod ^:private emit :STMT-PRINTLN [node label]
  (->> (:values node)
       (map #(str (emit % label)
                  "\tpop rax\n\tcall int_print\n\tmov rax, ' '\n\tcall chr_print\n"))
       (string/join "")
       (#(str % "\tcall eol_print\n"))))

(defmethod ^:private emit :STMT-BLOCK [node label]
  (->> (:stmts node)
       (map #(emit % label))
       (string/join "")))

(defmethod ^:private emit :STMT-IF [node label]
  (let [lbl1 (next! label)]
    (str (emit (:condition node) label)
         "\ttest rax, rax\n\tje .ll" lbl1 "\n"
         (emit (:body node) label)
         (if (:else node)
           (let [lbl2 (next! label)]
             (str "\tjmp .ll" lbl2 "\n.ll" lbl1 ":\n"
                  (emit (:else node) label)
                  ".ll" lbl2 ":\n"))
           (str ".ll" lbl1 ":\n")))))

(defmethod ^:private emit :STMT-WHILE [node label]
  (let [lbl1 (next! label)
        lbl2 (next! label)]
    (str ".ll" lbl1 ":\n"
         (emit (:condition node) label)
         "\ttest rax, rax\n\tje .ll" lbl2 "\n"
         (emit (:body node) label)
         "\tjmp .ll" lbl1 "\n.ll" lbl2 ":\n")))

(defmethod ^:private emit :STMT-EACH [node label]
  (let [values (:values node)
        id (get-in node [:target :value])
        lbl1 (next! label)
        lbl2 (next! label)]
    (str (if (:from values)
           (emit (:from values) label)
           "\tpush 0\n")
         "\tpop qword [" id "]\n"
         ".ll" lbl1 ":\n"
         "\tpush qword [" id "]\n"
         (emit (:to values) label)
         "\tpop rbx\n\tpop rax\n\tcmp rax, rbx\n\tjge .ll" lbl2 "\n"
         (emit (:body node) label)
         (if (:step values)
           (str (emit (:step values) label)
                "\tpop rax\n\tadd qword [" id "], rax\n")
           (str "\tinc qword [" id "]\n"))
         "\tjmp .ll" lbl1 "\n.ll" lbl2 ":\n")))

(defmethod ^:private emit :DECL-VARIABLE [node _]
  (str "\t" (get-in node [:token :value]) " dq 0\n"))

(defmethod ^:private emit :DECL-PROGRAM [node label]
  (str "\nsection .text\n_start:\n"
       (emit (:body node) label)
       "\tcall sys_exit\n\n"))

(defn make [nodes]
  (let [label (atom 0)
        program (->> nodes
                     (filter #(= (:type %) :DECL-PROGRAM))
                     (first)
                     (#(if % % {:type :DECL-PROGRAM :body {:type :STMT-BLOCK :stmts []}})))]
    (->> nodes
         (filter #(not= (:type %) :DECL-PROGRAM))
         (map #(emit % label))
         (string/join "")
         (#(str "global _start\n\n"
                "extern sys_exit\n"
                "extern chr_print\n"
                "extern eol_print\n"
                "extern int_print\n"
                "\nsection .data\n" %
                (emit program label))))))

