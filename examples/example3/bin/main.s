.globl main

.data
__fmt_int__: .asciz "%d "
__fmt_eol__: .asciz "\n"
n: .word 0
i: .word 0
j: .word 0

.text
main:
	li t0, 5
	addi sp, sp, -4
	sw t0, 0(sp)
	lw t0, 0(sp)
	addi sp, sp, 4
	la t1, n
	sw t0, 0(t1)
	addi sp, sp, -4
	sw t0, 0(sp)
	addi sp, sp, 4
	addi sp, sp, -4
	sw zero, 0(sp)
	lw t0, 0(sp)
	addi sp, sp, 4
	la t1, i
	sw t0, 0(t1)
.ll1:
	la t1, i
	sw t0, 0(t1)
	addi sp, sp, -4
	sw t0, 0(sp)
	la t0, n
	lw t1, 0(t0)
	addi sp, sp, -4
	sw t1, 0(sp)
	lw t1, 0(sp)
	addi sp, sp, 4
	lw t0, 0(sp)
	addi sp, sp, 4
	bge t0, t1, .ll2
	addi sp, sp, -4
	sw zero, 0(sp)
	lw t0, 0(sp)
	addi sp, sp, 4
	la t1, j
	sw t0, 0(t1)
.ll3:
	la t1, j
	sw t0, 0(t1)
	addi sp, sp, -4
	sw t0, 0(sp)
	la t0, n
	lw t1, 0(t0)
	addi sp, sp, -4
	sw t1, 0(sp)
	la t0, i
	lw t1, 0(t0)
	addi sp, sp, -4
	sw t1, 0(sp)
	lw t1, 0(sp)
	addi sp, sp, 4
	lw t0, 0(sp)
	addi sp, sp, 4
	sub t0, t0, t1
	addi sp, sp, -4
	sw t0, 0(sp)
	lw t1, 0(sp)
	addi sp, sp, 4
	lw t0, 0(sp)
	addi sp, sp, 4
	bge t0, t1, .ll4
	li t0, 0
	addi sp, sp, -4
	sw t0, 0(sp)
	lw a1, 0(sp)
	addi sp, sp, 4
	la a0, __fmt_int__
	call printf
	li t2, 1
	la t1, j
	lw t0, 0(t1)
	add t0, t0, t2
	sw t0, 0(t1)
	j .ll3
.ll4:
	addi sp, sp, -4
	sw zero, 0(sp)
	lw t0, 0(sp)
	addi sp, sp, 4
	la t1, j
	sw t0, 0(t1)
.ll5:
	la t1, j
	sw t0, 0(t1)
	addi sp, sp, -4
	sw t0, 0(sp)
	la t0, i
	lw t1, 0(t0)
	addi sp, sp, -4
	sw t1, 0(sp)
	li t0, 1
	addi sp, sp, -4
	sw t0, 0(sp)
	lw t1, 0(sp)
	addi sp, sp, 4
	lw t0, 0(sp)
	addi sp, sp, 4
	add t0, t0, t1
	addi sp, sp, -4
	sw t0, 0(sp)
	lw t1, 0(sp)
	addi sp, sp, 4
	lw t0, 0(sp)
	addi sp, sp, 4
	bge t0, t1, .ll6
	li t0, 1
	addi sp, sp, -4
	sw t0, 0(sp)
	lw a1, 0(sp)
	addi sp, sp, 4
	la a0, __fmt_int__
	call printf
	li t2, 1
	la t1, j
	lw t0, 0(t1)
	add t0, t0, t2
	sw t0, 0(t1)
	j .ll5
.ll6:
	la a0, __fmt_eol__
	call printf
	li t2, 1
	la t1, i
	lw t0, 0(t1)
	add t0, t0, t2
	sw t0, 0(t1)
	j .ll1
.ll2:
	li a0, 0
	ret
