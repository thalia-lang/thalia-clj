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
	push eax
	add esp, 4
.ll1:
	push DWORD PTR [i]
	push 10
	pop ebx
	pop eax
	cmp eax, ebx
	setl al
	movzx eax, al
	push eax
	pop eax
	test eax, eax
	je .ll2
	push DWORD PTR [i]
	push 2
	pop ebx
	pop eax
	cdq
	idiv ebx
	mov eax, edx
	push eax
	push 0
	pop ebx
	pop eax
	cmp eax, ebx
	sete al
	movzx eax, al
	push eax
	pop eax
	test eax, eax
	je .ll3
	push DWORD PTR [i]
	push OFFSET FLAT:__fmt_int__
	call printf
	add esp, 8
.ll3:
	push DWORD PTR [i]
	push 1
	pop ebx
	pop eax
	add eax, ebx
	push eax
	pop eax
	mov DWORD PTR [i], eax
	push eax
	add esp, 4
	jmp .ll1
.ll2:
	push 0
	pop eax
	mov DWORD PTR [i], eax
.ll4:
	push DWORD PTR [i]
	push 10
	pop ebx
	pop eax
	cmp eax, ebx
	jge .ll5
	push DWORD PTR [i]
	push OFFSET FLAT:__fmt_int__
	call printf
	add esp, 8
	push 2
	pop eax
	add DWORD PTR [i], eax
	jmp .ll4
.ll5:
	xor eax, eax
	ret
