# Thalia
> A fast, statically typed, general-purpose, procedural programming language

[![Version: v0.0.0](https://img.shields.io/badge/version-v0.0.0-red)](https://vstan02.github.io/thalia)
[![License: LGPL v3](https://img.shields.io/badge/License-LGPL%20v3-blue.svg)](http://www.gnu.org/licenses/lgpl-3.0)

## Contents
- [Building the project](#building-the-project)
  - [Prerequirements](#prerequirements)
  - [Running the program](#running-the-program)
  - [Installation](#installation)
- [Usage](#usage)
- [Options](#options)
- [Examples](#examples)
- [License](#license)
- [Contributing](#contributing)

## Building the project
### Prerequirements
- Java
- Clojure
- Leiningen
- GCC

These packages can usually be installed through your distributions package manager.

### Project setup
To setup the project for development, we simply have to run the `setup.sh` script:

```sh 
./setup.sh
```

### Running the program
If everything went well with the compilation we can run our compiler with `lein run`:

```
lein run './examples/example1/compile.json'
```

### Installation
To install the Thalia compiler just run the `install.sh` script:
```sh 
./install.sh
```

## Usage
To compile a Thalia project you can just run:
```sh
thalia config.json
```
where the `config.json` config file contains something like this:
```json 
{
  "src": "src/",
  "dest": "bin/",
  "target": "app"
}
```

## Options
FIXME: listing of options this app accepts.

## Examples
...

## License
Thalia is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
For more details, see [LICENSE](https://github.com/vstan02/thalia/blob/master/LICENSE) file.

## Contributing
Contributions are welcome.
If you have a feature request, or have found a bug, feel free to open a [new issue](https://github.com/vstan02/thalia/issues/new).
If you wish to contribute code, see [CONTRIBUTING.md](https://github.com/vstan02/thalia/blob/master/CONTRIBUTING.md) for more details.
