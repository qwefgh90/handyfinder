# handyfinder

[![Build Status](https://travis-ci.org/qwefgh90/handyfinder.svg?branch=master)](https://travis-ci.org/qwefgh90/handyfinder)

A handyfinder is tiny search program working on desktop to support multiple type of files

# Downloading

- You can download released versions from:  https://github.com/qwefgh90/handyfinder/releases

# Usage

1. In index page, add directories you want to index
2. and then "click run"
3. In search page, type keyword and search

# Functionality
- graphical interface like web
- string indexing from files on file systems
- after full indexing, update indexing when required or on scheduled time
- file's meta data show (modified timestamp, filename, path, size)
- opening dir and search result and so on...
- management of index based on directory to be indexed recursively and extension filtering

# System Feature
- a application is designed with Angularjs 1.5.5, bootstrap 3.3.6, angular-ui-bootstrap 1.3.2, javafx webview
- frontend is SPA (Single Page Application)
- backend is Restful API with embedded tomcat
- only local ip binding (127.0.0.1) 
- a lucene is used for indexing & search
- JSearch is used for body text of document which tika supports

# Future
- internal auth token
- usuful search condition
- sorting and search in result

# Run & Test

- When you want to contribute, use STS
- When you want to test, type "mvn test"
- When you want to make native binary, type "mvn jfx:native" (for windows, download Inno from http://www.jrsoftware.org/)

# Contribution

We would welcome any thoughts. When you find bugs, you can write issue and PR to "develop" branch.
