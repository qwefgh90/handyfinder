# handyfinder

[![Build Status](https://travis-ci.org/qwefgh90/handyfinder.svg?branch=develop)](https://travis-ci.org/qwefgh90/handyfinder)

A handy finder is search program for document indexing and searching on multiple platforms (like : http://file-finder.en.informer.com/Offline-Search-Engine/)

# feature
- a application is designed with Angularjs 1.5.5, bootstrap 3.3.6, angular-ui-bootstrap 1.3.2, javafx webview
- frontend is SPA (Single Page Application)
- backend is Restful API with embedded tomcat
- only local ip binding (127.0.0.1) with internal auth token
- a lucene is used for indexing & search
- JSearch is used for body text of document
- only utf-8

# functionality
- raw string indexing from files on file systems
- after full indexing, update indexing when required or on scheduled time
- file's meta data show (modified timestamp, filename, path, size)
- opening dir and logging search result and so on...
- management of index based on directory to be indexed recursively and extension filtering

# plan
- archive search (zip, tar.gz)
- open document file
