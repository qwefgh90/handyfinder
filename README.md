# handyfinder

A handyfinder is tiny search program working on desktop to support multiple type of files

Branches
- develop : [![Build Status](https://travis-ci.org/qwefgh90/handyfinder.svg?branch=develop)](https://travis-ci.org/qwefgh90/handyfinder)</br>
- master : [![Build Status](https://travis-ci.org/qwefgh90/handyfinder.svg?branch=master)](https://travis-ci.org/qwefgh90/handyfinder)

# Downloading

- You can download released versions from:  https://github.com/qwefgh90/handyfinder/releases

# Usage

1. In index page, add directories you want to index
2. and then "click run"
3. In search page, type keyword and search

# Functionality
- graphical interface like web
- index of files on file systems
- a index update when required
- file's meta data show (modified timestamp, filename, path, size)
- opening dir and files
- management of index based on directory which is visited recursively
- filters when searching
- a limit of disk usage

# System Feature
- a application is designed with Angularjs 1.5.5, bootstrap 3.3.6, angular-ui-bootstrap 1.3.2, javafx webview
- frontend is SPA (Single Page Application)
- backend is Restful API with embedded tomcat
- only local ip binding (127.0.0.1) 
- a lucene is used for indexing & search
- JSearch is used for body text of document which tika supports

# Run & Test
- When you want to contribute, use STS
- When you want to test, type "mvn clean test"
- When you want to make native binary, type "mvn jfx:native" (for windows, download Inno from http://www.jrsoftware.org/)

## Signing

* **windows.**
- `signtool.exe sign /f e:\opensource.p12 /p xx /t http://timestamp.comodoca.com/authenticode $f`
* **mac os.**
- `codesign -s "Open Source Developer, Changwon Ch" --deep handyfinder.app`


# Contribution

We would welcome any thoughts. When you find bugs, you can write issue and PR to "develop" branch.
