.intel_syntax noprefix

.extern printf

.global main

.data
__fmt_int__: .asciz "%d "
__fmt_eol__: .asciz "\n"
i: .long 0

.text
main:
	push 0
	pop eax
	mov DWORD PTR [i], eax
.ll1:
	push DWORD PTR [i]
	push 10
	pop ebx
	pop eax
	cmp eax, ebx
	jge .ll2
	push DWORD PTR [i]
	push OFFSET FLAT:__fmt_int__
	call printf
	add esp, 8
	push OFFSET FLAT:__fmt_eol__
	call printf
	add esp, 4
	push 2
	pop eax
	add DWORD PTR [i], eax
	jmp .ll1
.ll2:
	xor eax, eax
	ret
