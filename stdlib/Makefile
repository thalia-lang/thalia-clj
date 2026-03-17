BLANK =
define EOL

$(BLANK)
endef

CC = yasm -felf64

SRC_DIR = src
LIB_DIR = lib

LIBS := sys io str rand

LIB_SOURCE = $(SRC_DIR)/$(lib).asm
LIB_OBJECT = $(LIB_DIR)/$(lib).o
LIB_COMPILE = $(CC) $(LIB_SOURCE) -o $(LIB_OBJECT) $(EOL)

default: build

setup:
	mkdir -p $(LIB_DIR)

build: $(foreach lib, $(LIBS), $(LIB_SOURCE))
	$(foreach lib, $(LIBS), $(LIB_COMPILE))
